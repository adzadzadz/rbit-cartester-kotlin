package com.adriansaycon.rbit_cartester

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.Spinner
import androidx.core.content.ContextCompat
import com.adriansaycon.rbit_cartester.rest.Client
import com.adriansaycon.rbit_cartester.rest.data.Required
import com.adriansaycon.rbit_cartester.ui.login.LoginActivity
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var testName: String

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
                val client = Client()
                client.getRequiredData(this, findViewById(R.id.appCoordinatorLayout))
                Snackbar.make(findViewById(R.id.fabStart), "Syncing data.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            R.id.nav_logout -> {


                intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun readyForm() {
        val gson = Gson()
        val stringContent = readInternalFile("required_form_data_contents")
        val result : Required = gson.fromJson(stringContent.toString(), Required::class.java)

        // Class preparation
        val classList = arrayListOf<String>()
        result.classes.forEach {
            classList.add(it.name)
            classVals.add(it.id)
        }

        val spinnerClass: Spinner = findViewById(R.id.spinnerClass)
        val aaClass = ArrayAdapter(this, android.R.layout.simple_spinner_item, classList)
        aaClass.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerClass.prompt = "Select Class"
        spinnerClass.adapter = aaClass
        // Class preparation - END

        // Car preparation
        val carList = arrayListOf<String>()
        result.cars.forEach {
            carList.add(it.name)
            carVals.add(it.id)
        }

        val spinnerCar: Spinner = findViewById(R.id.spinnerCar)
        val aaCar = ArrayAdapter(this, android.R.layout.simple_spinner_item, carList)
        aaCar.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerCar.prompt = "Select Class"
        spinnerCar.adapter = aaCar
        // Car preparation - END

        // Student preparation
        val studentList = arrayListOf<String>()
        result.users.forEach {
            studentList.add(it.name)
            studentVals.add(it.userId)
        }

        val spinnerStudent: Spinner = findViewById(R.id.spinnerStudent)
        val aaStudent = ArrayAdapter(this, android.R.layout.simple_spinner_item, studentList)
        aaCar.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerStudent.prompt = "Select Class"
        spinnerStudent.adapter = aaStudent
        // Student preparation - END

        // Start FAB preparation
        val start: FloatingActionButton = findViewById(R.id.fabStart)
        start.setOnClickListener { view ->
            runStart(view)
        }
        // Pause FAB preparation
        val pause: FloatingActionButton = findViewById(R.id.fabPause)
        pause.setOnClickListener { view ->
            runPause(view)
        }
        // Save FAB preparation
        val save: FloatingActionButton = findViewById(R.id.fabSave)
        save.setOnClickListener { view ->
            runStop(view)

        }
    }

    fun writeInternalFile(filename: String?, content : String, mode : Int) {
        this.openFileOutput(filename, mode).use {
            it.write(content.toByteArray())

        }
    }

    fun readInternalFile(filename : String?): StringBuilder {
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
        return sb
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
    }

    private fun getLastLoc() {
        println("ADZ : this.getLastLoc()")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            val request = LocationRequest()
            request.interval = 5000
            request.fastestInterval = 5000
            request.priority = PRIORITY_HIGH_ACCURACY
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                null /* Looper */
            )


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
    private fun runStart(view : View) {
        val spinnerClass : Spinner = findViewById(R.id.spinnerClass)
        val spinnerCar   : Spinner = findViewById(R.id.spinnerCar)
        val spinnerStudent : Spinner = findViewById(R.id.spinnerStudent)

        val selectedClass   = this.classVals[spinnerClass.selectedItemId.toInt()]
        val selectedCar     = this.carVals[spinnerCar.selectedItemId.toInt()]
        val selectedStudent = this.studentVals[spinnerStudent.selectedItemId.toInt()]

        testName = "$selectedClass-$selectedCar-$selectedStudent-training"

        // Location management
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    println("ADZ LOCATION : ${location.longitude} : ${location.latitude}")
                    writeInternalFile(
                        testName ,
                        "{longitude : ${location.longitude}, latitude : ${location.latitude}}",
                        Context.MODE_APPEND
                    )

                    val myLoc = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(MarkerOptions().position(myLoc).title("Test route"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(myLoc))
                }
            }
        }
        // Location tracker
        getLastLoc()

        // UI changes
        val fabStart : FloatingActionButton = findViewById(R.id.fabStart)
        fabStart.isEnabled = false
        val formWrap : ScrollView = findViewById(R.id.formWrap)
        formWrap.visibility = View.INVISIBLE
        Snackbar.make(view, "Location tracker running.", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show()
    }

    private fun runPause(view : View) {
        fusedLocationClient.removeLocationUpdates(locationCallback)

        val fabStart : FloatingActionButton = findViewById(R.id.fabStart)
        fabStart.isEnabled = true

        Snackbar.make(view, "Location tracker paused.", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show()
    }

    /**
     * After "Stop" the App should then save everything Offline and Prepare it for Upload.
     * Also, there should be a Field for Notices always available.
     */
    private fun runStop(view : View) {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        readInternalFile(testName)

        val formWrap : ScrollView = findViewById(R.id.formWrap)
        formWrap.visibility = View.VISIBLE

        Snackbar.make(view, "Location tracker stopped. File saved.", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show()
    }
}
