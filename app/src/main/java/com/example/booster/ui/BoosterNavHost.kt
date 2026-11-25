package com.example.booster.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.booster.ui.components.AccessibilityPermissionDisclosureDialog
import com.example.booster.ui.components.ConsentConfirmationDialog
import com.example.booster.ui.components.StatusBadgeChip
import com.example.booster.ui.screens.AppsScreen
import com.example.booster.ui.screens.DashboardScreen
import com.example.booster.ui.screens.ProfilesScreen
import com.example.booster.ui.screens.ToolsScreen
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CobaltContainer
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OptimalGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPure
import com.example.ui.theme.TextSecondary
import kotlinx.serialization.Serializable

@Serializable
data object BoostRoute

@Serializable
data object ProfilesRoute

@Serializable
data object AppsRoute

@Serializable
data object ToolsRoute

private data class NavDestinationItem(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val boosterDestinations = listOf(
    NavDestinationItem(
        route = BoostRoute,
        labelRes = R.string.nav_dashboard,
        selectedIcon = Icons.Rounded.Bolt,
        unselectedIcon = Icons.Outlined.Bolt,
        testTag = "nav_boost",
    ),
    NavDestinationItem(
        route = ProfilesRoute,
        labelRes = R.string.nav_profiles,
        selectedIcon = Icons.Rounded.SportsEsports,
        unselectedIcon = Icons.Outlined.SportsEsports,
        testTag = "nav_profiles",
    ),
    NavDestinationItem(
        route = AppsRoute,
        labelRes = R.string.nav_apps,
        selectedIcon = Icons.Rounded.Apps,
        unselectedIcon = Icons.Outlined.Apps,
        testTag = "nav_apps",
    ),
    NavDestinationItem(
        route = ToolsRoute,
        labelRes = R.string.nav_tools,
        selectedIcon = Icons.Rounded.Terminal,
        unselectedIcon = Icons.Outlined.Terminal,
        testTag = "nav_tools",
    ),
)

@Composable
fun BoosterAppShell(
    state: BoosterUiState,
    viewModel: BoosterViewModel,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    val navigateTo: (Any) -> Unit = { target ->
        navController.navigate(target) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBlack,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = CarbonSurface,
                        contentColor = TextPure,
                        actionColor = NeonCyan,
                    )
                }
            },
            topBar = {
                BoosterTopBar(
                    isBoosted = state.snapshot.isActive,
                    isShizukuBound = state.shizukuStatus.isServiceBound,
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = CarbonSurface,
                        windowInsets = WindowInsets.navigationBars,
                        tonalElevation = 0.dp,
                        modifier = Modifier.border(1.dp, GlassBorder),
                    ) {
                        boosterDestinations.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonCyan,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = CobaltContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                ),
                                modifier = Modifier.testTag(item.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = CarbonSurface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .border(1.dp, GlassBorder),
                    ) {
                        Spacer(modifier = Modifier.weight(0.2f))
                        boosterDestinations.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = NeonCyan,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = CobaltContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                ),
                                modifier = Modifier.testTag(item.testTag),
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = BoostRoute,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 820.dp),
                    ) {
                        composable<BoostRoute> {
                            DashboardScreen(
                                state = state,
                                onSelectProfile = viewModel::selectProfile,
                                onBoostGame = viewModel::executePrimaryBoost,
                                onRevertSettings = viewModel::revertAllSettings,
                                onLaunchSelectedGame = viewModel::launchGame,
                                onToggleFpsOverlay = viewModel::toggleFpsOverlay,
                                onSelectBoostMode = viewModel::selectOverlayBoostMode,
                                onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
                            )
                        }
                        composable<ProfilesRoute> {
                            ProfilesScreen(
                                state = state,
                                onSelectProfile = viewModel::selectProfile,
                                onUpdateProfile = viewModel::updateProfile,
                                onBoostProfile = { id ->
                                    viewModel.selectProfile(id)
                                    viewModel.executePrimaryBoost()
                                },
                                onLaunchGame = viewModel::launchGame,
                            )
                        }
                        composable<AppsRoute> {
                            AppsScreen(
                                state = state,
                                onSearchQueryChange = viewModel::updateAppSearchQuery,
                                onToggleApp = viewModel::toggleBackgroundAppSelection,
                                onSelectAllTrimmable = viewModel::selectAllTrimmableApps,
                                onRefreshApps = viewModel::refreshBackgroundApps,
                                onTrimSelectedStandard = viewModel::trimSelectedBackgroundAppsOnly,
                                onTrimSelectedShizuku = viewModel::promptShizukuTrimApps,
                            )
                        }
                        composable<ToolsRoute> {
                            ToolsScreen(
                                state = state,
                                onRefreshIntegrations = viewModel::refreshSystemIntegrations,
                                onRequestShizukuAuth = viewModel::requestShizukuAuthorization,
                                onOpenShizukuApp = {
                                    viewModel.openPackageOrStore("moe.shizuku.privileged.api")
                                },
                                onPromptShizukuAnimScale = viewModel::promptShizukuAnimationScale,
                                onPromptShizukuRevertAnimScale = viewModel::promptShizukuRevertAnimationScale,
                                onPromptShizukuGameMode = viewModel::promptShizukuGameMode,
                                onPromptTermuxTemplate = viewModel::promptTermuxTemplate,
                                onCustomTermuxInputChange = viewModel::updateCustomTermuxInput,
                                onCustomTermuxBgChange = viewModel::updateCustomTermuxBackground,
                                onPromptCustomTermux = viewModel::promptCustomTermuxCommand,
                                onOpenTermuxApp = { viewModel.openPackageOrStore("com.termux") },
                                onOpenDndSettings = viewModel::openDndPolicySettings,
                                onOpenWriteSettings = viewModel::openWriteSettingsScreen,
                                onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
                                onOpenAppPermissions = viewModel::openAppDetailsSettings,
                            )
                        }
                    }
                }
            }
        }

        state.pendingConsentAction?.let { pending ->
            ConsentConfirmationDialog(
                action = pending,
                onConfirm = viewModel::confirmAndExecutePendingAction,
                onDismiss = viewModel::dismissPendingConsent,
            )
        }

        if (state.showAccessibilityPermissionDialog) {
            AccessibilityPermissionDisclosureDialog(
                onOpenSettings = viewModel::confirmOpenAccessibilitySettings,
                onDismiss = viewModel::dismissAccessibilityPermissionDialog,
            )
        }
    }
}

@Composable
private fun BoosterTopBar(
    isBoosted: Boolean,
    isShizukuBound: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CarbonSurface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_booster_launcher),
                contentDescription = stringResource(R.string.cd_brand_emblem),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, ElectricBlue.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.brand_header),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPure,
                )
                Text(
                    text = stringResource(R.string.brand_subheader),
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricBlue,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isShizukuBound) {
                StatusBadgeChip(
                    text = "SHIZUKU",
                    accentColor = NeonCyan,
                )
            }
            StatusBadgeChip(
                text = if (isBoosted) "BOOST ON" else "STANDBY",
                accentColor = if (isBoosted) OptimalGreen else TextSecondary,
            )
        }
    }
}
