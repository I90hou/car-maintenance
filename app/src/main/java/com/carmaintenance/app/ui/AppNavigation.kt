package com.carmaintenance.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.carmaintenance.app.ui.components.CockpitBottomBar
import com.carmaintenance.app.ui.components.CockpitTab
import com.carmaintenance.app.ui.screen.*
import com.carmaintenance.app.ui.viewmodel.HistoryViewModel
import com.carmaintenance.app.ui.viewmodel.MaintenanceViewModel
import com.carmaintenance.app.ui.viewmodel.ServiceCycleViewModel
import com.carmaintenance.app.ui.viewmodel.VehicleViewModel

/** 转场动画时长 */
private const val TRANSITION_MS = 300

/**
 * 转场保护：动画期间当前页面的生命周期处于 STARTED，
 * 此时点击残影中的可点组件不应触发跳转，仅 RESUMED 状态允许导航
 */
private fun NavHostController.canNavigate() =
    currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED

private fun NavHostController.navigateGuarded(route: String) {
    if (canNavigate()) navigate(route)
}

private fun NavHostController.navigateGuarded(route: String, builder: NavOptionsBuilder.() -> Unit) {
    if (canNavigate()) navigate(route, builder)
}

private fun NavHostController.popBackStackGuarded() {
    if (canNavigate()) popBackStack()
}

/**
 * 应用导航
 *
 * 一级页：概览 / 记录 / 设置（带底部三档导航）
 * 二级页：记录详情 / 记录表单 / 车辆编辑 / 车辆管理（带返回箭头，不带导航栏）
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // ViewModels
    val vehicleViewModel: VehicleViewModel = viewModel()
    val maintenanceViewModel: MaintenanceViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()
    val cycleViewModel: ServiceCycleViewModel = viewModel()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val bottomBar: @Composable () -> Unit = {
        CockpitBottomBar(
            currentRoute = currentRoute,
            onSelect = { tab ->
                navController.navigateGuarded(tab.route) {
                    popUpTo(CockpitTab.Overview.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }

    NavHost(
        navController = navController,
        startDestination = CockpitTab.Overview.route,
        enterTransition = { fadeIn(animationSpec = tween(TRANSITION_MS)) },
        exitTransition = { fadeOut(animationSpec = tween(TRANSITION_MS)) },
        popEnterTransition = { fadeIn(animationSpec = tween(TRANSITION_MS)) },
        popExitTransition = { fadeOut(animationSpec = tween(TRANSITION_MS)) }
    ) {
        // ========== 一级页 ==========

        // 概览
        composable(CockpitTab.Overview.route) {
            OverviewScreen(
                vehicleViewModel = vehicleViewModel,
                cycleViewModel = cycleViewModel,
                onAddVehicle = { navController.navigateGuarded("edit_vehicle") },
                onAddRecord = { vehicleId ->
                    navController.navigateGuarded("add_record/$vehicleId")
                },
                onOpenRecords = { navController.navigateGuarded(CockpitTab.Records.route) },
                bottomBar = bottomBar
            )
        }

        // 记录
        composable(CockpitTab.Records.route) {
            RecordsScreen(
                vehicleViewModel = vehicleViewModel,
                maintenanceViewModel = maintenanceViewModel,
                onAddRecord = { vehicleId ->
                    navController.navigateGuarded("add_record/$vehicleId")
                },
                onOpenRecord = { recordId ->
                    navController.navigateGuarded("record_detail/$recordId")
                },
                onDeleteRecord = { record ->
                    maintenanceViewModel.deleteRecord(record)
                },
                bottomBar = bottomBar
            )
        }

        // 设置
        composable(CockpitTab.Settings.route) {
            SettingsScreen(
                cycleViewModel = cycleViewModel,
                onVehicleManager = { navController.navigateGuarded("vehicle_manager") },
                bottomBar = bottomBar
            )
        }

        // ========== 二级页 ==========

        // 记录详情
        composable("record_detail/{recordId}") { backStackEntry ->
            val recordId = backStackEntry.arguments?.getString("recordId")?.toLongOrNull()
            if (recordId != null) {
                RecordDetailScreen(
                    recordId = recordId,
                    maintenanceViewModel = maintenanceViewModel,
                    cycleViewModel = cycleViewModel,
                    onBack = { navController.popBackStackGuarded() },
                    onEdit = { id -> navController.navigateGuarded("edit_record/$id") },
                    onDelete = { record ->
                        maintenanceViewModel.deleteRecord(record)
                        navController.popBackStackGuarded()
                    }
                )
            }
        }

        // 添加记录
        composable("add_record/{vehicleId}") { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.toLongOrNull()
            if (vehicleId != null) {
                RecordEditScreen(
                    vehicleId = vehicleId,
                    vehicleViewModel = vehicleViewModel,
                    maintenanceViewModel = maintenanceViewModel,
                    historyViewModel = historyViewModel,
                    onBack = { navController.popBackStackGuarded() }
                )
            }
        }

        // 编辑记录
        composable("edit_record/{recordId}") { backStackEntry ->
            val recordId = backStackEntry.arguments?.getString("recordId")?.toLongOrNull()
            if (recordId != null) {
                RecordEditScreen(
                    recordId = recordId,
                    vehicleViewModel = vehicleViewModel,
                    maintenanceViewModel = maintenanceViewModel,
                    historyViewModel = historyViewModel,
                    onBack = { navController.popBackStackGuarded() }
                )
            }
        }

        // 添加车辆
        composable("edit_vehicle") {
            VehicleEditScreen(
                vehicleViewModel = vehicleViewModel,
                onBack = { navController.popBackStackGuarded() }
            )
        }

        // 编辑车辆
        composable("edit_vehicle/{vehicleId}") { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.toLongOrNull()
            if (vehicleId != null) {
                VehicleEditScreen(
                    vehicleViewModel = vehicleViewModel,
                    vehicleId = vehicleId,
                    onBack = { navController.popBackStackGuarded() }
                )
            }
        }

        // 车辆管理
        composable("vehicle_manager") {
            VehicleManagerScreen(
                vehicleViewModel = vehicleViewModel,
                cycleViewModel = cycleViewModel,
                onBack = { navController.popBackStackGuarded() },
                onAddVehicle = { navController.navigateGuarded("edit_vehicle") },
                onEditVehicle = { vehicleId ->
                    navController.navigateGuarded("edit_vehicle/$vehicleId")
                }
            )
        }
    }
}
