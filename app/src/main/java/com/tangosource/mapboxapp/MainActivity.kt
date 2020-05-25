package com.tangosource.mapboxapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), PermissionsListener, OnMapReadyCallback {

    private var permissionsManager: PermissionsManager? = null
    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null

    // this is our view model
    private lateinit var mainViewModel: MainViewModel

    // the adaptar that will handle the addresses found
    private lateinit var searchAddressAdapter: SearchAdapter

    // the bounds of the visible region of our map
    private lateinit var latLngBounds: LatLngBounds

    // central point of our map
    private var centerLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, BuildConfig.MAPBOX_ACCESS_TOKEN)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        searchAddressAdapter = SearchAdapter(object : SearchListener {
            override fun onSelectAddress(address: CarmenFeature) {
//                  addMarker()
            }
        })

        rvAddressesList.apply {
            adapter = searchAddressAdapter
        }

/** When the user clicks on edit text to start typing address, we need to get both
 * the central position the visible region of our map  */
etAddress.setOnTouchListener { v, event ->
    if(MotionEvent.ACTION_UP == event.action) {
        centerLocation = mapboxMap?.cameraPosition?.target
        latLngBounds = mapboxMap?.projection?.visibleRegion?.latLngBounds!!
        cvAddresses.visibility = View.VISIBLE
    }

    false
}

        etAddress.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isEmpty()) {
                    searchAddressAdapter.updateAddressList(ArrayList())
                    cvAddresses.visibility = View.GONE
                    return
                }

                mainViewModel.fetchAddress(s.toString(), centerLocation, latLngBounds)
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })

        /** this observe will execute every time we have a response of our Places API request */
        mainViewModel.address.observe(this, Observer {
            searchAddressAdapter.updateAddressList(it)
        })
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        this.mapboxMap?.setStyle(Style.MAPBOX_STREETS) {
            enableLocationComponent(it)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionsManager?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(applicationContext, "This app needs location permissions", Toast.LENGTH_LONG)
            .show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (!granted) {
            Toast.makeText(
                applicationContext,
                "You didn\'t grant location permissions.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        mapboxMap?.style?.let { enableLocationComponent(it) }
    }

    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (!PermissionsManager.areLocationPermissionsGranted(this)) {
            permissionsManager = PermissionsManager(this)
            permissionsManager?.requestLocationPermissions(this)
            return
        }

        val locationComponent = mapboxMap?.locationComponent
        locationComponent?.activateLocationComponent(
            LocationComponentActivationOptions.builder(
                this,
                loadedMapStyle
            ).build()
        )
        locationComponent?.isLocationComponentEnabled = true
        locationComponent?.cameraMode = CameraMode.NONE
        locationComponent?.renderMode = RenderMode.COMPASS

        val lastLocation = locationComponent?.lastKnownLocation
        if (lastLocation != null) {
            val lat = lastLocation.latitude
            val lng = lastLocation.longitude
            val location = LatLng(lat, lng)

            val position = CameraPosition.Builder()
                .target(location)
                .zoom(10.0)
                .tilt(20.0)
                .build()

            mapboxMap?.animateCamera(CameraUpdateFactory.newCameraPosition(position), 3000)
        }
    }
}