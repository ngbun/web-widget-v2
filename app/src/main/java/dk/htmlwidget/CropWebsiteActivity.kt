package dk.htmlwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

class CropWebsiteActivity : Activity() {

    private var appWidgetId =
        AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId =
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )

        if (
            appWidgetId ==
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) {
            finish()
            return
        }

        val previewFile =
            File(
                cacheDir,
                "crop_preview_$appWidgetId.png"
            )

        if (!previewFile.exists()) {

            Toast.makeText(
                this,
                "Opdater widgetten først, og prøv derefter igen.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        val previewBitmap =
            BitmapFactory.decodeFile(
                previewFile.absolutePath
            )

        if (previewBitmap == null) {

            Toast.makeText(
                this,
                "Kunne ikke åbne forhåndsvisningen.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        val manager =
            AppWidgetManager.getInstance(this)

        val options =
            manager.getAppWidgetOptions(
                appWidgetId
            )

        val density =
            resources.displayMetrics.density

        val widgetWidth = (
            options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                300
            ) * density
        ).roundToInt()
            .coerceAtLeast(1)

        val widgetHeight = (
            options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                180
            ) * density
        ).roundToInt()
            .coerceAtLeast(1)

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL
            }

        root.addView(
            TextView(this).apply {

                text =
                    "Scroll gennem hjemmesiden. Tryk på det område, der skal vises i widgetten."

                textSize =
                    16f

                setPadding(
                    30,
                    20,
                    30,
                    20
                )
            }
        )

        val cropView =
            CropPreviewView(
                previewBitmap,
                widgetWidth,
                widgetHeight
            )

        val scrollView =
            ScrollView(this).apply {

                isFillViewport =
                    false

                addView(
                    cropView,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )
            }

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        root.addView(
            Button(this).apply {

                text =
                    "Gem dette område"

                setOnClickListener {

                    val prefs =
                        getSharedPreferences(
                            "settings",
                            MODE_PRIVATE
                        )

                    prefs.edit()
                        .putInt(
                            "cropBitmapX_$appWidgetId",
                            cropView.cropX
                        )
                        .putInt(
                            "cropBitmapY_$appWidgetId",
                            cropView.cropY
                        )
                        .putBoolean(
                            "cropEnabled_$appWidgetId",
                            true
                        )
                        .apply()

                    Toast.makeText(
                        this@CropWebsiteActivity,
                        "Området er gemt",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }
            }
        )

        setContentView(root)
    }

    private inner class CropPreviewView(
        private val bitmap: Bitmap,
        private val cropWidth: Int,
        private val cropHeight: Int
    ) : View(this) {

        var cropX =
            0
            private set

        var cropY =
            0
            private set

        private var displayScale =
            1f

        private var touchDownX =
            0f

        private var touchDownY =
            0f

        private val bitmapPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG or
                    Paint.FILTER_BITMAP_FLAG
            )

        private val framePaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                style =
                    Paint.Style.STROKE

                strokeWidth =
                    6f

                color =
                    android.graphics.Color.RED
            }

        private val shadePaint =
            Paint().apply {

                color =
                    android.graphics.Color.argb(
                        100,
                        0,
                        0,
                        0
                    )
            }

        init {

            val prefs =
                getSharedPreferences(
                    "settings",
                    MODE_PRIVATE
                )

            cropX =
                prefs.getInt(
                    "cropBitmapX_$appWidgetId",
                    0
                )

            cropY =
                prefs.getInt(
                    "cropBitmapY_$appWidgetId",
                    0
                )

            isClickable =
                true
        }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int
        ) {

            val measuredWidth =
                MeasureSpec.getSize(
                    widthMeasureSpec
                ).coerceAtLeast(1)

            displayScale =
                measuredWidth.toFloat() /
                    bitmap.width.toFloat()

            val measuredHeight =
                (
                    bitmap.height *
                        displayScale
                    ).roundToInt()
                    .coerceAtLeast(1)

            setMeasuredDimension(
                measuredWidth,
                measuredHeight
            )
        }

        override fun onDraw(
            canvas: Canvas
        ) {

            super.onDraw(canvas)

            if (
                width <= 0 ||
                height <= 0
            ) {
                return
            }

            displayScale =
                width.toFloat() /
                    bitmap.width.toFloat()

            val shownHeight =
                bitmap.height *
                    displayScale

            val destination =
                RectF(
                    0f,
                    0f,
                    width.toFloat(),
                    shownHeight
                )

            canvas.drawBitmap(
                bitmap,
                null,
                destination,
                bitmapPaint
            )

            val maxX =
                (
                    bitmap.width -
                        cropWidth
                    ).coerceAtLeast(0)

            val maxY =
                (
                    bitmap.height -
                        cropHeight
                    ).coerceAtLeast(0)

            cropX =
                cropX.coerceIn(
                    0,
                    maxX
                )

            cropY =
                cropY.coerceIn(
                    0,
                    maxY
                )

            val left =
                cropX *
                    displayScale

            val top =
                cropY *
                    displayScale

            val right =
                left +
                    cropWidth *
                        displayScale

            val bottom =
                top +
                    cropHeight *
                        displayScale

            canvas.drawRect(
                0f,
                0f,
                width.toFloat(),
                top,
                shadePaint
            )

            canvas.drawRect(
                0f,
                bottom,
                width.toFloat(),
                shownHeight,
                shadePaint
            )

            canvas.drawRect(
                0f,
                top,
                left,
                bottom,
                shadePaint
            )

            canvas.drawRect(
                right,
                top,
                width.toFloat(),
                bottom,
                shadePaint
            )

            canvas.drawRect(
                left,
                top,
                right,
                bottom,
                framePaint
            )
        }

        override fun onTouchEvent(
            event: MotionEvent
        ): Boolean {

            if (displayScale <= 0f) {
                return false
            }

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    touchDownX =
                        event.x

                    touchDownY =
                        event.y

                    /*
                     * Vi tager imod starten af trykket,
                     * men forhindrer IKKE ScrollView
                     * i at overtage ved en swipe-bevægelse.
                     */
                    parent.requestDisallowInterceptTouchEvent(
                        false
                    )

                    return true
                }

                MotionEvent.ACTION_MOVE -> {

                    /*
                     * ScrollView må overtage bevægelsen.
                     */
                    parent.requestDisallowInterceptTouchEvent(
                        false
                    )

                    return true
                }

                MotionEvent.ACTION_UP -> {

                    val movedX =
                        abs(
                            event.x -
                                touchDownX
                        )

                    val movedY =
                        abs(
                            event.y -
                                touchDownY
                        )

                    /*
                     * Kun et kort tryk flytter rammen.
                     * Swipe bruges til scrolling.
                     */
                    if (
                        movedX < 30f &&
                        movedY < 30f
                    ) {

                        val bitmapX =
                            (
                                event.x /
                                    displayScale
                                ).roundToInt()

                        val bitmapY =
                            (
                                event.y /
                                    displayScale
                                ).roundToInt()

                        val maxX =
                            (
                                bitmap.width -
                                    cropWidth
                                ).coerceAtLeast(0)

                        val maxY =
                            (
                                bitmap.height -
                                    cropHeight
                                ).coerceAtLeast(0)

                        cropX =
                            (
                                bitmapX -
                                    cropWidth / 2
                                ).coerceIn(
                                    0,
                                    maxX
                                )

                        cropY =
                            (
                                bitmapY -
                                    cropHeight / 2
                                ).coerceIn(
                                    0,
                                    maxY
                                )

                        invalidate()

                        performClick()
                    }

                    return true
                }

                MotionEvent.ACTION_CANCEL -> {

                    parent.requestDisallowInterceptTouchEvent(
                        false
                    )

                    return true
                }
            }

            return true
        }

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }
    }
}
