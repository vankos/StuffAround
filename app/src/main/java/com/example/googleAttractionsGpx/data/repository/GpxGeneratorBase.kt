package com.example.googleAttractionsGpx.data.repository

import com.example.googleAttractionsGpx.domain.models.PointData
import com.example.googleAttractionsGpx.domain.repository.IGpxGenerator

fun buildGpxContent(pointDataList: List<PointData>): String {
    val sb = StringBuilder()
    sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""").append("\n")
    sb.append("""<gpx version="1.1" creator="GooglePlaceGpxGenerator" xmlns="http://www.topografix.com/GPX/1/1" xmlns:osmand="https://osmand.net/docs/technical/osmand-file-formats/osmand-gpx">""")
        .append("\n")
    pointDataList.forEach { point ->
        sb.append("""  <wpt lat="${point.coordinates.latitude}" lon="${point.coordinates.longitude}">""").append("\n")
        val escapedName = point.name.replace("&", "&amp;")
        sb.append("""    <name>$escapedName</name>""").append("\n")
        val escapedDescription = point.description.replace("&", "&amp;")
        sb.append("""    <desc>$escapedDescription</desc>""").append("\n")
        if (point.color != null) {
            sb.append("""    <extensions>""").append("\n")
            sb.append("""      <osmand:color>${point.color}</osmand:color>""").append("\n")
            sb.append("""    </extensions>""").append("\n")
        }
        sb.append("""  </wpt>""").append("\n")
    }
    sb.append("</gpx>").append("\n")
    return sb.toString()
}

abstract class GpxGeneratorBase : IGpxGenerator {

    override fun generateGpx(pointDataList: List<PointData>): String = buildGpxContent(pointDataList)
}

