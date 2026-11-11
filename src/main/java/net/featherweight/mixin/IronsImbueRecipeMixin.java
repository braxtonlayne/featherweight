package net.featherweight.mixin;

import java.util.stream.IntStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Iron's Spellbooks builds one JEI "Imbue" recipe per imbuable item, and each lists every spell at every level
 * (two item stacks each). JEI materializes all of them at startup: about 500,000 item stacks in a large pack.
 * Show each spell at its lowest level only. Imbuing itself is unchanged. Does nothing if Iron's isn't loaded
 * or its code changes ({@code require = 0}).
 */
@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.jei.ArcaneAnvilJeiRecipe", remap = false)
abstract class IronsImbueRecipeMixin {
    @Redirect(method = "lambda$getRecipeItems$1",
            at = @At(value = "INVOKE", target = "Ljava/util/stream/IntStream;rangeClosed(II)Ljava/util/stream/IntStream;"),
            require = 0)
    private IntStream featherweight$lowestLevelOnly(int minLevel, int maxLevel) {
        return IntStream.of(minLevel);
    }
}
