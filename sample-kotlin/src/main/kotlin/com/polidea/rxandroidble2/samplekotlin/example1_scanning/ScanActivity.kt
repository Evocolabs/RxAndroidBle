package com.polidea.rxandroidble2.samplekotlin.example1_scanning


import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.samplekotlin.DeviceActivity
import com.polidea.rxandroidble2.samplekotlin.R
import com.polidea.rxandroidble2.samplekotlin.SampleApplication
import com.polidea.rxandroidble2.samplekotlin.example1a_background_scanning.BackgroundScanActivity
import com.polidea.rxandroidble2.samplekotlin.util.isLocationPermissionGranted
import com.polidea.rxandroidble2.samplekotlin.util.requestLocationPermission
import com.polidea.rxandroidble2.samplekotlin.util.showError
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_example1.background_scan_btn
import kotlinx.android.synthetic.main.activity_example1.scan_results
import kotlinx.android.synthetic.main.activity_example1.scan_toggle_btn


class ScanActivity : AppCompatActivity() {

    val REQUEST_ALL_RUNTIME_PERMISSIONS = 101

    private val rxBleClient get() = SampleApplication.rxBleClient

    private var scanDisposable: Disposable? = null

    private val resultsAdapter =
            ScanResultsAdapter { startActivity(DeviceActivity.newInstance(this, it.bleDevice.macAddress)) }

    private var hasClickedScan = false

    private val isScanning: Boolean
        get() = scanDisposable != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example1)
        configureResultList()

        background_scan_btn.setOnClickListener { startActivity(BackgroundScanActivity.newInstance(this)) }
        scan_toggle_btn.setOnClickListener { onScanToggleClick() }
    }

    private fun configureResultList() {
        with(scan_results) {
            setHasFixedSize(true)
            itemAnimator = null
            adapter = resultsAdapter
        }
    }


    private fun onScanToggleClick() {
        if (isScanning) {
            scanDisposable?.dispose()
        } else {
            if (rxBleClient.isScanRuntimePermissionGranted && isScanRuntimePermissionGranted()) {
                scanBleDevices()
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally {
                            Log.d("ScanActivity", "onScanToggleClick: ")
                            dispose()
                        }
                        .subscribe({
//                            Log.d("ScanActivity", "got device ${it.bleDevice.macAddress}")
                            if (it.bleDevice.name != "" && it.bleDevice.name != null) {
                                resultsAdapter.addScanResult(it)
                            }
                        }, { onScanFailure(it) })
                        .let { scanDisposable = it }
            } else {
                hasClickedScan = true
                requestScanRuntimePermission()
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
//                .setManufacturerData(64010,ByteArray(0))
//                .setDeviceName("devyyf")
//                .setDeviceAddress("22:22:22:11:22:09")
                .build()


        return rxBleClient.scanBleDevices(scanSettings, scanFilter)
    }

    private fun dispose() {
        scanDisposable = null
        resultsAdapter.clearScanResults()
        updateButtonUIState()
    }

    private fun onScanFailure(throwable: Throwable) {
        if (throwable is BleScanException) showError(throwable)
        else Log.d("ScanActivity", "onScanFailure: ")
    }

    private fun updateButtonUIState() =
            scan_toggle_btn.setText(if (isScanning) R.string.button_stop_scan else R.string.button_start_scan)

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (isLocationPermissionGranted(requestCode, grantResults) && hasClickedScan) {
            hasClickedScan = false
            scanBleDevices()
        } else if (isScanPermitted(requestCode, grantResults)) {
            hasClickedScan = false
            scanBleDevices()
        }
    }

    private val necessaryPermissions = Array(2){Manifest.permission.BLUETOOTH_SCAN; Manifest.permission.BLUETOOTH_CONNECT}

    private fun requestScanRuntimePermission() {
        ActivityCompat.requestPermissions(this,necessaryPermissions,REQUEST_ALL_RUNTIME_PERMISSIONS)
    }

    private fun isScanPermitted(requestCode: Int, grantResults: IntArray): Boolean {
        if (requestCode == REQUEST_ALL_RUNTIME_PERMISSIONS) {
            return rxBleClient.isScanRuntimePermissionGranted
        }
        return false
    }

    private fun isScanRuntimePermissionGranted() : Boolean {
        var isGranted = true
        if (VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            for (perm in necessaryPermissions) {
                if (ActivityCompat.checkSelfPermission(this,perm) != PackageManager.PERMISSION_GRANTED) {
                    isGranted = false
                }
            }
        }
        return isGranted && rxBleClient.isScanRuntimePermissionGranted
    }

    public override fun onPause() {
        super.onPause()
        // Stop scanning in onPause callback.
        if (isScanning) scanDisposable?.dispose()
    }
}