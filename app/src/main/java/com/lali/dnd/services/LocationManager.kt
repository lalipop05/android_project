import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
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

    val LOCATION_UPDATES_MILLI: Long = 5000

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

    fun locationPermsAndEnabled(context: Context, onResult: (Boolean) -> Unit) {
        val permissionsGranted = checkPermission(context)
        var enabled = false
        locationEnabled(context) { statusCode ->
            if (statusCode == LocationSettingsStatusCodes.SUCCESS) {
                onResult(enabled && permissionsGranted)
            } else {
                onResult(false)
            }
        }
    }

    fun locationEnabled(context: Context, onResult: (Int) -> Unit) {
        val locationRequestHighFiveMins = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATES_MILLI).build()
        val builder: LocationSettingsRequest.Builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequestHighFiveMins)
        val result: Task<LocationSettingsResponse> = LocationServices
            .getSettingsClient(context).checkLocationSettings(builder.build())

        result.addOnSuccessListener { response: LocationSettingsResponse ->
            onResult(LocationSettingsStatusCodes.SUCCESS)
        }.addOnFailureListener{ err ->
            if (err is ResolvableApiException) {
                // settings need to be changed
                val activity = context.findActivity()
                if (activity == null) {
                    onResult(LocationSettingsStatusCodes.DEVELOPER_ERROR)
                }
                try {
                    err.startResolutionForResult( activity!!, REQUEST_CHECK_SETTINGS )
                } catch (sendEx: IntentSender.SendIntentException) {
                    onResult(LocationSettingsStatusCodes.ERROR)
                }
            } else {
                onResult(LocationSettingsStatusCodes.ERROR)
            }
        }
    }
}