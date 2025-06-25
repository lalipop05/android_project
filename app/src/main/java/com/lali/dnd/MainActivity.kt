package com.lali.dnd


import LocationManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.common.GoogleApiAvailability


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
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
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.lali.dnd.ui.theme.DNDTheme
import com.lali.dnd.util.REQUEST_CHECK_SETTINGS

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                Log.d("GPS", "User enabled location settings")
                // Proceed with location operations
            }
            Activity.RESULT_CANCELED -> {
                Log.d("GPS", "User declined to enable location settings")
                // Handle the case where user didn't enable location
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        enableEdgeToEdge()
        setContent {
            DNDTheme {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(navController, startDestination = "permission_screen") {
                        composable("permission_screen") {
                            LocationPermissionRequester(
                                navController,
                                Modifier.padding(innerPadding)
                            )
                        }
                        composable("main_screen") {
                            MainButton(
                                navController,
                                modifier = Modifier.padding(innerPadding))
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun MainButton(navController: NavController, modifier: Modifier = Modifier) {
    var displayText by remember { mutableStateOf("")}
    val context = LocalContext.current

    Column (modifier = modifier){
        Button(onClick = {
            val permissionGranted = LocationManager.checkPermission(context, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))

            if (!permissionGranted) {
                navController.navigate("permission_screen")
            } else {
                LocationManager.getLastLocation(context) {location ->
                    if (location != null) {
                        displayText = location.toString()
                    } else {
                        displayText = "null"
                    }
                }
            }
        }) {
            Text("Get Location")
        }
        Text(text = displayText)
    }
}

@Composable
fun LocationPermissionRequester(navController: NavController, modifier: Modifier) {
    val context = LocalContext.current

    var displayText by remember { mutableStateOf("We need some permissions to continue!") }

    var foregroundPermissionsGranted by remember { mutableStateOf(false)};
    var backgroundPermissionGranted by remember { mutableStateOf(false) }

    val foregroundPermissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    val backgroundPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    val foregroundPermissionLauncher = rememberLauncherForActivityResult (
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all {it.value}) {
            Log.d("LocationPermission", "Foreground permissions granted")
            foregroundPermissionsGranted = true
            navController.navigate("background_permission_screen")
        }
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            backgroundPermissionGranted = true
            navController.navigate("main_screen")
        }
    }

    LaunchedEffect(Unit) {
        foregroundPermissionsGranted = LocationManager.checkPermission(context, foregroundPermissions)
        backgroundPermissionGranted = LocationManager.checkPermission(context, arrayOf(backgroundPermission))

        if (foregroundPermissionsGranted && backgroundPermissionGranted) {
            navController.navigate("main_screen")
        }
    }

    Column (modifier = modifier) {
        Text(text = displayText)
        Button(onClick = {
            var arePermissionsGranted = LocationManager.checkPermission(context, foregroundPermissions)
            if (arePermissionsGranted) {
                foregroundPermissionsGranted = true
            } else {
                foregroundPermissionLauncher.launch(foregroundPermissions)
            }

            arePermissionsGranted = LocationManager.checkPermission(context, arrayOf(backgroundPermission))
            if (arePermissionsGranted) {
                backgroundPermissionGranted = true
            } else {
                backgroundPermissionLauncher.launch(backgroundPermission)
            }

            if (backgroundPermissionGranted && foregroundPermissionsGranted) {
                navController.navigate("main_screen")
            } else {
                displayText = "We need permissions to experience the full potential of this app!"
            }
        }) {
            Text("Grant")
        }
    }
}