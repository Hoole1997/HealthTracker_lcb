package com.healthtracker.earthquake

import android.app.Activity
import android.os.Bundle
import android.widget.ImageView

class EarthquakeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_earthquake)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
    }
}