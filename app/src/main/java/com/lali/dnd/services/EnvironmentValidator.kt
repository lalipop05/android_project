package com.lali.dnd.services

import android.content.Context
import androidx.core.content.edit
import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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


class EnvironmentValidator {
    companion object {
        private const val STORAGE_NAME = "permission_tracker"
        private const val KEY_BACKGROUND_COUNT = "background_denied_count"
        private const val KEY_FOREGROUND_COUNT = "foreground_denied_count"

        private val LOCATION_UPDATES_MILLI: Long = 5000

        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )

        val FOREGROUND_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION)

        val BACKGROUND_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        fun incrementBackgroundDenialCount(context: Context) {
            val prefs = context.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE)
            val count = prefs.getInt(KEY_BACKGROUND_COUNT, 0)
            prefs.edit { putInt(KEY_BACKGROUND_COUNT, count + 1) }
        }

        fun backgroundPermanentlyDenied(context: Context): Boolean {
            val prefs = context.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE)
            val count = prefs.getInt(KEY_BACKGROUND_COUNT, 0)
            return count >= 1
        }

        fun incrementForegroundDenialCount(context: Context) {
            val prefs = context.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE)
            val count = prefs.getInt(KEY_FOREGROUND_COUNT, 0)
            prefs.edit { putInt(KEY_FOREGROUND_COUNT, count + 1) }
        }

        fun foregroundPermanentlyDenied(context: Context): Boolean {
            val prefs = context.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE)
            val count = prefs.getInt(KEY_FOREGROUND_COUNT, 0)
            return count >= 1
        }

        fun checkPermission(context: Context, permissions: Array<String>): Boolean {
            return permissions.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

        fun checkPermission(context: Context) : Boolean {
            return checkPermission(context, PERMISSIONS)
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

        fun checkLocationProviders(context: Context): Boolean {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }

        fun googlePlayServiceChecker(context: Context): Boolean{
            val g = GoogleApiAvailability.getInstance();
            val resultCode = g.isGooglePlayServicesAvailable(context)
            return resultCode == ConnectionResult.SUCCESS
        }

        fun checkInternetAvailability(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val connectedNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(connectedNetwork) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
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
    }
}