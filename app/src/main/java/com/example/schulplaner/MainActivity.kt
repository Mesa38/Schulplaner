package com.example.schulplaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import androidx.work.*
import com.example.schulplaner.ui.theme.SchulplanerTheme
import com.google.firebase.FirebaseApp
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {}
        
        scheduleReminders()
        setContent {
            SchulplanerTheme {
                var user by remember { mutableStateOf<com.google.firebase.auth.FirebaseUser?>(null) }
                
                LaunchedEffect(Unit) {
                    user = FirebaseHelper.getAuth()?.currentUser
                }

                if (user == null && FirebaseHelper.isInitialized()) {
                    LoginScreen(onLoginSuccess = { 
                        user = FirebaseHelper.getAuth()?.currentUser
                    })
                } else {
                    AbiPlanerApp()
                }
            }
        }
    }

    private fun scheduleReminders() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "homework_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

@Composable
fun AbiPlanerApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") { HomeScreen(navController) }
            composable("homework") { HomeworkScreen() }
            composable("grades") { GradesScreen() }
            composable("learning_center") { LearningCenterScreen() }
            composable("timetable") { TimetableScreen() }
            composable("exams") { ExamsScreen() }
            composable("abi_calc") { AbiCalcScreen() }
            composable("timer") { StudyTimerScreen() }
            composable("settings") { SettingsScreen() }
            composable("profile") { ProfileScreen() }
            composable("statistics") { StatisticsScreen() }
            composable("media_hub") { MediaHubScreen() }
            composable("absences") { AbsenceManagerScreen() }
            composable("achievements") { AchievementsScreen() }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        NavItem("home", "Home", Icons.Default.Home),
        NavItem("timetable", "Plan", Icons.Default.CalendarMonth),
        NavItem("statistics", "Stats", Icons.Default.AutoGraph),
        NavItem("profile", "Profil", Icons.Default.Person)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val currentRoute = currentRoute(navController)

        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
fun currentRoute(navController: NavController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}

data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
