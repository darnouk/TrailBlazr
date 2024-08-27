package com.example.trailblazr.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.Toast
import android.widget.ZoomControls
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trailblazr.R
import com.example.trailblazr.databinding.FragmentMapBinding
import com.example.trailblazr.model.Route
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var routeAdapter: RouteAdapter
    private var selectedRoute: String? = null  // Placeholder for the selected route

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Set up the map fragment
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Set up RecyclerView for paths
        setupRecyclerView()

        // Set up zoom controls
        setupZoomControls()

        // Handle rating submission
        setupRatingSubmission()

        // Test database connection and fetch routes
        lifecycleScope.launch {
            val isConnected = testDatabaseConnection()
            if (isConnected) {
                Log.d("MapFragment", "Connection successful")
                fetchRoutesFromDatabaseAndDisplay()
            } else {
                Log.e("MapFragment", "Connection failed")
            }
        }

        return root
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = binding.recyclerViewRoutes
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        routeAdapter = RouteAdapter(emptyList())
        recyclerView.adapter = routeAdapter
    }

    private fun setupZoomControls() {
        val zoomControls: ZoomControls = binding.zoomControls
        zoomControls.setOnZoomInClickListener {
            map.animateCamera(CameraUpdateFactory.zoomIn())
        }
        zoomControls.setOnZoomOutClickListener {
            map.animateCamera(CameraUpdateFactory.zoomOut())
        }
    }

    private fun setupRatingSubmission() {
        val ratingBar: RatingBar = binding.ratingBar
        val submitButton = binding.submitRatingButton

        submitButton.setOnClickListener {
            val rating = ratingBar.rating
            if (rating > 0 && selectedRoute != null) {
                lifecycleScope.launch {
                    val success = submitRatingToDatabase(rating)
                    if (success) {
                        Toast.makeText(requireContext(), "Rating submitted!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to submit rating.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Please select a route and rating.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun fetchRoutesFromDatabase(): List<Route> {
        return withContext(Dispatchers.IO) {
            val routes = mutableListOf<Route>()
            try {
                // JDBC connection to the database
                val url = "jdbc:postgresql://35.238.152.227:5432/trailblazr"
                val connection: Connection = DriverManager.getConnection(url, "postgres", "admin")
                val statement: Statement = connection.createStatement()
                val resultSet: ResultSet = statement.executeQuery("SELECT name, distance, biking_time, running_time, rating FROM routes")

                while (resultSet.next()) {
                    val name = resultSet.getString("name")
                    val distance = resultSet.getDouble("distance")
                    val bikingTime = resultSet.getString("biking_time")
                    val runningTime = resultSet.getString("running_time")
                    val rating = resultSet.getFloat("rating")

                    routes.add(Route(name, distance, bikingTime, runningTime, rating))
                }

                resultSet.close()
                statement.close()
                connection.close()
            } catch (e: SQLException) {
                Log.e("MapFragment", "SQL Error: ${e.message}")
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e("MapFragment", "General Error: ${e.message}")
                e.printStackTrace()
            }

            routes
        }
    }

    private suspend fun submitRatingToDatabase(rating: Float): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // JDBC connection to the database
                val url = "jdbc:postgresql://35.238.152.227:5432/trailblazr"
                val connection: Connection = DriverManager.getConnection(url, "postgres", "admin")
                val statement: Statement = connection.createStatement()

                // Use the dynamically selected route
                val currentRoute = selectedRoute ?: return@withContext false

                // Update the rating in the database
                statement.executeUpdate("UPDATE routes SET rating = $rating WHERE name = '$currentRoute'")

                statement.close()
                connection.close()
                true
            } catch (e: SQLException) {
                Log.e("MapFragment", "SQL Error: ${e.message}")
                e.printStackTrace()
                false
            } catch (e: Exception) {
                Log.e("MapFragment", "General Error: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    private suspend fun testDatabaseConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Attempt to establish a connection to the database
                val url = "jdbc:postgresql://35.238.152.227:5432/trailblazr"
                val connection: Connection = DriverManager.getConnection(url, "postgres", "admin")
                connection.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Default to Madison, Wisconsin
        val madison = LatLng(43.0731, -89.4012)
        map.addMarker(MarkerOptions().position(madison).title("Marker in Madison, WI"))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(madison, 10f)) // Adjust zoom level as needed

        // Check location permissions
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions if not granted
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Enable the location layer on the map
        map.isMyLocationEnabled = true

        // Get the last known location and center the map on it
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLocation = LatLng(location.latitude, location.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 6f)) // Adjust zoom level as needed
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
