package com.zrok.myanmaraikeyboard.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.TextView

/** Keeps the hint inside the same touch target as the character. */
class KeyboardKeyView(context: Context) : TextView(context) {
    enum class Kind { NORMAL, SHIFT, BACKSPACE, SYMBOLS, ENTER }
    var kind = Kind.NORMAL
    var cornerHint = ""
    var centeredHint = false
    var hintTopDp = 3
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cornerHint.isEmpty()) return
        hintPaint.color = currentTextColor
        hintPaint.alpha = 170
        hintPaint.textSize = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP, if (centeredHint) 12f else 10f, resources.displayMetrics)
        hintPaint.textAlign = if (centeredHint) Paint.Align.CENTER else Paint.Align.RIGHT
        val density = resources.displayMetrics.density
        val x = scrollX + if (centeredHint) width / 2f else width - 6f * density
        val y = scrollY + hintTopDp * density - hintPaint.fontMetrics.ascent
        canvas.drawText(cornerHint, x, y, hintPaint)
    }
}
