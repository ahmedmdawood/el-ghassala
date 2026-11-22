package dev.ahmeddawood.elghassala.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import dev.ahmeddawood.elghassala.UiEvent
import dev.ahmeddawood.elghassala.ui.components.CelebrationOverlay
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.ahmeddawood.elghassala.AppViewModel
import dev.ahmeddawood.elghassala.R
import dev.ahmeddawood.elghassala.ui.screens.DashboardScreen
import dev.ahmeddawood.elghassala.ui.screens.DayDetailScreen
import dev.ahmeddawood.elghassala.ui.screens.DaysScreen
import dev.ahmeddawood.elghassala.ui.screens.ExpenseFormScreen
import dev.ahmeddawood.elghassala.ui.screens.ExpensesScreen
import dev.ahmeddawood.elghassala.ui.screens.PeopleScreen
import dev.ahmeddawood.elghassala.ui.screens.PersonDetailScreen
import dev.ahmeddawood.elghassala.ui.screens.RoutesScreen
import dev.ahmeddawood.elghassala.ui.screens.SearchScreen
import dev.ahmeddawood.elghassala.ui.screens.SettingsScreen
import dev.ahmeddawood.elghassala.ui.screens.SettlementScreen
import dev.ahmeddawood.elghassala.ui.screens.StatsScreen
import dev.ahmeddawood.elghassala.ui.screens.TransferFormScreen
import dev.ahmeddawood.elghassala.ui.screens.TransfersScreen
import dev.ahmeddawood.elghassala.ui.screens.TransportFormScreen
import dev.ahmeddawood.elghassala.ui.screens.TransportScreen

object Routes {
    const val HOME = "home"
    const val DAYS = "days"
    const val SETTLE = "settle"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val DAY_DETAIL = "day/{dayId}"
    const val TRANSPORT = "transport"
    const val TRANSPORT_FORM = "transportForm?dayId={dayId}&editId={editId}"
    const val EXPENSES = "expenses"
    const val EXPENSE_FORM = "expenseForm?dayId={dayId}&editId={editId}"
    const val TRANSFERS = "transfers"
    const val TRANSFER_FORM = "transferForm?dayId={dayId}&editId={editId}&settlement={settlement}&from={from}&to={to}&amount={amount}"
    const val ROUTES_LIST = "routes"
    const val PEOPLE = "people"
    const val PERSON_DETAIL = "person/{personId}"
    const val SEARCH = "search"

    fun dayDetail(id: Long) = "day/$id"
    fun transportForm(dayId: Long? = null, editId: Long? = null) =
        "transportForm?dayId=${dayId ?: -1}&editId=${editId ?: -1}"
    fun expenseForm(dayId: Long? = null, editId: Long? = null) =
        "expenseForm?dayId=${dayId ?: -1}&editId=${editId ?: -1}"
    fun transferForm(dayId: Long? = null, editId: Long? = null, settlement: Boolean = false, from: Long = -1, to: Long = -1, amount: Long = -1) =
        "transferForm?dayId=${dayId ?: -1}&editId=${editId ?: -1}&settlement=$settlement&from=$from&to=$to&amount=$amount"
    fun personDetail(id: Long) = "person/$id"
}

private data class Tab(val route: String, val labelRes: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    Tab(Routes.DAYS, R.string.nav_days, Icons.Filled.CalendarMonth),
    Tab(Routes.SETTLE, R.string.nav_settle, Icons.Filled.Handshake),
    Tab(Routes.STATS, R.string.nav_stats, Icons.Filled.Insights),
    Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
)

private val tabRoutes = tabs.map { it.route }.toSet()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(vm: AppViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in tabRoutes

    val snackbarState = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    val celebration by vm.celebration.collectAsState()
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            val text = when (event) {
                is UiEvent.Message -> ctx.getString(event.resId)
                is UiEvent.Text -> event.text
                else -> null
            }
            if (text != null) {
                snackbarState.currentSnackbarData?.dismiss()
                snackbarState.showSnackbar(text, duration = androidx.compose.material3.SnackbarDuration.Short)
            }
        }
    }

    Box {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 12 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(250)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it / 12 } },
        ) {
            composable(Routes.HOME) { DashboardScreen(vm, nav) }
            composable(Routes.DAYS) { DaysScreen(vm, nav) }
            composable(Routes.SETTLE) { SettlementScreen(vm, nav) }
            composable(Routes.STATS) { StatsScreen(vm, nav) }
            composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
            composable(
                Routes.DAY_DETAIL,
                arguments = listOf(navArgument("dayId") { type = NavType.LongType }),
            ) { DayDetailScreen(vm, nav, it.arguments?.getLong("dayId") ?: -1L) }
            composable(Routes.TRANSPORT) { TransportScreen(vm, nav) }
            composable(
                Routes.TRANSPORT_FORM,
                arguments = listOf(
                    navArgument("dayId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("editId") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) {
                TransportFormScreen(
                    vm, nav,
                    dayId = it.arguments?.getLong("dayId")?.takeIf { d -> d != -1L },
                    editId = it.arguments?.getLong("editId")?.takeIf { d -> d != -1L },
                )
            }
            composable(Routes.EXPENSES) { ExpensesScreen(vm, nav) }
            composable(
                Routes.EXPENSE_FORM,
                arguments = listOf(
                    navArgument("dayId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("editId") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) {
                ExpenseFormScreen(
                    vm, nav,
                    dayId = it.arguments?.getLong("dayId")?.takeIf { d -> d != -1L },
                    editId = it.arguments?.getLong("editId")?.takeIf { d -> d != -1L },
                )
            }
            composable(Routes.TRANSFERS) { TransfersScreen(vm, nav) }
            composable(
                Routes.TRANSFER_FORM,
                arguments = listOf(
                    navArgument("dayId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("editId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("settlement") { type = NavType.BoolType; defaultValue = false },
                    navArgument("from") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("to") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("amount") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) {
                TransferFormScreen(
                    vm, nav,
                    dayId = it.arguments?.getLong("dayId")?.takeIf { d -> d != -1L },
                    editId = it.arguments?.getLong("editId")?.takeIf { d -> d != -1L },
                    presetSettlement = it.arguments?.getBoolean("settlement") ?: false,
                    presetFrom = it.arguments?.getLong("from") ?: -1L,
                    presetTo = it.arguments?.getLong("to") ?: -1L,
                    presetAmount = it.arguments?.getLong("amount") ?: -1L,
                )
            }
            composable(Routes.ROUTES_LIST) { RoutesScreen(vm, nav) }
            composable(Routes.PEOPLE) { PeopleScreen(vm, nav) }
            composable(
                Routes.PERSON_DETAIL,
                arguments = listOf(navArgument("personId") { type = NavType.LongType }),
            ) { PersonDetailScreen(vm, nav, it.arguments?.getLong("personId") ?: -1L) }
            composable(Routes.SEARCH) { SearchScreen(vm, nav) }
        }
    }
    celebration?.let { CelebrationOverlay(it) { vm.dismissCelebration() } }
    }
}
