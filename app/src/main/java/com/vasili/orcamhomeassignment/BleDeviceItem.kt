package com.vasili.orcamhomeassignment

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class BleDeviceItem(
    var name: String?,
    val address: String,
    val displayName: String
) : Parcelable
