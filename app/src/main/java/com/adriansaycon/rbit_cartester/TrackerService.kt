package com.adriansaycon.rbit_cartester

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.text.SimpleDateFormat
import java.util.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.os.Bundle
import android.os.Parcelable
import android.os.Build
import android.R
import android.annotation.TargetApi
import android.app.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN


class TrackerService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var lastLoc: Location

    val channelId = "ForegroundServiceChannel"
    val configLocInterval : Long = 5000

    override fun onBind(p0: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    // @TargetApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        var status = 0
        if (intent.action == "stopTracker") {
            println("ADZ : TRACKER : STOPPING")
            stopForeground(true)
            stopTrack()
            status = 0
            stopSelf()
        } else if(intent.action == "pauseTracker") {
            println("ADZ : TRACKER : PAUSING")
            stopForeground(true)
            status = 5
            stopTrack()
        } else if(intent.action == "startTracker") {
            val channelId =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel(channelId, "My Background Service")
                } else {
                    // If earlier version channel ID is not used
                    // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                    ""
                }

            val notificationBuilder = NotificationCompat.Builder(this, channelId )
            val notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.btn_star)
                .setPriority(PRIORITY_MIN)
                .setContentTitle("Car Tester")
                .setContentText("Background tracker running...")
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
            startForeground(101, notification)
            status = 10
            println("ADZ : TRACKER : STARTING")
            startTrack(intent)
        }
        println("ADZ : onStartCommand : WORKS!")
        return START_STICKY
    }

    private fun stopTrack() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun startTrack(intent : Intent) {
        val thisService = this
        // Location management
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){

                    thisService.writeInternalFile(
                        intent.getStringExtra("testName") ,
                        "{ " +
                                "\"epoch\" : \"${Calendar.getInstance().timeInMillis}\", " +
                                "\"longitude\" : ${location.longitude}, " +
                                "\"latitude\" : ${location.latitude} },",
                        Context.MODE_APPEND
                    )
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
        request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            null /* Looper */
        )
    }

}
