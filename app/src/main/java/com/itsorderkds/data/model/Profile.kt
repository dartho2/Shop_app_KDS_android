package com.itsorderkds.data.model
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.io.Serializable

@Parcelize
data class Profile(
    @SerializedName("FCM") val fcm: String,
    @SerializedName("active") val active: Boolean,
    @SerializedName("address") val address: String?,
    @SerializedName("balance") val balance: @RawValue List<BalanceItem>,
    @SerializedName("email") val email: String?,
    @SerializedName("file") val file: String?,
    @SerializedName("time") val time: Number,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("holidayleft") val holidayLeft: Int,
    @SerializedName("holidayuse") val holidayUse: Int,
    @SerializedName("holidayMonth") val holidayMonth: Int,
    @SerializedName("id") val id: String,
    @SerializedName("key") val key: String?,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("position") val position: @RawValue List<String>,
    @SerializedName("profile") val profile: String,
    @SerializedName("restaurant") val restaurant: String,
    @SerializedName("role") val role: String,
    @SerializedName("s_token") val sToken: String?,
    @SerializedName("s_url") val sUrl: String?,
    @SerializedName("secret") val secret: String?,
    @SerializedName("sumMonth") val sumMonth: Double,
    @SerializedName("tips") val tips: Double?,
    @SerializedName("url") val url: String?,
    @SerializedName("url_login") val urlLogin: String?,
    @SerializedName("username") val username: String,
    @SerializedName("wl") val wl: String?,
    @SerializedName("wp") val wp: String?,
    @SerializedName("__v") val v: Int,
    @SerializedName("_id") val _id: String,
    @SerializedName("holiday") val holiday: Int?,
    @SerializedName("jobs") val jobs: String?,
    @SerializedName("reward") val reward: Int,
    @SerializedName("skill") val skill: String?,
    @SerializedName("startwork") val startWork: String?,
    @SerializedName("transfer") val transfer: Double?,
    @SerializedName("totalTime") val totalTime: Double?,
    @SerializedName("lunchprice") val lunchprice: Double? = null,
    @SerializedName("currency") val currency: String = "",
    @SerializedName("lunch") val lunch: Double? = null,
    @SerializedName("percent") val percent: Double? = null
): Parcelable, Serializable {
    override fun toString(): String {
        return "Profile(id=$_id, restaurant=$restaurant)"
    }
}
@Parcelize
data class BalanceItem(
    @SerializedName("_id") val id: String,
    @SerializedName("status") val status: Boolean,
    @SerializedName("reward") val reward: Int,
    @SerializedName("transfer") val transfer: Double,
    @SerializedName("star") val star: Int,
    @SerializedName("time") val time: Double,
    @SerializedName("cash") val cash: Double,
    @SerializedName("date") val date: String,
    @SerializedName("total") val total: Double,
    @SerializedName("holiday") val holiday: Int,
    @SerializedName("holidayMonth") val holidayMonth: Int,
    @SerializedName("addons") val addons: Double,
    @SerializedName("extra") val extra: Double,
    @SerializedName("sick") val sick: Int,
    @SerializedName("zus") val zus: Double,
    @SerializedName("sumMonth") val sumMonth: Double,
    @SerializedName("lunch") val lunch: Double? = null,
    @SerializedName("percent") val percent: Double? = null,
    @SerializedName("social") val social: Int? = null,
    @SerializedName("tips") val tips: Double? = null,
    @SerializedName("desc") val desc: String? = null,
    @SerializedName("currency") val currency: String = "",
    var isExpanded: Boolean = false
): Parcelable, Serializable {
    override fun toString(): String {
        return "Profile(id=$id)"
    }
}
