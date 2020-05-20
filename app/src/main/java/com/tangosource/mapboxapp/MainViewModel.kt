package com.tangosource.mapboxapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import kotlinx.coroutines.launch
import java.lang.Exception

/**
 * Created by Roberto Ávalos on 17/05/20.
 */

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MainRepository()

    private val _searchingAddress = MutableLiveData<MutableList<CarmenFeature>>()
    val address: LiveData<MutableList<CarmenFeature>>
        get() = _searchingAddress

    fun fetchAddress(address: String, centerLocation: LatLng?, latLngBounds: LatLngBounds) {
        viewModelScope.launch {
            try {
                val carmenFeatures = repository.fetchAddress(address, centerLocation, latLngBounds)
                _searchingAddress.value = carmenFeatures
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}