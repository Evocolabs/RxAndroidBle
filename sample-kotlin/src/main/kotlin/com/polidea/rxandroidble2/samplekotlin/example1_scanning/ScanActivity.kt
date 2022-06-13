package com.polidea.rxandroidble2.samplekotlin.example1_scanning

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.samplekotlin.DeviceActivity
import com.polidea.rxandroidble2.samplekotlin.R
import com.polidea.rxandroidble2.samplekotlin.SampleApplication
import com.polidea.rxandroidble2.samplekotlin.example1a_background_scanning.BackgroundScanActivity
import com.polidea.rxandroidble2.samplekotlin.util.isScanPermissionGranted
import com.polidea.rxandroidble2.samplekotlin.util.requestScanPermission
import com.polidea.rxandroidble2.samplekotlin.util.showError
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_example1.*
import java.util.regex.Pattern

class ScanActivity : AppCompatActivity() {

    val SELECT_DEVICE_REQUEST_CODE = 0x05

    private val rxBleClient = SampleApplication.rxBleClient

    private var scanDisposable: Disposable? = null

    private val resultsAdapter =
        ScanResultsAdapter { startActivity(DeviceActivity.newInstance(this, it.macAddress)) }

    private var hasClickedScan = false

    private val isScanning: Boolean
        get() = scanDisposable != null

    private var bredrScanDisposable: Disposable? = null

    private val isBredrScanning: Boolean
        get() = bredrScanDisposable != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example1)
        configureResultList()

        background_scan_btn.setOnClickListener { onBackgroundClick() }
        scan_toggle_btn.setOnClickListener { onScanToggleClick() }
        bredr_scan_btn.setOnClickListener{ onScanBredrToggleClick() };
    }

    fun onBackgroundClick() {
        val btManager: BluetoothManager = (getSystemService(Context.BLUETOOTH_SERVICE)) as BluetoothManager
        val btAdapter: BluetoothAdapter = btManager.adapter
        for (device in rxBleClient.bondedDevices) {
            resultsAdapter.addScanResult(device)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SELECT_DEVICE_REQUEST_CODE -> when(resultCode) {
                Activity.RESULT_OK -> {
                    // The user chose to pair the app with a Bluetooth device.
                    val deviceToPair: BluetoothDevice? =
                        data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                    deviceToPair?.let { device ->
                        device.createBond()
                        // Continue to interact with the paired device.
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun configureResultList() {
        with(scan_results) {
            setHasFixedSize(true)
            itemAnimator = null
            adapter = resultsAdapter
        }
    }

    private fun onScanToggleClick() {
        Log.d("onScanToggleClick", "sdk version: %d".format(Build.VERSION.SDK_INT))
        if (isScanning) {
            scanDisposable?.dispose()
        } else {
            if (rxBleClient.isScanRuntimePermissionGranted) {
                resultsAdapter.clearScanResults()
                scanBleDevices()
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally { dispose() }
                    .subscribe({ it ->
                        if (it.bleDevice.name!=null && (it.bleDevice.name!!.lowercase().contains("orka")
                            ||it.bleDevice.name!!.lowercase().contains("ok"))) {
                            resultsAdapter.addScanResult(it.bleDevice)
                        } }, { onScanFailure(it) })
                    .let { scanDisposable = it }
            } else {
                hasClickedScan = true
                requestScanPermission(rxBleClient)
            }
        }
        updateButtonUIState()
    }



    private fun scanBleDevices(): Observable<ScanResult> {
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        val scanFilter = ScanFilter.Builder()
            .build()

        return rxBleClient.scanBleDevices(scanSettings, scanFilter)
    }

    private fun onScanBredrToggleClick() {
        if (isBredrScanning) {
            bredrScanDisposable?.dispose()
        } else {
            if (rxBleClient.isScanRuntimePermissionGranted) {
                resultsAdapter.clearScanResults()
                scanBredrDevices()
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally{ disposeBredr() }
                    .subscribe({
                        if (it.name != null && it.name!!.lowercase().contains("orka"))
                            resultsAdapter.addScanResult(it) }, {})
                    .let { bredrScanDisposable = it }
            } else {
                requestScanPermission(rxBleClient)
            }
        }
        updateButtonUIState()
    }

    private fun scanBredrDevices(): Observable<RxBleDevice> {
        return rxBleClient.scanBredrDevices()
    }

    private fun disposeBredr() {
        bredrScanDisposable = null
        updateButtonUIState()
    }

    private fun dispose() {
        scanDisposable = null
        updateButtonUIState()
    }

    private fun onScanFailure(throwable: Throwable) {
        if (throwable is BleScanException) showError(throwable)
        else Log.w("ScanActivity", "Scan failed", throwable)
    }

    private fun updateButtonUIState() {
        bredr_scan_btn.setText(
            if (isBredrScanning)
                R.string.button_stop_bredr_scan
            else
                R.string.button_start_bredr_scan)
        scan_toggle_btn.setText(if (isScanning) R.string.button_stop_scan else R.string.button_start_scan)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (isScanPermissionGranted(requestCode, grantResults) && hasClickedScan) {
            hasClickedScan = false
            scanBleDevices()
        }
    }

    public override fun onPause() {
        super.onPause()
        // Stop scanning in onPause callback.
        if (isScanning) scanDisposable?.dispose()
    }
}
