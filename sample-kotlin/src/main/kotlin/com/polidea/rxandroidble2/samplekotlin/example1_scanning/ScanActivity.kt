package com.polidea.rxandroidble2.samplekotlin.example1_scanning

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.polidea.rxandroidble2.RxBleDevice
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
import kotlinx.android.synthetic.main.activity_example1.*

class ScanActivity : AppCompatActivity() {

    private val rxBleClient = SampleApplication.rxBleClient

    private var scanDisposable: Disposable? = null

    private val resultsAdapter =
        ScanResultsAdapter { startActivity(DeviceActivity.newInstance(this, it.bleDevice.macAddress)) }

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

        background_scan_btn.setOnClickListener { startActivity(BackgroundScanActivity.newInstance(this)) }
        scan_toggle_btn.setOnClickListener { onScanToggleClick() }
        bredr_scan_btn.setOnClickListener()
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
            if (rxBleClient.isScanRuntimePermissionGranted) {
                scanBleDevices()
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally { dispose() }
                    .subscribe({ resultsAdapter.addScanResult(it) }, { onScanFailure(it) })
                    .let { scanDisposable = it }
            } else {
                hasClickedScan = true
                requestLocationPermission(rxBleClient)
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
                scanBredrDevices()
            }
        }
    }

    private fun scanBredrDevices() {
        rxBleClient.scanBredrDevices()
    }

    private fun dispose() {
        scanDisposable = null
        resultsAdapter.clearScanResults()
        updateButtonUIState()
    }

    private fun onScanFailure(throwable: Throwable) {
        if (throwable is BleScanException) showError(throwable)
    }

    private fun updateButtonUIState() =
        scan_toggle_btn.setText(if (isScanning) R.string.button_stop_scan else R.string.button_start_scan)

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (isLocationPermissionGranted(requestCode, grantResults) && hasClickedScan) {
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