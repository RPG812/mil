package com.massimport

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val stopFlag = intent.getBooleanExtra("stop", false)
        val csvPath = intent.getStringExtra("path")

        if (stopFlag) {
            Log.d("MassImport", "Stopping all import tasks (explicit stop)")
            WorkManager.getInstance(applicationContext).cancelAllWork()
        } else if (!csvPath.isNullOrEmpty()) {
            Log.d("MassImport", "Received path: $csvPath")

            WorkManager.getInstance(applicationContext).cancelAllWork()

            startUniqueImport(csvPath)
        } else {
            Log.e("MassImport", "No path provided to MainActivity")
        }

        finish()
    }

    private fun startUniqueImport(csvPath: String) {
        val data = workDataOf("path" to csvPath)

        val request = OneTimeWorkRequestBuilder<ImportWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "import_contacts",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
