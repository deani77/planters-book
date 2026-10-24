package com.selendang3.app.data

// Null means pending; a measured zero remains an observation.
data class RainfallValue(val millimeters: Double?, val rainyDays: Int?) {
    init {
        require(millimeters == null || (millimeters.isFinite() && millimeters >= 0))
        require(rainyDays == null || rainyDays in 0..31)
    }
    val pending: Boolean get() = millimeters == null || rainyDays == null
}
