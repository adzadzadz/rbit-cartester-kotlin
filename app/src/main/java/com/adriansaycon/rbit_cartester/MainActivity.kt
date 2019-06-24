package com.adriansaycon.rbit_cartester

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import android.view.MenuItem
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.View
import android.widget.Spinner
import androidx.core.content.ContextCompat
import com.adriansaycon.rbit_cartester.rest.Client
import com.adriansaycon.rbit_cartester.rest.data.Car
import com.adriansaycon.rbit_cartester.rest.data.Class
import com.adriansaycon.rbit_cartester.rest.data.User
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.extensions.CacheImplementation
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    var classVals = arrayListOf<Int>()
    var carVals   = arrayListOf<Int>()
    var studentVals = arrayListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Prepare data required for car testing
        readyForm()

        // Map init - Start
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        // Map init - End

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_dashboard -> {
                // Handle the camera action
                Log.d("adz", "dashboard")
            }
            R.id.nav_sync -> {
                Log.d("adz", "profile")
                Snackbar.make(findViewById(R.id.fabStart), "Should store data offline.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            R.id.nav_login -> {
                Log.d("adz", "login")
            }
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun readyForm() {
        val client = Client()
        client.getRequiredData(this, findViewById(R.id.appCoordinatorLayout))

        // Start FAB preparation
        val start: FloatingActionButton = findViewById(R.id.fabStart)
        start.setOnClickListener { view ->
            val spinnerClass : Spinner = findViewById(R.id.spinnerClass)
            val spinnerCar   : Spinner = findViewById(R.id.spinnerCar)
            val spinnerStudent : Spinner = findViewById(R.id.spinnerStudent)

            val selectedClass   = this.classVals[spinnerClass.selectedItemId.toInt()]
            val selectedCar     = this.carVals[spinnerCar.selectedItemId.toInt()]
            val selectedStudent = this.studentVals[spinnerStudent.selectedItemId.toInt()]

            getLastLoc()

//            writeInternalFile(selectedClass, selectedCar, selectedStudent)

            Snackbar.make(view, "Values: class? $selectedClass, car? $selectedCar, student? $selectedStudent", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        // Start FAB preparation - END

        // Pause FAB preparation
        val pause: FloatingActionButton = findViewById(R.id.fabPause)
        pause.setOnClickListener { view ->
            val spinnerClass : Spinner = findViewById(R.id.spinnerClass)
            Snackbar.make(view, "Selected value:  ${spinnerClass.selectedItem}, id? ${spinnerClass.selectedItemId}", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        // Pause FAB preparation - END

        // Save FAB preparation
        val save: FloatingActionButton = findViewById(R.id.fabSave)
        save.setOnClickListener { view ->
            val spinnerClass : Spinner = findViewById(R.id.spinnerClass)
            Snackbar.make(view, "Selected value:  ${spinnerClass.selectedItem}, id? ${spinnerClass.selectedItemId}", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        // Save FAB preparation - END
    }

    private fun writeInternalFile(testerClass : Int, car : Int, student : Int) {
        val filename = "$testerClass-$car-$student-training"
        val dateTime = java.util.Calendar.getInstance()
        val fileContents = "${dateTime.time} - Start location tracker"
        this.openFileOutput(filename, Context.MODE_PRIVATE).use {
            it.write(fileContents.toByteArray())
        }

        val fis = openFileInput(filename)
        val isr = InputStreamReader(fis)
        val bufferedReader = BufferedReader(isr)
        val sb = StringBuilder()
        var line : String?

        do {
            var isNotNull = false
            line = bufferedReader.readLine()
            if (line !== null) {
                isNotNull = true
                sb.append(line);
            }

        } while (isNotNull)


        println("ADZ FILE_CONTENT : $sb")
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        println("Adz: Works well my lad.");

        // Add a marker in Sydney and move the camera
        val myLoc = LatLng(9.51565785, 123.15609796800399)
        mMap.addMarker(MarkerOptions().position(myLoc).title("Current Loc Marker"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(myLoc))



    }

    private fun getLastLoc() {
        println("ADZ : this.getLastLoc()")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            println("ADZ : Permission Granted?")
            val mainHandler = Handler(Looper.getMainLooper())

            val thisActivity = this
            mainHandler.post(object : Runnable {
                @SuppressLint("MissingPermission")
                override fun run() {
                    fusedLocationClient = LocationServices.getFusedLocationProviderClient(thisActivity)
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { location : Location? ->
                            // Got last known location. In some rare situations this can be null.
                            println("ADZ Location : ${location?.latitude} , ${location?.longitude}")
                        }

                    mainHandler.postDelayed(this, 5000)
                }
            })
        } else {
            println("ADZ : ACCESS_FINE_LOCATION not permitted")
        }
    }


    /**
     * Quick Selection of "Person; Car; Tester; Class; Location of the Test"
     * At the Bottom of the Menu needs to be a "Start; Stop; Pause" Button. (Two Buttons to
     * be exact)
     * If the App is in Wi-Fi it should then start to upload the Offline Cache into a Database.
     */
    /**
     * After "Start" the App needs to automatically write down the GPS Position every few
     * seconds, including GPS Speed and Time with Date.
     */
    fun runStart() {

    }

    fun runPause() {

    }

    /**
     * After "Stop" the App should then save everything Offline and Prepare it for Upload.
     * Also, there should be a Field for Notices always available.
     */
    fun runStop() {

    }
}
