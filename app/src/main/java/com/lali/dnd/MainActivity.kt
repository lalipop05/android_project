package com.lali.dnd


import MyLocationManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

import android.app.Activity
import android.location.Location
// Extension functions learn more NavHost syntax
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.lali.dnd.services.FirebaseFunctionsManager
import com.lali.dnd.services.EnvironmentValidator
import com.lali.dnd.ui.theme.DNDTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var functions: FirebaseFunctions

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                Log.d("GPS", "User enabled location settings")
            }
            Activity.RESULT_CANCELED -> {
                Log.d("GPS", "User declined to enable location settings")
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
                                locationSettingsLauncher,
                                modifier = Modifier.padding(innerPadding))
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun MainButton(navController: NavController,
               locationSettingsLauncher: ActivityResultLauncher<IntentSenderRequest>,
               modifier: Modifier) {
    var displayText by remember { mutableStateOf("")}
    var responseText by remember { mutableStateOf("BUM")}
    var locationData by remember { mutableStateOf<Location?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    Column (modifier = modifier){
        Button(onClick = {
            val permissionGranted = EnvironmentValidator.checkPermission(context)
            val locationEnabled = EnvironmentValidator.checkLocationProviders(context)

            if (!permissionGranted) {
                navController.navigate("permission_screen")
            } else if (!locationEnabled) {
                EnvironmentValidator.locationEnabled(context, locationSettingsLauncher)
            } else if (!EnvironmentValidator.googlePlayServiceChecker(context))  {
                displayText = "GooglePlay services not available"
            }
            else {
                MyLocationManager.getLastLocation(context) {location ->
                    if (location != null) {
                        locationData = location
                        coroutineScope.launch {
                            val responseJson = withContext(Dispatchers.IO) {
                                MyLocationManager.getAddress(location)
                            }
                            displayText = responseJson ?: "null returned"
                        }
                    } else {
                        displayText = "Location is null"
                    }
                }
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
    var locationPermissionsGranted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        locationPermissionsGranted = EnvironmentValidator.checkPermission(context)
        if (locationPermissionsGranted) {
            navController.navigate("main_screen")
        }
    }

    Column(modifier = modifier) {
        Text(stringResource(R.string.permissions_reason, stringResource(R.string.app_name)))
        Text(stringResource(R.string.permissions_step_1))
        Text(stringResource(R.string.permissions_step_2))
        Text(stringResource(R.string.permissions_step_3))
        Text(stringResource(R.string.permissions_step_4))

        Button(onClick = {
            locationPermissionsGranted = EnvironmentValidator.checkPermission(context)
            if (!locationPermissionsGranted) {
                EnvironmentValidator.openSettings(context)
            } else {
                navController.navigate("main_screen")
            }
        }) {
            Text(
                when {
                    !locationPermissionsGranted -> "Grant Location Access"
                    else -> "Continue"
                }
            )
        }
    }
}
