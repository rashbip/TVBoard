package dev.tvboard.ime

import android.view.View
import android.widget.Button

/**
 * Handles smart D-Pad spatial movement, horizontal wrap-around, and key focus navigation.
 */
class DPadNavigator(
    private val getCardView: () -> View,
    private val getCurrentRows: () -> List<List<Button>>,
    private val getAllFocusableKeys: () -> List<Button>
) {

    fun handleKeyDown(keyCode: Int, currentFocus: View?): Boolean {
        val rows = getCurrentRows()

        when (keyCode) {
            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (currentFocus is Button) {
                    val rowIndex = findRowIndex(currentFocus, rows)
                    if (rowIndex != -1) {
                        val row = rows[rowIndex]
                        val colIndex = row.indexOf(currentFocus)
                        if (colIndex == 0) {
                            // Wrap around to right edge of the same row!
                            row.last().requestFocus()
                            return true
                        }
                    }
                }
                return navigateDirection(View.FOCUS_LEFT, currentFocus)
            }
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (currentFocus is Button) {
                    val rowIndex = findRowIndex(currentFocus, rows)
                    if (rowIndex != -1) {
                        val row = rows[rowIndex]
                        val colIndex = row.indexOf(currentFocus)
                        if (colIndex == row.size - 1) {
                            // Wrap around to left edge of the same row!
                            row.first().requestFocus()
                            return true
                        }
                    }
                }
                return navigateDirection(View.FOCUS_RIGHT, currentFocus)
            }
            android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                return navigateDirection(View.FOCUS_UP, currentFocus)
            }
            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                return navigateDirection(View.FOCUS_DOWN, currentFocus)
            }
        }
        return false
    }

    private fun findRowIndex(btn: Button, rows: List<List<Button>>): Int {
        for (i in rows.indices) {
            if (rows[i].contains(btn)) return i
        }
        return -1
    }

    private fun navigateDirection(direction: Int, currentFocus: View?): Boolean {
        if (currentFocus == null) {
            getAllFocusableKeys().firstOrNull()?.requestFocus()
            return true
        }
        val next = currentFocus.focusSearch(direction)
        if (next != null && next != currentFocus) {
            next.requestFocus()
            return true
        }
        return true
    }
}

