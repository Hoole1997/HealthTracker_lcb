package com.healthtracker.framework.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

inline fun <reified T> parseJson(json: String?, typeToken: TypeToken<T>): T? {
    return if (json != null) {
        Gson().fromJson(json, typeToken.type)
    } else {
        null
    }
}


inline fun <reified T> String.fromJson(typeToken: TypeToken<T>): T {
    val gson = Gson()
    return gson.fromJson(this, typeToken.type)
}

inline fun <reified T> List<T>.toJson(): String {
    val gson = Gson()
    return gson.toJson(this)
}