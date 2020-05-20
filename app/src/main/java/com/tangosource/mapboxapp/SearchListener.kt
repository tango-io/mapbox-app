package com.tangosource.mapboxapp

import com.mapbox.api.geocoding.v5.models.CarmenFeature

/**
 * Created by Roberto Ávalos on 17/05/20.
 */
interface SearchListener {
    fun onSelectAddress(address: CarmenFeature)
}