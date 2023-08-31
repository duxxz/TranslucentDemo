package com.demo.translucent.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.util.ArrayMap
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.demo.translucent.R
import com.demo.translucent.async.SafeRunnable
import com.demo.translucent.async.SafeTask
import com.demo.translucent.utils.isValidUrl
import com.demo.translucent.utils.lifecycleOwner
import com.demo.translucent.utils.matchParent
import com.demo.translucent.utils.parseAttrs
import com.opensource.svgaplayer.SVGACallback
import com.opensource.svgaplayer.SVGADrawable
import com.opensource.svgaplayer.SVGADynamicEntity
import com.opensource.svgaplayer.SVGAImageView
import com.opensource.svgaplayer.SVGAParser
import com.opensource.svgaplayer.SVGAVideoEntity
import java.lang.ref.WeakReference

/**
 * 加载本地 资源 文件使用
 * 增加了网络资源加载
 * ownParser 使用独立的解析器
 */
open class AssetsSVGAImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0, private var ownParser: Boolean = false
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes), DefaultLifecycleObserver {

    companion object {
        // 缓存 动画
        private val map = HashMap<String, WeakReference<SVGAVideoEntity>>()
    }

    private var assetPath: String? = null
    private var urlPath: String? = null
    private var playing: Boolean = false
    private val svgaView = SVGAImageView(context, null)

    private var backStop = svgaView.loops != 1
    var autoPlay = true

    private var parseLock: Any? = null
    private val parser by lazy { if (ownParser) SVGAParser(context) else SVGAParser.shareParser() }

    var owner: LifecycleOwner? = null
        set(value) {
            field?.lifecycle?.removeObserver(this)
            value?.lifecycle?.addObserver(this)
            field = value
        }

    var svgaStepListener: SafeTask<Pair<Int, Double>>? = null

    var svgaEndListener: SafeRunnable? = null

    private val sScaleTypeArray = arrayOf(
        ImageView.ScaleType.MATRIX,
        ImageView.ScaleType.FIT_XY,
        ImageView.ScaleType.FIT_START,
        ImageView.ScaleType.FIT_CENTER,
        ImageView.ScaleType.FIT_END,
        ImageView.ScaleType.CENTER,
        ImageView.ScaleType.CENTER_CROP,
        ImageView.ScaleType.CENTER_INSIDE
    )

    init {
        R.styleable.AssetsSVGAImageView.parseAttrs(context, attrs) {
            getDrawable(R.styleable.AssetsSVGAImageView_android_src)?.also {
                svgaView.setImageDrawable(it)
            }
            getInt(R.styleable.AssetsSVGAImageView_android_scaleType, -1).also {
                if (it >= 0) {
                    svgaView.scaleType = sScaleTypeArray.getOrNull(it) ?: ImageView.ScaleType.CENTER_CROP
                }
            }
            ownParser = getBoolean(R.styleable.AssetsSVGAImageView_own_parser, false)
            val frameWidth = getDimensionPixelSize(R.styleable.AssetsSVGAImageView_frame_width, 0)
            if (frameWidth > 0) {
                parser.setFrameSize(frameWidth, parser.mFrameHeight)
            }
            svgaView.loops = getInt(R.styleable.AssetsSVGAImageView_loopCount, 0)
            backStop = getBoolean(R.styleable.AssetsSVGAImageView_svga_back_stop, svgaView.loops != 1)
            autoPlay = getBoolean(R.styleable.AssetsSVGAImageView_autoPlay, true)
            getString(R.styleable.AssetsSVGAImageView_svga_end_mode)?.let {
                when (it) {
                    "0" -> {
                        svgaView.fillMode = SVGAImageView.FillMode.Backward
                    }

                    "1" -> {
                        svgaView.fillMode = SVGAImageView.FillMode.Forward
                    }

                    "2" -> {
                        svgaView.fillMode = SVGAImageView.FillMode.Clear
                    }
                }
            }
            getString(R.styleable.AssetsSVGAImageView_source)?.also {
                openAssets(it)
            }
        }
        //这里存在一定的延时性，默认关闭避免setImage无效
//        svgaView.clearsAfterStop = false
        super.addView(svgaView, matchParent, matchParent)

        svgaView.callback = object : SVGACallback {
            override fun onFinished() {
                svgaEndListener?.run()
                playing = false
            }

            override fun onPause() {

            }

            override fun onRepeat() {

            }

            override fun onStep(frame: Int, percentage: Double) {
                svgaStepListener?.run(Pair(frame, percentage))
            }

        }
    }

    val isLoading get() = svgaView.drawable == null || svgaView.drawable is ColorDrawable

    val isEmptyDrawable: Boolean get() = !isAnimating && isLoading

    private var userPause = false
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        pause(false)
    }

    fun pause(action: Boolean = true) {
        if (action) userPause = true
        if (backStop && loops != 1 && isAnimating) {
            pauseAnimation()
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        if (loops == 1 && isAnimating && svgaView.fillMode == SVGAImageView.FillMode.Clear) {
            stopAnimation(true)
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        resume(false)
    }

    fun resume(action: Boolean = true) {
        if (action) userPause = false
        if (backStop && !userPause) {
            startAnimation()
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (changedView == this) {
            when (visibility) {
                VISIBLE -> resume(false)
                else -> pause(false)
            }
        }
    }

    fun openAssets(asset: String?) {
        asset ?: return
        if (asset == this.assetPath && svgaView.drawable is SVGADrawable) {
            if (!isAnimating) start()
            return
        }
        this.assetPath = asset
        svgaView.stopAnimation()
        val cache = map[asset]?.get()
        if (cache != null && (cache.mFrameWidth == 0 || (parser.mFrameWidth > 0 && parser.mFrameWidth < cache.mFrameWidth))) {
            start(cache)
        } else {
            // 去获取
            getSVGAVideoEntity(asset)
        }
    }

    val isAnimating: Boolean
        get() = svgaView.isAnimating

    var loops: Int
        get() = svgaView.loops
        set(value) {
            svgaView.loops = value
        }

    var scaleType: ImageView.ScaleType
        get() = svgaView.scaleType
        set(value) {
            svgaView.scaleType = value
        }

    fun resetFrame() {
        svgaView.stepToFrame(0, false)
    }

    fun stopAnimation(clear: Boolean = false) {
        svgaView.stopAnimation(clear)
        //fixme: 手动清理一下
        if (clear) svgaView.clear()
        playing = false
    }

    fun pauseAnimation() {
        svgaView.pauseAnimation()
        playing = false
    }

    /**
     * Decode_svga文件。
     */
    private fun getSVGAVideoEntity(assetPath: String?, imageUrl: String? = null, imgKey: String? = null, imgRes: Int? = null) {
        assetPath ?: return
        val lock = Any()
        parseLock = lock
        parser.decodeFromAssets(
            assetPath, createParseCallback(
                WeakReference(this),
                lock, url = assetPath, imageUrl = imageUrl, imgKey = imgKey, imgRes = imgRes
            )
        )
    }

    fun openAssetReplaceImg(asset: String?, imageUrl: String, imgKey: String) {
        asset ?: return
        if (asset == this.assetPath && svgaView.drawable is SVGADrawable) {
            if (!isAnimating) start(imageUrl = imageUrl, imgKey = imgKey)
            return
        }
        this.assetPath = asset
        svgaView.stopAnimation()
        val cache = map[asset]?.get()
        if (cache != null && (cache.mFrameWidth == 0 || (parser.mFrameWidth > 0 && parser.mFrameWidth < cache.mFrameWidth))) {
            start(cache, imageUrl = imageUrl, imgKey = imgKey)
        } else {
            // 去获取
            getSVGAVideoEntity(asset, imageUrl, imgKey)
        }
    }

    fun openAssetReplaceRes(asset: String?, imgRes: Int?, imgKey: String) {
        asset ?: return
        if (asset == this.assetPath && svgaView.drawable is SVGADrawable) {
            if (!isAnimating) start(imgRes = imgRes, imgKey = imgKey)
            return
        }
        this.assetPath = asset
        svgaView.stopAnimation()
        val cache = map[asset]?.get()
        if (cache != null && (cache.mFrameWidth == 0 || (parser.mFrameWidth > 0 && parser.mFrameWidth < cache.mFrameWidth))) {
            start(cache, imgRes = imgRes, imgKey = imgKey)
        } else {
            // 去获取
            getSVGAVideoEntity(asset, imageUrl = null, imgRes = imgRes, imgKey = imgKey)
        }
    }

    /**
     * 设置弱引用的加载,防止 Memory Leaking
     * int: -1 失败 0-加载成功未播放 1-播放
     */
    private val loadURLCallback = ArrayMap<Any, ((Int) -> Unit)>()
    private fun createParseCallback(
        ref: WeakReference<AssetsSVGAImageView>,
        syncLock: Any,
        url: String? = null,
        imageUrl: String? = null,
        imgRes: Int? = null,
        imgKey: String? = null,
        onResult: ((Int) -> Unit)? = null
    ): SVGAParser.ParseCompletion {
        onResult?.let {
            loadURLCallback.put(syncLock, onResult)
        }
        return object : SVGAParser.ParseCompletion {
            private var lock = syncLock
            private var cacheKey = url
            override fun onComplete(videoItem: SVGAVideoEntity) {
                ref.get()?.loadURLCompletion(lock, cacheKey, videoItem, true, imageUrl, imgKey, imgRes)
            }

            override fun onError() {
                ref.get()?.loadURLCompletion(lock, cacheKey, null, false, imgRes = imgRes)
            }
        }
    }

    private fun loadURLCompletion(
        lock: Any,
        cacheKey: String?,
        videoItem: SVGAVideoEntity?,
        success: Boolean,
        imageUrl: String? = null,
        imgKey: String? = null,
        imgRes: Int? = null
    ) {
        if (!cacheKey.isNullOrEmpty() && videoItem != null) {
            map[cacheKey] = WeakReference(videoItem)
        }
        if (lock == parseLock) {
            parseLock = null
            val result = loadURLCallback.remove(lock)
            result?.invoke(if (success) 0 else -1)
            if (success) {
                start(videoItem, imageUrl, imgKey, imgRes = imgRes)
                result?.invoke(1)
            }
        }
    }

    fun setFrameSize(width: Int) {
        parser.setFrameSize(width, parser.mFrameHeight)
    }

    fun setImageResource(res: Int) {
        stopAnimation()
        parseLock = null
        assetPath = null
        svgaView.setImageResource(res)
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        svgaView.isSelected = selected
    }

    fun setImageBitmap(res: Bitmap) {
        stopAnimation()
        parseLock = null
        assetPath = null
        svgaView.setImageBitmap(res)
    }

    fun loadImageUrl(setter: ImageView.() -> Unit) {
        stopAnimation()
        parseLock = null
        assetPath = null
        setter.invoke(svgaView)
    }

    fun startAnimation() {
        svgaView.startAnimation()
        playing = true
    }

    private fun start(videoItem: SVGAVideoEntity? = null, imageUrl: String? = null, imgKey: String? = null, imgRes: Int? = null) {
        if (videoItem != null) {
            //todo: 使用glide加载图片bitmap,利用缓存
            val dynamicEntity = if ((imgRes ?: 0) > 0 && !imgKey.isNullOrEmpty()) {
                SVGADynamicEntity().apply {
                    setDynamicImage(BitmapFactory.decodeResource(resources, imgRes!!), imgKey)
                }
            } else if (!imageUrl.isNullOrEmpty() && imageUrl.isValidUrl() && !imgKey.isNullOrEmpty()) {
                SVGADynamicEntity().apply {
                    setDynamicImage(imageUrl, imgKey)
                }
            } else null
            svgaView.setVideoItem(videoItem, dynamicEntity)
        }
        if (autoPlay || playing) startAnimation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (loops == 0) startAnimation()
        owner = try {
            val childLifecycle = FragmentManager.findFragment<Fragment>(this)
            childLifecycle
        } catch (ignore: Exception) {
            context.lifecycleOwner()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearAnimation()
        pauseAnimation()
        loadURLCallback.clear()
        owner?.lifecycle?.removeObserver(this)
        owner = null
    }
}