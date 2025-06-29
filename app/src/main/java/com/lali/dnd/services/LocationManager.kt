import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.lali.dnd.util.REQUEST_CHECK_SETTINGS
import com.lali.dnd.util.findActivity

object LocationManager {
    val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    val FOREGROUND_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION)

    val BACKGROUND_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    private val LOCATION_UPDATES_MILLI: Long = 5000

    @SuppressLint("MissingPermission")
    fun getLastLocation(context: Context, onResult: (Location?) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                onResult(location)
            }.addOnFailureListener{
                onResult(null)
            }
    }

    @SuppressLint("MissingPermission")
    fun getPreciseLocation(context: Context, onResult: (Location?) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            onResult(location)
        }.addOnFailureListener {
            onResult(null)
        }

    }

    fun checkPermission(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun checkPermission(context: Context) : Boolean {
        return checkPermission(context, PERMISSIONS)
    }

    fun googlePlayServiceChecker(context: Context): Boolean{
        val g = GoogleApiAvailability.getInstance();
        val resultCode = g.isGooglePlayServicesAvailable(context)
        return if (resultCode == ConnectionResult.SUCCESS) {
            true
        } else {
            false
        }
    }

    fun locationEnabled(context: Context, locationSettingsLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        val locationRequestHighFiveMins = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATES_MILLI
        ).build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequestHighFiveMins)

        val result = LocationServices
            .getSettingsClient(context)
            .checkLocationSettings(builder.build())

        result.addOnSuccessListener { response ->
            Log.d("GPS", "Location settings are satisfied")
            // Proceed with location operations
        }.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Create IntentSenderRequest for the modern API
                    val intentSenderRequest = IntentSenderRequest.Builder(
                        exception.resolution
                    ).build()

                    // Launch using the modern launcher
                    locationSettingsLauncher.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e("GPS", "Error launching location settings dialog", sendEx)
                }
            } else {
                Log.e("GPS", "Location settings error", exception)
            }
        }
    }

    fun openSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("Permission", "Failed to open settings: ${e.message}")
        }
    }
}