package com.healthtracker.framework.util

import android.content.Context
import android.text.TextUtils
import com.tencent.mmkv.MMKV

object SpUtils {
    private var _mv: MMKV? = null

    fun init(context: Context) {
        MMKV.initialize(context)
    }


    private fun getMV(): MMKV? {
        if (_mv == null) {
            synchronized(SpUtils::class.java) {
                if (_mv == null) {
                    _mv = MMKV.defaultMMKV()
                }
            }
        }
        return _mv
    }


    fun getLong(key: String?, defValue: Long): Long {
        if (!TextUtils.isEmpty(key)) {
            try {
                getMV()?.apply { return decodeLong(key, defValue) }
            } catch (_: Throwable) {
            }
        }

        return defValue
    }

    fun putLong(key: String?, value: Long): Boolean {
        if (!TextUtils.isEmpty(key)) {
            try {
                getMV()?.apply { return encode(key, value) }
            } catch (_: Throwable) {
            }
        }
        return false
    }

    fun getString(key: String?, defValue: String): String {
        if (!TextUtils.isEmpty(key)) {
            try {
                getMV()?.apply { return decodeString(key, defValue) ?: "" }
            } catch (_: Throwable) {
            }
        }
        return defValue
    }

    fun getStringSet(key: String?): MutableSet<String> {
        if (!TextUtils.isEmpty(key)) {
            try {
                getMV()?.apply { return decodeStringSet(key, LinkedHashSet(), LinkedHashSet::class.java) ?: LinkedHashSet() }
            } catch (_: Throwable) {
            }
        }
        return LinkedHashSet()
    }

    fun putStringSet(key: String?, value: MutableSet<String>): Boolean {
        if (!TextUtils.isEmpty(key)) {
            try {
                getMV()?.apply { return encode(key, value) }
            } catch (_: Throwable) {
            }
        }
        return false
    }

    fun getString(key: String?): String {
        if (!TextUtils.isEmpty(key)) {
            try {
                getMV()?.apply { return decodeString(key, "") ?: "" }
            } catch (_: Throwable) {
            }
        }
        return ""
    }

    fun putString(key: String?, value: String?): Boolean {
        if (!TextUtils.isEmpty(key)) {
            try {
                getMV()?.apply { return encode(key, value) }
            } catch (_: Throwable) {
            }
        }
        return false
    }

    fun getBoolean(key: String?, defValue: Boolean): Boolean {
        if (!TextUtils.isEmpty(key)) {
            try {
                getMV()?.apply { return decodeBool(key, defValue) }
            } catch (_: Throwable) {
            }
        }
        return defValue
    }

    fun putBoolean(key: String?, value: Boolean): Boolean {
        if (!TextUtils.isEmpty(key)) {
            try {
                getMV()?.apply { return encode(key, value) }
            } catch (_: Throwable) {
            }
        }
        return false
    }

    fun getInt(key: String?, defValue: Int): Int {
        if (!TextUtils.isEmpty(key)) {
            try {
                getMV()?.apply { return decodeInt(key, defValue) }
            } catch (_: Throwable) {
            }
        }
        return defValue
    }

    fun putInt(key: String?, value: Int): Boolean {
        if (!TextUtils.isEmpty(key)) {
            try {
                getMV()?.apply { return encode(key, value) }
            } catch (_: Throwable) {
            }
        }
        return false
    }

    fun getFloat(key: String?, defValue: Float): Float {
        if (!TextUtils.isEmpty(key)) {
            try {
                getMV()?.apply { return decodeFloat(key, defValue) }
            } catch (_: Throwable) {
            }
        }
        return defValue
    }

    fun putFloat(key: String?, value: Float): Boolean {
        if (!TextUtils.isEmpty(key)) {
            try {
                getMV()?.apply { return encode(key, value) }
            } catch (_: Throwable) {
            }
        }
        return false
    }

    fun getDouble(key: String?, defValue: Double): Double {
        if (!TextUtils.isEmpty(key)) {
            try {
                getMV()?.apply { return decodeDouble(key, defValue) }
            } catch (_: Throwable) {
            }
        }
        return defValue
    }

    fun putDouble(key: String?, value: Double): Boolean {
        if (!TextUtils.isEmpty(key)) {
            try {
                getMV()?.apply { return encode(key, value) }
            } catch (_: Throwable) {
            }
        }
        return false
    }

    fun remove(key: String?): Boolean {
        if (!TextUtils.isEmpty(key)) {
            try {
                getMV()?.removeValueForKey(key)
            } catch (_: Throwable) {
            }
        }
        return false
    }

    fun contain(key: String?): Boolean {
        var result = false
        if (!TextUtils.isEmpty(key)) {
            try {
              result = getMV()?.containsKey(key) ?: false
            } catch (_: Throwable) {

            }
        }
        return result
    }

}