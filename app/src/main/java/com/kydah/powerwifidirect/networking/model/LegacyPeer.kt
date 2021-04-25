package com.kydah.powerwifidirect.networking.model

import android.os.Parcel
import android.os.Parcelable

data class LegacyPeer(var deviceID : String?, var portNumber : String?, var accessPointData: AccessPointData? = null) : Parcelable{

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readParcelable(AccessPointData::class.java.classLoader)) {
    }

    override fun equals(other: Any?): Boolean {
        return deviceID == (other as LegacyPeer).deviceID
    }

    override fun hashCode(): Int {
        return deviceID.hashCode()
    }

    override fun toString(): String {
        return deviceID!!
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(deviceID)
        parcel.writeString(portNumber)
        parcel.writeParcelable(accessPointData, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LegacyPeer> {
        override fun createFromParcel(parcel: Parcel): LegacyPeer {
            return LegacyPeer(parcel)
        }

        override fun newArray(size: Int): Array<LegacyPeer?> {
            return arrayOfNulls(size)
        }
    }
}