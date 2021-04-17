package com.kydah.powerwifidirect.networking.model

data class Peer(var deviceID : String, var portNumber : String, var accessPointData: AccessPointData? = null) {

    override fun equals(other: Any?): Boolean {
        return deviceID == (other as Peer).deviceID
    }

    override fun hashCode(): Int {
        return deviceID.hashCode()
    }
}