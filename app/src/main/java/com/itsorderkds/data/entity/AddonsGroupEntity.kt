package com.itsorderkds.data.entity

import java.io.Serializable

data class AddonsGroupEntity(
    val addons: List<AddonEntity>
) : Serializable
