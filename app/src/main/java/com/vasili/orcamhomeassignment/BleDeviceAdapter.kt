package com.vasili.orcamhomeassignment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

/*
* Created By Vasili.
*
* Note for Code Review: this class extends androidx.recyclerview.widget.ListAdapter to provide streamlined diff tools
* when submitting new device list. this provides smooth (remove / insert / change) animations and
* prevents us from having to handle calls like (notifyItemChanged / notifyDataSetChanged.. and so on)
*
* */
class BleDeviceAdapter :
    ListAdapter<BleDeviceItem, BleDeviceViewHolder>(object : DiffUtil.ItemCallback<BleDeviceItem>() {

        override fun areItemsTheSame(oldItem: BleDeviceItem, newItem: BleDeviceItem): Boolean {
            return oldItem.address == newItem.address
        }

        override fun areContentsTheSame(oldItem: BleDeviceItem, newItem: BleDeviceItem): Boolean {
            return oldItem.displayName == newItem.displayName
        }
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BleDeviceViewHolder {
        val context = parent.context
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_ble_device, parent, false)
        return BleDeviceViewHolder(view)
    }

    override fun onBindViewHolder(holderDevice: BleDeviceViewHolder, position: Int) {
        val item = getItem(position)
        holderDevice.bind(item)
    }
}