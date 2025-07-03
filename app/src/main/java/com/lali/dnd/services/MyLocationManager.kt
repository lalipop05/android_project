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
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.gson.Gson
import com.lali.dnd.BuildConfig
import com.lali.dnd.model.GeoCodingResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException


object MyLocationManager {


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

    fun getAddress(location: Location): String? {
        val jsonBody = sendReverseGeocodingRequest(location)
        if (jsonBody == null) return null
        return extractAddressFromJson(jsonBody)
    }

    private fun extractAddressFromJson(jsonBody: String): String? {
        val gson = Gson()
        val response = gson.fromJson(jsonBody, GeoCodingResponse::class.java)
        return response.results.firstOrNull()?.formattedAddress
    }

    private fun sendReverseGeocodingRequest(location: Location): String? {
        val mapsUrl = buildReverseGeocodingUrl(location.latitude, location.longitude)

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(mapsUrl)
            .build()

        return try {
            client.newCall(request).execute().use {response ->
                if (!response.isSuccessful) {
                    null
                } else {
                    response.body?.string()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun buildReverseGeocodingUrl(lat: Double, lng: Double): String {
        val apiKey = BuildConfig.MAPS_API_KEY
        val builder = Uri.Builder()
            .scheme("https")
            .authority("maps.googleapis.com")
            .appendPath("maps")
            .appendPath("api")
            .appendPath("geocode")
            .appendPath("json")
            .appendQueryParameter("latlng", "$lat,$lng")
            .appendQueryParameter("key", apiKey)
        Log.d("Link", builder.build().toString())
        return builder.build().toString()
    }
}