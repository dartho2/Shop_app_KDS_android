package com.itsorderkds.data.entity.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.itsorderkds.data.entity.*
import java.util.Date

class Converters {

    @TypeConverter
    fun fromOrderProductList(value: List<OrderProductEntity>?): String? =
        Gson().toJson(value)

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    @TypeConverter
    fun fromOrderAdditionalList(value: List<AdditionalFeeEntity>?): String? =
        Gson().toJson(value)

    @TypeConverter
    fun toOrderProductList(value: String?): List<OrderProductEntity>? =
        Gson().fromJson(value, object : TypeToken<List<OrderProductEntity>>() {}.type)

    @TypeConverter
    fun toOrderAdditionalList(value: String?): List<AdditionalFeeEntity>? =
        Gson().fromJson(value, object : TypeToken<List<AdditionalFeeEntity>>() {}.type)

    @TypeConverter
    fun fromAddonsGroupList(value: List<AddonsGroupEntity>?): String? =
        Gson().toJson(value)

    @TypeConverter
    fun toAddonsGroupList(value: String?): List<AddonsGroupEntity>? =
        Gson().fromJson(value, object : TypeToken<List<AddonsGroupEntity>>() {}.type)

    @TypeConverter
    fun fromAddonList(value: List<AddonEntity>?): String? =
        Gson().toJson(value)

    @TypeConverter
    fun toAddonList(value: String?): List<AddonEntity>? =
        Gson().fromJson(value, object : TypeToken<List<AddonEntity>>() {}.type)
}
