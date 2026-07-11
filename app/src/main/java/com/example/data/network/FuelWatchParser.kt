package com.example.data.network

import android.util.Log
import android.util.Xml
import com.example.data.model.FuelStation
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.StringReader

object FuelWatchParser {
    private const val TAG = "FuelWatchParser"

    fun parse(xmlString: String, day: String = "today"): List<FuelStation> {
        val stations = mutableListOf<FuelStation>()
        try {
            val parser = Xml.newPullParser() ?: return emptyList()
            parser.setInput(StringReader(xmlString))
            return parseFeed(parser, day)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing XML string: ${e.message}", e)
        }
        return stations
    }

    fun parse(inputStream: InputStream, day: String = "today"): List<FuelStation> {
        val stations = mutableListOf<FuelStation>()
        try {
            val parser = Xml.newPullParser() ?: return emptyList()
            parser.setInput(inputStream, null)
            return parseFeed(parser, day)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing XML input stream: ${e.message}", e)
        }
        return stations
    }

    private fun parseFeed(parser: XmlPullParser, day: String): List<FuelStation> {
        val stations = mutableListOf<FuelStation>()
        var eventType = parser.eventType
        var currentStation: MutableFuelStation? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name.equals("item", ignoreCase = true)) {
                        currentStation = MutableFuelStation(day = day)
                    } else if (currentStation != null) {
                        when (name.lowercase()) {
                            "title" -> currentStation.title = parser.nextText().trim()
                            "description" -> currentStation.description = parser.nextText().trim()
                            "brand" -> currentStation.brand = parser.nextText().trim()
                            "price" -> currentStation.price = parser.nextText().trim().toDoubleOrNull() ?: 0.0
                            "trading-name" -> currentStation.tradingName = parser.nextText().trim()
                            "location" -> currentStation.location = parser.nextText().trim()
                            "address" -> currentStation.address = parser.nextText().trim()
                            "phone" -> currentStation.phone = parser.nextText().trim()
                            "latitude" -> currentStation.latitude = parser.nextText().trim().toDoubleOrNull() ?: 0.0
                            "longitude" -> currentStation.longitude = parser.nextText().trim().toDoubleOrNull() ?: 0.0
                            "site-features" -> currentStation.siteFeatures = parser.nextText().trim()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name.equals("item", ignoreCase = true) && currentStation != null) {
                        val station = currentStation.toFuelStation()
                        // Ensure we have some basic fields like trading name and address
                        if (station.tradingName.isNotEmpty() || station.title.isNotEmpty()) {
                            // If trading name is empty, extract from title or description
                            val finalTradingName = if (station.tradingName.isEmpty()) {
                                station.title.substringBefore("-").trim()
                            } else {
                                station.tradingName
                            }
                            stations.add(station.copy(tradingName = finalTradingName))
                        }
                        currentStation = null
                    }
                }
            }
            eventType = parser.next()
        }
        return stations
    }

    private class MutableFuelStation(
        var title: String = "",
        var description: String = "",
        var brand: String = "",
        var price: Double = 0.0,
        var tradingName: String = "",
        var location: String = "",
        var address: String = "",
        var phone: String = "",
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,
        var siteFeatures: String = "",
        var day: String = "today"
    ) {
        fun toFuelStation(): FuelStation {
            return FuelStation(
                title = title,
                description = description,
                brand = brand,
                price = price,
                tradingName = tradingName,
                location = location,
                address = address,
                phone = phone,
                latitude = latitude,
                longitude = longitude,
                siteFeatures = siteFeatures,
                day = day
            )
        }
    }
}
