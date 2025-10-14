package com.rhsdalya.slideruler

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton: Button = findViewById(R.id.start_measure_button)
        startButton.setOnClickListener {
            // 버튼을 누르면 ARMeasureActivity로 이동
            val intent = Intent(this, ARMeasureActivity::class.java)
            startActivity(intent)
        }
    }
}