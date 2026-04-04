package com.itsorderkds.data.model

data class DispatchResponse(
    val status: String,                 // np. "QUEUED"
    val courier: String? = null,        // np. "STAVA"
    val response: Any? = null           // surowa zwrotka backendu; socket i tak dowiezie resztę
)