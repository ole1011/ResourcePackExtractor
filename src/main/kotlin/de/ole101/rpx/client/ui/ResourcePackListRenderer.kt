package de.ole101.rpx.client.ui

import de.ole101.rpx.util.ResourcePackUtil.getSafeDisplayName
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.resource.ResourcePackProfile
import net.minecraft.text.Text

class ResourcePackListRenderer {

    fun render(
        context: DrawContext,
        textRenderer: TextRenderer,
        profiles: List<ResourcePackProfile>,
        selectedIndex: Int,
        width: Int,
        height: Int
    ) {
        val top = 30
        val bottom = height - 40
        val left = width / 2 - 150
        val right = width / 2 + 150

        // background
        context.fill(left - 2, top - 2, right + 2, bottom + 2, 0x88000000.toInt())

        profiles.forEachIndexed { index, profile ->
            val rowTop = top + index * 14
            if (rowTop + 14 > bottom) return@forEachIndexed

            // current selection
            if (index == selectedIndex) {
                context.fill(left, rowTop, right, rowTop + 14, 0x55FFFFFF)
            }

            val name: Text = Text.literal(getSafeDisplayName(profile))
            context.drawTextWithShadow(textRenderer, name, left + 4, rowTop + 3, -1)
        }
    }

    fun getHoveredIndex(mouseX: Int, mouseY: Int, width: Int, profileCount: Int): Int {
        val top = 30
        val left = width / 2 - 150
        val right = width / 2 + 150

        if (mouseX !in left..right || mouseY < top) return -1

        val index = (mouseY - top) / 14
        return if (index in 0 until profileCount) index else -1
    }
}
