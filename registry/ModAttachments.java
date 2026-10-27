package nullmod.registry;

import java.util.function.Supplier;

import nullmod.NullMod;
import nullmod.core.ChronosData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NullMod.MODID);

    public static final Supplier<AttachmentType<ChronosData>> CHRONOS = ATTACHMENTS.register("chronos",
            () -> AttachmentType.serializable(ChronosData::new).copyOnDeath().build());

    private ModAttachments() {}
}
