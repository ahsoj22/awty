package edu.uw.ischool.jtay25.awty

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var messageEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var intervalEditText: EditText
    private lateinit var startStopButton: Button
    private var isRunning = false

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var userMessage: String
    private var interval: Int = 0

    private val nagTask = object : Runnable {
        override fun run() {
            Toast.makeText(this@MainActivity, userMessage, Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "Toast displayed: $userMessage")

            handler.postDelayed(this, interval.toLong())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        messageEditText = findViewById(R.id.messageEditText)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        intervalEditText = findViewById(R.id.intervalEditText)
        startStopButton = findViewById(R.id.startStopButton)

        startStopButton.setOnClickListener {
            if (isRunning) {
                stopNagging()
            } else {
                if (validateInputs()) {
                    startNagging()
                } else {
                    Toast.makeText(this, "Please enter valid inputs.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        val intervalText = intervalEditText.text.toString()
        return messageEditText.text.isNotEmpty() &&
                phoneNumberEditText.text.isNotEmpty() &&
                intervalText.isNotEmpty() &&
                intervalText.toIntOrNull()?.let { it > 0 } == true
    }

    private fun startNagging() {
        val message = messageEditText.text.toString()
        val phoneNumber = phoneNumberEditText.text.toString()

        interval = intervalEditText.text.toString().toInt() * 60 * 1000  // Convert to milliseconds
        userMessage = "$phoneNumber: $message"

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
}
