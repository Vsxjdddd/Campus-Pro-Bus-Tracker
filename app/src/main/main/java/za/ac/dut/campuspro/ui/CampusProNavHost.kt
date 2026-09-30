package za.ac.dut.campuspro.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import za.ac.dut.campuspro.CampusProApp
import za.ac.dut.campuspro.data.Role
import za.ac.dut.campuspro.ui.screens.AuthScreen
import za.ac.dut.campuspro.ui.screens.DriverHomeScreen
import za.ac.dut.campuspro.ui.screens.StudentHomeScreen
import za.ac.dut.campuspro.ui.screens.TrackingScreen
import za.ac.dut.campuspro.ui.screens.WelcomeScreen

object Routes {
    const val WELCOME = "welcome"
    const val AUTH = "auth/{mode}/{role}"
    const val STUDENT = "student"
    const val DRIVER = "driver"
    const val TRACK = "track/{tripId}"

    fun auth(mode: String, role: Role) = "auth/$mode/${role.name}"
    fun track(tripId: Long) = "track/$tripId"
    fun home(role: Role) = if (role == Role.STUDENT) STUDENT else DRIVER
}

/**
 * Screen flow:
 * Welcome -> Login/Register -> Student home -> Live tracking
 *                           -> Driver home
 */
@Composable
fun CampusProNavHost(navController: NavHostController = rememberNavController()) {
    val app = LocalContext.current.applicationContext as CampusProApp
    // Stay logged in between app launches
    val start = remember {
        app.repository.currentUser.value?.let { Routes.home(it.role) } ?: Routes.WELCOME
    }

    fun goToAndClear(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = start) {

        composable(Routes.WELCOME) {
            WelcomeScreen(onOpenAuth = { mode, role -> navController.navigate(Routes.auth(mode, role)) })
        }

        composable(
            Routes.AUTH,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("role") { type = NavType.StringType }
            )
        ) { entry ->
            AuthScreen(
                initialMode = entry.arguments?.getString("mode") ?: "login",
                initialRole = Role.valueOf(entry.arguments?.getString("role") ?: Role.STUDENT.name),
                onBack = { navController.popBackStack() },
                onLoggedIn = { role -> goToAndClear(Routes.home(role)) }
            )
        }

        composable(Routes.STUDENT) {
            StudentHomeScreen(
                onTrack = { tripId -> navController.navigate(Routes.track(tripId)) },
                onLoggedOut = { goToAndClear(Routes.WELCOME) }
            )
        }

        composable(
            Routes.TRACK,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) {
            TrackingScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.DRIVER) {
            DriverHomeScreen(onLoggedOut = { goToAndClear(Routes.WELCOME) })
        }
    }
}
