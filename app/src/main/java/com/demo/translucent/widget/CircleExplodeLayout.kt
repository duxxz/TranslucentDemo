package com.demo.translucent.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import com.demo.translucent.utils.floatDp
import com.demo.translucent.utils.phoneScreenHeight
import com.demo.translucent.utils.phoneScreenWidth
import kotlin.math.PI
import kotlin.math.cos

class CircleExplodeLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var mPath: Path = Path()
    private var mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    private val mRectF: RectF = RectF()

    private val startRadius by lazy { resources.floatDp(20f) }
    private var maxRadius: Float = 0f
    private var curRadius: Float = 0f
    private val centerPoint: PointF = PointF()

    override fun dispatchDraw(canvas: Canvas) {
        if (Build.VERSION.SDK_INT >= 28) {
            dispatchDraw28(canvas)
        } else {
            dispatchDraw27(canvas)
        }
    }

    private fun dispatchDraw27(canvas: Canvas) {
        canvas.saveLayer(mRectF, null)
        super.dispatchDraw(canvas)
        canvas.drawPath(genPath(), mPaint)
        canvas.restore()
    }

    private fun dispatchDraw28(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(genPath())
        super.dispatchDraw(canvas)
        canvas.restore()
    }

    private var loadAnim: ValueAnimator? = null
    private val animListener by lazy {
        ValueAnimator.AnimatorUpdateListener { animation ->
            (animation.animatedValue as? Float)?.let { rr ->
                curRadius = rr
                invalidate()
            }
        }
    }


    private val ph = context.phoneScreenHeight()
    private val pw = context.phoneScreenWidth()
    private val ratio by lazy { 2 * cos(45 * PI / 180).toFloat() }

    /**
     * 圆点扩散动画, 圆点 由 startX, startY 判定
     */
    fun startLoad(point: PointF? = null) {
        if (loadAnim?.isRunning == true) {
            return
        }

        //todo: 计算圆圈半径
        mRectF.set(0f, 0f, pw.toFloat(), ph.toFloat())

        val centerX = pw / 2f
        val centerY = ph / 2f
        maxRadius = if (point != null) {
            centerPoint.set(point)
            val mr = (ph - point.y).coerceAtLeast(point.y)
            mr * 2 / ratio
        } else {
            centerPoint.set(centerX, centerY)
            ph / ratio
        }

        loadAnim?.cancel()
        loadAnim = ValueAnimator.ofFloat(startRadius, maxRadius).apply {
            duration = 900
            interpolator = AccelerateInterpolator()
        }
        loadAnim?.addUpdateListener(animListener)
        loadAnim?.start()
    }

    private fun genPath(): Path {
        mPath.reset()
        mPath.addCircle(centerPoint.x, centerPoint.y, curRadius, Path.Direction.CW)
        return mPath
    }
}