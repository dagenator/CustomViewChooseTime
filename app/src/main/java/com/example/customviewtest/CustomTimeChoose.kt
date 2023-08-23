package com.example.customviewtest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class CustomTimeChoose @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var time = CustomClockTime(LocalDateTime.now(), 0)
    private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM")
    private val hourFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH")
    private val minutesFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("mm")
    private var height = 0f

    private var xCenter = 0
    private var yCenter = 0

    private var extra = 0

    private val textPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
        strokeWidth = 10f
    }

    private val linePaint = Paint().apply {
        strokeWidth = 10f
        style = Paint.Style.FILL
    }

    var delayInMinutes = 0f
    var speed = 1L

    // Обнаружение и расчет скрола
    private val gestureDetector =
        GestureDetector(context, CustomScaleListener { x, y -> doOnScroll(x, y) })


    init {
        context.theme.obtainStyledAttributes(
            attrs, R.styleable.CustomTimeChoose, 0, 0
        ).apply {

            try {
                delayInMinutes = getFloat(R.styleable.CustomTimeChoose_delayMinutes, 0.0f)
                textPaint.apply {
                    color = getColor(
                        R.styleable.CustomTimeChoose_textColor,
                        context.getColor(R.color.md_theme_light_primary)
                    )
                    textSize = getFloat(R.styleable.CustomTimeChoose_testSize, 55f)
                    height = textSize
                }
                speed = getInteger(R.styleable.CustomTimeChoose_speed, 1).toLong()
                extra = getInteger(R.styleable.CustomTimeChoose_extra, 0)
                linePaint.apply {
                    color = getColor(
                        R.styleable.CustomTimeChoose_borderColor,
                        context.getColor(R.color.md_theme_light_primary)
                    )
                }

            } finally {
                recycle()
            }
        }
    }

    var oneSymbolWidth = textPaint.measureText("0")
    private var shiftBetween = oneSymbolWidth * 4

    public fun setCustomViewTime(newTime: CustomClockTime) {
        time = newTime
        val width = textPaint.measureText(time.time.format(dayFormatter))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        return gestureDetector.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        xCenter = measuredWidth / 2
        yCenter = measuredHeight / 2
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            drawTime(it, dayFormatter, -shiftBetween - oneSymbolWidth, extra)
            drawTime(it, hourFormatter, 0f, extra)
            drawTime(it, minutesFormatter, shiftBetween, extra)
        }
    }

    private fun drawTime(canvas: Canvas, formatter: DateTimeFormatter, shift: Float, step: Int) {
        if (step == 0) {
            drawTimeWithAlfha(time.time, canvas, formatter, shift)
            return
        }

        val diffMinutes = 10L
        val alphaStep = 255/step
        for (i in -(step-1)..(step-1)) {
            var dif = step - abs(i)
            drawTimeWithAlfha(
                time.time.minusMinutes(diffMinutes * i),
                canvas,
                formatter,
                shift,
                if (i == 0) 255 else (alphaStep*dif),
                i
            )
        }
    }

    private fun drawTimeWithAlfha(
        time: LocalDateTime,
        canvas: Canvas,
        formatter: DateTimeFormatter,
        shift: Float,
        alpha: Int = 255,
        verticalShift: Int = 0
    ) {
        textPaint.apply {
            this.alpha = alpha
        }
        canvas.drawText(
            time.format(formatter),
            xCenter.toFloat() + shift,
            yCenter.toFloat() + (verticalShift * height),
            textPaint
        )
        canvas.drawLine(
            xCenter - oneSymbolWidth * 2,
            yCenter + height,
            xCenter - oneSymbolWidth * 2,
            yCenter - (height * 1.5).toFloat(),
            linePaint
        )
        canvas.drawLine(
            xCenter + oneSymbolWidth * 2,
            yCenter + height,
            xCenter + oneSymbolWidth * 2,
            yCenter - (height * 1.5).toFloat(),
            linePaint
        )
    }

    private fun doOnScroll(negativeScroll: Boolean, firstMotionEvent: MotionEvent) {
        // расчет движения в диапазоне определенных цифр
        var result = time.time
        if (firstMotionEvent.x < xCenter - oneSymbolWidth * 2) {
            if (negativeScroll) {
                result = time.time.minusHours(speed)
            } else {
                result = time.time.plusHours(speed)
            }
        } else if (firstMotionEvent.x > (xCenter - oneSymbolWidth * 2) && firstMotionEvent.x < (xCenter + oneSymbolWidth * 2)) {
            if (negativeScroll) {
                result = time.time.minusMinutes(speed)
            } else {
                result = time.time.plusMinutes(speed)
            }
        } else {
            if (negativeScroll) {
                result = time.time.minusSeconds(speed)
            } else {
                result = time.time.plusSeconds(speed)
            }
        }

        if (result > LocalDateTime.now()) {
            if (delayInMinutes == 0f || (delayInMinutes > 0 && result < LocalDateTime.now()
                    .plusMinutes(delayInMinutes.toLong()))
            ) {
                time.time = result
            }
        }

        requestLayout()
        invalidate()
    }
}

data class CustomClockTime(var time: LocalDateTime, var delay: Long)

private class CustomScaleListener(private val doOnScroll: (Boolean, MotionEvent) -> Unit) :
    GestureDetector.OnGestureListener {
    override fun onDown(e: MotionEvent): Boolean {
        return true
    }

    override fun onShowPress(e: MotionEvent) {
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(
        e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float
    ): Boolean {
        if (distanceY > 0) {
            doOnScroll(false, e1)
        } else {
            doOnScroll(true, e1)
        }
        return false
    }

    override fun onLongPress(e: MotionEvent) {
    }

    override fun onFling(
        e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float
    ): Boolean {
        return false
    }
}