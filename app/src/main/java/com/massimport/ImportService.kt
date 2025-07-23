package com.massimport

import android.app.IntentService
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.provider.ContactsContract
import android.util.Log

class ImportService : IntentService("ImportService") {
    override fun onHandleIntent(intent: Intent?) {
        val path = intent?.getStringExtra("path") ?: return
        Log.d("ImportService", "Started service with path=$path")

        val contacts = CsvParser.parse(path)
        if (contacts.isEmpty()) {
            Log.d("ImportService", "No contacts parsed")
            return
        }

        val resolver = contentResolver
        Log.d("ImportService", "Parsed ${contacts.size} contacts from CSV")

        contacts.forEachIndexed { index, contact ->
            Log.d("ImportService", "[$index] Creating contact: name=${contact.name}, phone=${contact.phone}, isNew=${contact.isNew}")

            try {
                // 1. Создаём пустой RawContact
                val rawContactUri = resolver.insert(ContactsContract.RawContacts.CONTENT_URI, ContentValues())
                if (rawContactUri == null) {
                    Log.e("ImportService", "Failed to insert RawContact for ${contact.name}")
                    return@forEachIndexed
                }

                val rawContactId = ContentUris.parseId(rawContactUri)
                Log.d("ImportService", "RawContact created with ID=$rawContactId")

                // 2. Добавляем имя (data_version = 1)
                val nameValues = ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                    put("data2", contact.name) // копия в data2, как у ручных
                    put("data10", 1)
                    put("data11", 0)
                    put("data_version", 1) // ключевое изменение
                }
                resolver.insert(ContactsContract.Data.CONTENT_URI, nameValues)

                // 3. Добавляем телефон (data_version = 0)
                val phoneValues = ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    put(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.phone)
                    put("data4", normalizeNumber(contact.phone)) // нормализованный номер
                    put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    put("data2", 2) // mobile type code
                    put("data_version", 0)
                }
                resolver.insert(ContactsContract.Data.CONTENT_URI, phoneValues)

                Log.d("ImportService", "Contact ${contact.name} created successfully")

            } catch (e: Exception) {
                Log.e("ImportService", "Error creating contact ${contact.name}", e)
            }
        }

        Log.d("ImportService", "Finished importing ${contacts.size} contacts")
    }

    fun normalizeNumber(phone: String): String {
        return phone.replace(" ", "").replace("-", "")
    }
}
