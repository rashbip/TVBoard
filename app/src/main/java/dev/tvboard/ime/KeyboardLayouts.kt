package dev.tvboard.ime

/**
 * All character layouts for English, Bengali, and Numbers/Symbols.
 */
object KeyboardLayouts {

    enum class Mode { EN, BN, NUM_SYM }

    // 1. English QWERTY
    val enRows = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "@"),
        listOf("z", "x", "c", "v", "b", "n", "m", ".", "-", "_")
    )

    // 2. Complete Bengali Letters on MAIN layout:
    // Row 1: Primary Vowels (স্বরবর্ণ)
    // Row 2: Vowel signs / কার
    // Rows 3-5: Consonants (ব্যঞ্জনবর্ণ)
    val bnMainRows = listOf(
        listOf("অ", "আ", "ই", "ঈ", "উ", "ঊ", "ঋ", "এ", "ঐ", "ও", "ঔ"),
        listOf("া", "ি", "ী", "ু", "ূ", "ৃ", "ে", "ৈ", "ো", "ৌ", "্"),
        listOf("ক", "খ", "গ", "ঘ", "ঙ", "চ", "ছ", "জ", "ঝ", "ঞ", "ট", "ঠ"),
        listOf("ড", "ঢ", "ণ", "ত", "থ", "দ", "ধ", "ন", "প", "ফ", "ব", "ভ"),
        listOf("ম", "য", "র", "ল", "শ", "ষ", "স", "হ", "ড়", "ঢ়", "য়", "ৎ", "ং")
    )

    // 3. Numbers & Symbols Page (Secondary Layer for both EN and BN)
    val numSymRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "০"),
        listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")"),
        listOf("+", "-", "=", "/", "\\", "_", ":", ";", "?", "।")
    )
}

