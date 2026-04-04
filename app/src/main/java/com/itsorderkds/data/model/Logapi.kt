package com.itsorderkds.data.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Logapi(
    @SerializedName("page") val page: String?,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("level") val userId: String,
    @SerializedName("message") val action: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(page)
        parcel.writeString(timestamp)
        parcel.writeString(userId)
        parcel.writeString(action)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Logapi> {
        override fun createFromParcel(parcel: Parcel): Logapi {
            return Logapi(parcel)
        }

        override fun newArray(size: Int): Array<Logapi?> {
            return arrayOfNulls(size)
        }
    }
}
