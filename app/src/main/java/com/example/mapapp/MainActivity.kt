package com.example.mapapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.isGone
import by.kirich1409.viewbindingdelegate.viewBinding
import com.example.mapapp.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.layers.GeoObjectTapEvent
import com.yandex.mapkit.layers.GeoObjectTapListener
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.search.*
import java.util.*
import com.yandex.runtime.Error
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity(), GeoObjectTapListener, InputListener {

    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private var array = arrayListOf<String>()
    private val adapterPlace: AdapterPlace by lazy {
        AdapterPlace(array)
    }
    private var searchManager: SearchManager? = null
    private var searchSession: Session? = null
    private val binding: ActivityMainBinding by viewBinding()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapInit()
        setContentView(R.layout.activity_main)
        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()
        rvInit()
        mapClick()
        btnMe()
        etPlace()
    }

    //Adapter click
    private fun adapterClick(point: Point) {
        adapterPlace.onClick = {
            binding.rvNames.isGone = true
            array.clear()
            hideKeyboard()
            binding.mapview.map.move(
                CameraPosition(Point(point.latitude, point.longitude),
                    17.0f,
                    0.0f,
                    0.0f),
                Animation(Animation.Type.SMOOTH, 1F),
                null)
            binding.etNamePlace.text = null
        }
    }

    //Hide keyboard
    private fun hideKeyboard() {
        val v = this.currentFocus
        val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        assert(v != null)
        imm.hideSoftInputFromWindow(v!!.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    //Add map click
    private fun mapClick() {
        binding.mapview.map.addTapListener(this)
        binding.mapview.map.addInputListener(this)
    }

    //Init recyclerview
    private fun rvInit() {
        binding.rvNames.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@MainActivity.adapterPlace
        }
    }

    //Edit text with textWatcher
    private fun etPlace() {
        binding.etNamePlace.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                array.clear()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                array.clear()
                if (s.isNullOrEmpty()) {
                    binding.etNamePlace.hint = "Введите название улицы/дома/района"
                    binding.rvNames.isGone = true
                } else {
                    Log.e("TAG", s.toString())
                    requestSearch(s.toString())
                    binding.rvNames.isGone = false
                }
            }
        })
    }

    //Btn click
    private fun btnMe() {
        binding.fab.setOnClickListener {
            getCurrentLocation()
        }
    }

    //Map initialize
    private fun mapInit() {
        MapKitFactory.setApiKey(BuildConfig.MAP_API_KEY)
        MapKitFactory.initialize(this)
    }

    //Get location and check permission for locality
    private fun getCurrentLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                fusedLocationProvider.lastLocation.addOnCompleteListener(this) {
                    val location: Location? = it.result
                    if (location == null) {
                        Toast.makeText(this, "Failure", Toast.LENGTH_SHORT).show()
                    } else {
                        binding.mapview.map.move(
                            CameraPosition(Point(location.latitude, location.longitude),
                                17.0f,
                                0.0f,
                                0.0f),
                            Animation(Animation.Type.SMOOTH, 1F),
                            null)
                    }
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermission()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION),
            BuildConfig.PERMISSION_REQUEST_ACCESS_LOCATION.toInt()
        )
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BuildConfig.PERMISSION_REQUEST_ACCESS_LOCATION.toInt()) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            }
        } else {
            Toast.makeText(this, "Failure", Toast.LENGTH_SHORT).show()
        }
    }

    //Work with lifecycle
    override fun onStop() {
        super.onStop()
        mapInit()
        binding.mapview.onStop()
        MapKitFactory.getInstance().onStop()
    }

    override fun onStart() {
        super.onStart()
        binding.mapview.onStart()
        MapKitFactory.getInstance().onStart()
    }

    //Object tap (it's not working for street's)
    override fun onObjectTap(geoObjectTapEvent: GeoObjectTapEvent): Boolean {
        when {
            geoObjectTapEvent.geoObject.name?.isNotBlank() == true -> {
                binding.etNamePlace.text = null
                binding.etNamePlace.hint = geoObjectTapEvent.geoObject.name
            }
            else -> {
                binding.etNamePlace.text = null
                binding.etNamePlace.hint = "Введите название улицы/дома/района"
            }
        }
        return true
    }

    //Search request with name of place
    private fun requestSearch(query: String) {
        searchManager = SearchFactory.getInstance().createSearchManager(
            SearchManagerType.ONLINE
        )
        //Your point
        val point = Geometry.fromPoint(Point(42.87, 74.59))

        searchSession = searchManager!!.submit(query, point, SearchOptions(),
            object : Session.SearchListener {
                override fun onSearchError(error: Error) {
                    Toast.makeText(this@MainActivity, "Try again", Toast.LENGTH_SHORT)
                        .show()
                }

                @SuppressLint("NotifyDataSetChanged")
                override fun onSearchResponse(response: Response) {
                    array.clear()
                    val all = response.collection.children.firstOrNull()?.obj
                        ?.metadataContainer
                        ?.getItem(ToponymObjectMetadata::class.java)
                        ?.address
                        ?.formattedAddress

                    if (all != null) {
                        array.add(all)
                    }

                    adapterPlace.notifyDataSetChanged()

                    for (searchResult in response.collection.children) {
                        val resultLocation = searchResult.obj!!.geometry[0].point
                        if (resultLocation != null) {
                            adapterClick(resultLocation)
                        }
                    }
                }
            })
    }

    //On map tap (it's working for street's)
    override fun onMapTap(map: Map, point: Point) {
        pointToName(point)
    }

    override fun onMapLongTap(map: Map, point: Point) {
    }

    //Function that convert point to name
    private fun pointToName(point: Point?) {
        searchManager = SearchFactory.getInstance().createSearchManager(
            SearchManagerType.ONLINE
        )
        searchSession =
            point?.let { it ->
                searchManager!!.submit(
                    it,
                    20,
                    SearchOptions(),
                    object : Session.SearchListener {
                        override fun onSearchError(error: Error) {
                            Toast.makeText(this@MainActivity, "Try again", Toast.LENGTH_SHORT)
                                .show()
                        }

                        override fun onSearchResponse(response: Response) {
                            val all = response.collection.children.firstOrNull()?.obj
                                ?.metadataContainer
                                ?.getItem(ToponymObjectMetadata::class.java)
                                ?.address
                                ?.formattedAddress

                            if (all != null) {
                                binding.etNamePlace.hint = all
                            }
                        }
                    })
            }
    }

}
