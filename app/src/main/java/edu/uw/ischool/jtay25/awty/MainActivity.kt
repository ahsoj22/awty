package edu.uw.ischool.jtay25.awty

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var messageEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var intervalEditText: EditText
    private lateinit var startStopButton: Button
    private var isRunning = false

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var userMessage: String
    private lateinit var phoneNumber: String
    private var interval: Int = 0

    private lateinit var telephonyManager: TelephonyManager
    private var isPhoneIdle = true

    companion object {
        private const val SMS_PERMISSION_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        messageEditText = findViewById(R.id.messageEditText)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        intervalEditText = findViewById(R.id.intervalEditText)
        startStopButton = findViewById(R.id.startStopButton)

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)

        startStopButton.setOnClickListener {
            if (isRunning) {
                stopNagging()
            } else {
                if (validateInputs()) {
                    // Check SMS permission
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                        == PackageManager.PERMISSION_GRANTED) {
                        startNagging()
                    } else {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_CODE)
                    }
                } else {
                    Toast.makeText(this, "Please enter valid inputs.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            isPhoneIdle = when (state) {
                TelephonyManager.CALL_STATE_IDLE -> true
                TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING -> false
                else -> true
            }
            Log.d("MainActivity", "Phone state changed. Idle: $isPhoneIdle")
        }
    }

    private val nagTask = object : Runnable {
        override fun run() {
            if (isPhoneIdle) {
                sendSMS()
            } else {
                Log.d("MainActivity", "Phone is not idle, SMS postponed")
            }
            handler.postDelayed(this, interval.toLong())
        }
    }

    private fun validateInputs(): Boolean {
        val intervalText = intervalEditText.text.toString()
        return messageEditText.text.isNotEmpty() &&
                phoneNumberEditText.text.isNotEmpty() &&
                intervalText.isNotEmpty() &&
                intervalText.toIntOrNull()?.let { it in 1..3600 } == true  // Limit interval to 1-3600 seconds
    }

    private fun startNagging() {
        userMessage = messageEditText.text.toString()
        phoneNumber = phoneNumberEditText.text.toString()
        interval = (intervalEditText.text.toString().toInt() * 60 * 1000).coerceAtMost(3600000)

        Log.d("MainActivity", "Starting nagging with message: $userMessage and interval: $interval ms")

        handler.post(nagTask)
        isRunning = true
        startStopButton.text = "Stop"
    }

    private fun stopNagging() {
        handler.removeCallbacks(nagTask)
        isRunning = false
        startStopButton.text = "Start"
        Log.d("MainActivity", "Nagging stopped")
    }

    private fun sendSMS() {
        try {
            val formattedNumber = if (!phoneNumber.startsWith("+")) {
                "+1$phoneNumber" // Default to US country code if no country code is present
            } else {
                phoneNumber
            }

            Log.d("MainActivity", "Sending SMS to: $formattedNumber with message: $userMessage")

            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(formattedNumber, null, userMessage, null, null)

            Toast.makeText(this, "Message sent to $formattedNumber", Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "SMS sent successfully")
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SMS: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("MainActivity", "Error sending SMS: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startNagging()
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
    }
}
