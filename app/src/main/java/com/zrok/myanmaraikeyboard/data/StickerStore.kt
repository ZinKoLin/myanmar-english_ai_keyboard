package com.zrok.myanmaraikeyboard.data

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** App-created reaction stickers and user-selected images; never uses Gboard's private catalog. */
class StickerStore(private val context: Context) {
    data class Sticker(val file: File, val label: String, val mime: String)
    private val directory get() = File(context.filesDir, "stickers").apply { mkdirs() }
    private val reactions = listOf(
        "မင်္ဂလာပါ" to "👋", "ကျေးဇူးပါ" to "🙏", "ဟုတ်ကဲ့" to "👍", "ချစ်တယ်" to "❤️",
        "ဝမ်းသာတယ်" to "😄", "ဟားဟား" to "😂", "တောင်းပန်ပါတယ်" to "🥺", "စောင့်ဦးနော်" to "⏳",
        "ကောင်းသောညပါ" to "🌙", "အိုကေ" to "👌", "ဂုဏ်ယူပါတယ်" to "🎉", "အားတင်းထား" to "💪"
    )
    fun list(): List<Sticker> {
        val builtins = reactions.mapIndexed { index, (label, _) ->
            Sticker(File(directory, "reaction_$index.png"), label, "image/png")
        }
        val imported = directory.listFiles().orEmpty().filter { it.name.startsWith("custom_") }
            .sortedByDescending { it.lastModified() }.map { Sticker(it, "ကိုယ်ပိုင် sticker", mime(it.extension)) }
        return imported + builtins
    }
    fun materialize(sticker: Sticker): File {
        if (sticker.file.exists()) return sticker.file
        val index = sticker.file.name.removePrefix("reaction_").substringBefore('.').toInt()
        val (label, emoji) = reactions[index]
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.rgb(226, 237, 255)
            canvas.drawRoundRect(14f, 14f, 498f, 498f, 90f, 90f, paint)
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 200f
            canvas.drawText(emoji, 256f, 290f, paint)
            paint.color = Color.rgb(6, 46, 111)
            paint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            paint.textSize = 54f
            while (paint.measureText(label) > 442 && paint.textSize > 24) paint.textSize -= 1
            canvas.drawText(label, 256f, 402f, paint)
            val temporary = File(directory, ".${UUID.randomUUID()}.tmp")
            try {
                temporary.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                check(temporary.renameTo(sticker.file))
            } finally { temporary.delete() }
        } finally { bitmap.recycle() }
        return sticker.file
    }
    fun uri(sticker: Sticker): Uri = FileProvider.getUriForFile(context, "${context.packageName}.stickers", materialize(sticker))

    fun import(uri: Uri): Sticker {
        require(list().count { it.file.name.startsWith("custom_") } < 100) { "ကိုယ်ပိုင် sticker ၁၀၀ ပြည့်နေပါပြီ။" }
        val type = context.contentResolver.getType(uri)
        val extension = when (type) {
            "image/png" -> "png"; "image/webp" -> "webp"; "image/gif" -> "gif"; "image/jpeg" -> "jpg"
            else -> error("PNG, WebP, GIF သို့မဟုတ် JPG ကို ရွေးပါ။")
        }
        val destination = File(directory, "custom_${UUID.randomUUID()}.$extension")
        try {
            context.contentResolver.openInputStream(uri)!!.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 16 * 1024 * 1024) { "Sticker ကို 16 MB အောက် ရွေးပါ။" }
                        output.write(buffer, 0, count)
                    }
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(destination.path, bounds)
            require(bounds.outWidth in 1..4096 && bounds.outHeight in 1..4096) { "ပုံကို ဖတ်မရပါ သို့မဟုတ် 4096 px ထက် ကြီးနေပါသည်။" }
            return Sticker(destination, "ကိုယ်ပိုင် sticker", type!!)
        } catch (error: Throwable) { destination.delete(); throw error }
    }
    fun remove(sticker: Sticker) { if (sticker.file.name.startsWith("custom_")) sticker.file.delete() }
    private fun mime(extension: String) = when (extension) {
        "gif" -> "image/gif"; "webp" -> "image/webp"; "jpg" -> "image/jpeg"; else -> "image/png"
    }
}
