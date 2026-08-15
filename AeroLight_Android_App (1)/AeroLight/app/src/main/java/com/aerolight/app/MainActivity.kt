package com.aerolight.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.aerolight.app.databinding.ActivityMainBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var notificationHelper: NotificationHelper

    // Firebase Realtime Database URL for the AeroLight Ethiopia project
    private val databaseUrl = "https://aerolight-eth-default-rtdb.firebaseio.com"

    // Path in the database where the device writes live sensor data.
    // Change this if your device writes to a different node, e.g. "sensors/device1".
    private val dataPath = "sensorData"

    // How many points to keep on screen per chart
    private val maxPoints = 50
    private var pointIndex = 0f

    private val tempEntries = mutableListOf<Entry>()
    private val humidityEntries = mutableListOf<Entry>()
    private val gasEntries = mutableListOf<Entry>()

    private var lastAlertedStatus: String = SensorReading.STATUS_OK

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op — if denied, alerts simply won't show */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notificationHelper = NotificationHelper(this)
        askNotificationPermissionIfNeeded()

        setupChart(binding.chartTemperature, "Temperature (°C)", Color.parseColor("#E53935"))
        setupChart(binding.chartHumidity, "Humidity (%)", Color.parseColor("#1E88E5"))
        setupChart(binding.chartGas, "Gas Level", Color.parseColor("#43A047"))

        listenForSensorData()
    }

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupChart(chart: com.github.mikephil.charting.charts.LineChart, label: String, color: Int) {
        val dataSet = LineDataSet(mutableListOf(), label).apply {
            this.color = color
            setCircleColor(color)
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
        }
        chart.data = LineData(dataSet)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.invalidate()
    }

    private fun listenForSensorData() {
        val database = FirebaseDatabase.getInstance(databaseUrl)
        val ref = database.getReference(dataPath)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reading = parseSnapshot(snapshot) ?: return
                updateUi(reading)
                appendToCharts(reading)
                checkForAlert(reading)
            }

            override fun onCancelled(error: DatabaseError) {
                binding.textStatus.text = "Connection error: ${error.message}"
            }
        })
    }

    /**
     * Adjust the key names below ("temperature", "humidity", "gas", "status")
     * to match exactly what your ESP32/Arduino device writes to Firebase.
     */
    private fun parseSnapshot(snapshot: DataSnapshot): SensorReading? {
        if (!snapshot.exists()) return null
        val temperature = snapshot.child("temperature").getValue(Double::class.java)?.toFloat() ?: return null
        val humidity = snapshot.child("humidity").getValue(Double::class.java)?.toFloat() ?: 0f
        val gas = snapshot.child("gas").getValue(Double::class.java)?.toFloat() ?: 0f
        val status = snapshot.child("status").getValue(String::class.java) ?: deriveStatus(gas)

        return SensorReading(
            temperature = temperature,
            humidity = humidity,
            gasLevel = gas,
            status = status
        )
    }

    /** Fallback if the device doesn't write its own status field. Tune thresholds to your sensor. */
    private fun deriveStatus(gas: Float): String = when {
        gas >= 700f -> SensorReading.STATUS_DANGER
        gas >= 400f -> SensorReading.STATUS_WARN
        else -> SensorReading.STATUS_OK
    }

    private fun updateUi(reading: SensorReading) {
        binding.textTemperature.text = "Temperature: %.1f°C".format(reading.temperature)
        binding.textHumidity.text = "Humidity: %.1f%%".format(reading.humidity)
        binding.textGas.text = "Gas Level: %.1f".format(reading.gasLevel)

        val (label, color) = when (reading.status) {
            SensorReading.STATUS_DANGER -> "DANGER" to Color.parseColor("#D32F2F")
            SensorReading.STATUS_WARN -> "WARN" to Color.parseColor("#F9A825")
            else -> "OK" to Color.parseColor("#388E3C")
        }
        binding.textStatus.text = "Status: $label"
        binding.textStatus.setTextColor(color)
    }

    private fun appendToCharts(reading: SensorReading) {
        pointIndex += 1f
        addEntry(tempEntries, binding.chartTemperature, reading.temperature)
        addEntry(humidityEntries, binding.chartHumidity, reading.humidity)
        addEntry(gasEntries, binding.chartGas, reading.gasLevel)
    }

    private fun addEntry(
        entries: MutableList<Entry>,
        chart: com.github.mikephil.charting.charts.LineChart,
        value: Float
    ) {
        entries.add(Entry(pointIndex, value))
        if (entries.size > maxPoints) entries.removeAt(0)

        val dataSet = chart.data.getDataSetByIndex(0) as LineDataSet
        dataSet.values = entries
        chart.data.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.setVisibleXRangeMaximum(maxPoints.toFloat())
        chart.moveViewToX(pointIndex)
        chart.invalidate()
    }

    /** Only fires a new notification when status changes to WARN/DANGER, not on every reading. */
    private fun checkForAlert(reading: SensorReading) {
        val isAlertLevel = reading.status == SensorReading.STATUS_WARN ||
            reading.status == SensorReading.STATUS_DANGER

        if (isAlertLevel && reading.status != lastAlertedStatus) {
            notificationHelper.showAlert(reading.status, reading)
        }
        lastAlertedStatus = reading.status
    }
}
