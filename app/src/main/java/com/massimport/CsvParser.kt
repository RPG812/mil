package com.massimport

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

object CsvParser {
    fun parse(path: String): List<Contact> {
        val contacts = mutableListOf<Contact>()

        try {
            val file = File(path)
            if (!file.exists()) {
                throw IllegalArgumentException("CSV file not found: $path")
            }

            BufferedReader(FileReader(file)).use { reader ->
                reader.lineSequence().forEachIndexed { index, line ->
                    if (line.isBlank()) return@forEachIndexed

                    val parts = line.split(",")
                    if (parts.size < 2) {
                        Log.w("MassImport", "Invalid line at $index: $line")
                        return@forEachIndexed
                    }

                    val name = parts[0].trim()
                    val phone = parts[1].trim()

                    contacts.add(Contact(name, phone))
                }
            }
        } catch (e: Exception) {
            Log.e("MassImport", "CsvParser failed: ${e.message}", e)
            throw e
        }

        return contacts
    }
}
