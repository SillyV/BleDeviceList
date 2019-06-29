package com.vasili.orcamhomeassignment

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.HashSet


class MainActivity : AppCompatActivity(), BleManager.OnBleManagerCallback {

    companion object {
        const val LOCATION_PERMISSION_REQUEST = 102
        const val REQUEST_ENABLE_LOCATION = 103
        const val REQUEST_ENABLE_BLUETOOTH = 101
        const val DISCOVERY_ACTIVE = "discovery.active"
        const val FOUND_DEVICES = "found.devices"
        private val permissions = arrayOf(
                ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION
        )
    }

    private val bleManager: BleManager by lazy { BleManager(this) }

    private val bleDeviceAdapter = BleDeviceAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViewElements()
        bleManager.setListener(this)
        savedInstanceState ?: safeStartDiscovery()
    }

    private fun safeStartDiscovery() {
        if (verifyLocationEnabled() && (
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                                verifyPermissionsGranted())
        ) {
            bleManager.startDiscovery()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(DISCOVERY_ACTIVE, bleManager.active)
        outState.putParcelableArrayList(FOUND_DEVICES, ArrayList(bleManager.foundDevices))
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        restoreActiveState(savedInstanceState)
        restoreDeviceList(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            when (resultCode) {
                Activity.RESULT_OK -> safeStartDiscovery()
                Activity.RESULT_CANCELED -> setDisabled()
            }
        } else if (requestCode == REQUEST_ENABLE_LOCATION) {
            safeStartDiscovery()
        }
    }

    override fun onDestroy() {
        bleManager.stopDiscovery()
        super.onDestroy()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            val permissionsGranted = verifyResponsePermissionsGranted(grantResults, permissions)
            if (permissionsGranted) safeStartDiscovery()
        }
    }

    override fun onBluetoothNotAvailable() {
        val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
    }

    override fun onDiscoveryUpdate(devices: List<BleDeviceItem>) {
        bleDeviceAdapter.submitList(devices)
    }

    override fun onDiscoveryStop() {
        progressbar_scanning.visibility = View.GONE
        textview_scan.setText(R.string.main_start_scan)
        textview_scan.setOnClickListener { safeStartDiscovery() }
    }


    override fun onDiscoveryStart() {
        progressbar_scanning.visibility = View.VISIBLE
        textview_scan.setText(R.string.main_stop_scan)
        textview_scan.setOnClickListener { bleManager.stopDiscovery() }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun requestMultiplePermissions() {
        val remainingPermissions: MutableList<String> = ArrayList()
        for (permission in permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                remainingPermissions.add(permission)
            }
        }
        requestPermissions(remainingPermissions.toTypedArray(), LOCATION_PERMISSION_REQUEST)
    }



    @RequiresApi(Build.VERSION_CODES.M)
    private fun verifyResponsePermissionsGranted(grantResults: IntArray, permissions: Array<String>): Boolean {
        var granted = true
        for (i in grantResults.indices) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(permissions[i]) && granted) displayRationaleDialog()
                granted = false
            }
        }
        return granted
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun displayRationaleDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setMessage(getString(R.string.no_permissions))
                .setPositiveButton(getString(R.string.allow)) { _, _ -> requestMultiplePermissions() }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun verifyPermissionsGranted(): Boolean {
        for (permission in permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestMultiplePermissions()
                return false
            }
        }
        return true
    }

    private fun verifyLocationEnabled(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!gpsEnabled && !networkEnabled) {
            displayLocationDisabledDialog()
            return false
        }
        return true
    }

    private fun displayLocationDisabledDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(getString(R.string.no_location_title))
                .setMessage(getString(R.string.location_question))
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    navigateToLocationSettings()
                }
                .setNegativeButton(android.R.string.no) { dialog, _ -> dialog.dismiss() }
                .show()
    }

    private fun navigateToLocationSettings() {
        startActivityForResult(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_ENABLE_LOCATION
        )
    }


    private fun restoreDeviceList(savedInstanceState: Bundle) {
        savedInstanceState.getParcelableArrayList<BleDeviceItem>(FOUND_DEVICES)
                ?.toMutableList()
                ?.let {
                    bleManager.foundDevices = HashSet(it)
                    bleManager.sendItemsToView()
                }
    }

    private fun restoreActiveState(savedInstanceState: Bundle) {
        if (savedInstanceState.getBoolean(DISCOVERY_ACTIVE)) {
            bleManager.resumeDiscovery()
        } else {
            onDiscoveryStop()
        }
    }


    private fun initViewElements() {
        recyclerview_devices.layoutManager = LinearLayoutManager(this)
        recyclerview_devices.adapter = bleDeviceAdapter
        textview_scan.setOnClickListener { safeStartDiscovery() }

    }

    private fun setDisabled() {
        progressbar_scanning.visibility = View.GONE
        textview_scan.setText(R.string.main_enable_bluetooth)
        textview_scan.setOnClickListener { safeStartDiscovery() }
    }

}
