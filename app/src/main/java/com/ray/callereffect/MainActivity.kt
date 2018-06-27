package com.ray.callereffect

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*;

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        swit.setSlideListener(object : SlideCallerView.SlideListener{
            override fun onSlideToAnswer() {
                Toast.makeText(this@MainActivity, "Answer!", Toast.LENGTH_SHORT).show()
            }

            override fun onSlideToHangup() {
                Toast.makeText(this@MainActivity, "Hangup!", Toast.LENGTH_SHORT).show()
            }

        })
    }
}
