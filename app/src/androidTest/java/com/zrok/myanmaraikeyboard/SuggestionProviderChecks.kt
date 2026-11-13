package com.zrok.myanmaraikeyboard

import android.content.Context
import android.os.Build
import android.view.textservice.TextServicesManager
import com.zrok.myanmaraikeyboard.data.OnlineWordSuggestions
import com.zrok.myanmaraikeyboard.data.SystemWordSuggestions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

object SuggestionProviderChecks {
    @JvmStatic fun run(context: Context): String = runBlocking {
        val manager = context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
        val provider = if (Build.VERSION.SDK_INT >= 31) manager.currentSpellCheckerInfo?.packageName else "system"
        val checked = try { withContext(Dispatchers.Main) {
            val result = CompletableDeferred<SystemWordSuggestions.Result>()
            lateinit var checker: SystemWordSuggestions
            checker = SystemWordSuggestions(context) {
                checker.cached("hipopatamus")?.let { result.complete(it) }
            }
            try {
                checker.request("hipopatamus")
                withTimeout(5000) { result.await() }
            } finally { checker.close() }
        } } catch (_: TimeoutCancellationException) { null }
        val systemReport = if (checked == null) "Google service unavailable: offline/online fallback used" else
            "Google spelling: hipopatamus correct=${checked.correct}, alternatives=${checked.words}"
        val online = OnlineWordSuggestions()
        val words = online.fetch("hipopatamus")
        check(words != null && "hippopotamus" in words) { "Online suggestions unavailable: $words" }
        check(online.cached("hipopatamus") == words) { "Session cache missing" }
        "System provider: $provider\n$systemReport\n" +
            "Online: hipopatamus => ${words.joinToString()}\nOnline cache: PASS\n"
    }
}
