package com.demo.translucent.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.TextUtils
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.graphics.BitmapCompat
import androidx.lifecycle.LifecycleOwner
import com.demo.translucent.widget.AssetsSVGAImageView

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

fun IntArray.parseAttrs(context: Context, attrs: AttributeSet?, parser: TypedArray.() -> Unit) {
    attrs ?: return
    val array = context.obtainStyledAttributes(attrs, this)
    parser.invoke(array)
    array.recycle()
}

fun String?.isValidUrl(): Boolean {
    if (this.isNullOrEmpty()) {
        return false
    }
    val scheme = Uri.parse(this).scheme
    return TextUtils.equals("http", scheme) || TextUtils.equals("https", scheme) || TextUtils.equals("file", scheme)
}

fun Context.lifecycleOwner(): LifecycleOwner? {
    var curContext: Context? = this
    var maxDepth = 20
    while (maxDepth-- > 0 && curContext !is LifecycleOwner) {
        curContext = (curContext as? ContextWrapper)?.baseContext
    }
    return if (curContext is LifecycleOwner) {
        curContext
    } else {
        null
    }
}

/**
 * 直接将view绘制到Canvas上
 *
 * @return
 */
fun View.createViewBitmap(): Bitmap? {
    val bitmap = Bitmap.createBitmap(this.measuredWidth, this.measuredHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    this.draw(canvas)
    return bitmap
}

/**
 * 内部有AssetsSVGAImageView的截图
 * @return
 */
fun View.createAnimViewBitmap(): Bitmap? {
    if (this is AssetsSVGAImageView) {
        return this.createViewBitmap()
    }
    val svgaSets = HashMap<View, Drawable?>()
    saveSvgaView(this, svgaSets)
    val bitmap = this.createViewBitmap()
    restoreSvgaView(svgaSets)
    return bitmap
}

private fun saveSvgaView(view: View, sets: HashMap<View, Drawable?>) {
    when (view) {
        is AssetsSVGAImageView -> {
            sets[view] = view.background
            view.createViewBitmap()?.let {
                view.background = BitmapDrawable(view.resources, it)
            }
        }

        is ViewGroup -> {
            for (index in 0 until view.childCount) {
                saveSvgaView(view.getChildAt(index), sets)
            }
        }
    }
}

private fun restoreSvgaView(sets: HashMap<View, Drawable?>) {
    sets.forEach { (v, bg) -> v.background = bg }
}

const val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
const val wrapContent = ViewGroup.LayoutParams.WRAP_CONTENT