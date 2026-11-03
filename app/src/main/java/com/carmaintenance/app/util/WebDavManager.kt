package com.carmaintenance.app.util

import android.util.Log
import androidx.room.withTransaction
import com.carmaintenance.app.data.AppDatabase
import com.carmaintenance.app.data.DbMutex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * WebDAV 同步管理器
 *
 * 同步策略：云端优先
 * 1. 下载云端数据，有则以云端为准导入
 * 2. 将本地数据上传到云端备份
 */
class WebDavManager(
    private val serverUrl: String,
    private val username: String = "",
    private val password: String = ""
) {
    companion object {
        private const val TAG = "WebDavManager"
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val baseUrl = serverUrl.trimEnd('/')
    private val dataFile = "car_maintenance_data.json"

    private fun encodeUrlSegment(s: String): String =
        URLEncoder.encode(s, StandardCharsets.UTF_8.name()).replace("+", "%20")

    private fun buildFileUrl(): String {
        return "$baseUrl/${encodeUrlSegment(dataFile)}"
    }

    /** 对给定 URL 路径逐段创建目录（MKCOL） */
    private fun ensureParentDir(fileUrl: String) {
        // fileUrl 形如 https://dav.jianguoyun.com/dav/myfolder/car_maintenance_data.json
        // 从 host 后第一段开始，逐段尝试 MKCOL
        val idx = fileUrl.indexOf('/', 8) // 跳过 "https://"
        if (idx < 0) return

        val segments = fileUrl.substring(idx).split('/').filter { it.isNotBlank() }
        // 最后一段是文件名，不创建
        val dirs = segments.dropLast(1)

        var currentPath = fileUrl.substring(0, idx) // host 部分
        for (dir in dirs) {
            currentPath += "/$dir"
            try {
                val rb = Request.Builder()
                    .url(currentPath)
                    .method("MKCOL", null)
                addAuth(rb)
                client.newCall(rb.build()).execute().use { response ->
                    Log.i(TAG, "MKCOL $currentPath -> ${response.code}")
                    // 201 Created 成功；405/409 表示已存在，忽略；其他算错误
                    if (response.isSuccessful) return@use
                    val code = response.code
                    if (code == 405 || code == 409) return@use
                    if (code == 401 || code == 403 || code == 404) {
                        throw IOException("MKCOL 失败 HTTP $code")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "MKCOL $currentPath 出错: ${e.message}")
                // 继续，有些 WebDAV 不支持 MKCOL 已存在的目录
            }
        }
    }

    /** 执行同步，返回 Pair(是否成功, 错误消息) */
    suspend fun sync(db: AppDatabase): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            // 1. 下载（网络操作不占用数据库锁，避免阻塞用户保存）
            val remoteJson = downloadData()

            // 2. 只在数据库读写期间持锁，且导入是事务的，界面不会闪空
            val localJson = DbMutex.mutex.withLock {
                if (remoteJson != null) {
                    if (isRemoteNewer(db, remoteJson)) {
                        importFromJson(db, remoteJson)
                        Log.i(TAG, "已从云端导入 ${remoteJson.length} 字节")
                    } else {
                        Log.i(TAG, "云端数据不比本地新，跳过导入")
                    }
                } else {
                    Log.i(TAG, "云端暂无数据，将首次上传")
                }
                exportToJson(db)
            }

            // 3. 上传
            uploadData(localJson)
            Pair(true, "同步成功")
        } catch (e: Exception) {
            val msg = e.message ?: e.javaClass.simpleName
            Log.e(TAG, "同步失败: $msg", e)
            Pair(false, msg)
        }
    }

    /** 判断云端数据是否比本地新（云端记录为空时返回 false，避免清空本地） */
    private suspend fun isRemoteNewer(db: AppDatabase, remoteJson: String): Boolean {
        return try {
            val json = JSONObject(remoteJson)
            val records = json.optJSONArray("maintenanceRecords") ?: return false
            if (records.length() == 0) return false

            var remoteMax = 0L
            for (i in 0 until records.length()) {
                val t = records.getJSONObject(i).optLong("updatedAt", 0L)
                if (t > remoteMax) remoteMax = t
            }

            val localMax = db.maintenanceRecordDao().getMaxUpdatedAt() ?: 0L
            remoteMax > localMax
        } catch (e: Exception) {
            Log.w(TAG, "比较云端/本地时间失败: ${e.message}")
            false
        }
    }

    /** 下载云端数据 */
    private fun downloadData(): String? {
        val url = buildFileUrl()
        ensureParentDir(url)
        Log.i(TAG, "GET $url")

        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .header("Depth", "0")
            .header("Accept", "*/*")
        addAuth(requestBuilder)

        client.newCall(requestBuilder.build()).execute().use { response ->
            val bodyText = response.body?.string().orEmpty()
            Log.i(TAG, "GET 响应: ${response.code}")

            when {
                response.isSuccessful -> return bodyText
                response.code == 404 -> return null
                else -> throw IOException("下载失败 HTTP ${response.code}: ${bodyText.take(200)}")
            }
        }
    }

    /** 上传数据到云端（PUT 409 时先 DELETE 再 PUT） */
    private fun uploadData(json: String) {
        val url = buildFileUrl()
        ensureParentDir(url)
        Log.i(TAG, "PUT $url (${json.length} bytes)")

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .url(url)
            .put(body)
            .header("Content-Type", "application/json; charset=utf-8")
            .header("Overwrite", "T")
            .header("Depth", "0")
        addAuth(requestBuilder)

        client.newCall(requestBuilder.build()).execute().use { response ->
            Log.i(TAG, "PUT 响应: ${response.code}")

            when {
                response.isSuccessful -> return
                response.code == 409 -> {
                    // 冲突：先删再传
                    Log.w(TAG, "PUT 409 冲突，尝试 DELETE 后重新 PUT")
                    deleteFile(url)
                    // 重新 PUT
                    val retryBuilder = Request.Builder()
                        .url(url)
                        .put(json.toRequestBody(mediaType))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .header("Overwrite", "T")
                        .header("Depth", "0")
                    addAuth(retryBuilder)
                    client.newCall(retryBuilder.build()).execute().use { retry ->
                        retry.close()
                        if (!retry.isSuccessful) {
                            throw IOException("DELETE 后 PUT 仍失败 HTTP ${retry.code}")
                        }
                    }
                }
                response.code == 401 || response.code == 403 ->
                    throw IOException("认证失败 HTTP ${response.code}，请检查用户名密码")
                response.code == 404 ->
                    throw IOException("路径不存在 HTTP 404，请确认 WebDAV 地址正确且目录已创建")
                else ->
                    throw IOException("上传失败 HTTP ${response.code}")
            }
        }
    }

    private fun deleteFile(url: String) {
        val rb = Request.Builder().url(url).delete()
        addAuth(rb)
        client.newCall(rb.build()).execute().use { it.close() }
    }

    /** 设置认证 */
    private fun addAuth(requestBuilder: Request.Builder) {
        if (username.isNotBlank() || password.isNotBlank()) {
            val credentials = Credentials.basic(username, password)
            requestBuilder.header("Authorization", credentials)
        }
    }

    /** 导出数据库为 JSON */
    private suspend fun exportToJson(db: AppDatabase): String = withContext(Dispatchers.IO) {
        val json = JSONObject()

        // 车辆
        val vehicles = db.vehicleDao().getAllVehiclesSuspend()
        val vehiclesArray = JSONArray()
        for (v in vehicles) {
            vehiclesArray.put(JSONObject().apply {
                put("id", v.id)
                put("brand", v.brand)
                put("model", v.model)
                put("plateNumber", v.plateNumber)
                put("purchaseDate", v.purchaseDate)
                put("createdAt", v.createdAt)
                put("updatedAt", v.updatedAt)
                put("sortOrder", v.sortOrder)
            })
        }
        json.put("vehicles", vehiclesArray)

        // 保养记录
        val recordsArray = JSONArray()
        for (v in vehicles) {
            val records = db.maintenanceRecordDao().getRecordsByVehicleSuspend(v.id)
            for (r in records) {
                recordsArray.put(JSONObject().apply {
                    put("id", r.id)
                    put("vehicleId", r.vehicleId)
                    put("date", r.date)
                    put("mileage", r.mileage)
                    put("partsCost", r.partsCost)
                    put("laborCost", r.laborCost)
                    put("totalCost", r.totalCost)
                    put("location", r.location)
                    put("notes", r.notes)
                    put("createdAt", r.createdAt)
                    put("updatedAt", r.updatedAt)
                })
            }
        }
        json.put("maintenanceRecords", recordsArray)

        // 零配件
        val partsArray = JSONArray()
        for (i in 0 until recordsArray.length()) {
            val recordObj = recordsArray.getJSONObject(i)
            val recordId = recordObj.getLong("id")
            val parts = db.partItemDao().getPartsByRecordSuspend(recordId)
            for (p in parts) {
                partsArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("recordId", p.recordId)
                    put("name", p.name)
                    put("quantity", p.quantity)
                    put("cost", p.cost)
                    put("isUnitPriceMode", p.isUnitPriceMode)
                })
            }
        }
        json.put("parts", partsArray)

        json.toString()
    }

    /** 从 JSON 导入数据（云端优先）
     *  整个「清空 + 写入」放在一个事务里，Room 只在事务提交后通知一次，
     *  界面不会先闪空列表再恢复 */
    private suspend fun importFromJson(db: AppDatabase, jsonString: String) = withContext(Dispatchers.IO) {
        val json = JSONObject(jsonString)

        db.withTransaction {
            // 清空现有数据（先子表后父表，避免级联外键异常）
            db.partItemDao().deleteAll()
            db.maintenanceRecordDao().deleteAll()
            db.vehicleDao().deleteAll()

        // 导入车辆
        val vehiclesArray = json.getJSONArray("vehicles")
        val vehicleIds = mutableSetOf<Long>()
        for (i in 0 until vehiclesArray.length()) {
            val obj = vehiclesArray.getJSONObject(i)
            val vid = obj.getLong("id")
            db.vehicleDao().insert(
                com.carmaintenance.app.data.entity.Vehicle(
                    id = vid,
                    brand = obj.getString("brand"),
                    model = obj.getString("model"),
                    plateNumber = obj.optString("plateNumber", ""),
                    purchaseDate = obj.getLong("purchaseDate"),
                    createdAt = obj.getLong("createdAt"),
                    updatedAt = obj.getLong("updatedAt"),
                    sortOrder = obj.optInt("sortOrder", 0)
                )
            )
            vehicleIds.add(vid)
        }

        // 导入保养记录
        val recordsArray = json.getJSONArray("maintenanceRecords")
        val recordIds = mutableSetOf<Long>()
        for (i in 0 until recordsArray.length()) {
            val obj = recordsArray.getJSONObject(i)
            val rid = obj.getLong("id")
            val vid = obj.getLong("vehicleId")
            // 跳过找不到所属车辆的孤儿记录
            if (!vehicleIds.contains(vid)) {
                Log.w(TAG, "跳过孤儿保养记录 id=$rid vehicleId=$vid")
                continue
            }
            db.maintenanceRecordDao().insert(
                com.carmaintenance.app.data.entity.MaintenanceRecord(
                    id = rid,
                    vehicleId = vid,
                    date = obj.getLong("date"),
                    mileage = obj.getInt("mileage"),
                    partsCost = obj.optDouble("partsCost", 0.0),
                    laborCost = obj.optDouble("laborCost", 0.0),
                    totalCost = obj.optDouble("totalCost", 0.0),
                    location = obj.optString("location", ""),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.getLong("createdAt"),
                    updatedAt = obj.getLong("updatedAt")
                )
            )
            recordIds.add(rid)
        }

        // 导入零配件
        val partsArray = json.getJSONArray("parts")
        for (i in 0 until partsArray.length()) {
            val obj = partsArray.getJSONObject(i)
            val rid = obj.getLong("recordId")
            // 跳过找不到所属记录的孤儿零配件，避免外键约束崩溃
            if (!recordIds.contains(rid)) {
                Log.w(TAG, "跳过孤儿零配件 ${obj.optString("name")} recordId=$rid")
                continue
            }
            db.partItemDao().insert(
                com.carmaintenance.app.data.entity.PartItem(
                    id = obj.getLong("id"),
                    recordId = rid,
                    name = obj.getString("name"),
                    quantity = obj.getInt("quantity"),
                    cost = obj.optDouble("cost", 0.0),
                    isUnitPriceMode = obj.optBoolean("isUnitPriceMode", true)
                )
            )
        }
        }
    }
}
