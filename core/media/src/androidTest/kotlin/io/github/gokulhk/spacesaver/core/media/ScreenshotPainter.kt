package io.github.gokulhk.spacesaver.core.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

/** Draws a realistic chat-app screenshot: mostly UI and anti-aliased text, one small picture. */
object ScreenshotPainter {
    const val WIDTH = 1080
    const val HEIGHT = 2400

    private val WORDS =
        "the quick brown fox jumps over the lazy dog lorem ipsum dolor sit amet storage photos video saved today".split(
            " ",
        )

    /** Paints the screenshot, embedding [picture] as a shared photo. */
    fun paint(picture: Bitmap): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        fill.color = Color.rgb(15, 118, 110)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), 220f, fill)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 44f }
        text.color = Color.WHITE
        canvas.drawText("SpaceSaver chat", 48f, 150f, text)
        var y = 280f
        var line = 0
        while (y < HEIGHT - 240) {
            val mine = line % 3 == 1
            fill.color = if (mine) Color.rgb(204, 251, 241) else Color.rgb(227, 236, 234)
            val left = if (mine) 300f else 48f
            if (line == 4) {
                canvas.drawBitmap(picture, null, Rect(48, y.toInt(), 648, y.toInt() + 450), null)
                y += 480f
            } else {
                canvas.drawRoundRect(RectF(left, y, left + 732f, y + 150f), 36f, 36f, fill)
                text.color = Color.rgb(24, 32, 31)
                text.textSize = 38f
                canvas.drawText(sentence(line, 0), left + 32f, y + 62f, text)
                canvas.drawText(sentence(line, 5), left + 32f, y + 118f, text)
                y += 180f
            }
            line++
        }
        return bitmap
    }

    private fun sentence(
        line: Int,
        offset: Int,
    ): String = (0 until 6).joinToString(" ") { WORDS[(line * 7 + offset + it * 3) % WORDS.size] }
}
