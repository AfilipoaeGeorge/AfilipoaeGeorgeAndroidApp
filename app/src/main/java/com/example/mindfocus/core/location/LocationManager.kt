package com.example.mindfocus.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager as AndroidLocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.Locale

data class LocationData(
    val latitude: Double,
    val longitude: Double
)

data class AddressData(
    val streetNumber: String?,
    val street: String?,
    val city: String?,
    val county: String?,
    val country: String?,
    val fullAddress: String
)

class LocationManager(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? AndroidLocationManager
        return locationManager?.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER) == true ||
               locationManager?.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER) == true
    }
    
    suspend fun getCurrentLocation(): LocationData? {
        if (!hasLocationPermission()) {
            android.util.Log.w("LocationManager", "No location permission granted")
            return null
        }
        
        if (!isLocationEnabled()) {
            android.util.Log.w("LocationManager", "Location services are disabled on device")
            return null
        }
        
        return try {
            val cancellationTokenSource = CancellationTokenSource()
            
            // Try to get current location - may return null if location is not immediately available
            var location: Location? = withContext(Dispatchers.IO) {
                try {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    ).await()
                } catch (e: Exception) {
                    android.util.Log.w("LocationManager", "getCurrentLocation failed: ${e.message}")
                    null
                }
            }
            
            // If location is null, wait a bit and try again (sometimes location needs time to be available)
            if (location == null) {
                android.util.Log.d("LocationManager", "First attempt returned null, waiting 1000ms and trying again...")
                delay(1000)
                location = withContext(Dispatchers.IO) {
                    try {
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            cancellationTokenSource.token
                        ).await()
                    } catch (e: Exception) {
                        android.util.Log.w("LocationManager", "Second getCurrentLocation attempt failed: ${e.message}")
                        null
                    }
                }
            }
            
            // If still null, try with balanced priority
            if (location == null) {
                android.util.Log.d("LocationManager", "High accuracy returned null, trying balanced priority...")
                delay(500)
                location = withContext(Dispatchers.IO) {
                    try {
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            cancellationTokenSource.token
                        ).await()
                    } catch (e: Exception) {
                        android.util.Log.w("LocationManager", "Balanced priority getCurrentLocation failed: ${e.message}")
                        null
                    }
                }
            }
            
            location?.let {
                android.util.Log.d("LocationManager", "Got current location: ${it.latitude}, ${it.longitude}")
                LocationData(
                    latitude = it.latitude,
                    longitude = it.longitude
                )
            } ?: run {
                android.util.Log.d("LocationManager", "getCurrentLocation returned null after all retries")
                null
            }
        } catch (e: SecurityException) {
            android.util.Log.e("LocationManager", "SecurityException: Permission denied", e)
            null
        } catch (e: Exception) {
            android.util.Log.e("LocationManager", "Error getting location: ${e.message}", e)
            null
        }
    }
    
    suspend fun getLastKnownLocation(): LocationData? {
        if (!hasLocationPermission()) {
            android.util.Log.w("LocationManager", "No location permission for last known location")
            return null
        }
        
        if (!isLocationEnabled()) {
            android.util.Log.w("LocationManager", "Location services are disabled, cannot get last known location")
            return null
        }
        
        return try {
            val location: Location? = withContext(Dispatchers.IO) {
                try {
                    fusedLocationClient.lastLocation.await()
                } catch (e: Exception) {
                    android.util.Log.w("LocationManager", "getLastKnownLocation failed: ${e.message}")
                    null
                }
            }
            
            location?.let {
                android.util.Log.d("LocationManager", "Got last known location: ${it.latitude}, ${it.longitude}")
                LocationData(
                    latitude = it.latitude,
                    longitude = it.longitude
                )
            } ?: run {
                android.util.Log.d("LocationManager", "getLastKnownLocation returned null")
                null
            }
        } catch (e: SecurityException) {
            android.util.Log.e("LocationManager", "SecurityException: Permission denied", e)
            null
        } catch (e: Exception) {
            android.util.Log.e("LocationManager", "Error getting last known location: ${e.message}", e)
            null
        }
    }
    
    /**
     * Gets address from coordinates using reverse geocoding
     */
    suspend fun getAddressFromCoordinates(latitude: Double, longitude: Double): AddressData? {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Geocoder is available (API 33+)
                val geocoderAvailable = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    Geocoder.isPresent()
                } else {
                    true // Assume available on older versions
                }
                
                if (!geocoderAvailable) {
                    android.util.Log.w("LocationManager", "Geocoder is not available")
                    return@withContext null
                }
                
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses: List<Address>? = try {
                    geocoder.getFromLocation(latitude, longitude, 1)
                } catch (e: Exception) {
                    android.util.Log.e("LocationManager", "Error getting address from coordinates: ${e.message}", e)
                    null
                }
                
                addresses?.firstOrNull()?.let { address ->
                    buildAddressData(address)
                }
            } catch (e: Exception) {
                android.util.Log.e("LocationManager", "Error in getAddressFromCoordinates: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * Formats location as a full address string with coordinates
     */
    suspend fun formatLocation(latitude: Double?, longitude: Double?): String? {
        if (latitude == null || longitude == null) {
            return null
        }
        
        val coordinates = String.format("%.4f, %.4f", latitude, longitude)
        val addressData = getAddressFromCoordinates(latitude, longitude)
        
        return if (addressData != null) {
            "${addressData.fullAddress} ($coordinates)"
        } else {
            coordinates
        }
    }
    
    /**
     * Formats location as coordinates only
     */
    fun formatLocationAsCoordinates(latitude: Double?, longitude: Double?): String? {
        if (latitude == null || longitude == null) {
            return null
        }
        
        return String.format("%.4f, %.4f", latitude, longitude)
    }
    
    private fun buildAddressData(address: Address): AddressData {
        val streetNumber = address.subThoroughfare
        val street = address.thoroughfare
        val city = address.locality ?: address.subAdminArea
        val county = address.adminArea
        val country = address.countryName
        
        val addressParts = mutableListOf<String>()
        streetNumber?.let { addressParts.add(it) }
        street?.let { addressParts.add(it) }
        city?.let { addressParts.add(it) }
        county?.let { addressParts.add(it) }
        country?.let { addressParts.add(it) }
        
        val fullAddress = addressParts.joinToString(", ")
        
        return AddressData(
            streetNumber = streetNumber,
            street = street,
            city = city,
            county = county,
            country = country,
            fullAddress = fullAddress
        )
    }
}


