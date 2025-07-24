package com.massimport

import android.app.IntentService
import android.content.ContentProviderOperation
import android.content.Intent
import android.os.Build
import android.provider.ContactsContract
import android.util.Log

class ImportService : IntentService("ImportService") {
    override fun onHandleIntent(intent: Intent?) {
        // Log manufacturer for potential OEM-specific logic
        val manufacturer = Build.MANUFACTURER.lowercase()
        Log.d("ImportService", "Device manufacturer: $manufacturer")

        val path = intent?.getStringExtra("path") ?: return
        Log.d("ImportService", "Started service with path=$path")

        val contacts = CsvParser.parse(path)
        if (contacts.isEmpty()) {
            Log.d("ImportService", "No contacts parsed")
            return
        }

        Log.d("ImportService", "Parsed ${contacts.size} contacts from CSV")

        val ops = ArrayList<ContentProviderOperation>()
        var batchNumber = 1

        contacts.forEachIndexed { index, contact ->
            val rawContactIndex = ops.size

            // Insert RawContact
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // Insert Name (data_version = 1)
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                    .withValue("data2", contact.name)
                    .withValue("data10", 1)
                    .withValue("data11", 0)
                    .withValue("data_version", 0)
                    .build()
            )

            // Insert Phone (data_version = 0)
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.phone)
                    .withValue("data4", normalizeNumber(contact.phone))
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .withValue("data2", 2)
                    .withValue("data_version", 0)
                    .build()
            )

            // Execute batch every 100 contacts (3 operations per contact)
            if ((index + 1) % 100 == 0) {
                executeBatch(ops, batchNumber)
                ops.clear()
                batchNumber++
            }
        }

        // Execute remaining operations if less than 100 contacts
        if (ops.isNotEmpty()) {
            executeBatch(ops, batchNumber)
        }

        Log.d("ImportService", "Finished importing ${contacts.size} contacts")
    }

    private fun executeBatch(ops: ArrayList<ContentProviderOperation>, batchNumber: Int) {
        // Measure execution time
        val startTime = System.currentTimeMillis()
        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            val duration = System.currentTimeMillis() - startTime

            // Log batch details: number, contacts count and duration
            Log.d(
                "ImportService",
                "Batch #$batchNumber inserted: ${ops.size / 3} contacts (${ops.size} ops) in ${duration}ms"
            )
        } catch (e: Exception) {
            Log.e("ImportService", "Batch #$batchNumber insert failed", e)
        }
    }

    private fun normalizeNumber(phone: String): String {
        return phone.replace(" ", "").replace("-", "")
    }
}
