package com.zrok.myanmaraikeyboard.data

import android.content.res.AssetManager
import com.zrok.myanmaraikeyboard.model.MonLexicon

internal object MonDictionary {
    @Volatile private var loaded: MonLexicon? = null
    fun get(assets: AssetManager): MonLexicon = loaded ?: synchronized(this) {
        loaded ?: assets.open("mon_burmese_lexicon.tsv").bufferedReader(Charsets.UTF_8)
            .use { MonLexicon(it.lineSequence()) }.also { loaded = it }
    }
}
