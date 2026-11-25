package com.carmaintenance.app.util

/**
 * 保养项目名称归一化
 *
 * 周期规则挂在项目名称上，而用户录入时同一个部件可能写成「机滤」「机油滤芯」
 * 「机油滤清器」。不归一化的话，规则就匹配不上记录，周期永远算不出来。
 */
object PartNames {

    /** 别名 → 规范名 */
    private val ALIASES = mapOf(
        "机滤" to "机油滤清器",
        "机油滤芯" to "机油滤清器",
        "滤清器" to "机油滤清器",
        "空滤" to "空气滤芯",
        "空滤芯" to "空气滤芯",
        "空调滤网" to "空调滤芯",
        "空调滤清器" to "空调滤芯",
        "电瓶" to "蓄电池",
        "刹车皮" to "刹车片",
        "防冻水" to "防冻液"
    )

    /** 去掉首尾空白并映射到规范名 */
    fun canonical(name: String): String {
        val trimmed = name.trim()
        return ALIASES[trimmed] ?: trimmed
    }

    /** 两个名称是否指向同一个项目 */
    fun same(a: String, b: String): Boolean = canonical(a) == canonical(b)
}
