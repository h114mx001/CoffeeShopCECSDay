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

class DebugMain : AppCompatActivity() {

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

            // Read all data sectors
            for (sector in 0 until mifareClassic.sectorCount) {
                // Authenticate the sector using the default key
                mifareClassic.authenticateSectorWithKeyA(sector, MifareClassic.KEY_DEFAULT)

                // Read all blocks in the sector
                for (block in 0 until mifareClassic.getBlockCountInSector(sector)) {
//                    val data = mifareClassic.readBlock(mifareClassic.sectorToBlock(sector) + block)
//                    Log.d(TAG, "Sector $sector, Block $block: ${data.toHexString()}")
                      Log.d(TAG, "Sector $sector, Block $block")
                }
            }

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
        private const val TAG = "DebugMain"
    }
}

// Helper function to convert a byte array to a hexadecimal string
fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}