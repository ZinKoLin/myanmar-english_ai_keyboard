package com.zrok.myanmaraikeyboard

import android.content.Context
import android.graphics.BitmapFactory
import com.zrok.myanmaraikeyboard.data.StickerStore
import com.zrok.myanmaraikeyboard.data.AppPreferences
import com.zrok.myanmaraikeyboard.model.SupportedLanguages
import androidx.core.content.FileProvider
import java.io.File

object RevisionChecks {
    @JvmStatic fun run(context: Context): String {
        val store = StickerStore(context)
        val builtins = store.list().filter { it.file.name.startsWith("reaction_") }
        check(builtins.size == 12)
        builtins.forEach {
            val uri = store.uri(it)
            val image = context.contentResolver.openInputStream(uri)!!.use(BitmapFactory::decodeStream)
            check(image != null && image.width == 512 && image.height == 512)
            image.recycle()
        }
        val sample = File(context.filesDir, "stickers/probe.gif")
        sample.writeBytes(android.util.Base64.decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7", android.util.Base64.DEFAULT))
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.stickers", sample)
            val imported = store.import(uri)
            check(imported.mime == "image/gif")
            check(imported.file.readBytes().contentEquals(sample.readBytes()))
            store.remove(imported)
            check(!imported.file.exists())
        } finally { sample.delete() }
        check(listOf("mnw", "shn", "ksw", "kac").all { SupportedLanguages.require(it).experimental })
        AppPreferences(context).keyboardTone = 3
        return "PASS: 12 sticker PNGs readable through FileProvider; GIF import preserved bytes and removal worked; 4 experimental languages available; system click selected\n"
    }
}
