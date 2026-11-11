package net.featherweight.mixin;

import java.util.Map;
import java.util.Optional;
import net.featherweight.ExpiringTemplateMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StructureTemplateManager.class)
abstract class StructureTemplateManagerMixin {
    @Shadow @Final @Mutable
    private Map<ResourceLocation, Optional<StructureTemplate>> structureRepository;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void featherweight$useExpiringCache(CallbackInfo ci) {
        this.structureRepository = new ExpiringTemplateMap();
    }
}
