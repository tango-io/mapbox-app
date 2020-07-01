package com.tangosource.mapboxapp

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
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
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), PermissionsListener, OnMapReadyCallback {

    private var permissionsManager: PermissionsManager? = null
    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null

    // this is our view model
    private lateinit var mainViewModel: MainViewModel

    // the adaptar that will handle the addresses found
    private lateinit var searchAddressAdapter: SearchAdapter

    // central point of our map
    private var centerLocation: LatLng? = null

    private lateinit var symbolManager: SymbolManager

    @SuppressLint("ClickableViewAccessibility")
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
                val point = address.geometry() as Point
                // create a new LatLng object
                val latLng = LatLng(point.latitude(), point.longitude())
                cvAddresses.visibility = View.GONE
                addMarker(latLng)
                moveCameraToLocation(latLng)
            }
        })

        rvAddressesList.apply {
            adapter = searchAddressAdapter
        }

        /** When the user clicks on edit text to start typing address, we need to get both
         * the central position the visible region of our map  */
        etAddress.setOnTouchListener { v, event ->
            if (MotionEvent.ACTION_UP == event.action) {
                centerLocation = mapboxMap?.cameraPosition?.target
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

                mainViewModel.fetchAddress(s.toString(), centerLocation)
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

        /** this observe will execute every time we have a response of our Places API request
         * If addresses list is empty cvAddresses will hide otherwise will be visible to the user
         * with the addresses found
         */
        mainViewModel.address.observe(this, Observer {
            cvAddresses.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
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
            initMarkerIconSymbolManager(it)
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
            moveCameraToLocation(location)
        }
    }

    private fun moveCameraToLocation(location: LatLng) {
        val position = CameraPosition.Builder()
            .target(location) // the location where to camera will move
            .zoom(10.0) // the zoom of our map
            .tilt(20.0) // title in degrees
            .build()
        // animateCamera method let us move the camera map. Needs to parameters
        // the new position of the camera and the millisecond that will
        mapboxMap?.animateCamera(CameraUpdateFactory.newCameraPosition(position), 3000)
    }

    private fun initMarkerIconSymbolManager(loadedMapStyle: Style) {
        // .addImage() is the methodo we use to set the image for our symbol
        loadedMapStyle.addImage(
            "marker_icon", BitmapFactory.decodeResource(
                this.resources, R.drawable.red_marker
            )
        )
        // sumboManager needs the mapView, the instance of our
        // mapbox map and the style that we chose for our map
        symbolManager = SymbolManager(mapView!!, mapboxMap!!, loadedMapStyle)

        // true, the icon will be visible even if it collides with other previously drawn symbols.
        symbolManager.iconAllowOverlap = true

        // true, other symbols can be visible even if they collide with the icon.
        symbolManager.iconIgnorePlacement = true
    }


    /** Adds a new Marker*/
    private fun addMarker(latLng: LatLng) {
        val symbolOptions = SymbolOptions()

        symbolOptions
            // set the location on which the marker will be set
            .withLatLng(latLng)
            // the image id
            .withIconImage("marker_icon")
            // the icon size
            .withIconSize(0.3f)
            // Offset distance of icon from its anchor
            .withIconOffset(arrayOf(0f, -7f))

        symbolManager.create(symbolOptions)
    }
}