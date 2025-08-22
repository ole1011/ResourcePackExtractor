package de.ole101.rpx.client.ui

import de.ole101.rpx.util.formatFileSize
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import java.io.File
import kotlin.math.roundToInt

class ProgressPanelRenderer {

    fun render(
        context: DrawContext,
        textRenderer: TextRenderer,
        width: Int,
        height: Int,
        isExtracting: Boolean,
        isCompleted: Boolean,
        extractedBytes: Long,
        totalBytes: Long,
        currentFile: String?,
        message: String?,
        error: String?
    ) {
        if (!isExtracting && !isCompleted && error == null) return

        val panelTop = height - 70 - 46
        val panelLeft = width / 2 - 150
        val panelRight = width / 2 + 150

        // background
        context.fill(panelLeft, panelTop, panelRight, panelTop + 46, 0xAA000000.toInt())

        val statusY = panelTop + 6
        val progressBarY = panelTop + 20
        val percentY = progressBarY + 12

        val displayMessage = when {
            error != null -> "Error: $error".take(60)
            isCompleted -> message ?: "Completed"
            else -> message ?: "Preparing..."
        }
        val textColor = if (error != null) 0xFF5555FF.toInt() else 0xFFFFFFFF.toInt()
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(displayMessage), width / 2, statusY, textColor)

        if (totalBytes > 0) {
            renderProgressBar(context, panelLeft, panelRight, progressBarY, extractedBytes, totalBytes)
            renderProgressText(context, textRenderer, width / 2, percentY, extractedBytes, totalBytes)
        }

        if (currentFile != null && error == null && !isCompleted) {
            renderCurrentFile(context, textRenderer, width / 2, panelTop + 46 - 10, currentFile)
        }
    }

    private fun renderProgressBar(
        context: DrawContext,
        panelLeft: Int,
        panelRight: Int,
        progressBarY: Int,
        extractedBytes: Long,
        totalBytes: Long
    ) {
        val barLeft = panelLeft + 10
        val barRight = panelRight - 10
        val barBottom = progressBarY + 8

        // background
        context.fill(barLeft, progressBarY, barRight, barBottom, 0xFF222222.toInt())

        // fill
        val fraction = if (totalBytes > 0) (extractedBytes.toDouble() / totalBytes.toDouble()).coerceIn(0.0, 1.0) else 0.0
        val filled = barLeft + ((barRight - barLeft) * fraction).roundToInt()
        context.fill(barLeft, progressBarY, filled, barBottom, 0xFF55AA55.toInt())
    }

    private fun renderProgressText(
        context: DrawContext,
        textRenderer: TextRenderer,
        centerX: Int,
        y: Int,
        extractedBytes: Long,
        totalBytes: Long
    ) {
        val fraction = if (totalBytes > 0) extractedBytes.toDouble() / totalBytes.toDouble() else 0.0
        val percentText = if (totalBytes > 0) {
            "${(fraction * 100).roundToInt()}% (${formatFileSize(extractedBytes)} / ${formatFileSize(totalBytes)})"
        } else {
            formatFileSize(extractedBytes)
        }
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(percentText), centerX, y, 0xA0FFFFFF.toInt())
    }

    private fun renderCurrentFile(
        context: DrawContext,
        textRenderer: TextRenderer,
        centerX: Int,
        y: Int,
        currentFile: String
    ) {
        val separator = File.separatorChar
        val filename = currentFile.substringAfterLast(separator).substringAfterLast('/')
        val truncatedFilename = filename.take(40)
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(truncatedFilename), centerX, y, 0x80FFFFFF.toInt())
    }
}
