package com.example.coffeeshopcecsday

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*



class MainActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Check for available NFC Adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Log.e(TAG, "NFC is not available")
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        disableForegroundDispatch()
    }

    private fun readBlock(mifareClassic: MifareClassic, sector: Int, block: Int): ByteArray {
        mifareClassic.authenticateSectorWithKeyA(sector, MifareClassic.KEY_DEFAULT)
        val data = mifareClassic.readBlock(mifareClassic.sectorToBlock(sector) + block)
        return data
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Check if the intent contains an NFC tag
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return

            // Check if the tag is a MIFARE Classic 1K card
            val mifareClassic = MifareClassic.get(tag)
            if (mifareClassic == null || mifareClassic.type != MifareClassic.TYPE_CLASSIC) {
                Log.e(TAG, "Tag is not a MIFARE Classic 1K card")
                return
            }

            // Connect to the tag
            mifareClassic.connect()

            // Read UID
            // val uid = mifareClassic.uid.toHexString()
            // Log.d(TAG, "UID: $uid")

            // Read user name
            val name = this.readBlock(mifareClassic, 0, 1)
            val asciiName = String(name, Charsets.US_ASCII)
            Log.d(TAG, "Name: $asciiName")

            // Read user email
            val email = this.readBlock(mifareClassic, 1, 0) + this.readBlock(mifareClassic, 1, 1) + this.readBlock(mifareClassic, 1, 2)
            val asciiEmail = String(email, Charsets.US_ASCII)
            Log.d(TAG, "Email: $asciiEmail")

            // Read current amount of money
            val amount = this.readBlock(mifareClassic, 2, 0).toLong(ByteOrder.LITTLE_ENDIAN)
            Log.d(TAG, "Amount: $amount")

            // Read expired date
            val expiredDate = this.readBlock(mifareClassic, 3, 0).toLong(ByteOrder.LITTLE_ENDIAN)
//            val dateFormat = SimpleDateFormat.getDateTimeInstance()
//            dateFormat.time = expiredDate * 1000
//            val expiredDateString = dateFormat.format(Date())
            Log.d(TAG, "Expired Date String: $expiredDate")

            // Log.d(TAG, "Expired Date: $expiredDate")

            // Close the connection to the tag
            mifareClassic.close()
        }
    }

    private fun enableForegroundDispatch() {
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    private fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(this)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}


// Helper function to convert a byte array to a long integer (little endian)
fun ByteArray.toLong(byteOrder: ByteOrder): Long {
    var value = 0L
    for (i in 0 until 8) {
        value = value or (this[i].toLong() shl (i * 8))
    }
    return if (byteOrder == ByteOrder.LITTLE_ENDIAN) value else java.lang.Long.reverseBytes(value)
}