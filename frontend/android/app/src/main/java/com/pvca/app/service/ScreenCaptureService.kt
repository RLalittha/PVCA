package com.pvca.app.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.pvca.app.MainActivity
import com.pvca.app.R
import com.pvca.app.data.analyzer.MlKitObjectDetector
import com.pvca.app.data.crop.ObjectCropAnalyzer
import com.pvca.app.data.model.DetectedObjectItem
import com.pvca.app.data.model.MatchResult
import com.pvca.app.data.model.ProductItem
import com.pvca.app.data.network.BackendConfig
import com.pvca.app.data.network.BackendOrderService
import com.pvca.app.data.network.CreateOrderResult
import com.pvca.app.data.network.HybridMatchingService
import com.pvca.app.data.repository.LocalAssetCatalogueRepository
import com.pvca.app.service.overlay.OverlayViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android Foreground Service managing system-wide screen capture (MediaProjection),
 * on-device ML Kit object detection, and interactive floating blue dot overlays.
 *
 * Adheres strictly to security and privacy guidelines:
 * - Session must be explicitly authorized by the user.
 * - Always shows a persistent foreground notification with a Stop control.
 * - Image processing is on-device; full screenshots are never logged or persisted.
 * - Only the selected object crop is transmitted to the matching service.
 */
class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "PVCA_OverlayService"

        const val ACTION_START = "com.pvca.app.action.START_CAPTURE"
        const val ACTION_STOP = "com.pvca.app.action.STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "pvca_screen_scanner_channel"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun start(context: Context, resultCode: Int, resultData: Intent) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private lateinit var windowManager: WindowManager
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val objectDetector = MlKitObjectDetector()
    private val cropAnalyzer = ObjectCropAnalyzer()
    private lateinit var matchingService: HybridMatchingService
    private val orderService = BackendOrderService()

    private var screenWidth = 1080
    private var screenHeight = 2400
    private var screenDensity = 400

    // Floating Views
    private var controlBarView: View? = null
    private val activeMarkerViews = mutableListOf<View>()
    private var shoppingCardController: OverlayViews.ShoppingCardController? = null

    // Frame capture synchronization
    @Volatile
    private var isScanPending = false
    private var latestFullBitmap: Bitmap? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val catalogueRepo = LocalAssetCatalogueRepository(applicationContext)
        matchingService = HybridMatchingService(catalogueRepo)

        handlerThread = HandlerThread("PvcaScreenCapture").apply { start() }
        backgroundHandler = Handler(handlerThread!!.looper)

        createNotificationChannel()
        computeScreenMetrics()
    }

    /**
     * Stops and releases all screen-capture resources (MediaProjection, VirtualDisplay,
     * ImageReader, bitmaps, and floating overlays) without stopping the service itself.
     *
     * Safe to call multiple times — all references are null-checked before release.
     * Called before launching the PayPal browser to ensure the payment page is never
     * captured, and also from onDestroy() for normal teardown.
     */
    private fun stopCapture() {
        // Cancel any pending scan immediately
        isScanPending = false

        // Remove all floating overlays
        clearMarkers()
        dismissShoppingCard()
        controlBarView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
            controlBarView = null
        }

        // Release VirtualDisplay first so no more frames arrive
        virtualDisplay?.release()
        virtualDisplay = null

        // Close ImageReader to free surface and buffer queue
        imageReader?.close()
        imageReader = null

        // Discard any retained screen bitmap
        latestFullBitmap?.recycle()
        latestFullBitmap = null

        // Stop MediaProjection last — this may trigger onStop() callback, which is now safe
        mediaProjection?.stop()
        mediaProjection = null

        Log.i(TAG, "stopCapture: all screen-capture resources released.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        if (action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_START) {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_RESULT_DATA)
            }

            if (resultCode != Activity.RESULT_OK || resultData == null) {
                Log.e(TAG, "Screen capture permission not granted.")
                stopSelf()
                return START_NOT_STICKY
            }

            startForegroundWithNotification()

            try {
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
                if (mediaProjection == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        // The system (or stopCapture()) stopped MediaProjection.
                        // Release remaining resources but do NOT call stopSelf() here —
                        // if this was triggered by our own stopCapture() during checkout,
                        // the service must remain alive long enough to fire startActivity().
                        Log.i(TAG, "MediaProjection.onStop(): releasing resources, service continues.")
                        // VirtualDisplay and ImageReader are already released by stopCapture()
                        // before mediaProjection.stop() is called, so this callback is a no-op
                        // in that path. If the system stopped projection independently, do a
                        // safe partial cleanup and then stop the service.
                        if (virtualDisplay != null || imageReader != null) {
                            // System-initiated stop: clean up and exit
                            virtualDisplay?.release(); virtualDisplay = null
                            imageReader?.close(); imageReader = null
                            latestFullBitmap?.recycle(); latestFullBitmap = null
                            stopSelf()
                        }
                        // If already cleaned up by stopCapture(), do nothing here.
                    }
                }, Handler(Looper.getMainLooper()))

                setupVirtualDisplay()
                showFloatingControlBar()
                _isServiceRunning.value = true
                Toast.makeText(this, "PVCA Visual Shopping active. Tap 'Scan' anytime!", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start MediaProjection: ${e.message}", e)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return START_NOT_STICKY
    }

    private fun computeScreenMetrics() {
        val metrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            screenWidth = bounds.width().coerceAtLeast(1)
            screenHeight = bounds.height().coerceAtLeast(1)
            screenDensity = resources.configuration.densityDpi
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            screenWidth = metrics.widthPixels.coerceAtLeast(1)
            screenHeight = metrics.heightPixels.coerceAtLeast(1)
            screenDensity = metrics.densityDpi
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PVCA Screen Scanner",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifies when PVCA is active for system-wide visual shopping."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PVCA Visual Shopping Active")
            .setContentText("Tap 'Scan' on the floating bar to detect products.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Scanning", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun setupVirtualDisplay() {
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "PvcaScreenCapture",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            backgroundHandler
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = try {
                reader.acquireLatestImage()
            } catch (e: Exception) {
                null
            } ?: return@setOnImageAvailableListener

            if (isScanPending) {
                isScanPending = false
                val bitmap = extractBitmapFromImage(image)
                image.close()
                if (bitmap != null) {
                    serviceScope.launch {
                        processCapturedFrame(bitmap)
                    }
                }
            } else {
                image.close()
            }
        }, backgroundHandler)
    }

    /**
     * Converts an [Image] from [ImageReader] into an in-memory [Bitmap].
     * Handles row stride / row padding safely to prevent pixel skew.
     */
    private fun extractBitmapFromImage(image: Image): Bitmap? {
        return try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            if (rowPadding > 0) {
                val clean = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
                if (clean != bitmap) bitmap.recycle()
                clean
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert Image to Bitmap: ${e.message}", e)
            null
        }
    }

    private fun showFloatingControlBar() {
        if (controlBarView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 30
            y = 120
        }

        controlBarView = OverlayViews.createFloatingControlBar(
            context = this,
            windowManager = windowManager,
            params = params,
            onScanClicked = {
                triggerScreenScan()
            },
            onStopClicked = {
                stopSelf()
            }
        )

        try {
            windowManager.addView(controlBarView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating control bar: ${e.message}", e)
        }
    }

    private fun triggerScreenScan() {
        clearMarkers()
        dismissShoppingCard()
        Toast.makeText(this, "Scanning screen...", Toast.LENGTH_SHORT).show()
        isScanPending = true
    }

    private suspend fun processCapturedFrame(bitmap: Bitmap) {
        latestFullBitmap?.recycle()
        latestFullBitmap = bitmap

        val detectionResult = withContext(Dispatchers.Default) {
            objectDetector.detect(bitmap)
        }

        detectionResult.fold(
            onSuccess = { result ->
                if (result.objects.isEmpty()) {
                    Toast.makeText(this@ScreenCaptureService, "No objects detected on screen.", Toast.LENGTH_SHORT).show()
                } else {
                    renderBlueDotMarkers(result.objects, bitmap)
                    Toast.makeText(
                        this@ScreenCaptureService,
                        "Found ${result.objects.size} object(s). Tap any blue dot!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onFailure = { error ->
                Log.e(TAG, "ML Kit detection error: ${error.message}", error)
                Toast.makeText(this@ScreenCaptureService, "Scan error: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun renderBlueDotMarkers(objects: List<DetectedObjectItem>, frameBitmap: Bitmap) {
        clearMarkers()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val markerRadiusPx = (20 * resources.displayMetrics.density).toInt()

        objects.forEach { obj ->
            val cx = (obj.normalizedBoundingBox.centerX() * screenWidth).toInt()
            val cy = (obj.normalizedBoundingBox.centerY() * screenHeight).toInt()

            val markerParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (cx - markerRadiusPx).coerceAtLeast(0)
                y = (cy - markerRadiusPx).coerceAtLeast(0)
            }

            val markerView = OverlayViews.createBlueDotMarker(this, obj) {
                onMarkerSelected(obj, frameBitmap)
            }

            try {
                windowManager.addView(markerView, markerParams)
                activeMarkerViews.add(markerView)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to render blue dot: ${e.message}", e)
            }
        }
    }

    private fun clearMarkers() {
        activeMarkerViews.forEach { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {}
        }
        activeMarkerViews.clear()
    }

    private fun onMarkerSelected(obj: DetectedObjectItem, frameBitmap: Bitmap) {
        val crop = cropAnalyzer.cropBitmap(frameBitmap, obj.rawBoundingBox)
        showShoppingCard(obj, crop)
    }

    private fun showShoppingCard(obj: DetectedObjectItem, cropBitmap: Bitmap?) {
        dismissShoppingCard()

        currentMatchedProduct = null

        val controller = OverlayViews.createFloatingShoppingCard(
            context = this,
            onCloseClicked = {
                dismissShoppingCard()
            },
            onRefineClicked = { query ->
                executeProductMatch(obj, cropBitmap, query)
            },
            onCheckoutClicked = {
                val product = currentMatchedProduct
                if (product != null) {
                    initiatePayPalCheckout(product)
                } else {
                    Toast.makeText(this, "Please wait for product match to complete.", Toast.LENGTH_SHORT).show()
                }
            }
        )

        shoppingCardController = controller

        // Display thumbnail and category
        if (cropBitmap != null) {
            controller.thumbnailView.setImageBitmap(cropBitmap)
            controller.thumbnailView.visibility = View.VISIBLE
        } else {
            controller.thumbnailView.visibility = View.GONE
        }
        controller.categoryText.text = "Detected: ${obj.category} (Score: ${String.format("%.0f%%", (obj.confidence ?: 0.5f) * 100)})"

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val cardParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(controller.rootView, cardParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add shopping card: ${e.message}", e)
            return
        }

        // Run initial visual match
        executeProductMatch(obj, cropBitmap, null)
    }

    private fun executeProductMatch(obj: DetectedObjectItem, cropBitmap: Bitmap?, userQuery: String?) {
        val ctrl = shoppingCardController ?: return

        // Safeguard: reset matched product and disable checkout while search is in progress
        currentMatchedProduct = null
        ctrl.checkoutButton.isEnabled = false
        ctrl.checkoutButton.text = if (userQuery != null) "Searching..." else "Finding Product..."
        ctrl.progressBar.visibility = View.VISIBLE
        ctrl.statusText.text = if (userQuery != null) "Searching for '$userQuery'..." else "Finding products matching visual crop..."

        serviceScope.launch {
            val base64Crop = if (cropBitmap != null && userQuery == null) {
                withContext(Dispatchers.IO) {
                    cropAnalyzer.bitmapToJpegBase64(cropBitmap)
                }
            } else {
                null
            }

            val result = withContext(Dispatchers.IO) {
                matchingService.findBestMatch(
                    selectedObject = obj,
                    cropLabels = emptyList(),
                    sceneLabels = emptyList(),
                    userQuery = userQuery,
                    cropImageBase64 = base64Crop
                )
            }

            ctrl.progressBar.visibility = View.GONE

            when (result) {
                is MatchResult.Matched -> {
                    val candidate = result.candidate
                    ctrl.statusText.text = "Product Identified"
                    ctrl.productTitleText.text = candidate.product.title
                    ctrl.productDetailsText.text = "${candidate.product.currency} $${String.format("%.2f", candidate.product.price)} • ${candidate.product.brand}"
                    ctrl.matchBadgeText.text = if (candidate.identifiedAs != null) {
                        "AI Identified: ${candidate.identifiedAs} (${String.format("%.0f%%", candidate.score * 100)})"
                    } else {
                        "Visual Match (${String.format("%.0f%%", candidate.score * 100)})"
                    }
                    ctrl.matchCardLayout.visibility = View.VISIBLE
                    ctrl.checkoutButton.isEnabled = true
                    ctrl.checkoutButton.text = "Checkout with PayPal Sandbox ($${String.format("%.2f", candidate.product.price)})"

                    // Store product for checkout
                    currentMatchedProduct = candidate.product
                }
                is MatchResult.NoMatch -> {
                    ctrl.statusText.text = result.reason
                    ctrl.productTitleText.text = "No direct catalogue match"
                    ctrl.productDetailsText.text = "Try refining with a keyword below"
                    ctrl.matchBadgeText.text = "Catalogue Search"
                    ctrl.matchCardLayout.visibility = View.VISIBLE
                    ctrl.checkoutButton.isEnabled = false
                    ctrl.checkoutButton.text = "No Product Matched"
                    currentMatchedProduct = null
                }
            }
        }
    }

    private var currentMatchedProduct: ProductItem? = null

    private fun initiatePayPalCheckout(product: ProductItem) {
        val ctrl = shoppingCardController ?: return

        ctrl.checkoutButton.isEnabled = false
        ctrl.checkoutButton.text = "Creating PayPal Order..."
        ctrl.statusText.text = "Connecting to PayPal Sandbox backend..."

        serviceScope.launch {
            val returnUrl = "${BackendConfig.BASE_URL}/api/orders/web-return?status=success"
            val cancelUrl = "${BackendConfig.BASE_URL}/api/orders/web-return?status=cancelled"

            val result = withContext(Dispatchers.IO) {
                orderService.createOrder(
                    productId = product.id,
                    quantity = 1,
                    returnUrl = returnUrl,
                    cancelUrl = cancelUrl
                )
            }

            when (result) {
                is CreateOrderResult.Success -> {
                    ctrl.statusText.text = "Order Created! Opening PayPal Sandbox..."
                    ctrl.checkoutButton.text = "Order Created"

                    // PRIVACY FIX: Stop all screen capture and remove overlays BEFORE
                    // opening the browser. This ensures the PayPal payment page is never
                    // captured by MediaProjection, VirtualDisplay, or ImageReader.
                    stopCapture()

                    // Open PayPal Sandbox checkout in external browser
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(result.approvalUrl)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(browserIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Could not open PayPal browser: ${e.message}", e)
                        // Overlays are already gone; show a toast instead of updating the card
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ScreenCaptureService,
                                "Could not open browser: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    // Service work is done — stop cleanly after browser is launched
                    stopSelf()
                }
                is CreateOrderResult.Error -> {
                    ctrl.statusText.text = "Checkout error: ${result.message}"
                    ctrl.checkoutButton.isEnabled = true
                    ctrl.checkoutButton.text = "Retry Checkout"
                }
            }
        }
    }

    private fun dismissShoppingCard() {
        shoppingCardController?.let { controller ->
            try {
                windowManager.removeView(controller.rootView)
            } catch (e: Exception) {}
        }
        shoppingCardController = null
        currentMatchedProduct = null
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false

        // stopCapture() is idempotent — safe to call even if already called (e.g., during checkout)
        stopCapture()

        handlerThread?.quitSafely()
        handlerThread = null

        serviceScope.cancel()
        Log.i(TAG, "ScreenCaptureService destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
