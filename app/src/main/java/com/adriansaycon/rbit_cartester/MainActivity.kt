package com.adriansaycon.rbit_cartester

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
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
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.adriansaycon.rbit_cartester.data.LoginDataSource
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
import java.io.File
import java.io.InputStreamReader
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var testName: String
    private lateinit var lastLoc: Location

    /**
     * The filename for the stored required data (Class, Car, Student)
     */
    val savedRequiredDataFilename = "required_form_data_contents"
    val accessFineLocationRequestCode = 143
    val configLocInterval : Long = 5000

    var classVals = arrayListOf<Int>()
    var carVals   = arrayListOf<Int>()
    var studentVals = arrayListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        readyForm()
        val liveIndicator : ScrollView = findViewById(R.id.indicatorView)
        liveIndicator.visibility = View.INVISIBLE

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
//
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.main, menu)
//        return true
//    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        return when (item.itemId) {
//            R.id.action_settings -> true
//            else -> super.onOptionsItemSelected(item)
//        }
//    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_dashboard -> {
                // Handle the camera action
                Log.d("adz", "dashboard")
            }
            R.id.nav_sync -> {
                runSync()
            }
            R.id.nav_logout -> {
                val source = LoginDataSource()
                source.logout()
                intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun readyForm(): Boolean {
        val gson = Gson()

        var result : Required
        val file : File = this.getFileStreamPath(savedRequiredDataFilename)
        if(!file.exists()) {
            val client = Client()
            client.getRequiredData(this, findViewById(R.id.appCoordinatorLayout))
            return false
        }

        val stringContent = readInternalFile("required_form_data_contents")
        result = gson.fromJson(stringContent.toString(), Required::class.java)

        // Class preparation
        val classList = arrayListOf<String>()
        result.classes.forEach {
            classList.add(it.name)
            classVals.add(it.id)
        }

        val spinnerClass: Spinner = findViewById(R.id.spinnerClass)
        val aaClass = ArrayAdapter(this, android.R.layout.simple_spinner_item, classList)
        aaClass.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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
        spinnerCar.adapter = aaCar
        // Car preparation - END

        // Student preparation
        val studentList = arrayListOf<String>()
        result.users.forEach {
            if (it.name == null) {
                studentList.add("UID: ${it.userId}")
            } else {
                studentList.add(it.name)
            }
            studentVals.add(it.userId)
        }

        val spinnerStudent: Spinner = findViewById(R.id.spinnerStudent)
        val aaStudent = ArrayAdapter(this, android.R.layout.simple_spinner_item, studentList)
        aaStudent.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStudent.adapter = aaStudent
        // Student preparation - END

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
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
        } else {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    accessFineLocationRequestCode)

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            accessFineLocationRequestCode -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    println("ADZ : wow, location permission approved.")
                    readyForm()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    println("ADZ : wow, location permission denied. Good for you!")
                    Snackbar.make(findViewById(R.id.fabStart), "Well, we can't do anything now.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
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

//        println("ADZ FILE_CONTENT : $sb")
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

    @SuppressLint("MissingPermission")
    private fun getLastLoc() {
        println("ADZ : this.getLastLoc()")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest()
        request.interval = configLocInterval
        request.fastestInterval = configLocInterval
        request.priority = PRIORITY_HIGH_ACCURACY
        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            null /* Looper */
        )
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val spinnerClass : Spinner = findViewById(R.id.spinnerClass)
        val spinnerCar   : Spinner = findViewById(R.id.spinnerCar)
        val spinnerStudent : Spinner = findViewById(R.id.spinnerStudent)

        val selectedClass   = this.classVals[spinnerClass.selectedItemId.toInt()]
        val selectedCar     = this.carVals[spinnerCar.selectedItemId.toInt()]
        val selectedStudent = this.studentVals[spinnerStudent.selectedItemId.toInt()]

        testName = "training-$selectedClass-$selectedCar-$selectedStudent"
        val currentDate: String = DateFormat.getDateInstance().format(Calendar.getInstance().time)
        val epoch = Calendar.getInstance().timeInMillis
        val dateIndicator : TextView = findViewById(R.id.dateTimeValue)
        val avgSpeedIndicator : TextView = findViewById(R.id.AvgSpeedValue)

        // Write initial data for file
        writeInternalFile(
            testName ,
            "start{ " +
                        "\"date\" : \"$currentDate\"" +
                        "\"class_id\" : $selectedClass, " +
                        "\"car_id\" : $selectedCar, " +
                        "\"student_id\" : $selectedStudent, " +
                        "\"data\" : [",
            Context.MODE_APPEND
        )

        // Location management
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    println("ADZ LOCATION : ${location.longitude} : ${location.latitude}")
                    writeInternalFile(
                        testName ,
                        "{ " +
                                    "\"UTC\" : $epoch" +
                                    "\"longitude\" : ${location.longitude}, " +
                                    "\"latitude\" : ${location.latitude} }, " ,
                        Context.MODE_APPEND
                    )

                    val myLoc = LatLng(location.latitude, location.longitude)

                    // Avg Speed calc
                    if (::lastLoc.isInitialized) {
                        var distanceInMeters = lastLoc.distanceTo(location)
                        var speed = distanceInMeters / configLocInterval
                        avgSpeedIndicator.text = "${speed * 1000}kmh "
                        var parser = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        parser.timeZone = TimeZone.getTimeZone("UTC")
                        dateIndicator.text = "${parser.format(Date(epoch))}"
                    }

                    mMap.addMarker(MarkerOptions().position(myLoc).title("Test route"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(myLoc))
                    lastLoc = location
                }
            }
        }
        // Location tracker
        getLastLoc()

        // UI changes
        val fabStart : FloatingActionButton = findViewById(R.id.fabStart)
        fabStart.isEnabled = false
        fabStart.hide()
        val formWrap : ScrollView = findViewById(R.id.formWrap)
        formWrap.visibility = View.INVISIBLE
        Snackbar.make(view, "Location tracker running.", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show()
        val indicatorWrap : ScrollView = findViewById(R.id.indicatorView)
        indicatorWrap.visibility = View.VISIBLE
    }

    private fun runPause(view : View) {
        fusedLocationClient.removeLocationUpdates(locationCallback)

        val fabStart : FloatingActionButton = findViewById(R.id.fabStart)
        fabStart.isEnabled = true
        fabStart.show()

        Snackbar.make(view, "Location tracker paused.", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show()
    }

    /**
     * After "Stop" the App should then save everything Offline and Prepare it for UploadActivity.
     * Also, there should be a Field for Notices always available.
     */
    private fun runStop(view : View) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // Close data file
        writeInternalFile(
            testName ,
            "]}end",
            Context.MODE_APPEND
        )

//        readInternalFile(testName)

        val formWrap : ScrollView = findViewById(R.id.formWrap)
        formWrap.visibility = View.VISIBLE
        val indicatorWrap : ScrollView = findViewById(R.id.indicatorView)
        indicatorWrap.visibility = View.INVISIBLE
        val fabStart : FloatingActionButton = findViewById(R.id.fabStart)
        fabStart.isEnabled = true
        fabStart.show()

        Snackbar.make(view, "Location tracker stopped. File saved.", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show()
    }

    /**
     * Syncs app from/to backend data
     */
    private fun runSync() {
        val client = Client()
        // Download required server data / Sync from server
        client.getRequiredData(this, findViewById(R.id.appCoordinatorLayout))

        // Upload cached data / Sync to server
        val dir = this.filesDir
        dir.listFiles().forEach {
            if ("training" in it.name) {
                println("ADZ : CONVERT : TRAINING_FILE : ${it.name}")
                val fileString = readInternalFile(it.name)
                println("ADZ : CONVERT : TRAINING_FILE : CONTENT $fileString")
                client.uploadData(this, findViewById(R.id.fabStart), it.name, fileString.toString())
            }
        }

        Snackbar.make(findViewById(R.id.fabStart), "Syncing data.", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show()
    }
}
