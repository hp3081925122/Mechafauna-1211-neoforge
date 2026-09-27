package com.hp.mechafauna.item;

import com.hp.mechafauna.client.renderer.MechArmorRenderer;
import com.hp.mechafauna.registry.ModArmorMaterials;
import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class MechArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MechArmorItem(ArmorItem.Type type) {
        super(ModArmorMaterials.MECH, type, new Item.Properties());
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private MechArmorRenderer renderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(
                LivingEntity livingEntity,
                ItemStack itemStack,
                EquipmentSlot equipmentSlot,
                HumanoidModel<?> original
            ) {
                if (this.renderer == null) {
                    this.renderer = new MechArmorRenderer();
                }
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.Level level, List<Component> tooltip, TooltipFlag flag) {
        String key = switch (this.type) {
            case HELMET -> "item.mechafauna.mech_helmet.tooltip";
            case CHESTPLATE -> "item.mechafauna.mech_chestplate.tooltip";
            case LEGGINGS -> "item.mechafauna.mech_leggings.tooltip";
            case BOOTS -> "item.mechafauna.mech_boots.tooltip";
            default -> null;
        };
        if (key != null) {
            tooltip.add(Component.translatable(key, Component.keybind("key.mechafauna.fire_support")).withStyle(ChatFormatting.GRAY));
        }
        if (ReinforcementCoreItem.hasArmorCooldownUpgrade(stack)) {
            tooltip.add(Component.translatable("item.mechafauna.reinforcement_core.armor_upgrade").withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
