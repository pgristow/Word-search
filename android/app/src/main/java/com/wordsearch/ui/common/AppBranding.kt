package com.wordsearch.ui.common

/**
 * Single source of truth for the app's name/branding. Change [NAME] here and every place
 * that shows the app name (top bars, load-screen logo, etc.) updates automatically.
 */
object AppBranding {
    const val NAME = "WordPop"

    /**
     * The name split into stacked lines for the load-screen logo. If the name starts with
     * "Word" we break after it (WORD / POP); otherwise the whole name is one line.
     */
    val logoLines: List<String> = run {
        if (NAME.length > 4 && NAME.take(4).equals("Word", ignoreCase = true)) {
            listOf("WORD", NAME.drop(4).uppercase())
        } else {
            listOf(NAME.uppercase())
        }
    }
}
