package com.healthtracker.framework.util

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import com.healthtracker.framework.R

fun getRobotoMedium(context: Context) = ResourcesCompat.getFont(context, R.font.inter_medium)
fun getRobotoBold(context: Context) = ResourcesCompat.getFont(context, R.font.inter_bold)
fun getRobotoRegular(context: Context) = ResourcesCompat.getFont(context, R.font.inter_regular)
fun getRobotoLight(context: Context) = ResourcesCompat.getFont(context, R.font.inter_light)