package com.massimport

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val path = intent.getStringExtra("path")
        Log.d("MainActivity", "Launching ImportService with path=$path")

        val serviceIntent = Intent(this, ImportService::class.java)
        serviceIntent.putExtra("path", path)
        startService(serviceIntent)

        finish()
    }
}
