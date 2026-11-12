package com.pvca.app.service.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.pvca.app.data.model.DetectedObjectItem
import com.pvca.app.data.model.ProductItem

/**
 * Factory and builder for system-wide floating overlay views.
 *
 * Implements:
 * 1. Floating Control Bar (draggable, scan button, stop button, status indicator).
 * 2. Interactive Blue Markers (placed at detected object coordinates).
 * 3. Floating Product Card (shows thumbnail, matched product, refinement, and PayPal Sandbox checkout).
 */
object OverlayViews {

    private fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * Creates a rounded [GradientDrawable] background.
     */
    private fun createRoundedBackground(
        fillColor: Int,
        cornerRadiusPx: Float,
        strokeColor: Int? = null,
        strokeWidthPx: Int = 0
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            cornerRadius = cornerRadiusPx
            if (strokeColor != null && strokeWidthPx > 0) {
                setStroke(strokeWidthPx, strokeColor)
            }
        }
    }

    /**
     * Builds the persistent, draggable floating control bar for PVCA screen scanning.
     */
    fun createFloatingControlBar(
        context: Context,
        windowManager: WindowManager,
        params: WindowManager.LayoutParams,
        onScanClicked: () -> Unit,
        onStopClicked: () -> Unit
    ): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = dpToPx(context, 12)
            val padV = dpToPx(context, 8)
            setPadding(padH, padV, padH, padV)
            background = createRoundedBackground(
                fillColor = Color.parseColor("#1E293B"),
                cornerRadiusPx = dpToPx(context, 20).toFloat(),
                strokeColor = Color.parseColor("#0070BA"),
                strokeWidthPx = dpToPx(context, 1)
            )
            elevation = dpToPx(context, 8).toFloat()
        }

        // 1. PVCA Brand indicator with glowing dot
        val brandLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, dpToPx(context, 8), 0)
        }

        val dotView = View(context).apply {
            val size = dpToPx(context, 10)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dpToPx(context, 6)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#00CFE8"))
            }
        }
        brandLayout.addView(dotView)

        val brandText = TextView(context).apply {
            text = "PVCA"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        brandLayout.addView(brandText)
        root.addView(brandLayout)

        // 2. Scan Screen Action Button
        val scanBtn = Button(context).apply {
            text = "🔍 Scan"
            setTextColor(Color.WHITE)
            textSize = 12f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            val btnPadH = dpToPx(context, 10)
            val btnPadV = dpToPx(context, 4)
            setPadding(btnPadH, btnPadV, btnPadH, btnPadV)
            background = createRoundedBackground(
                fillColor = Color.parseColor("#0070BA"),
                cornerRadiusPx = dpToPx(context, 12).toFloat()
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(context, 32)
            ).apply {
                marginEnd = dpToPx(context, 8)
            }
            setOnClickListener { onScanClicked() }
        }
        root.addView(scanBtn)

        // 3. Stop Action Button
        val stopBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.parseColor("#EF4444"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val btnSize = dpToPx(context, 28)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize)
            background = createRoundedBackground(
                fillColor = Color.parseColor("#334155"),
                cornerRadiusPx = dpToPx(context, 14).toFloat()
            )
            setOnClickListener { onStopClicked() }
        }
        root.addView(stopBtn)

        // Touch listener for dragging the floating control bar
        root.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isDragging = true
                            params.x = initialX + dx
                            params.y = initialY + dy
                            try {
                                windowManager.updateViewLayout(root, params)
                            } catch (e: Exception) {}
                        }
                        return isDragging
                    }
                    MotionEvent.ACTION_UP -> {
                        return isDragging
                    }
                }
                return false
            }
        })

        return root
    }

    /**
     * Builds an interactive Blue Marker Dot view to position over a detected object.
     */
    fun createBlueDotMarker(
        context: Context,
        detectedObject: DetectedObjectItem,
        onMarkerTapped: () -> Unit
    ): View {
        val sizePx = dpToPx(context, 40)

        val container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
        }

        // Outer pulsing ring / border
        val outerCircle = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx).apply {
                gravity = Gravity.CENTER
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#0070BA"))
                setStroke(dpToPx(context, 2), Color.WHITE)
            }
            elevation = dpToPx(context, 6).toFloat()
        }
        container.addView(outerCircle)

        // Inner glowing core
        val innerCore = View(context).apply {
            val innerSize = dpToPx(context, 14)
            layoutParams = FrameLayout.LayoutParams(innerSize, innerSize).apply {
                gravity = Gravity.CENTER
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#00CFE8"))
            }
        }
        container.addView(innerCore)

        container.setOnClickListener {
            onMarkerTapped()
        }

        return container
    }

    /**
     * Data holder and controller for the Floating Shopping Card.
     */
    class ShoppingCardController(
        val rootView: View,
        val thumbnailView: ImageView,
        val categoryText: TextView,
        val statusText: TextView,
        val matchCardLayout: LinearLayout,
        val productTitleText: TextView,
        val productDetailsText: TextView,
        val matchBadgeText: TextView,
        val queryEditText: EditText,
        val searchButton: Button,
        val checkoutButton: Button,
        val progressBar: ProgressBar,
        val closeButton: TextView
    )

    /**
     * Builds the floating shopping card dialog.
     */
    fun createFloatingShoppingCard(
        context: Context,
        onCloseClicked: () -> Unit,
        onRefineClicked: (String) -> Unit,
        onCheckoutClicked: () -> Unit
    ): ShoppingCardController {
        val pad = dpToPx(context, 16)

        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val cardWidth = dpToPx(context, 330)
            layoutParams = LinearLayout.LayoutParams(cardWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(pad, pad, pad, pad)
            background = createRoundedBackground(
                fillColor = Color.parseColor("#0F172A"),
                cornerRadiusPx = dpToPx(context, 16).toFloat(),
                strokeColor = Color.parseColor("#0070BA"),
                strokeWidthPx = dpToPx(context, 1)
            )
            elevation = dpToPx(context, 12).toFloat()
        }

        // Header: Brand + Title + Close Button
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(context, 12)
            }
        }

        val titleCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val title = TextView(context).apply {
            text = "PVCA Visual Discovery"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
        }
        titleCol.addView(title)

        val categoryText = TextView(context).apply {
            text = "Detected: Object"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
        }
        titleCol.addView(categoryText)
        header.addView(titleCol)

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 16f
            gravity = Gravity.CENTER
            val size = dpToPx(context, 28)
            layoutParams = LinearLayout.LayoutParams(size, size)
            background = createRoundedBackground(
                fillColor = Color.parseColor("#1E293B"),
                cornerRadiusPx = dpToPx(context, 14).toFloat()
            )
            setOnClickListener { onCloseClicked() }
        }
        header.addView(closeBtn)
        cardLayout.addView(header)

        // Object Thumbnail Preview
        val thumbnailView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(context, 90)
            ).apply {
                bottomMargin = dpToPx(context, 10)
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = createRoundedBackground(
                fillColor = Color.parseColor("#1E293B"),
                cornerRadiusPx = dpToPx(context, 8).toFloat()
            )
        }
        cardLayout.addView(thumbnailView)

        // Progress Bar
        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(context, 4)
            ).apply {
                bottomMargin = dpToPx(context, 8)
            }
        }
        cardLayout.addView(progressBar)

        // Status text
        val statusText = TextView(context).apply {
            text = "Analyzing visual features and searching catalogue..."
            setTextColor(Color.parseColor("#00CFE8"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(context, 8)
            }
        }
        cardLayout.addView(statusText)

        // Matched Product Details Layout
        val matchCardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val innerPad = dpToPx(context, 10)
            setPadding(innerPad, innerPad, innerPad, innerPad)
            background = createRoundedBackground(
                fillColor = Color.parseColor("#1E293B"),
                cornerRadiusPx = dpToPx(context, 8).toFloat()
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(context, 10)
            }
        }

        val matchBadgeText = TextView(context).apply {
            text = "Matched Product"
            setTextColor(Color.parseColor("#00CFE8"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(context, 4)
            }
        }
        matchCardLayout.addView(matchBadgeText)

        val productTitleText = TextView(context).apply {
            text = "Product Name"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(context, 2)
            }
        }
        matchCardLayout.addView(productTitleText)

        val productDetailsText = TextView(context).apply {
            text = "$0.00 • Merchant"
            setTextColor(Color.parseColor("#10B981"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        matchCardLayout.addView(productDetailsText)
        cardLayout.addView(matchCardLayout)

        // Search Refinement Input Row
        val searchRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(context, 12)
            }
        }

        val queryEditText = EditText(context).apply {
            hint = "Refine (e.g. ceiling fan remote)"
            setHintTextColor(Color.parseColor("#64748B"))
            setTextColor(Color.WHITE)
            textSize = 12f
            maxLines = 1
            inputType = InputType.TYPE_CLASS_TEXT
            val editPad = dpToPx(context, 8)
            setPadding(editPad, editPad, editPad, editPad)
            background = createRoundedBackground(
                fillColor = Color.parseColor("#1E293B"),
                cornerRadiusPx = dpToPx(context, 8).toFloat(),
                strokeColor = Color.parseColor("#334155"),
                strokeWidthPx = dpToPx(context, 1)
            )
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(context, 36), 1f).apply {
                marginEnd = dpToPx(context, 6)
            }
        }
        searchRow.addView(queryEditText)

        val searchButton = Button(context).apply {
            text = "Refine"
            setTextColor(Color.WHITE)
            textSize = 11f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            val btnPad = dpToPx(context, 6)
            setPadding(btnPad, 0, btnPad, 0)
            background = createRoundedBackground(
                fillColor = Color.parseColor("#0070BA"),
                cornerRadiusPx = dpToPx(context, 8).toFloat()
            )
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(context, 65),
                dpToPx(context, 36)
            )
            setOnClickListener {
                val q = queryEditText.text.toString().trim()
                if (q.isNotEmpty()) {
                    onRefineClicked(q)
                }
            }
        }
        searchRow.addView(searchButton)
        cardLayout.addView(searchRow)

        // PayPal Sandbox Checkout Button
        val checkoutButton = Button(context).apply {
            text = "Checkout with PayPal Sandbox"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = createRoundedBackground(
                fillColor = Color.parseColor("#0070BA"),
                cornerRadiusPx = dpToPx(context, 10).toFloat()
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(context, 44)
            )
            setOnClickListener { onCheckoutClicked() }
        }
        cardLayout.addView(checkoutButton)

        return ShoppingCardController(
            rootView = cardLayout,
            thumbnailView = thumbnailView,
            categoryText = categoryText,
            statusText = statusText,
            matchCardLayout = matchCardLayout,
            productTitleText = productTitleText,
            productDetailsText = productDetailsText,
            matchBadgeText = matchBadgeText,
            queryEditText = queryEditText,
            searchButton = searchButton,
            checkoutButton = checkoutButton,
            progressBar = progressBar,
            closeButton = closeBtn
        )
    }
}
