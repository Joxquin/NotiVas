package com.notivas.data.repository.copilot.tools

import android.os.Build
import android.text.Html

object HtmlUtils {
    fun cleanHtml(html: String): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(html).toString().trim()
            }
        } catch (e: Exception) {
            html.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
        }
    }
}
