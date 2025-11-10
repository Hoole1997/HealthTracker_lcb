package com.healthtracker.framework.util

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import com.healthtracker.framework.R

fun getRobotoMedium(context: Context) = ResourcesCompat.getFont(context, R.font.roboto_medium)
fun getRobotoBold(context: Context) = ResourcesCompat.getFont(context, R.font.roboto_bold)
fun getRobotoRegular(context: Context) = ResourcesCompat.getFont(context, R.font.roboto_regular)
fun getRobotoLight(context: Context) = ResourcesCompat.getFont(context, R.font.roboto_light)