package com.polidea.rxandroidble2.samplekotlin.example1_scanning

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.samplekotlin.example1_scanning.ScanResultsAdapter.ViewHolder
import com.polidea.rxandroidble2.scan.ScanResult

internal class ScanResultsAdapter(
    private val onClickListener: (RxBleDevice) -> Unit
) : RecyclerView.Adapter<ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(android.R.id.text1)
        val deviceAddr: TextView = itemView.findViewById(android.R.id.text2)
    }

    private val data = mutableListOf<RxBleDevice>()

    fun addScanResult(bleScanResult: RxBleDevice) {
        // Not the best way to ensure distinct devices, just for the sake of the demo.
        data.withIndex()
            .firstOrNull { it.value == bleScanResult }
            ?.let {
                // device already in data list => update
                data[it.index] = bleScanResult
                notifyItemChanged(it.index)
            }
            ?: run {
                // new device => add to data list
                with(data) {
                    add(bleScanResult)
                    sortBy { it.macAddress }
                }
                notifyDataSetChanged()
            }
    }

    fun clearScanResults() {
        data.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(data[position]) {
            holder.deviceName.text = String.format("Device Name: %s", name)
            holder.deviceAddr.text = String.format("addr: %s", macAddress)
            holder.itemView.setOnClickListener { onClickListener(this) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        LayoutInflater.from(parent.context)
            .inflate(android.R.layout.two_line_list_item, parent, false)
            .let { ViewHolder(it) }
}
