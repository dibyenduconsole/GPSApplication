package com.app.gpsapplication

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.app.gpsapplication.service.LocationService
import com.app.gpsapplication.util.Util

class MainActivity : AppCompatActivity() {
    var mLocationService: LocationService = LocationService()
    lateinit var mServiceIntent: Intent
    lateinit var mActivity: Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mActivity = this@MainActivity


        if (!Util.isLocationEnabledOrNot(mActivity)) {
            Util.showAlertLocation(
                mActivity,
                getString(R.string.gps_enable),
                getString(R.string.please_turn_on_gps),
                getString(
                    R.string.ok
                )
            )
        }

        requestPermissionsSafely(
            arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION), 200
        )

        findViewById<TextView>(R.id.txtStartService).setOnClickListener {
            mLocationService = LocationService()
            mServiceIntent = Intent(this, mLocationService.javaClass)
            if (!Util.isMyServiceRunning(mLocationService.javaClass, mActivity)) {
                startService(mServiceIntent)
                Toast.makeText(
                    mActivity,
                    getString(R.string.service_start_successfully),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    mActivity,
                    getString(R.string.service_already_running),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun requestPermissionsSafely(
        permissions: Array<String>,
        requestCode: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions!!, requestCode)
        }
    }

    override fun onDestroy() {
        if (::mServiceIntent.isInitialized) {
            stopService(mServiceIntent)
        }
        super.onDestroy()
    }
}