package com.vasili.orcamhomeassignment

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_ble_device.view.*

class BleDeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    fun bind(item: BleDeviceItem) {
        itemView.address.text = item.address
        itemView.name.text = item.displayName
    }

}
