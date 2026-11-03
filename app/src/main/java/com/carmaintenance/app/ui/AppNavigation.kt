package com.carmaintenance.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.carmaintenance.app.ui.screen.*
import com.carmaintenance.app.ui.viewmodel.HistoryViewModel
import com.carmaintenance.app.ui.viewmodel.MaintenanceViewModel
import com.carmaintenance.app.ui.viewmodel.VehicleViewModel

/** 转场动画时长（默认 700ms 偏慢，加速一倍多） */
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

private fun NavHostController.popBackStackGuarded() {
    if (canNavigate()) popBackStack()
}

/**
 * 应用导航
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    // ViewModels
    val vehicleViewModel: VehicleViewModel = viewModel()
    val maintenanceViewModel: MaintenanceViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()
    
    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { fadeIn(animationSpec = tween(TRANSITION_MS)) },
        exitTransition = { fadeOut(animationSpec = tween(TRANSITION_MS)) },
        popEnterTransition = { fadeIn(animationSpec = tween(TRANSITION_MS)) },
        popExitTransition = { fadeOut(animationSpec = tween(TRANSITION_MS)) }
    ) {
        // 首页 - 车辆列表
        composable("home") {
            HomeScreen(
                vehicleViewModel = vehicleViewModel,
                maintenanceViewModel = maintenanceViewModel,
                onAddVehicle = { navController.navigateGuarded("edit_vehicle") },
                onEditVehicle = { vehicleId ->
                    navController.navigateGuarded("edit_vehicle/$vehicleId")
                },
                onAddRecord = { vehicleId ->
                    navController.navigateGuarded("add_record/$vehicleId")
                },
                onEditRecord = { recordId ->
                    navController.navigateGuarded("edit_record/$recordId")
                },
                onSettings = {
                    navController.navigateGuarded("settings")
                }
            )
        }
        
        // 添加/编辑车辆
        composable("edit_vehicle") {
            VehicleEditScreen(
                vehicleViewModel = vehicleViewModel,
                onBack = { navController.popBackStackGuarded() }
            )
        }
        
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
        
        // 添加保养记录
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
        
        // 编辑保养记录
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
        
        // 设置
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStackGuarded() },
                onVehicleManager = { navController.navigateGuarded("vehicle_manager") }
            )
        }

        // 车辆管理
        composable("vehicle_manager") {
            VehicleManagerScreen(
                vehicleViewModel = vehicleViewModel,
                onBack = { navController.popBackStackGuarded() },
                onAddVehicle = { navController.navigateGuarded("edit_vehicle") },
                onEditVehicle = { vehicleId ->
                    navController.navigateGuarded("edit_vehicle/$vehicleId")
                }
            )
        }
    }
}
