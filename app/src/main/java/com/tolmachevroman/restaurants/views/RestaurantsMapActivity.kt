package com.tolmachevroman.restaurants.views

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AdapterView
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.tolmachevroman.restaurants.R
import com.tolmachevroman.restaurants.datasources.webservice.Error
import com.tolmachevroman.restaurants.datasources.webservice.Resource
import com.tolmachevroman.restaurants.models.restaurants.Restaurant
import com.tolmachevroman.restaurants.utils.Utils
import com.tolmachevroman.restaurants.viewmodels.RestaurantsViewModel
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_restaurants_map.*
import javax.inject.Inject

class RestaurantsMapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnMarkerClickListener, AdapterView.OnItemSelectedListener {

    val TAG = "MapsActivity"

    private lateinit var googleMap: GoogleMap
    private lateinit var restaurantsViewModel: RestaurantsViewModel
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var utils: Utils

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurants_map)

        //allow only after map has been loaded
        spinner.isEnabled = false

        restaurantsViewModel = ViewModelProviders.of(this, viewModelFactory).get(RestaurantsViewModel::class.java)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Log.d(TAG, "onItemSelected $position")
        restaurantsViewModel.cuisineInput.value = position
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(TAG, "onMapReady")
        this.googleMap = googleMap
        this.googleMap.setOnMapLoadedCallback(this)
        this.googleMap.setOnMarkerClickListener(this)
    }

    override fun onMapLoaded() {
        Log.d(TAG, "onMapLoadedCallback")

        restaurantsViewModel.restaurants
                .observe(this, Observer<Resource<List<Restaurant>>> { resource ->
                    when (resource?.status) {
                        Resource.Status.SUCCESS -> {
                            hideLoading()
                            if (resource.data != null && resource.data.isNotEmpty()) {
                                Log.d(TAG, "observer -> SUCCESS, ${resource.data.size} items")
                                showMarkers(resource.data)
                            }
                        }
                        Resource.Status.ERROR -> {
                            hideLoading()
                            if (resource.error != null) {
                                Log.d(TAG, "observer -> ERROR, ${resource.error}")
                                showErrorMessage(resource.error)
                            }
                        }
                        Resource.Status.LOADING -> {
                            showLoading()
                            Log.d(TAG, "observer -> LOADING")
                        }
                    }
                })


        spinner.onItemSelectedListener = this
        spinner.isEnabled = true

        //initialize query on the first time, get cached value on configuration change
        if (!restaurantsViewModel.initialized)
            restaurantsViewModel.cuisineInput.value = 0
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        return true
    }

    private fun constructMarkerOptions(restaurant: Restaurant): MarkerOptions {
        val point = LatLng(restaurant.lat, restaurant.lng)
        val icon = when (restaurant.cuisine) {
            1 -> BitmapDescriptorFactory.fromBitmap(utils.getBitmap(R.drawable.ic_peru))
            2 -> BitmapDescriptorFactory.fromBitmap(utils.getBitmap(R.drawable.ic_italy))
            3 -> BitmapDescriptorFactory.fromBitmap(utils.getBitmap(R.drawable.ic_chile))
            else -> null
        }
        return MarkerOptions().position(point).title(restaurant.name).icon(icon)
    }

    private fun showMarkers(restaurants: List<Restaurant>) {
        googleMap.clear()
        val builder = LatLngBounds.Builder()
        restaurants.map {
            Pair(it, googleMap.addMarker(constructMarkerOptions(it)))
        }.map {
            it.second.tag = it.first
            it.second
        }.map {
            builder.include(it.position)
        }
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 40))
    }

    private fun showErrorMessage(error: Error) {
        Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
    }

    private fun showLoading() {
        progressbar.visibility = VISIBLE
    }

    private fun hideLoading() {
        progressbar.visibility = GONE
    }
}
