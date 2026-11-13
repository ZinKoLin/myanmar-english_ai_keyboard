package com.zrok.myanmaraikeyboard.ime

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

/** Offline emoji picker; glyphs use Android's system emoji font. */
class EmojiPanel(
    context: Context,
    private val insert: (String) -> Unit,
    private val backspace: () -> Unit,
    private val configureDelete: (View) -> Unit,
    private val stickers: () -> Unit,
    private val close: () -> Unit
) : LinearLayout(context) {
    private val groups = linkedMapOf(
        "😀" to "😀 😃 😄 😁 😆 😅 😂 🤣 😊 🙂 🙃 😉 😍 🥰 😘 😗 😙 😚 😋 😛 😜 🤪 😝 🤑 🤗 🤭 🤫 🤔 🤐 🤨 😐 😑 😶 😏 😒 🙄 😬 🤥 😌 😔 😪 🤤 😴 😷 🤒 🤕 🤢 🤮 🤧 🥵 🥶 🥴 😵 🤯 😎 🤓 🧐 🥳 😕 😟 🙁 ☹️ 😮 😯 😲 😳 🥺 😦 😧 😨 😰 😥 😢 😭 😱 😖 😣 😞 😓 😩 😫 😤 😡 😠 🤬 😈 👿 💀 ☠️ 💩 🤡 👻 👽 🤖",
        "👋" to "👋 🤚 🖐️ ✋ 🖖 👌 🤌 🤏 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 🖕 👇 ☝️ 👍 👎 ✊ 👊 🤛 🤜 👏 🙌 👐 🤲 🤝 🙏 💪 🦾 🦵 🦶 👂 👃 🧠 👀 👁️ 👅 👄 👶 🧒 👦 👧 🧑 👨 👩 👴 👵 👮 🧑‍⚕️ 🧑‍🏫 🧑‍💻 🧑‍🍳 👷 🧑‍🚀 🧕 👰 🤵 🧚 🧜 🧙",
        "❤️" to "❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 💟 💯 💢 💥 💫 💦 💨 🕳️ 💬 🗨️ 🗯️ 💭 💤 ✨ ⭐ 🌟 🔥 🎉 🎊 🎈 🎁 🎀 🏆 🥇 🥈 🥉 ✅ ❌ ❓ ❗ ⚠️ 🚫 🔴 🟠 🟡 🟢 🔵 🟣 ⚫ ⚪ 🟤",
        "🐱" to "🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🙈 🙉 🙊 🐔 🐧 🐦 🐤 🦆 🦅 🦉 🦇 🐺 🐗 🐴 🦄 🐝 🐛 🦋 🐌 🐞 🐜 🦗 🕷️ 🐢 🐍 🦎 🐙 🦑 🦐 🦀 🐠 🐟 🐬 🐳 🦈 🐘 🦒 🦓 🦍 🦧 🐅 🐆 🌸 🌹 🌺 🌻 🌼 🌷 🌱 🌲 🌳 🌴 🌵 🍀 🍁 🍂 🍃 🌍 🌙 ☀️ 🌈 ☁️ ⛈️ ❄️",
        "🍎" to "🍎 🍏 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🍆 🥑 🥦 🥬 🥒 🌶️ 🌽 🥕 🧄 🧅 🥔 🍠 🥐 🍞 🥖 🥨 🥯 🥞 🧇 🧀 🍖 🍗 🥩 🥓 🍔 🍟 🍕 🌭 🥪 🌮 🌯 🥚 🍳 🥘 🍲 🥗 🍿 🍚 🍛 🍜 🍝 🍣 🍤 🥟 🍦 🍧 🍨 🍩 🍪 🎂 🍰 🧁 🍫 🍬 🍭 🍮 🍯 🍼 🥛 ☕ 🍵 🧃 🥤 🍺 🍻 🥂 🍷",
        "⚽" to "⚽ 🏀 🏈 ⚾ 🎾 🏐 🏉 🎱 🏓 🏸 🥅 🏒 🏏 ⛳ 🏹 🎣 🥊 🥋 🎽 🛹 🛼 ⛸️ 🎿 🏂 🏊 🚴 🏃 🧘 🎮 🎲 🎯 🎸 🎹 🎤 🎧 🎵 🎶 🚗 🚕 🚌 🚎 🏎️ 🚓 🚑 🚒 🚚 🚲 🛵 🏍️ ✈️ 🚀 🚁 🚂 🚆 🚢 ⛵ 🏠 🏡 🏢 🏥 🏫 🏪 🏨 🏰 ⛩️ 🕌 🛕 ⛪ 📱 💻 ⌨️ 🖥️ 🖨️ 📷 🎥 💡 📚 📖 ✏️ 🖊️ 📝 📌 📎 🔑 🔒 🔓 🔔 📅 ⏰ ⌚ 💰 💵 💳 🇲🇲 🇬🇧 🇺🇸 🇹🇭 🇸🇬 🇯🇵 🇰🇷"
    )
    private val grid: GridView
    init {
        orientation = VERTICAL
        val header = LinearLayout(context)
        val abc = TextView(context).apply { text = "ABC"; gravity = Gravity.CENTER; textSize = 16f; setOnClickListener { close() } }
        header.addView(abc, LayoutParams(0, dp(44), 1f))
        groups.keys.forEach { label ->
            header.addView(TextView(context).apply {
                text = label; textSize = 23f; gravity = Gravity.CENTER
                contentDescription = "Emoji category $label"
                setOnClickListener { showGroup(label) }
            }, LayoutParams(0, dp(44), 1f))
        }
        header.addView(TextView(context).apply {
            text = "⌫"; textSize = 22f; gravity = Gravity.CENTER
            contentDescription = "Delete"
            setOnClickListener { backspace() }
            configureDelete(this)
        }, LayoutParams(0, dp(44), 1f))
        addView(header)
        addView(TextView(context).apply {
            text = "Stickers / ပုံနှင့် GIF  ›"; textSize = 14f; gravity = Gravity.CENTER
            setTextColor(Color.rgb(168, 199, 250)); contentDescription = "Open stickers"
            setOnClickListener { stickers() }
        }, LayoutParams(-1, dp(36)))
        grid = GridView(context).apply {
            numColumns = 7
            verticalSpacing = dp(4)
            horizontalSpacing = dp(2)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
        }
        addView(grid, LayoutParams(LayoutParams.MATCH_PARENT, dp(245)))
        showGroup(groups.keys.first())
    }
    private fun showGroup(group: String) {
        val values = groups.getValue(group).split(" ")
        grid.adapter = object : BaseAdapter() {
            override fun getCount() = values.size
            override fun getItem(position: Int) = values[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, recycled: View?, parent: ViewGroup?): View =
                ((recycled as? TextView) ?: TextView(context)).apply {
                    text = values[position]
                    textSize = 27f
                    gravity = Gravity.CENTER
                    setTextColor(Color.BLACK)
                    contentDescription = "Insert emoji " + values[position]
                    setOnClickListener { insert(values[position]) }
                    layoutParams = AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, dp(48))
                }
        }
        grid.setOnItemClickListener { _, _, position, _ -> insert(values[position]) }
        grid.smoothScrollToPosition(0)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
