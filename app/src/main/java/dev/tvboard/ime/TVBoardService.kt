package dev.tvboard.ime

import android.os.Handler
import android.os.Looper
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class TVBoardService : InputMethodService() {

    private var currentMode = KeyboardLayouts.Mode.EN
    private var isShifted = false
    private var isFloating = true

    private lateinit var keyboardWrapper: LinearLayout
    private lateinit var keyboardCard: LinearLayout
    private lateinit var keysContainer: LinearLayout
    private lateinit var langToggleBtn: Button
    private lateinit var numToggleBtn: Button
    private lateinit var floatToggleBtn: Button
    private lateinit var textPreview: TextView
    private lateinit var shiftBtn: Button
    private lateinit var backspaceBtn: Button

    private val currentRows = mutableListOf<List<Button>>()
    private val allFocusableKeys = mutableListOf<Button>()

    // D-Pad navigator component
    private val dPadNavigator = DPadNavigator(
        getCardView = { keyboardCard },
        getCurrentRows = { currentRows },
        getAllFocusableKeys = { allFocusableKeys }
    )

    // Auto-repeat for Backspace
    private val handler = Handler(Looper.getMainLooper())
    private var isDeleting = false
    private val deleteRunnable = object : Runnable {
        override fun run() {
            if (isDeleting) {
                performBackspace()
                handler.postDelayed(this, 60)
            }
        }
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null) as LinearLayout
        keyboardWrapper = view
        keyboardCard = view.findViewById(R.id.keyboard_card)
        keysContainer = view.findViewById(R.id.keys_container)
        langToggleBtn = view.findViewById(R.id.action_lang)
        numToggleBtn = view.findViewById(R.id.action_num)
        floatToggleBtn = view.findViewById(R.id.action_float)
        textPreview = view.findViewById(R.id.text_preview)
        shiftBtn = view.findViewById(R.id.key_shift)
        backspaceBtn = view.findViewById(R.id.key_backspace)

        setupQuickActions(view)
        setupBottomRow(view)
        renderKeys()
        applyFloatMode()

        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        updateTextPreview()
        keyboardCard.post {
            allFocusableKeys.firstOrNull { it.id == R.id.action_lang }?.requestFocus()
        }
    }

    private fun setupQuickActions(root: View) {
        langToggleBtn.setOnClickListener {
            currentMode = if (currentMode == KeyboardLayouts.Mode.BN) KeyboardLayouts.Mode.EN else KeyboardLayouts.Mode.BN
            updateTopBarLabels()
            renderKeys()
        }

        numToggleBtn.setOnClickListener {
            currentMode = if (currentMode == KeyboardLayouts.Mode.NUM_SYM) KeyboardLayouts.Mode.EN else KeyboardLayouts.Mode.NUM_SYM
            updateTopBarLabels()
            renderKeys()
        }

        floatToggleBtn.setOnClickListener {
            isFloating = !isFloating
            applyFloatMode()
        }

        root.findViewById<Button>(R.id.action_paste).setOnClickListener {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
            if (!clipText.isNullOrEmpty()) {
                currentInputConnection?.commitText(clipText, 1)
                updateTextPreview()
            }
        }

        root.findViewById<Button>(R.id.action_clear).setOnClickListener {
            val ic = currentInputConnection ?: return@setOnClickListener
            ic.performContextMenuAction(android.R.id.selectAll)
            ic.commitText("", 1)
            updateTextPreview()
        }

        root.findViewById<Button>(R.id.action_hide).setOnClickListener {
            requestHideSelf(0)
        }
    }

    private fun updateTopBarLabels() {
        when (currentMode) {
            KeyboardLayouts.Mode.EN -> {
                langToggleBtn.text = "EN"
                numToggleBtn.text = "123"
                shiftBtn.visibility = View.VISIBLE
                shiftBtn.text = if (isShifted) "⇪" else "⇧"
            }
            KeyboardLayouts.Mode.BN -> {
                langToggleBtn.text = "বাং"
                numToggleBtn.text = "১২৩"
                shiftBtn.visibility = View.GONE
            }
            KeyboardLayouts.Mode.NUM_SYM -> {
                langToggleBtn.text = "EN"
                numToggleBtn.text = "ABC"
                shiftBtn.visibility = View.GONE
            }
        }
    }

    private fun setupBottomRow(root: View) {
        shiftBtn.setOnClickListener {
            if (currentMode == KeyboardLayouts.Mode.EN) {
                isShifted = !isShifted
                shiftBtn.text = if (isShifted) "⇪" else "⇧"
                renderKeys()
            }
        }

        root.findViewById<Button>(R.id.key_space).setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
            updateTextPreview()
        }

        backspaceBtn.setOnClickListener {
            performBackspace()
        }

        root.findViewById<Button>(R.id.key_enter).setOnClickListener {
            val ic = currentInputConnection ?: return@setOnClickListener
            val info = currentInputEditorInfo
            if (info != null && (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
                val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
                if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(action)
                    return@setOnClickListener
                }
            }
            sendKey(KeyEvent.KEYCODE_ENTER)
        }
    }

    private fun performBackspace() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (selected.isNullOrEmpty()) {
            ic.deleteSurroundingText(1, 0)
        } else {
            ic.commitText("", 1)
        }
        updateTextPreview()
    }

    private fun updateTextPreview() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
        val after = ic.getTextAfterCursor(100, 0)?.toString() ?: ""
        val full = before + after
        textPreview.text = if (full.isEmpty()) "" else full
    }

    private fun renderKeys() {
        keysContainer.removeAllViews()
        currentRows.clear()

        val rowsData = when (currentMode) {
            KeyboardLayouts.Mode.EN -> KeyboardLayouts.enRows
            KeyboardLayouts.Mode.BN -> KeyboardLayouts.bnMainRows
            KeyboardLayouts.Mode.NUM_SYM -> KeyboardLayouts.numSymRows
        }

        val inflater = LayoutInflater.from(this)
        val rowHeight = if (currentMode == KeyboardLayouts.Mode.BN) 40 else 44
        val keyTextSize = if (currentMode == KeyboardLayouts.Mode.BN) 19f else 18f

        for (row in rowsData) {
            val rowButtons = mutableListOf<Button>()
            val rowLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(rowHeight)
                ).apply {
                    bottomMargin = dpToPx(3)
                }
                clipChildren = false
                clipToPadding = false
                orientation = LinearLayout.HORIZONTAL
            }

            for (keyChar in row) {
                val displayChar = if (currentMode == KeyboardLayouts.Mode.EN && isShifted && keyChar.length == 1 && keyChar[0].isLetter()) {
                    keyChar.uppercase()
                } else {
                    keyChar
                }

                val btn = (inflater.inflate(R.layout.key_button, rowLayout, false) as Button).apply {
                    text = displayChar
                    textSize = keyTextSize
                    isFocusable = true
                    isFocusableInTouchMode = true
                    setOnClickListener {
                        currentInputConnection?.commitText(text, 1)
                        updateTextPreview()
                    }
                }
                rowButtons.add(btn)
                rowLayout.addView(btn)
            }
            currentRows.add(rowButtons)
            keysContainer.addView(rowLayout)
        }

        collectFocusableKeys()
    }

    private fun collectFocusableKeys() {
        allFocusableKeys.clear()
        fun traverse(view: View) {
            if (view is Button) {
                allFocusableKeys.add(view)
            } else if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    traverse(view.getChildAt(i))
                }
            }
        }
        traverse(keyboardCard)
    }

    private fun applyFloatMode() {
        val dm = resources.displayMetrics
        val screenWidth = dm.widthPixels

        val lp = keyboardCard.layoutParams as LinearLayout.LayoutParams
        if (isFloating) {
            // Float Mode: 55% width docked bottom-right with TV floating margins
            lp.width = (screenWidth * 0.55).toInt()
            lp.setMargins(0, 0, dpToPx(16), dpToPx(16))
            floatToggleBtn.text = "⊡" // Compact icon for Dock
        } else {
            // Full Width Mode: flush bottom
            lp.width = LinearLayout.LayoutParams.MATCH_PARENT
            lp.setMargins(0, 0, 0, 0)
            floatToggleBtn.text = "⧉" // Compact icon for Float
        }
        keyboardCard.layoutParams = lp
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!isInputViewShown) {
            // When keyboard view is closed/hidden, pass all events back to the
            // target app/activity so standard OK/Enter actions work normally!
            return super.onKeyDown(keyCode, event)
        }

        val currentFocus = keyboardCard.findFocus()

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (dPadNavigator.handleKeyDown(keyCode, currentFocus)) {
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                if (currentFocus is Button) {
                    currentFocus.isPressed = true
                    if (currentFocus.id == R.id.key_backspace) {
                        if (!isDeleting) {
                            isDeleting = true
                            performBackspace()
                            handler.postDelayed(deleteRunnable, 350)
                        }
                    } else {
                        currentFocus.performClick()
                    }
                    return true
                }
            }
            KeyEvent.KEYCODE_BACK -> {
                requestHideSelf(0)
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (!isInputViewShown) {
            return super.onKeyUp(keyCode, event)
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            isDeleting = false
            handler.removeCallbacks(deleteRunnable)
            val currentFocus = keyboardCard.findFocus()
            currentFocus?.isPressed = false
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun sendKey(keyCode: Int) {
        val ic = currentInputConnection ?: return
        val now = System.currentTimeMillis()
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
