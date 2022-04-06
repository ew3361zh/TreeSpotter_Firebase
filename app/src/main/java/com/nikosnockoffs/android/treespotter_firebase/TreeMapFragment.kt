package com.nikosnockoffs.android.treespotter_firebase

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.GeoPoint
import java.util.*
import kotlin.collections.ArrayList


private const val TAG = "TREE_MAP_FRAGMENT"

class TreeMapFragment : Fragment() {

    private lateinit var addTreeButton: FloatingActionButton

    // need to ask app if its been granted permission to user location
    private var locationPermissionGranted = false

    // move map to user's location but not do it again
    private var movedMapToUsersLocation = false

    // get user's location - requires adding dependency and Gradle sync after importing this
    private var fusedLocationProvider: FusedLocationProviderClient? = null

    // need reference to google map that's being displayed
    private var map: GoogleMap? = null

    // keep track of all trees marked on the map
    private val treeMarkers = mutableListOf<Marker>()

    // keep track of all the tree objects
    private var treeList = listOf<Tree>()

    private val treeViewModel: TreeViewModel by lazy {
        ViewModelProvider(requireActivity()).get(TreeViewModel::class.java)
    }

    // when this function gets called, the map is ready (downloaded and drawn)
    private val mapReadyCallback = OnMapReadyCallback { googleMap ->
        Log.d(TAG, "Google map ready")
        map = googleMap

        googleMap.setOnInfoWindowClickListener { marker ->
            val treeForMarker = marker.tag as Tree
            requestDeleteTree(treeForMarker)
        }
        updateMap()
    }

    private fun requestDeleteTree(tree: Tree) {
        AlertDialog.Builder(requireActivity())
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.confirm_delete_tree, tree.name))
            .setPositiveButton(android.R.string.ok) { dialog, id ->
                treeViewModel.deleteTree(tree)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, id ->
                // do nothing
            }
            .create()
            .show()

    }

    private fun updateMap() {
        // draw markers only when map is ready
        drawTrees()

        // draw blue dot at user's location
        // show no location msg if location permission not granted or if device location not enabled
        if (locationPermissionGranted) {
            if (!movedMapToUsersLocation) {
                moveMapToUserLocation()
            }
        }

    }

    private fun setAddTreeButtonEnabled(isEnabled: Boolean) {
        addTreeButton.isClickable = isEnabled
        addTreeButton.isEnabled = isEnabled

        if (isEnabled) {
            addTreeButton.backgroundTintList = AppCompatResources.getColorStateList(requireActivity(),
            android.R.color.holo_orange_light)
        } else {
            addTreeButton.backgroundTintList = AppCompatResources.getColorStateList(requireActivity(),
                android.R.color.darker_gray)
        }
    }

    private fun showSnackbar( message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
    }

    private fun requestLocationPermission() {
        // has user already granted permission?
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // if location permission already granted, turn on add tree button, and initialize locaiton provider
            locationPermissionGranted = true
            Log.d(TAG, "permission already granted")
            updateMap()
            setAddTreeButtonEnabled(true)
            fusedLocationProvider = LocationServices.getFusedLocationProviderClient(requireActivity())
        } else {
            // need to ask for permission
            val requestLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                // this creates the launcher to ask for permission to use location
                if (granted) {
                    Log.d(TAG, "User granted permission")
                    setAddTreeButtonEnabled(true)
                    locationPermissionGranted = true
                    fusedLocationProvider = LocationServices.getFusedLocationProviderClient(requireActivity())
                } else {
                    Log.d(TAG, "User did not grant permission")
                    setAddTreeButtonEnabled(false)
                    locationPermissionGranted = false
                    showSnackbar(getString(R.string.give_permission))
                }
                // want to call this updateMap function regardless of whether the user grants location permission
                updateMap()
            }
            // launching specific permission request
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveMapToUserLocation() {
        // two things to do before we can use map
        // one - make sure map is ready
        // two - users location being enabled and available to us

        if (map == null) {
            // map is a global variable storing ref to google map so if it's unavailable, can't do squat
            return
        }

        // only running this code if app's been granted permissions so supressing missing permissions strong warning
        if (locationPermissionGranted) {
            map?.isMyLocationEnabled = true
            map?.uiSettings?.isMyLocationButtonEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true

            fusedLocationProvider?.lastLocation?.addOnCompleteListener { getLocationTask ->
                val location = getLocationTask.result
                if (location != null) {
                    Log.d(TAG, "User's location $location")
                    val center = LatLng(location.latitude, location.longitude)
                    val zoomLevel = 8f // floating point number so need f
                    map?.moveCamera(CameraUpdateFactory.newLatLngZoom(center, zoomLevel))
                    movedMapToUsersLocation = true
                } else {
                    showSnackbar(getString(R.string.no_location))
                }
            }

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Constraint view that contains map fragment and floating action button
        val mainView = inflater.inflate(R.layout.fragment_tree_map, container, false)

        addTreeButton = mainView.findViewById(R.id.add_tree)
        addTreeButton.setOnClickListener {
            // add tree at user's location - if permission granted and available
            addTreeAtLocation()

        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment?
        mapFragment?.getMapAsync(mapReadyCallback)

        // disable add tree button until location is available
        setAddTreeButtonEnabled(false)

        // request user's permssion for app to access device location
        requestLocationPermission()

        // draw existing trees on map

        treeViewModel.latestTrees.observe(requireActivity()) { latestTrees ->
            treeList = latestTrees
            drawTrees()

        }

        return mainView
    }

    @SuppressLint("MissingPermission")
    private fun addTreeAtLocation() {

        // don't know where user is, can't add tree
        if (map == null) {
            return
        }
        if (fusedLocationProvider == null) {
            return
        }
        if(!locationPermissionGranted) {
            showSnackbar(getString(R.string.grant_location_permission))
            return
        }

        fusedLocationProvider?.lastLocation?.addOnCompleteListener(requireActivity()){ locationRequestTask ->
            val location = locationRequestTask.result
            if(location !=null) {
                getTreeName { treeName ->
                    val tree = Tree(
                        name = treeName,
                        dateSpotted = Date(),
                        location = GeoPoint(location.latitude, location.longitude)
                    )
                    treeViewModel.addTree(tree)

                    moveMapToUserLocation()
                    showSnackbar(getString(R.string.added_tree, treeName))
                }
            } else {
                showSnackbar(getString(R.string.no_location))
            }
        }
    }

    private fun drawTrees(){
        if (map==null) {
            return
        }

        for (marker in treeMarkers) {
            marker.remove() // remove markers before we readd them so we don't have duplicates
        }

        for (tree in treeList) {
            // make a mark of each tree
            // add tree to map
            tree.location?.let { geoPoint ->

                // check if tree is favorited
                val isFavorite = tree.favorite ?: false
                // based on value, set icon
                val iconId = if(isFavorite) R.drawable.filled_heart_small else R.drawable.tree_small

                // this will allow us only to work with non-null Geopoint trees
                val markerOptions = MarkerOptions()
                    .position(LatLng(geoPoint.latitude, geoPoint.longitude))
                    .title(tree.name)
                    .snippet("Spotted on ${tree.dateSpotted}")
                    .icon(BitmapDescriptorFactory.fromResource(iconId))

                map?.addMarker(markerOptions)?.also { marker ->
                    treeMarkers.add(marker)
                    marker.tag = tree
                }

            }
        }

    }

    private fun getTreeName( callback: (String) -> Unit ) {

        AlertDialog.Builder(requireActivity())
            .setTitle(getString(R.string.ask_tree_type))
            .setItems(R.array.tree_options) { dialog, which ->
                // put tree options into an array resource
                val treeOptions = resources.getStringArray(R.array.tree_options)
                // have selection of tree name as val based on index user selects from array
                val treePicked = treeOptions[which]
                callback(treePicked)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, id ->
                // pass
            }
            .create()
            .show()


    }

    companion object {
        @JvmStatic
        fun newInstance() = TreeMapFragment()
    }
}