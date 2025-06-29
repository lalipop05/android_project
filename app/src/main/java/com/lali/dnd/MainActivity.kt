package com.lali.dnd


import LocationManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
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
import android.provider.Settings
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
import com.google.firebase.functions.HttpsCallableResult
import com.lali.dnd.services.FirebaseFunctionsManager
import com.lali.dnd.services.PermissionTracker
import com.lali.dnd.ui.theme.DNDTheme
import com.lali.dnd.util.REQUEST_CHECK_SETTINGS
import java.security.Permission

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var functions: FirebaseFunctions

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
        functions = Firebase.functions
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
    var responseText by remember { mutableStateOf("BUM")}
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

            FirebaseFunctionsManager.callFunction("on_call_example")  // Updated function name
                .addOnSuccessListener { result ->
                    val data = result.data as? Map<String, Any>
                    val message = data?.get("message") as? String
                    responseText = message ?: "No message"
                }
                .addOnFailureListener { e ->
                    responseText = "Error: ${e.message}"
                }
        }) {
            Text("Get Location")
        }
        Text(text = displayText + responseText)
    }
}

@Composable
fun LocationPermissionRequester(navController: NavController, modifier: Modifier) {
    val context = LocalContext.current
    var displayText by remember { mutableStateOf("We need some location permissions to continue!") }
    var foregroundPermissionsGranted by remember { mutableStateOf(false) }
    var backgroundPermissionGranted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        foregroundPermissionsGranted = LocationManager.checkPermission(context, LocationManager.FOREGROUND_PERMISSIONS)
        backgroundPermissionGranted = LocationManager.checkPermission(context, LocationManager.BACKGROUND_PERMISSIONS)
    }

    LaunchedEffect(foregroundPermissionsGranted, backgroundPermissionGranted) {
        if (foregroundPermissionsGranted && backgroundPermissionGranted) {
            navController.navigate("main_screen")
        }
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        backgroundPermissionGranted = isGranted
        if (!isGranted) {
            PermissionTracker.incrementBackgroundDenialCount(context)
        }
    }

    val foregroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        foregroundPermissionsGranted = permissions.all { it.value }
        if (!foregroundPermissionsGranted) {
            PermissionTracker.incrementForegroundDenialCount(context)
        }
    }


    Column(modifier = modifier) {
        Text(text = displayText)
        Button(onClick = {
            when {
                !foregroundPermissionsGranted -> {
                    foregroundPermissionsGranted = LocationManager.checkPermission(context, LocationManager.FOREGROUND_PERMISSIONS)
                    if (foregroundPermissionsGranted) {
                        displayText = "Great! Now we need background location access."
                    } else {
                        if (PermissionTracker.foregroundPermanentlyDenied(context)) {
                            LocationManager.openSettings(context)
                        } else {
                            foregroundPermissionLauncher.launch(LocationManager.FOREGROUND_PERMISSIONS)
                        }
                    }
                }
                !backgroundPermissionGranted -> {
                    backgroundPermissionGranted = LocationManager.checkPermission(context, LocationManager.BACKGROUND_PERMISSIONS)
                    if (PermissionTracker.backgroundPermanentlyDenied(context)) {
                        LocationManager.openSettings(context)
                    } else {
                        backgroundPermissionLauncher.launch(LocationManager.BACKGROUND_PERMISSIONS[0])
                    }
                }
            }
        }) {
            Text(
                when {
                    !foregroundPermissionsGranted -> "Grant Location Access"
                    !backgroundPermissionGranted -> "Grant Background Access"
                    else -> "Continue"
                }
            )
        }
    }
}
