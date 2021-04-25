package com.kydah.powerwifidirect.networking.model

import android.os.Parcel
import android.os.Parcelable

data class AccessPointData( var SSID : String,  var passphrase : String,  var inetAddress : String) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(SSID)
        parcel.writeString(passphrase)
        parcel.writeString(inetAddress)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AccessPointData> {
        override fun createFromParcel(parcel: Parcel): AccessPointData {
            return AccessPointData(parcel)
        }

        override fun newArray(size: Int): Array<AccessPointData?> {
            return arrayOfNulls(size)
        }
    }
}