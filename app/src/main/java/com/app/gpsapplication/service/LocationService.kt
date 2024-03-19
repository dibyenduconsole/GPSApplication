package com.app.gpsapplication.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.app.gpsapplication.LocationData
import com.app.gpsapplication.receiver.RestartBackgroundService
import com.google.android.gms.location.*
import com.google.firebase.Firebase
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class LocationService : Service() {
    //Private lateinit var database:DatabaseReference
    val database = FirebaseDatabase.getInstance().reference
    val timerTAG = "TimerData"
    val locationTAG = "LocationData"
    val userID = "9732097320"
    var counter = 0
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    private val TAG = "LocationService"

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) createNotificationChanel() else startForeground(
            1,
            Notification()
        )
        requestLocationUpdates()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChanel() {
        val NOTIFICATION_CHANNEL_ID = "com.getlocationbackground"
        val channelName = "Background Service"
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager =
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)
        val notificationBuilder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification: Notification = notificationBuilder.setOngoing(true)
            .setContentTitle("App is running count::" + counter)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startTimer()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stoptimertask()
        val broadcastIntent = Intent()
        broadcastIntent.action = "restartservice"
        broadcastIntent.setClass(this, RestartBackgroundService::class.java)
        this.sendBroadcast(broadcastIntent)
    }

    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    fun startTimer() {
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                var count = counter++
                if (latitude != 0.0 && longitude != 0.0) {
                    Log.d(
                        "Location::",
                        latitude.toString() + ":::" + longitude.toString() + "Count" +
                                count.toString()
                    )

                    sendDataToFirebase(timerTAG, LocationData(""+latitude, ""+longitude, getCurrentDateTime(), ""+System.currentTimeMillis()))
                }
            }
        }
        timer!!.schedule(
            timerTask,
            0,
            1000
        ) //1 * 60 * 1000 1 minute
    }

    fun stoptimertask() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest()
        request.setInterval(10000)
        request.setFastestInterval(5000)
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        val client: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)

        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permission == PackageManager.PERMISSION_GRANTED) { // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location: Location = locationResult.getLastLocation()
                    if (location != null) {
                        latitude = location.latitude
                        longitude = location.longitude
                        Log.d("Location Service", "location update $location")
                        sendDataToFirebase(locationTAG, LocationData(""+latitude, ""+longitude, getCurrentDateTime(), ""+System.currentTimeMillis()))
                    }
                }
            }, null)
        }
    }

    fun sendDataToFirebase(tag: String, locationData: LocationData){
        database?.let {ref->
            CoroutineScope(Dispatchers.IO).launch {
                val myRef = ref.child(userID).child(tag)
                myRef.push().setValue(locationData)
            }
        }

    }


    fun getCurrentDateTime():String{
        val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm a")
        val date = Date()
        val current = formatter.format(date)
        return ""+current
    }
}