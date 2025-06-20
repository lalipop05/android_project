package com.lali.dnd


import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
// Extension functions learn more NavHost syntax
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.lali.dnd.ui.theme.DNDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DNDTheme {
                val navController = rememberNavController()

                Scaffold (modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(navController, startDestination = "foreground_permission_screen") {
                        composable("foreground_permission_screen") {
                            foregroundLocationPermissionRequester(navController, Modifier.padding(innerPadding))
                        }
                        composable ("background_permission_screen" ) {
                            backgroundLocationPermissionRequester(navController, Modifier.padding(innerPadding))
                        }
                        composable ("main_screen") {
                            MainButton(modifier = Modifier.padding(innerPadding))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainButton(modifier: Modifier = Modifier) {
    Button(onClick = {

    }, modifier = modifier) {
        Text("Main Bunny")
    }
}

@Composable
fun foregroundLocationPermissionRequester(navController: NavController, modifier: Modifier) {
    val context = LocalContext.current

    var foregroundPermissionsGranted by remember { mutableStateOf(false)};
    val foregroundPermissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val foregroundPermissionLauncher = rememberLauncherForActivityResult (
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all {it.value}) {
            Log.d("LocationPermission", "Foreground permissions granted")
            foregroundPermissionsGranted = true
            navController.navigate("background_permission_screen")
        }
    }

    val areForegroundPermissionsGranted = foregroundPermissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        foregroundPermissionsGranted = areForegroundPermissionsGranted
    }

    if (!foregroundPermissionsGranted) {
        LaunchedEffect(Unit) {
            foregroundPermissionLauncher.launch(foregroundPermissions)
        }
    } else {
        navController.navigate("background_permission_screen")
    }

    Text("Foreground: " + foregroundPermissionsGranted.toString(), modifier = modifier)
}

@Composable
fun backgroundLocationPermissionRequester(navController: NavController, modifier: Modifier) {
    val context = LocalContext.current
    var backgroundPermissionGranted by remember { mutableStateOf(false) }

    val backgroundPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            backgroundPermissionGranted = true
            navController.navigate("main_screen")
        }
    }

    LaunchedEffect(Unit) {
        if (!backgroundPermissionGranted) {
            backgroundPermissionLauncher.launch(backgroundPermission)
        } else {
            navController.navigate("main_screen")
        }
    }

    Text("Background: " + backgroundPermissionGranted.toString(), modifier = modifier)
}