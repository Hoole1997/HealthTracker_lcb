package net.corekit.core.ext

import com.healthtracker.framework.util.SpUtils
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


class DataStoreStringDelegate(private val key: String, private val def: String? = null) :
    ReadWriteProperty<Any?, String?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String? {
        return SpUtils.getString(key)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        SpUtils.putString(key,value)
    }
}

class DataStoreLongDelegate(private val key: String, private val def: Long = 0L) :
    ReadWriteProperty<Any?, Long> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Long {
        return SpUtils.getLong(key, def)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
        SpUtils.putLong(key, value)
    }
}

class DataStoreBoolDelegate(private val key: String, private val def: Boolean = false) :
    ReadWriteProperty<Any?, Boolean> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        return SpUtils.getBoolean(key, def)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        SpUtils.putBoolean(key, value)
    }
}

class DataStoreFloatDelegate(private val key: String, private val def: Float = 0f) :
    ReadWriteProperty<Any?, Float> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Float {
        return SpUtils.getFloat(key, def)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
        SpUtils.putFloat(key, value)
    }
}


class DataStoreIntDelegate(private val key: String, private val def: Int = 0) :
    ReadWriteProperty<Any?, Int> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return SpUtils.getInt(key, def)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        SpUtils.putInt(key, value)
    }
}


