package com.lali.dnd.model

import com.google.gson.annotations.SerializedName

data class GeoCodingResponse (
    val results: List<Result>,
    val status: String
)

data class Result (
    @SerializedName("formatted_address")
    val formattedAddress: String,
    val geometry: Geometry
)

data class Geometry(
    val location: LatLng
)

data class LatLng(
    val lat: Double,
    val lng: Double
)
