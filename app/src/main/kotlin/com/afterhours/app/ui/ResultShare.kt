package com.afterhours.app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File

/**
 * Shares a love-language result through the Android share sheet as an image card plus a line of text.
 * The image is drawn with the platform canvas, so every script (Devanagari, Gujarati, Latin) renders.
 */
object ResultShare {
    private const val W = 1080
    private const val H = 1350
    private const val PAD = 96f

    fun shareLoveLanguage(
        activity: Activity,
        eyebrow: String,
        result: String,
        description: String,
        mixTitle: String,
        mix: List<Pair<String, Int>>,
        shareText: String,
        chooserTitle: String,
    ) {
        val intent = try {
            val file = drawCard(activity, eyebrow, result, description, mixTitle, mix)
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            // Could not draw or store the image: share the text on its own.
            Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }
        }
        try {
            activity.startActivity(Intent.createChooser(intent, chooserTitle))
        } catch (e: ActivityNotFoundException) {
            // Nothing on the phone can share. Nothing to do.
        }
    }

    private fun drawCard(activity: Activity, eyebrow: String, result: String, description: String, mixTitle: String, mix: List<Pair<String, Int>>): File {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val ink = 0xFF1A1215.toInt()
        val accent = 0xFFD9B36C.toInt()
        val text = 0xFFF5EEE6.toInt()
        val muted = 0xFFB8AAA0.toInt()

        val bg = Paint().apply { shader = LinearGradient(0f, 0f, W.toFloat(), H.toFloat(), 0xFF2A1720.toInt(), ink, Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bg)
        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f; color = accent }
        c.drawRoundRect(RectF(48f, 48f, W - 48f, H - 48f), 48f, 48f, frame)

        var y = PAD + 40f
        val width = (W - 2 * PAD).toInt()
        y += block(c, eyebrow.uppercase(), paint(34f, accent, bold = true, spacing = .12f), width, y) + 28f
        y += block(c, result, paint(96f, accent, bold = true), width, y) + 36f
        y += block(c, description, paint(44f, text), width, y) + 64f
        y += block(c, mixTitle.uppercase(), paint(30f, muted, bold = true, spacing = .12f), width, y) + 24f

        val barBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF }
        val barFg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        val label = paint(36f, text)
        val pct = paint(36f, muted)
        mix.forEach { (name, p) ->
            c.drawText(name, PAD, y + 36f, label)
            val pctText = "$p%"
            c.drawText(pctText, W - PAD - pct.measureText(pctText), y + 36f, pct)
            y += 56f
            c.drawRoundRect(RectF(PAD, y, W - PAD, y + 14f), 7f, 7f, barBg)
            c.drawRoundRect(RectF(PAD, y, PAD + (W - 2 * PAD) * p / 100f, y + 14f), 7f, 7f, barFg)
            y += 44f
        }

        val brand = paint(40f, accent, bold = true, spacing = .08f)
        val name = "TRUTH OR TROUBLE"
        c.drawText(name, (W - brand.measureText(name)) / 2f, H - PAD - 10f, brand)

        val dir = File(activity.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "love-language.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return file
    }

    private fun paint(size: Float, color: Int, bold: Boolean = false, spacing: Float = 0f) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        letterSpacing = spacing
    }

    /** Draws wrapped text at [y] and returns its height. */
    private fun block(c: Canvas, s: String, p: TextPaint, width: Int, y: Float): Float {
        val layout = StaticLayout.Builder.obtain(s, 0, s.length, p, width).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        c.save()
        c.translate(PAD, y)
        layout.draw(c)
        c.restore()
        return layout.height.toFloat()
    }
}
