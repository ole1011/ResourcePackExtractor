package de.ole101.rpx.mixin;

import de.ole101.rpx.client.screen.ExtractPackScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackScreen.class)
public abstract class PackSelectionScreenMixin extends Screen {

    protected PackSelectionScreenMixin(Text title) { super(title); }

    @Inject(method = "init", at = @At("RETURN"))
    private void addExtractButton(CallbackInfo callbackInfo) {
        ButtonWidget.Builder builder = ButtonWidget.builder(Text.translatable("rpx.pack.extract"),
                button -> this.client.setScreen(new ExtractPackScreen(this.client.currentScreen))).size(98, 20);

        addDrawableChild(builder.position(this.width - 98 - 5, 5).build());
    }
}
