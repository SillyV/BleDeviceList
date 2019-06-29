package com.vasili.orcamhomeassignment

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context

/*
* Created By Vasili.
*
* Note for Code Review: Many Ble Devices do not advertise their name when discovered.
* a possible solution to getting their name is connecting to the Gatt service and
* reading the GAP name characteristic
* */
class BleManager(private val context: Context) {

    var active: Boolean = false
    var foundDevices: MutableSet<BleDeviceItem> = HashSet()

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    interface OnBleManagerCallback {
        fun onBluetoothNotAvailable()
        fun onDiscoveryStop()
        fun onDiscoveryStart()
        fun onDiscoveryUpdate(devices: List<BleDeviceItem>)
    }

    private var listener: OnBleManagerCallback? = null
    private val scanCallback: ScanCallback? = object : ScanCallback() {

        override fun onScanFailed(errorCode: Int) {
            listener?.onDiscoveryStop()
            active = false
        }

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (callbackType != ScanSettings.CALLBACK_TYPE_MATCH_LOST) {
                handleDevice(result)
                sendItemsToView()
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { result -> handleDevice(result) }
            sendItemsToView()
        }
    }

    internal fun sendItemsToView() {
        listener?.onDiscoveryUpdate(
            foundDevices
                .toList()
        )
    }

    private fun handleDevice(result: ScanResult) {
        val bleDeviceItem = mapResultToBleDeviceItem(result)
        bleDeviceItem?.let { addDevice(it) }
    }

    private fun mapResultToBleDeviceItem(result: ScanResult): BleDeviceItem? {
        val name = result.device?.name ?: result.scanRecord?.deviceName
        val displayName = name ?: "Unknown Device"
        return result.device?.address?.let { BleDeviceItem(name, it, displayName) }
    }

    private fun addDevice(bleDeviceItem: BleDeviceItem): Boolean {
        return foundDevices.add(bleDeviceItem)
    }

    fun startDiscovery() {
        foundDevices.clear()
        enableBluetooth() ?: scan()
    }

    fun resumeDiscovery() {
        enableBluetooth() ?: scan()
    }

    private fun scan() {
        sendItemsToView()
        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
        listener?.onDiscoveryStart()
        active = true
    }

    private fun enableBluetooth(): BluetoothAdapter? {
        return bluetoothAdapter
            .takeIf { !it.isEnabled }
            ?.apply {
                listener?.onBluetoothNotAvailable()
            }
    }

    fun stopDiscovery() {
        listener?.onDiscoveryStop()
        active = false
        bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
    }

    fun setListener(listener: OnBleManagerCallback?) {
        this.listener = listener
    }

}
