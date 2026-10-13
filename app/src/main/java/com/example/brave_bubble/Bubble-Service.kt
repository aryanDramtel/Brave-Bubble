package com.example.brave_bubble

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PixelFormat
import android.graphics.RectF
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.quicksettings.TileService
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

class BubbleView(context: Context) : FrameLayout(context) {
    val iconView = BubbleIconView(context)
    val textView = TextView(context).apply {
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        textSize = 15f
        typeface = ResourcesCompat.getFont(context, R.font.ubuntu)
    }

    init {
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(12, 8, 12, 8)
            val iconSize = 36
            addView(iconView, LinearLayout.LayoutParams(iconSize, iconSize))
            addView(textView, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        }
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        setWillNotDraw(false)
    }

    var progress: Float = 0f
    var bgColorInt: Int = Color.parseColor("#000080")
    var ringColorInt: Int = Color.parseColor("#50C878")
    var hideProgressTrack: Boolean = false
    var showBorder: Boolean = true
    var shape: Int = 0

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val rectF = RectF()
    private val shapePath = Path()
    private val progressPath = Path()
    private val pathMeasure = PathMeasure()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val strokeThickness = 12f
        val halfStroke = strokeThickness / 2f
        val inset = if (shape == 0) halfStroke else strokeThickness
        rectF.set(inset, inset, width - inset, height - inset)

        bgPaint.color = bgColorInt
        shapePath.reset()
        when (shape) {
            1 -> shapePath.addRoundRect(rectF, width * 0.18f, height * 0.18f, Path.Direction.CW)
            2 -> shapePath.addRect(rectF, Path.Direction.CW)
            else -> shapePath.addOval(rectF, Path.Direction.CW)
        }
        canvas.drawPath(shapePath, bgPaint)

        if (showBorder) {
            if (!hideProgressTrack) {
                borderTrackPaint.color = Color.argb(50, Color.red(ringColorInt), Color.green(ringColorInt), Color.blue(ringColorInt))
                borderTrackPaint.strokeWidth = strokeThickness
                borderTrackPaint.strokeJoin = Paint.Join.ROUND
                canvas.drawPath(shapePath, borderTrackPaint)

                progressPaint.color = ringColorInt
                progressPaint.strokeWidth = strokeThickness
                if (shape == 0) {
                    val sweepAngle = 360f * progress.coerceIn(0f, 1f)
                    canvas.drawArc(rectF, -90f, sweepAngle, false, progressPaint)
                } else {
                    pathMeasure.setPath(shapePath, false)
                    progressPath.reset()
                    pathMeasure.getSegment(0f, pathMeasure.length * progress.coerceIn(0f, 1f), progressPath, true)
                    canvas.drawPath(progressPath, progressPaint)
                }
            } else {
                progressPaint.color = ringColorInt
                progressPaint.strokeWidth = strokeThickness
                canvas.drawPath(shapePath, progressPaint)
            }
        }
    }

    fun updateAppearance(bg: Int, ring: Int, prog: Float, hideTrack: Boolean, borderEnabled: Boolean, bubbleShape: Int) {
        bgColorInt = bg
        ringColorInt = ring
        progress = prog
        hideProgressTrack = hideTrack
        showBorder = borderEnabled
        shape = bubbleShape.coerceIn(0, 2)
        invalidate()
    }
}

class BubbleIconView(context: android.content.Context) : View(context) {
    var mode: Int = 0
        set(value) {
            field = value
            invalidate()
        }
    var iconAlpha: Int = 255

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        paint.alpha = iconAlpha.coerceIn(0, 255)
        val cx = width / 2f
        val cy = height / 2f
        val unit = minOf(width, height) / 32f
        canvas.save()
        canvas.translate(cx - 16f * unit, cy - 16f * unit)
        canvas.scale(unit, unit)
        when (mode) {
            0 -> {
                canvas.drawArc(6f, 8f, 26f, 28f, 225f, 90f, false, paint)
                canvas.drawArc(10f, 12f, 22f, 24f, 225f, 90f, false, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(16f, 24f, 1.8f, paint)
                paint.style = Paint.Style.STROKE
            }
            1 -> {
                canvas.drawCircle(16f, 16f, 11f, paint)
                canvas.drawLine(16f, 16f, 16f, 9f, paint)
                canvas.drawLine(16f, 16f, 22f, 19f, paint)
            }
            2 -> {
                canvas.drawRoundRect(6f, 9f, 26f, 23f, 2f, 2f, paint)
                canvas.drawRect(26f, 13f, 28f, 19f, paint)
                canvas.drawLine(11f, 16f, 21f, 16f, paint)
                canvas.drawLine(16f, 11f, 16f, 21f, paint)
            }
            3 -> {
                canvas.drawArc(6f, 12f, 22f, 28f, 180f, 180f, false, paint)
                canvas.drawArc(12f, 7f, 25f, 22f, 180f, 180f, false, paint)
                canvas.drawArc(19f, 13f, 30f, 25f, 180f, 180f, false, paint)
                canvas.drawLine(7f, 20f, 28f, 20f, paint)
            }
            else -> {
                canvas.drawCircle(16f, 16f, 11f, paint)
                canvas.drawLine(16f, 14f, 16f, 22f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(16f, 10f, 1.5f, paint)
                paint.style = Paint.Style.STROKE
            }
        }
        canvas.restore()
    }
}

class BubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: BubbleView
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var prefs: SharedPreferences
    
    private val handler = Handler(Looper.getMainLooper())
    private var lastRxBytes: Long = 0
    private var lastTxBytes: Long = 0
    private var totalBytesUsed: Long = 0
    private var currentMode = 0 
    private var currentAltitudeMeters: Double? = null
    private var currentLocation: Location? = null
    private var locationManager: LocationManager? = null
    private val locationListener = LocationListener { location ->
        currentLocation = location
        currentAltitudeMeters = if (location.hasAltitude()) location.altitude else null
        if (currentMode == 4) updateInfoBubble()
    }
    
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        currentMode = prefs.getInt("bubble_mode", 0)
        startCurrentModeLogic()
        updateBubbleAppearance()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("BubblePrefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        currentMode = prefs.getInt("bubble_mode", 0)

        prefs.edit().putBoolean("is_bubble_running", true).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(this, ComponentName(this, BubbleTileService::class.java))
        }

        totalBytesUsed = prefs.getLong("total_bytes_used", 0L)
        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()

        try {
            startForegroundServiceNotification()
            createFloatingBubble()
            startCurrentModeLogic()
        } catch (e: Exception) {
            Toast.makeText(this, "Init error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startForegroundServiceNotification() {
        val channelId = "bubble_service_channel"
        val channel = NotificationChannel(channelId, "System Monitor", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Bubble Active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            val foregroundTypes = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                if (currentMode == 4 && hasLocationPermission()) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
            startForeground(1, notification, foregroundTypes)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && currentMode == 4 && hasLocationPermission()) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }
    }

    private fun createFloatingBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val initialSize = prefs.getInt("bubble_size", 220)
        
        params = WindowManager.LayoutParams(
            initialSize, initialSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        bubbleView = BubbleView(this)
        updateBubbleAppearance()

        bubbleView.setOnTouchListener(object : View.OnTouchListener {
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isDragging = false
            var lastTapTime = 0L

            val longPressRunnable = Runnable {
                if (!isDragging) stopSelf()
            }

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastTapTime < 300) {
                            handler.removeCallbacks(longPressRunnable)
                            cycleMode()
                            lastTapTime = 0L
                            return true
                        }
                        lastTapTime = currentTime
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        handler.postDelayed(longPressRunnable, 600)
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (abs(dx) > 10 || abs(dy) > 10) {
                            isDragging = true
                            handler.removeCallbacks(longPressRunnable)
                        }
                        if (isDragging) {
                            params.x = initialX + dx.toInt()
                            params.y = initialY + dy.toInt()
                            if (bubbleView.isAttachedToWindow) windowManager.updateViewLayout(bubbleView, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(longPressRunnable)
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(bubbleView, params)
    }

    private fun cycleMode() {
        currentMode = (currentMode + 1) % 5
        prefs.edit().putInt("bubble_mode", currentMode).apply()
    }

    private fun startCurrentModeLogic() {
        handler.removeCallbacks(updateNetworkSpeed)
        handler.removeCallbacks(updateClock)
        handler.removeCallbacks(updateBattery)
        handler.removeCallbacks(updateInfo)
        locationManager?.removeUpdates(locationListener)
        if (currentMode == 4) startForegroundServiceNotification()
        
        when (currentMode) {
            0 -> handler.post(updateNetworkSpeed)
            1 -> handler.post(updateClock)
            2 -> handler.post(updateBattery)
            4 -> {
                bubbleView.iconView.mode = 4
                refreshAltitude()
                handler.post(updateInfo)
            }
            else -> {
                bubbleView.iconView.mode = currentMode.coerceIn(0, 4)
                bubbleView.textView.text = "Weather\nnot configured"
            }
        }
    }

    private fun updateBubbleAppearance() {
        if (!::bubbleView.isInitialized) return

        val size = prefs.getInt("bubble_size", 220)
        val opacity = prefs.getFloat("bubble_opacity", 0.8f)
        val baseBgColorInt = prefs.getInt("bg_color_int", Color.parseColor("#000080"))
        val ringColorInt = prefs.getInt("border_color_int", Color.parseColor("#50C878"))
        val showBorder = prefs.getBoolean("show_border", true)
        val bubbleShape = prefs.getInt("bubble_shape", 0)

        val bgAlpha = (opacity.coerceIn(0f, 1f) * 255).toInt()
        val finalBgColorInt = Color.argb(bgAlpha, Color.red(baseBgColorInt), Color.green(baseBgColorInt), Color.blue(baseBgColorInt))
        val finalBorderColorInt = ringColorInt
        val fontScalePercent = prefs.getFloat("bubble_font_scale_percent", 100f).coerceIn(50f, 200f)
        val fontAlpha = (prefs.getFloat("bubble_font_opacity", 1f).coerceIn(0f, 1f) * 255).toInt()
        bubbleView.textView.textSize = (15f * fontScalePercent / 100f).coerceIn(8f, 40f)
        bubbleView.textView.setTextColor(Color.argb(fontAlpha, 255, 255, 255))
        bubbleView.iconView.iconAlpha = fontAlpha
        bubbleView.iconView.mode = currentMode.coerceIn(0, 4)

        params.width = size
        params.height = size

        // Calculate progress for Data Monitor or Battery, else 0
        var currentProgress = 0f
        var hideTrack = true

        if (currentMode == 0) {
            val limitGb = prefs.getFloat("data_limit_gb", 5.0f)
            val limitBytes = limitGb * 1024L * 1024L * 1024L
            totalBytesUsed = prefs.getLong("total_bytes_used", 0L)
            currentProgress = if (limitBytes > 0) (totalBytesUsed.toDouble() / limitBytes).toFloat() else 0f
            hideTrack = false
        } else if (currentMode == 2) {
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            currentProgress = batLevel / 100f
            hideTrack = false
        }

        bubbleView.updateAppearance(finalBgColorInt, finalBorderColorInt, currentProgress, hideTrack, showBorder, bubbleShape)

        if (bubbleView.isAttachedToWindow) {
            windowManager.updateViewLayout(bubbleView, params)
        }
    }

    // MODE 0: Data Monitor
    private val updateNetworkSpeed = object : Runnable {
        override fun run() {
            if (!::bubbleView.isInitialized || !bubbleView.isAttachedToWindow) {
                handler.postDelayed(this, 1000)
                return
            }

            val currentRx = TrafficStats.getTotalRxBytes()
            val currentTx = TrafficStats.getTotalTxBytes()
            var rxDiff = currentRx - lastRxBytes
            var txDiff = currentTx - lastTxBytes

            if (rxDiff < 0) rxDiff = currentRx
            if (txDiff < 0) txDiff = currentTx
            val tickBytes = rxDiff + txDiff
            
            val speedKbps = tickBytes / 1024f
            
            // Clean typography, no emojis
            val speedText = if (speedKbps >= 1024) String.format("%.1f\nMB/s", speedKbps / 1024f) else String.format("%.0f\nKB/s", speedKbps)
            bubbleView.textView.text = speedText
            bubbleView.iconView.mode = 0

            totalBytesUsed += tickBytes
            prefs.edit().putLong("total_bytes_used", totalBytesUsed).apply()

            val limitGb = prefs.getFloat("data_limit_gb", 5.0f)
            val limitBytes = limitGb * 1024L * 1024L * 1024L
            bubbleView.progress = if (limitBytes > 0) (totalBytesUsed.toDouble() / limitBytes).toFloat() else 0f
            bubbleView.invalidate()

            lastRxBytes = currentRx
            lastTxBytes = currentTx
            handler.postDelayed(this, 1000)
        }
    }

    // MODE 1: World Clock
    private val updateClock = object : Runnable {
        override fun run() {
            if (!::bubbleView.isInitialized || !bubbleView.isAttachedToWindow) {
                handler.postDelayed(this, 1000)
                return
            }
            
            val tzString = prefs.getString("clock_timezone", "Africa/Conakry") ?: "Africa/Conakry"
            val sdf = SimpleDateFormat("HH:mm\nss", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone(tzString)
            
            // Extract city name for display (e.g. "Africa/Conakry" -> "Conakry")
            val city = tzString.split("/").last().replace("_", " ")
            
            bubbleView.textView.text = "$city\n${sdf.format(Date())}"
            bubbleView.iconView.mode = 1
            bubbleView.invalidate()
            
            handler.postDelayed(this, 1000)
        }
    }

    // MODE 2: Battery Tracker
    private val updateBattery = object : Runnable {
        override fun run() {
            if (!::bubbleView.isInitialized || !bubbleView.isAttachedToWindow) {
                handler.postDelayed(this, 5000)
                return
            }
            
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = registerReceiver(null, filter)
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            
            val statusText = if (isCharging) "Charging" else "Battery"
            bubbleView.textView.text = "$statusText\n$batLevel%"
            bubbleView.iconView.mode = 2
            
            bubbleView.progress = batLevel / 100f
            bubbleView.invalidate()
            
            handler.postDelayed(this, 5000) // Battery updates slower to save power
        }
    }

    private val updateInfo = object : Runnable {
        override fun run() {
            if (currentMode != 4) return
            if (::bubbleView.isInitialized && bubbleView.isAttachedToWindow) updateInfoBubble()
            handler.postDelayed(this, 60_000)
        }
    }

    private fun hasLocationPermission(): Boolean =
        checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun refreshAltitude() {
        if (!hasLocationPermission()) {
            currentLocation = null
            currentAltitudeMeters = null
            updateInfoBubble()
            return
        }
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager = manager
        try {
            val hasFinePermission =
                checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val providers = if (hasFinePermission) {
                listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            } else {
                listOf(LocationManager.NETWORK_PROVIDER)
            }
            providers.forEach { provider ->
                if (manager.isProviderEnabled(provider)) {
                    manager.getLastKnownLocation(provider)?.let { location ->
                        currentLocation = location
                        if (location.hasAltitude()) currentAltitudeMeters = location.altitude
                    }
                    manager.requestLocationUpdates(provider, 60_000L, 0f, locationListener, Looper.getMainLooper())
                }
            }
        } catch (e: SecurityException) {
            currentLocation = null
            currentAltitudeMeters = null
        }
        updateInfoBubble()
    }

    private fun updateInfoBubble() {
        if (!::bubbleView.isInitialized) return
        val lines = mutableListOf<String>()
        if (prefs.getBoolean("info_show_altitude", true)) {
            lines += currentAltitudeMeters?.let { "Alt: ${it.toInt()} m" } ?: "Alt: --"
        }
        if (prefs.getBoolean("info_show_lunar_phase", true)) {
            lines += "Lunar: ${lunarPhase(Date())}"
        }
        if (prefs.getBoolean("info_show_sun_distance", true)) {
            lines += "Sun: ${String.format(Locale.getDefault(), "%.3f AU", sunDistanceAu(Date()))}"
        }
        if (prefs.getBoolean("info_show_moon_distance", true)) {
            lines += "Moon: ${String.format(Locale.getDefault(), "%,d km", moonDistanceKm(Date()))}"
        }
        if (prefs.getBoolean("info_show_sunrise_sunset", true)) {
            val location = currentLocation
            lines += if (location == null) {
                "Sunrise/sunset: --"
            } else {
                sunriseSunset(Date(), location.latitude, location.longitude)
            }
        }
        bubbleView.textView.text = lines.joinToString("\n")
        bubbleView.iconView.mode = 4
        bubbleView.invalidate()
    }

    private fun sunriseSunset(date: Date, latitude: Double, longitude: Double): String {
        val utc = TimeZone.getTimeZone("UTC")
        val dayStart = Calendar.getInstance(utc, Locale.US).apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val day = dayStart.get(Calendar.DAY_OF_YEAR)
        val yearLength = if (dayStart.getActualMaximum(Calendar.DAY_OF_YEAR) == 366) 366.0 else 365.0
        val gamma = 2 * PI / yearLength * (day - 1)
        val equationOfTime = 229.18 * (
            0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma)
            )
        val declination = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
            0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
            0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
        val latitudeRadians = Math.toRadians(latitude)
        val cosHourAngle = (
            cos(Math.toRadians(90.833)) / (cos(latitudeRadians) * cos(declination)) -
                kotlin.math.tan(latitudeRadians) * kotlin.math.tan(declination)
            )
        if (cosHourAngle > 1.0) return "Sun below horizon"
        if (cosHourAngle < -1.0) return "Sun above horizon"

        val hourAngleDegrees = Math.toDegrees(kotlin.math.acos(cosHourAngle))
        val solarNoonUtcMinutes = 720.0 - 4.0 * longitude - equationOfTime
        val sunriseUtcMinutes = solarNoonUtcMinutes - 4.0 * hourAngleDegrees
        val sunsetUtcMinutes = solarNoonUtcMinutes + 4.0 * hourAngleDegrees
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        val sunrise = Date(dayStart.timeInMillis + (sunriseUtcMinutes * 60_000).toLong())
        val sunset = Date(dayStart.timeInMillis + (sunsetUtcMinutes * 60_000).toLong())
        val daylightMinutes = (8.0 * hourAngleDegrees).toInt()
        return "Sun ${formatter.format(sunrise)}-${formatter.format(sunset)}\nDaylight ${daylightMinutes / 60}h ${daylightMinutes % 60}m"
    }

    private fun lunarPhase(date: Date): String {
        val synodicMonthDays = 29.530588853
        val referenceNewMoon = 947_182_440_000.0
        val phase = ((date.time - referenceNewMoon) / 86_400_000.0 % synodicMonthDays) / synodicMonthDays
        val normalizedPhase = (phase + 1.0) % 1.0
        return when {
            normalizedPhase < 0.03 || normalizedPhase > 0.97 -> "New moon"
            normalizedPhase < 0.22 -> "Waxing crescent"
            normalizedPhase < 0.28 -> "First quarter"
            normalizedPhase < 0.47 -> "Waxing gibbous"
            normalizedPhase < 0.53 -> "Full moon"
            normalizedPhase < 0.72 -> "Waning gibbous"
            normalizedPhase < 0.78 -> "Last quarter"
            else -> "Waning crescent"
        }
    }

    private fun sunDistanceAu(date: Date): Double {
        val daysFromJ2000 = (date.time - 946_728_000_000.0) / 86_400_000.0
        val meanAnomaly = Math.toRadians((357.529 + 0.98560028 * daysFromJ2000) % 360.0)
        return 1.00014 - 0.01671 * cos(meanAnomaly) - 0.00014 * cos(2 * meanAnomaly)
    }

    private fun moonDistanceKm(date: Date): Int {
        val daysFromPerigee = (date.time - 947_203_200_000.0) / 86_400_000.0
        val anomaly = 2 * PI * daysFromPerigee / 27.55455
        return (385_001.0 * (1 - 0.0549 * cos(anomaly))).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        handler.removeCallbacks(updateNetworkSpeed)
        handler.removeCallbacks(updateClock)
        handler.removeCallbacks(updateBattery)
        handler.removeCallbacks(updateInfo)
        locationManager?.removeUpdates(locationListener)
        
        prefs.edit().putBoolean("is_bubble_running", false).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(this, ComponentName(this, BubbleTileService::class.java))
        }

        if (::bubbleView.isInitialized && bubbleView.isAttachedToWindow) {
            windowManager.removeView(bubbleView)
        }
    }
}