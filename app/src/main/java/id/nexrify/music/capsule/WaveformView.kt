package id.nexrify.music.capsule

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.sin

/** Bar gelombang kecil di sisi kanan kapsul. Bergerak saat lagu diputar, diam rendah saat dijeda. */
class WaveformView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xB3FFFFFF.toInt()
        style = Paint.Style.FILL
    }
    private val rect = RectF()
    private val barCount = 7
    private var phase = 0f
    private var active = false

    private val animator = ValueAnimator.ofFloat(0f, (Math.PI * 2).toFloat()).apply {
        duration = 1100L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    fun setActive(playing: Boolean) {
        if (active == playing) return
        active = playing
        if (playing && isAttachedToWindow) animator.start() else {
            animator.cancel()
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (active) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val gap = w / (barCount * 2f)
        val barW = gap
        val minH = h * 0.18f
        for (i in 0 until barCount) {
            val level = if (active) {
                0.35f + 0.65f * abs(sin(phase + i * 0.9f))
            } else 0.0f
            val barH = minH + (h - minH) * level
            val left = i * (barW + gap) + gap / 2f
            val top = (h - barH) / 2f
            rect.set(left, top, left + barW, top + barH)
            canvas.drawRoundRect(rect, barW / 2f, barW / 2f, paint)
        }
    }
}
