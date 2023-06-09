package com.demo.translucent.utils

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.ViewGroup
import android.view.WindowManager

fun Resources.floatDp(number: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, number, this.displayMetrics)

fun screenRealSize(context: Context): Pair<Int, Int> {
    //屏幕绝对高度
    val conf = DisplayMetrics()
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    wm.defaultDisplay.getRealMetrics(conf)
    val screenSizeOne = conf.widthPixels
    val screenSizeTwo = conf.heightPixels
    //获取最大值
    return screenSizeOne.coerceAtMost(screenSizeTwo) to screenSizeTwo.coerceAtLeast(screenSizeOne)
}

/**
 * 获取屏幕绝对高度.+缓存设置
 */
fun Context.phoneScreenHeight(): Int {
    return screenRealSize(this).second
}

/**
 * 获取屏幕绝对宽度.+缓存设置
 */
fun Context.phoneScreenWidth(): Int {
    return screenRealSize(this).first
}

const val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
const val wrapContent = ViewGroup.LayoutParams.WRAP_CONTENT