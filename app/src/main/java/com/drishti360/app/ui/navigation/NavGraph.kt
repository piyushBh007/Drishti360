package com.drishti360.app.ui.navigation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.drishti360.app.data.mock.MockData
import com.drishti360.app.data.models.NGO
import com.drishti360.app.ui.components.PrimaryActionButton
import com.drishti360.app.ui.screens.AuditTrailScreen
import com.drishti360.app.ui.screens.CctvScreen
import com.drishti360.app.ui.screens.CommandCenterScreen
import com.drishti360.app.ui.screens.InspectionReportScreen
import com.drishti360.app.ui.screens.LiveInspectionScreen
import com.drishti360.app.ui.screens.NgoDetailScreen
import com.drishti360.app.ui.screens.NgoListScreen
import com.drishti360.app.ui.theme.LightBackground
import com.drishti360.app.ui.theme.LightSurface
import com.drishti360.app.ui.theme.PrimaryBlue
import com.drishti360.app.ui.theme.RiskHighRed
import com.drishti360.app.ui.theme.TextDarkPrimary
import com.drishti360.app.ui.theme.TextDarkSecondary
import com.drishti360.app.ui.viewmodels.AuditViewModel
import com.drishti360.app.ui.viewmodels.AuthViewModel
import com.drishti360.app.ui.viewmodels.CctvViewModel
import com.drishti360.app.ui.viewmodels.InspectionViewModel
import com.drishti360.app.ui.viewmodels.MainViewModel

sealed class Screen(val route: String, val title: String, val navLabel: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", "Home", Icons.Default.Home)
    object Ngos : Screen("ngos", "NGOs", "NGOs", Icons.Default.Business)
    object Cctv : Screen("cctv", "CCTV", "CCTV", Icons.Default.Videocam)
    object Inspect : Screen("inspect", "Inspect", "Inspect", Icons.Default.LocationOn)
    object Report : Screen("report", "Report", "Report", Icons.Default.Assignment)
    object Audit : Screen("audit", "Audit", "Audit", Icons.Default.CheckCircle)
}

@Composable
fun DrishtiNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel? = null,
    startDestination: String = Screen.Home.route,
    mainViewModel: MainViewModel = viewModel(),
    inspectionViewModel: InspectionViewModel = viewModel(),
    cctvViewModel: CctvViewModel = viewModel(),
    auditViewModel: AuditViewModel = viewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // EXACT 5 Bottom Navigation items: Home | NGOs | CCTV | Inspect | Audit
    val bottomBarItems = listOf(
        Screen.Home,
        Screen.Ngos,
        Screen.Cctv,
        Screen.Inspect,
        Screen.Audit
    )

    val safePopBack: () -> Unit = {
        if (!navController.popBackStack()) {
            navController.navigate(Screen.Home.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = LightSurface,
                tonalElevation = 6.dp
            ) {
                bottomBarItems.forEach { screen ->
                    val selected = currentRoute == screen.route ||
                            (screen == Screen.Ngos && currentRoute?.startsWith("ngo_detail/") == true) ||
                            (screen == Screen.Inspect && (currentRoute?.startsWith("inspect") == true || currentRoute?.startsWith("report") == true))
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.navLabel) },
                        label = {
                            Text(
                                text = screen.navLabel,
                                fontSize = 10.sp,
                                maxLines = 1,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                val targetRoute = if (screen == Screen.Inspect) {
                                    val activeId = inspectionViewModel.activeNgoId.value
                                        ?: mainViewModel.ngos.value.firstOrNull()?.id
                                    if (!activeId.isNullOrEmpty()) "inspect/$activeId" else Screen.Inspect.route
                                } else {
                                    screen.route
                                }
                                navController.navigate(targetRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            unselectedIconColor = TextDarkSecondary,
                            unselectedTextColor = TextDarkSecondary,
                            indicatorColor = LightSurface
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Admin Dashboard
            composable("admin_dashboard") {
                com.drishti360.app.ui.screens.AdminDashboardScreen(
                    authViewModel = authViewModel ?: viewModel(),
                    onLogout = { /* Logout handled in AdminDashboardScreen via authViewModel */ },
                    onAddNgo = { navController.navigate("add_ngo") },
                    onManageInspectors = { navController.navigate("manage_inspectors") },
                    onOpenNgos = { navController.navigate(Screen.Ngos.route) },
                    onOpenCameras = { navController.navigate(Screen.Cctv.route) },
                    onOpenInspections = { navController.navigate("inspections_list") },
                    onOpenAlerts = { navController.navigate("alerts_list") },
                    onOpenAudit = { navController.navigate(Screen.Audit.route) }
                )
            }

            // Alerts List (Admin & Monitoring)
            composable("alerts_list") {
                com.drishti360.app.ui.screens.AlertsListScreen(
                    viewModel = mainViewModel,
                    onBack = { navController.popBackStack() },
                    onSelectNgo = { ngoId ->
                        inspectionViewModel.setActiveNgoId(ngoId)
                        navController.navigate("ngo_detail/$ngoId")
                    }
                )
            }

            // Inspections List (Admin & Monitoring)
            composable("inspections_list") {
                com.drishti360.app.ui.screens.InspectionsListScreen(
                    onBack = { navController.popBackStack() },
                    onSelectInspection = { inspectionId ->
                        // Navigate to detail or audit
                        navController.navigate(Screen.Audit.route)
                    }
                )
            }
            // Screen 1: Home
            composable(Screen.Home.route) {
                CommandCenterScreen(
                    viewModel = mainViewModel,
                    authViewModel = authViewModel,
                    onSelectNgo = { ngo ->
                        inspectionViewModel.setActiveNgoId(ngo.id)
                        navController.navigate("ngo_detail/${ngo.id}")
                    },
                    onSeeAllNgos = {
                        navController.navigate(Screen.Ngos.route)
                    }
                )
            }

            // Screen 1.5: Add NGO
            composable("add_ngo") {
                com.drishti360.app.ui.screens.AddNgoScreen(
                    mainViewModel = mainViewModel,
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Screen 1.6: Manage Inspectors
            composable("manage_inspectors") {
                com.drishti360.app.ui.screens.ManageInspectorsScreen(
                    mainViewModel = mainViewModel,
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Screen 1.7: Edit NGO
            composable(
                route = "edit_ngo/{ngoId}",
                arguments = listOf(navArgument("ngoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val ngoId = backStackEntry.arguments?.getString("ngoId")
                val targetNgo = if (!ngoId.isNullOrEmpty()) mainViewModel.getNgoById(ngoId) else null
                
                if (targetNgo == null) {
                    NgoUnavailableScreen(onBack = { navController.popBackStack() })
                } else {
                    com.drishti360.app.ui.screens.EditNgoScreen(
                        ngo = targetNgo,
                        mainViewModel = mainViewModel,
                        authViewModel = authViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Screen 2: NGO List Screen
            composable(Screen.Ngos.route) {
                val authState = authViewModel?.authState?.collectAsState()?.value
                val isAdmin = (authState as? AuthViewModel.AuthState.Authenticated)?.role == "admin"
                NgoListScreen(
                    viewModel = mainViewModel,
                    isAdmin = isAdmin,
                    onBack = safePopBack,
                    onAddNgo = { navController.navigate("add_ngo") },
                    onSelectNgo = { ngo ->
                        inspectionViewModel.setActiveNgoId(ngo.id)
                        navController.navigate("ngo_detail/${ngo.id}")
                    }
                )
            }

            // NGO Detail Screen for specific ngoId route parameter
            composable(
                route = "ngo_detail/{ngoId}",
                arguments = listOf(navArgument("ngoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val ngoId = backStackEntry.arguments?.getString("ngoId")
                androidx.compose.runtime.LaunchedEffect(ngoId) {
                    if (!ngoId.isNullOrEmpty()) {
                        inspectionViewModel.setActiveNgoId(ngoId)
                    }
                }
                val targetNgo = if (!ngoId.isNullOrEmpty()) mainViewModel.getNgoById(ngoId) else null
                if (targetNgo == null) {
                    NgoUnavailableScreen(onBack = { navController.popBackStack() })
                } else {
                    NgoDetailScreen(
                        ngo = targetNgo,
                        viewModel = mainViewModel,
                        authViewModel = authViewModel,
                        onBack = { navController.popBackStack() },
                        onEditNgo = { navController.navigate("edit_ngo/${targetNgo.id}") },
                        onRefreshAudit = { auditViewModel.loadAuditTrail() },
                        onInitiateInspection = {
                            Log.d("NGO_FLOW", "[NGO_FLOW]\nSTART_INSPECT ngoId=${targetNgo.id}\nSTART_INSPECT ngoName=${targetNgo.name}")
                            inspectionViewModel.initiateSurpriseInspection(targetNgo.id, targetNgo.name)
                            navController.navigate("inspect/${targetNgo.id}")
                        }
                    )
                }
            }

            // Screen 3: CCTV AI Monitoring
            composable(Screen.Cctv.route) {
                CctvScreen(
                    viewModel = cctvViewModel,
                    onBack = safePopBack
                )
            }

            // Default Inspect Workflow Route
            composable(Screen.Inspect.route) {
                val activeNgoId by inspectionViewModel.activeNgoId.collectAsState()
                val ngos by mainViewModel.ngos.collectAsState()
                val targetNgo = if (!activeNgoId.isNullOrEmpty()) {
                    mainViewModel.getNgoById(activeNgoId!!)
                } else null

                if (targetNgo == null) {
                    NgoSelectionPromptScreen(
                        ngos = ngos,
                        onSelectNgo = { selectedNgo ->
                            Log.d("NGO_FLOW", "[NGO_FLOW]\nSTART_INSPECT ngoId=${selectedNgo.id}\nSTART_INSPECT ngoName=${selectedNgo.name}")
                            inspectionViewModel.initiateSurpriseInspection(selectedNgo.id, selectedNgo.name)
                            navController.navigate("inspect/${selectedNgo.id}")
                        }
                    )
                } else {
                    Log.d("NGO_FLOW", "[NGO_FLOW]\nINSPECTION_SCREEN ngoId=${targetNgo.id}\nngoName=${targetNgo.name}")
                    LiveInspectionScreen(
                        ngo = targetNgo,
                        allNgos = ngos,
                        onSelectNgo = { selectedNgo ->
                            Log.d("NGO_FLOW", "[NGO_FLOW]\nSTART_INSPECT ngoId=${selectedNgo.id}\nSTART_INSPECT ngoName=${selectedNgo.name}")
                            inspectionViewModel.setActiveNgoId(selectedNgo.id)
                            inspectionViewModel.initiateSurpriseInspection(selectedNgo.id, selectedNgo.name)
                        },
                        viewModel = inspectionViewModel,
                        onBack = safePopBack,
                        onVerifyLocation = { selectedId ->
                            Log.d("NGO_FLOW", "[NGO_FLOW]\nREPORT ngoId=$selectedId")
                            inspectionViewModel.verifyLocation()
                            navController.navigate("report/$selectedId")
                        }
                    )
                }
            }

            // Parameterized Inspect Workflow Route for exact ngoId
            composable(
                route = "inspect/{ngoId}",
                arguments = listOf(navArgument("ngoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val ngoId = backStackEntry.arguments?.getString("ngoId")
                android.util.Log.d("NGO_FLOW", "[NGO_FLOW]\nNAVIGATION ngoId=$ngoId")
                androidx.compose.runtime.LaunchedEffect(ngoId) {
                    if (!ngoId.isNullOrEmpty()) {
                        inspectionViewModel.setActiveNgoId(ngoId)
                        val targetName = mainViewModel.getNgoById(ngoId)?.name ?: "Unknown NGO ($ngoId)"
                        inspectionViewModel.initiateSurpriseInspection(ngoId, targetName)
                    }
                }
                val ngos by mainViewModel.ngos.collectAsState()
                val activeNgoId by inspectionViewModel.activeNgoId.collectAsState()
                val targetNgo = if (!activeNgoId.isNullOrEmpty()) {
                    mainViewModel.getNgoById(activeNgoId!!)
                } else if (!ngoId.isNullOrEmpty()) {
                    mainViewModel.getNgoById(ngoId)
                } else null

                if (targetNgo == null) {
                    NgoUnavailableScreen(onBack = safePopBack)
                } else {
                    Log.d("NGO_FLOW", "[NGO_FLOW]\nINSPECTION_SCREEN ngoId=${targetNgo.id}\nngoName=${targetNgo.name}")
                    LiveInspectionScreen(
                        ngo = targetNgo,
                        allNgos = ngos,
                        onSelectNgo = { selectedNgo ->
                            Log.d("NGO_FLOW", "[NGO_FLOW]\nSTART_INSPECT ngoId=${selectedNgo.id}\nSTART_INSPECT ngoName=${selectedNgo.name}")
                            inspectionViewModel.setActiveNgoId(selectedNgo.id)
                            inspectionViewModel.initiateSurpriseInspection(selectedNgo.id, selectedNgo.name)
                        },
                        viewModel = inspectionViewModel,
                        onBack = safePopBack,
                        onVerifyLocation = { selectedId ->
                            android.util.Log.d("NGO_FLOW", "[NGO_FLOW]\nREPORT ngoId=$selectedId")
                            inspectionViewModel.verifyLocation()
                            navController.navigate("report/$selectedId")
                        }
                    )
                }
            }

            // Default Report Route
            composable(Screen.Report.route) {
                val activeNgoId by inspectionViewModel.activeNgoId.collectAsState()
                val targetNgo = if (!activeNgoId.isNullOrEmpty()) mainViewModel.getNgoById(activeNgoId!!) else null

                if (targetNgo == null) {
                    NgoUnavailableScreen(onBack = safePopBack)
                } else {
                    InspectionReportScreen(
                        ngo = targetNgo,
                        viewModel = inspectionViewModel,
                        onBack = safePopBack,
                        onSubmitComplete = {
                            navController.navigate(Screen.Audit.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }

            // Parameterized Report Route for exact ngoId
            composable(
                route = "report/{ngoId}",
                arguments = listOf(navArgument("ngoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val ngoId = backStackEntry.arguments?.getString("ngoId")
                if (!ngoId.isNullOrEmpty()) {
                    inspectionViewModel.setActiveNgoId(ngoId)
                }
                val targetNgo = if (!ngoId.isNullOrEmpty()) mainViewModel.getNgoById(ngoId) else null

                if (targetNgo == null) {
                    NgoUnavailableScreen(onBack = safePopBack)
                } else {
                    InspectionReportScreen(
                        ngo = targetNgo,
                        viewModel = inspectionViewModel,
                        onBack = safePopBack,
                        onSubmitComplete = {
                            navController.navigate(Screen.Audit.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }

            // Screen 6: Audit
            composable(Screen.Audit.route) {
                AuditTrailScreen(
                    viewModel = auditViewModel,
                    onBackToCommandCenter = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NgoUnavailableScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = RiskHighRed,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "NGO Information Unavailable",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "The requested NGO could not be found or loaded.",
            fontSize = 13.sp,
            color = TextDarkSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryActionButton(
            text = "Go Back",
            onClick = onBack
        )
    }
}

@Composable
fun NgoSelectionPromptScreen(
    ngos: List<NGO>,
    onSelectNgo: (NGO) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Select NGO for Field Inspection",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Please select an NGO from the list below to begin field inspection:",
            fontSize = 12.sp,
            color = TextDarkSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ngos.size) { index ->
                val ngoItem = ngos[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectNgo(ngoItem) },
                    colors = CardDefaults.cardColors(containerColor = LightSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = ngoItem.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDarkPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "📍 ${ngoItem.location.city}, ${ngoItem.location.state} • ${ngoItem.riskScore?.let { "${it.level.name} Risk (${it.score})" } ?: "Not Assessed"}",
                                fontSize = 11.sp,
                                color = TextDarkSecondary
                            )
                        }
                        Text("Inspect →", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
