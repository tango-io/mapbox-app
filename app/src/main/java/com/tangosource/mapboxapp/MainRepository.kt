package com.tangosource.mapboxapp

import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Roberto Ávalos on 17/05/20.
 */

class MainRepository {

    suspend fun fetchAddress(
        address: String,
        centerLocation: LatLng?
    ): MutableList<CarmenFeature> {
        return suspendCoroutine { continuation ->
            val reverseGeocode = getMapboxGeocoding(address, centerLocation)
            reverseGeocode.enqueueCall(object : Callback<GeocodingResponse> {
                override fun onResponse(
                    call: Call<GeocodingResponse>,
                    response: Response<GeocodingResponse>
                ) {
                    val results = response.body()?.features()

                    if (results != null && results.size > 0) {
                        continuation.resume(results)
                    } else {
                        continuation.resume(ArrayList())
                    }
                }

                override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                    t.printStackTrace()
                    continuation.resume(ArrayList())
                }
            })
        }
    }

    private fun getMapboxGeocoding(
        address: String,
        centerLocation: LatLng?
    ): MapboxGeocoding {

        val builder = MapboxGeocoding.builder()
            .accessToken(BuildConfig.MAPBOX_ACCESS_TOKEN)
            .query(address)
            .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)

        return builder.build()
    }
}