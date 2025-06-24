package com.lali.dnd.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

const val REQUEST_CHECK_SETTINGS = 1001
const val REQUEST_PERMISSIONS = 1002
const val REQUEST_CAMERA = 1003

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}