package com.zrok.myanmaraikeyboard.ime

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.zrok.myanmaraikeyboard.data.StickerStore
import kotlinx.coroutines.*

class StickerPanel(context: Context, private val send: (StickerStore.Sticker) -> Unit,
    private val import: () -> Unit, private val emojis: () -> Unit, private val close: () -> Unit
) : LinearLayout(context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store = StickerStore(context)
    private var stickers = store.list()
    init {
        orientation = VERTICAL
        val header = LinearLayout(context).apply { isBaselineAligned = false }
        fun button(label: String, description: String = label, action: () -> Unit) = TextView(context).apply {
            text = label; contentDescription = description; textSize = 15f; gravity = Gravity.CENTER
            setTextColor(Color.rgb(227, 227, 227)); setOnClickListener { action() }
        }
        header.addView(button("ABC", "Return to keys", close), LayoutParams(0, dp(44), 1f))
        header.addView(button("😀", "Open emoji", emojis), LayoutParams(0, dp(44), 1f))
        header.addView(button("Stickers") {}, LayoutParams(0, dp(44), 2f))
        header.addView(button("＋ ပုံ / GIF", "Import sticker", import), LayoutParams(0, dp(44), 2f))
        addView(header)
        addView(TextView(context).apply {
            text = "ပုံလက်ခံသော app တွင် နှိပ်ထည့်ပါ · ကိုယ်ပိုင်ပုံကို ဖိထားလျှင် ဖျက်နိုင်သည်"
            textSize = 11f; setTextColor(Color.LTGRAY); setPadding(dp(8), 0, dp(8), dp(6))
        })
        val adapter = object : BaseAdapter() {
            override fun getCount() = stickers.size
            override fun getItem(position: Int) = stickers[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, recycled: View?, parent: ViewGroup?): View {
                val sticker = stickers[position]
                return ((recycled as? ImageView) ?: ImageView(context)).apply {
                    tag = sticker.file.name
                    setImageDrawable(null)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setPadding(dp(5), dp(5), dp(5), dp(5))
                    layoutParams = AbsListView.LayoutParams(-1, dp(100))
                    contentDescription = "Insert sticker ${sticker.label}"
                    setOnClickListener { send(sticker) }
                    setOnLongClickListener {
                        if (!sticker.file.name.startsWith("custom_")) return@setOnLongClickListener false
                        val action = button("ဤ sticker ကို ဖျက်ရန်") {}
                        action.setBackgroundColor(Color.rgb(90, 40, 40))
                        val popup = PopupWindow(action, dp(190), dp(48), true)
                        popup.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.DKGRAY))
                        action.setOnClickListener { store.remove(sticker); stickers = store.list(); notifyDataSetChanged(); popup.dismiss() }
                        popup.showAsDropDown(this, 0, -height - dp(48))
                        true
                    }
                    scope.launch {
                        val bitmap = withContext(Dispatchers.IO) { runCatching {
                            val file = store.materialize(sticker)
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(file.path, bounds)
                            val options = BitmapFactory.Options().apply {
                                inSampleSize = (maxOf(bounds.outWidth, bounds.outHeight) / dp(120)).coerceAtLeast(1)
                            }
                            BitmapFactory.decodeFile(file.path, options)
                        }.getOrNull() }
                        if (tag == sticker.file.name) setImageBitmap(bitmap)
                    }
                }
            }
        }
        addView(GridView(context).apply { numColumns = 3; this.adapter = adapter }, LayoutParams(-1, dp(235)))
    }
    override fun onDetachedFromWindow() { scope.cancel(); super.onDetachedFromWindow() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
