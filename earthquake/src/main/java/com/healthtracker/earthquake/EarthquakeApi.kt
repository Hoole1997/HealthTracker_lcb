package com.healthtracker.earthquake

import com.healthtracker.earthquake.model.EarthquakeProperties
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.TimeZone

/**
 * 最基础的接口请求类：使用 HttpURLConnection 同步请求，字符串手动解析。
 */
class EarthquakeApi {

    /**
     * 请求昨天 00:00 到今天 00:00 的地震事件，并返回 features[0].properties。
     * 简单同步实现，调用方需在非主线程调用以避免 ANR。
     */
    fun fetchYesterdayTopProperties(): EarthquakeProperties? {
        val (start, end) = computeDateRange()
        val url = "https://earthquake.usgs.gov/fdsnws/event/1/query" +
                "?format=geojson&starttime=$start&endtime=$end&eventtype=earthquake&orderby=magnitude&limit=10"

        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                readTimeout = 10_000
                connectTimeout = 10_000
                setRequestProperty("Accept", "application/json")
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            conn.disconnect()

            parseFirstProperties(body)
        } catch (_: Exception) {
            null
        }
    }


    /**
     * 计算昨天 00:00 到今天 00:00（UTC）的日期字符串，格式 yyyy-MM-dd。
     * 使用旧版时间 API，避免启用 desugaring 的需求。
     */
    private fun computeDateRange(): Pair<String, String> {
        val tz = TimeZone.getTimeZone("UTC")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = tz

        val todayStr = sdf.format(Date())

        val cal = Calendar.getInstance(tz)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        val yesterdayStr = sdf.format(cal.time)

        return yesterdayStr to todayStr
    }

    /**
     * 从完整 JSON 中提取 features[0].properties 并解析为 EarthquakeProperties。
     * 仅针对预期结构做最小手动解析，不做通用 JSON 兼容处理。
     */
    private fun parseFirstProperties(json: String): EarthquakeProperties? {
        val featuresIdx = json.indexOf("\"features\"")
        if (featuresIdx == -1) return null

        val arrayStart = json.indexOf('[', featuresIdx)
        if (arrayStart == -1) return null

        // 找到数组中的第一个对象
        val firstObjStart = json.indexOf('{', arrayStart)
        if (firstObjStart == -1) return null

        // 在第一个对象内定位 properties 对象
        val propsKeyIdx = json.indexOf("\"properties\"", firstObjStart)
        if (propsKeyIdx == -1) return null
        val propsObjStart = json.indexOf('{', propsKeyIdx)
        if (propsObjStart == -1) return null
        val propsObj = extractJsonObject(json, propsObjStart) ?: return null

        // 逐字段正则解析（简化实现）
        fun str(key: String): String? {
            val r = Regex("\"$key\"\\s*:\\s*\"(.*?)\"", RegexOption.DOT_MATCHES_ALL)
            return r.find(propsObj)?.groupValues?.get(1)
        }
        fun numDouble(key: String): Double? {
            val r = Regex("\"$key\"\\s*:\\s*([-]?[0-9]+(?:\\.[0-9]+)?)")
            return r.find(propsObj)?.groupValues?.get(1)?.toDoubleOrNull()
        }
        fun numInt(key: String): Int? {
            val r = Regex("\"$key\"\\s*:\\s*([-]?[0-9]+)")
            return r.find(propsObj)?.groupValues?.get(1)?.toIntOrNull()
        }
        fun numLong(key: String): Long? {
            val r = Regex("\"$key\"\\s*:\\s*([-]?[0-9]+)")
            return r.find(propsObj)?.groupValues?.get(1)?.toLongOrNull()
        }
        fun isNull(key: String): Boolean {
            val r = Regex("\"$key\"\\s*:\\s*null")
            return r.containsMatchIn(propsObj)
        }

        return EarthquakeProperties(
            mag = numDouble("mag"),
            place = if (!isNull("place")) str("place") else null,
            time = numLong("time"),
            updated = numLong("updated"),
            tz = if (!isNull("tz")) numInt("tz") else null,
            url = if (!isNull("url")) str("url") else null,
            detail = if (!isNull("detail")) str("detail") else null,
            felt = numInt("felt"),
            cdi = numDouble("cdi"),
            mmi = numDouble("mmi"),
            alert = if (!isNull("alert")) str("alert") else null,
            status = if (!isNull("status")) str("status") else null,
            tsunami = numInt("tsunami"),
            sig = numInt("sig"),
            net = if (!isNull("net")) str("net") else null,
            code = if (!isNull("code")) str("code") else null,
            ids = if (!isNull("ids")) str("ids") else null,
            sources = if (!isNull("sources")) str("sources") else null,
            types = if (!isNull("types")) str("types") else null,
            nst = numInt("nst"),
            dmin = numDouble("dmin"),
            rms = numDouble("rms"),
            gap = numInt("gap"),
            magType = if (!isNull("magType")) str("magType") else null,
            type = if (!isNull("type")) str("type") else null,
            title = if (!isNull("title")) str("title") else null,
        )
    }

    /**
     * 从指定位置提取完整 JSON 对象字符串，处理简单字符串转义。
     */
    private fun extractJsonObject(src: String, start: Int): String? {
        var i = start
        var depth = 0
        var inString = false
        var sb = StringBuilder()
        while (i < src.length) {
            val ch = src[i]
            sb.append(ch)
            if (ch == '"') {
                // 处理转义引号
                var backslashes = 0
                var j = i - 1
                while (j >= 0 && src[j] == '\\') { backslashes++; j-- }
                if (backslashes % 2 == 0) inString = !inString
            }
            if (!inString) {
                if (ch == '{') depth++
                else if (ch == '}') {
                    depth--
                    if (depth == 0) break
                }
            }
            i++
        }
        return if (depth == 0) sb.toString() else null
    }
}