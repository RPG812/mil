# MassImport Contacts

An Android application for bulk importing contacts from a CSV file using ADB.

## Features
- Import contacts from a CSV file.
- Supported CSV format:
  ```
  Name,Phone
  Hanna,+34 444 333 444
  Tor,+34 222 222 222
  God,+34 111 11 11 11
  ```
- Creates local phone contacts (`account_type=null`) similar to manually created ones.
- Normalizes phone numbers (stores a clean version in `data4`).
- Processes contacts in batches of 100 for optimized performance.
- Logs execution time for each batch.

## Compatibility
Tested on:
- Samsung (OneUI)
- OnePlus (OxygenOS)

## Usage
1. Copy the CSV file to the device under `/storage/emulated/0/Download/`.
2. Launch the app with the CSV path:
   ```
   adb shell am start -n com.massimport/.MainActivity --es path "/storage/emulated/0/Download/contacts.csv"
   ```
3. The contacts will be imported into the local phonebook.

## Permissions
The app requires the following permissions:
- `WRITE_CONTACTS`
- `READ_CONTACTS`
- `READ_EXTERNAL_STORAGE`

## Testing
For stress tests, you can generate CSV files with thousands of contacts.

## License
MIT
mass-import
