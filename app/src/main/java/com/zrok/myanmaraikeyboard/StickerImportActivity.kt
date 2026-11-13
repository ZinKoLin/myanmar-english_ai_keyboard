package com.zrok.myanmaraikeyboard

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.zrok.myanmaraikeyboard.data.StickerStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StickerImportActivity : ComponentActivity() {
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) { finish(); return@registerForActivityResult }
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { StickerStore(this@StickerImportActivity).import(uri) } }
            Toast.makeText(this@StickerImportActivity,
                result.fold({ "Sticker ထည့်ပြီးပါပြီ။ Keyboard → emoji → Stickers မှာ ရွေးနိုင်ပါပြီ။" },
                    { it.message ?: "ပုံကို ထည့်မရပါ။" }), Toast.LENGTH_LONG).show()
            finish()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) picker.launch(arrayOf("image/png", "image/webp", "image/gif", "image/jpeg"))
    }
}
