package com.adriansaycon.rbit_cartester

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent.getActivity
import android.app.Service
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.IBinder
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
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.adriansaycon.rbit_cartester.data.ActionStatus
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
import kotlinx.android.synthetic.main.live_indicator.*
import org.w3c.dom.Text
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var testName: String
    private lateinit var trackerIntent: Intent
    private lateinit var lastLoc: Location

    // UI Objects
    private lateinit var fabNote : FloatingActionButton
    private lateinit var formWrap : ScrollView
    private lateinit var liveIndicator : ScrollView
    private lateinit var noteWrap : CardView
    private lateinit var noteContent : EditText

    private var lastNote : String? = null
    private var isNoteOpened : Boolean = false
    private var actionStatus : Int = 0

    /**
     * The filename for the stored required data (Class, Car, Student)
     */
    val savedRequiredDataFilename = "required_form_data_contents"
    val accessFineLocationRequestCode = 143
    val configLocInterval = TrackerService().configLocInterval

    var classVals = arrayListOf<Int>()
    var carVals   = arrayListOf<Int>()
    var studentVals = arrayListOf<Int>()

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        readyForm()
        liveIndicator = findViewById(R.id.indicatorView)
        formWrap = findViewById(R.id.formWrap)
        noteWrap = findViewById(R.id.noteWrap)
        noteContent = findViewById(R.id.noteContent)
        fabNote = findViewById(R.id.fabNote)
        fabNote.hide()

        noteWrap.visibility = View.GONE
        liveIndicator.visibility = View.GONE

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

        if (actionStatus != 0) {
            runStop(findViewById((R.id.fabSave)))
        }

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

        val stringContent = this.readInternalFile("required_form_data_contents")
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
            start.setOnClickListener {
                runStart(noteWrap)
            }
            // Pause FAB preparation
            val pause: FloatingActionButton = findViewById(R.id.fabPause)
            pause.setOnClickListener {
                runPause(noteWrap)
            }
            // Save FAB preparation
            val save: FloatingActionButton = findViewById(R.id.fabSave)
            save.setOnClickListener {
                runStop(noteWrap)
            }
            // FAB Open Notes area
            val note: FloatingActionButton = findViewById(R.id.fabNote)
            note.setOnClickListener {
                if (!isNoteOpened) {
                    formWrap.visibility = View.GONE
                    noteWrap.visibility = View.VISIBLE
                    isNoteOpened = true
                } else {
                    noteWrap.visibility = View.GONE
                    noteWrap.hideKeyboard()
                    isNoteOpened = false
                    if (actionStatus !== 10) {
                        formWrap.visibility = View.VISIBLE
                    }

                }
            }
            // Cancel Note
            val cancelNoteButton   : Button = findViewById(R.id.cancelNoteButton)
            cancelNoteButton.setOnClickListener {
                if (actionStatus === 0) {
                    formWrap.visibility = View.VISIBLE
                }
                noteWrap.visibility = View.GONE
                noteWrap.hideKeyboard()
                isNoteOpened = false
            }
            // Create Note
            val createNoteButton   : Button = findViewById(R.id.createNoteButton)
            createNoteButton.setOnClickListener {
                if (actionStatus === 0) {
                    formWrap.visibility = View.VISIBLE
                }
                lastNote = noteContent.text.toString()
                createNote()
                noteWrap.visibility = View.GONE
                noteWrap.hideKeyboard()
                noteContent.text = null
                isNoteOpened = false
                Snackbar.make(findViewById(R.id.fabStart), "Note saved. Sync to upload to server.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
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

    private fun createNote() {
        writeInternalFile(
            "$testName-notes",
            "{\"epoch\" :\" ${Calendar.getInstance().timeInMillis}\", " +
                    "\"note\" : \"${lastNote.toString()}\"},",
            Context.MODE_APPEND
        )
    }


    private fun startTrack() {
        val avgSpeedIndicator : TextView = findViewById(R.id.AvgSpeedValue)
        val dateIndicator : TextView = findViewById(R.id.dateTimeValue)

        locationCallback = object : LocationCallback() {
            @SuppressLint("SetTextI18n")
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {

                    // Clear note
                    if (lastNote !== null) {
                        lastNote = null
                    }

                    val myLoc = LatLng(location.latitude, location.longitude)

                    // Avg Speed calc
                    if (::lastLoc.isInitialized) {
                        var distanceInMeters = lastLoc.distanceTo(location) * 1000
                        var speed = distanceInMeters / ((configLocInterval / 60) / 60)
                        avgSpeedIndicator.text = "${(speed * 1000).roundToInt()} kmh "
                        var parser = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        parser.timeZone = TimeZone.getTimeZone("UTC")
                        dateIndicator.text = parser.format(Date(Calendar.getInstance().timeInMillis))
                    }

                    println("ADZ : LOCATIONS : RUNNING WOOOOT")
                    mMap.addMarker(MarkerOptions().position(myLoc).title("Test route"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(myLoc))
                    lastLoc = location
                }
            }
        }

        getLastLoc()
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

        if (actionStatus === 0) {
            // Write initial data for file
            this.writeInternalFile(
                testName ,
                "start{ " +
                        "\"date\" : \"$currentDate\", " +
                        "\"class_id\" : $selectedClass, " +
                        "\"car_id\" : $selectedCar, " +
                        "\"student_id\" : $selectedStudent, " +
                        "\"data\" : [",
                Context.MODE_APPEND
            )
        }

        trackerIntent = Intent(this, TrackerService::class.java)
        trackerIntent.also { intent ->
            intent.putExtra("testName", testName)
            intent.action = "startTracker"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                ContextCompat.startForegroundService(this, intent)
            }
            startTrack()
        }
        println("ADZ : LOCATION : ACTION : START")

        // UI changes
        val fabStart : FloatingActionButton = findViewById(R.id.fabStart)
        fabStart.isEnabled = false
        fabStart.hide()
        liveIndicator.visibility = View.VISIBLE
        formWrap.visibility = View.GONE
        fabNote.show()

        Snackbar.make(view, "Location tracker running.", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show()

        actionStatus = 10
    }

    private fun runPause(view : View) {
        if (actionStatus === 10) {

            trackerIntent.action = "pauseTracker"
            startService(trackerIntent)
            fusedLocationClient.removeLocationUpdates(locationCallback)

            val fabStart : FloatingActionButton = findViewById(R.id.fabStart)
            fabStart.isEnabled = true
            fabStart.show()

            Snackbar.make(view, "Location tracker paused.", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
            actionStatus = 5
        }
    }

    /**
     * After "Stop" the App should then save everything Offline and Prepare it for UploadActivity.
     * Also, there should be a Field for Notices always available.
     */
    private fun runStop(view : View) {
        if (actionStatus !== 0) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            trackerIntent.action = "stopTracker"
            startService(trackerIntent)
            fusedLocationClient.removeLocationUpdates(locationCallback)

            val notes = this.readInternalFile("$testName-notes")

            if (notes !== null) {
                // Close data file
                this.writeInternalFile(
                    testName ,
                    "],[$notes]}end",
                    Context.MODE_APPEND
                )
            } else {
                // Close data file
                this.writeInternalFile(
                    testName ,
                    "]}end",
                    Context.MODE_APPEND
                )
            }

            // UI Changes
            indicatorView.visibility = View.GONE
            noteWrap.visibility = View.GONE
            noteWrap.hideKeyboard()
            formWrap.visibility = View.VISIBLE
            isNoteOpened = false

            val fabStart : FloatingActionButton = findViewById(R.id.fabStart)
            fabStart.isEnabled = true
            fabStart.show()
            fabNote.hide()

            Snackbar.make(view, "Location tracker stopped. File saved.", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
            actionStatus = 0
        }
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
                if ("notes" !in it.name) {
                    println("ADZ : CONVERT : TRAINING_FILE : ${it.name}")
                    val fileString = this.readInternalFile(it.name)
                    println("ADZ : CONVERT : TRAINING_FILE : CONTENT $fileString")
                    client.uploadData(this, findViewById(R.id.fabStart), it.name, fileString.toString())
                }
            }
        }

        Snackbar.make(findViewById(R.id.fabStart), "Syncing data.", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show()
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
}
