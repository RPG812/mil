package com.massimport

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentProviderOperation
import android.content.Context
import android.os.Build
import android.provider.ContactsContract
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class ImportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "import_contacts_channel"
        const val LOG_TAG = "MassImport"
        const val BATCH_SIZE = 20
    }

    override suspend fun doWork(): Result {
        val path = inputData.getString("path") ?: return Result.failure()

        Log.d(LOG_TAG, "Starting import: $path")

        val contacts = try {
            CsvParser.parse(path)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "CSV parse failed: ${e.message}", e)
            return Result.failure()
        }

        if (contacts.isEmpty()) {
            Log.e(LOG_TAG, "No contacts found in CSV")
            return Result.failure()
        }

        setForeground(createForegroundInfo(0, contacts.size))

        val ops = ArrayList<ContentProviderOperation>()
        var batchNumber = 1

        val rawContactUri = ContactsContract.RawContacts.CONTENT_URI.buildUpon()
            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
            .build()

        val dataUri = ContactsContract.Data.CONTENT_URI.buildUpon()
            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
            .build()

        contacts.forEachIndexed { index, contact ->
            if (isStopped) {
                Log.d(LOG_TAG, "Worker cancelled at contact #$index")
                return Result.failure()
            }

            val rawContactIndex = ops.size

            // Insert RawContact
            ops.add(
                ContentProviderOperation.newInsert(rawContactUri)
                    .withYieldAllowed(true)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // Insert Name
            ops.add(
                ContentProviderOperation.newInsert(dataUri)
                    .withYieldAllowed(true)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                    .withValue("data2", contact.name)
                    .build()
            )

            // Insert Phone
            ops.add(
                ContentProviderOperation.newInsert(dataUri)
                    .withYieldAllowed(true)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.phone)
                    .withValue("data4", normalizeNumber(contact.phone))
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build()
            )

            if ((index + 1) % BATCH_SIZE == 0) {
                val success = executeBatch(ops, batchNumber)
                if (!success) return Result.retry()

                ops.clear()
                batchNumber++

                setForeground(createForegroundInfo(index + 1, contacts.size))
            }
        }

        if (!isStopped && ops.isNotEmpty()) {
            val success = executeBatch(ops, batchNumber)
            if (!success) return Result.retry()
        }

        setForeground(createForegroundInfo(contacts.size, contacts.size))
        Log.d(LOG_TAG, "Import finished: ${contacts.size} contacts")

        return Result.success(workDataOf("imported" to contacts.size))
    }


    private fun executeBatch(ops: ArrayList<ContentProviderOperation>, batchNumber: Int): Boolean {
        val start = System.currentTimeMillis()
        return try {
            applicationContext.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            val duration = System.currentTimeMillis() - start
            Log.d(LOG_TAG, "Batch #$batchNumber inserted (${ops.size / 3} contacts) in ${duration}ms")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - start
            Log.e(LOG_TAG, "Batch #$batchNumber failed after ${duration}ms", e)
            false
        }
    }

    private fun normalizeNumber(phone: String): String {
        return phone.replace(" ", "").replace("-", "")
    }

    private fun createForegroundInfo(current: Int, total: Int): ForegroundInfo {
        val context = applicationContext
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Import Contacts",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Importing contacts")
            .setContentText("Imported $current of $total")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, if (total == 0) 0 else (current * 100 / total), false)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
}
