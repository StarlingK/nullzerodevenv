package nullmod;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import nullmod.net.ModNetwork;
import nullmod.registry.ModAttachments;
import nullmod.registry.ModEffects;
import nullmod.registry.ModEntities;
import nullmod.registry.ModGameRules;
import nullmod.registry.ModItems;
import nullmod.registry.ModPotions;
import nullmod.registry.ModSounds;
import nullmod.registry.ModTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(NullMod.MODID)
public class NullMod {
    public static final String MODID = "nullmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NullMod(IEventBus modBus, ModContainer container) {
        ModAttachments.ATTACHMENTS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModPotions.POTIONS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModTabs.TABS.register(modBus);

        ModGameRules.init();
        modBus.addListener(ModNetwork::register);

        container.registerConfig(ModConfig.Type.COMMON, NullConfig.SPEC);
    }
}
