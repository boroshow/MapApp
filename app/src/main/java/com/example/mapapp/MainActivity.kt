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
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.layers.GeoObjectTapEvent
import com.yandex.mapkit.layers.GeoObjectTapListener
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.search.*
import java.util.*
import com.yandex.runtime.Error

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
        }
    }

    private fun hideKeyboard(){
        val v = this.currentFocus
        val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        assert(v != null)
        imm.hideSoftInputFromWindow(v!!.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    private fun mapClick() {
        binding.mapview.map.addTapListener(this)
        binding.mapview.map.addInputListener(this)
    }

    private fun rvInit() {
        binding.rvNames.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@MainActivity.adapterPlace
        }
    }

    private fun etPlace() {
        binding.etNamePlace.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                array.clear()
                if (binding.etNamePlace.text.isNotEmpty()) {
                    requestSearch(binding.etNamePlace.text.toString())
                    binding.rvNames.isGone = false
                } else {
                    binding.rvNames.isGone = true
                    binding.etNamePlace.hint = "Введите название города/страны/места"
                }
            }
        })
    }

    private fun btnMe() {
        binding.fab.setOnClickListener {
            getCurrentLocation()
        }
    }

    private fun mapInit() {
        MapKitFactory.setApiKey(BuildConfig.MAP_API_KEY)
        MapKitFactory.initialize(this)
    }

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

    override fun onObjectTap(geoObjectTapEvent: GeoObjectTapEvent): Boolean {
        when {
            geoObjectTapEvent.geoObject.name?.isNotBlank() == true -> {
                binding.etNamePlace.hint = geoObjectTapEvent.geoObject.name
            }
            geoObjectTapEvent.geoObject.descriptionText?.isNotBlank() == true -> {
                binding.etNamePlace.hint = geoObjectTapEvent.geoObject.descriptionText
            }
            else -> {
                binding.etNamePlace.hint = "Введите название города/страны/места"
            }

        }
        return true
    }

    private fun requestSearch(query: String) {
        searchManager = SearchFactory.getInstance().createSearchManager(
            SearchManagerType.ONLINE
        )
        val point = Geometry.fromPoint(Point(42.87, 74.59))
        searchSession = searchManager!!.submit(query, point, SearchOptions(),
            object : Session.SearchListener {
                override fun onSearchError(error: Error) {
                    Log.e("TAG", "Error")
                }

                @SuppressLint("NotifyDataSetChanged")
                override fun onSearchResponse(response: Response) {
                    Log.e("TAG", query)

                    val city = response.collection.children.firstOrNull()?.obj
                        ?.metadataContainer
                        ?.getItem(ToponymObjectMetadata::class.java)
                        ?.address
                        ?.components
                        ?.firstOrNull { it.kinds.contains(Address.Component.Kind.LOCALITY) }
                        ?.name

                    val street = response.collection.children.firstOrNull()?.obj
                        ?.metadataContainer
                        ?.getItem(ToponymObjectMetadata::class.java)
                        ?.address
                        ?.components
                        ?.firstOrNull { it.kinds.contains(Address.Component.Kind.STREET) }
                        ?.name

                    val district = response.collection.children.firstOrNull()?.obj
                        ?.metadataContainer
                        ?.getItem(ToponymObjectMetadata::class.java)
                        ?.address
                        ?.components
                        ?.firstOrNull { it.kinds.contains(Address.Component.Kind.DISTRICT) }
                        ?.name

                    if (street != null) {
                        array.add("$city $street")
                    }
                    if (district != null) {
                        array.add(district)
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

    override fun onMapTap(map: Map, point: Point) {
        pointToName(point)
    }

    private fun pointToName(point: Point?) {
        if (point != null) {
        } else {
            Toast.makeText(this, "Point is null", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapLongTap(p0: Map, p1: Point) {
    }

}