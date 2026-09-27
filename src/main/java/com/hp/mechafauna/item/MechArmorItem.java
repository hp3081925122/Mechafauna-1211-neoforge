package com.hp.mechafauna.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.util.GeckoLibUtil;
import com.hp.mechafauna.client.renderer.MechArmorRenderer;
import com.hp.mechafauna.registry.ModArmorMaterials;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;

public final class MechArmorItem extends Item implements GeoItem {
    private final ArmorType armorType;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MechArmorItem(ArmorType armorType, Item.Properties properties) {
        super(properties.humanoidArmor(ModArmorMaterials.MECH, armorType));
        this.armorType = armorType;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<MechArmorItem, HumanoidRenderState> renderer;

            @Override
            public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack stack, EquipmentSlot slot) {
                if (this.renderer == null) {
                    this.renderer = new MechArmorRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltipAdder, TooltipFlag flag) {
        String key = switch (this.armorType) {
            case HELMET -> "item.mechafauna.mech_helmet.tooltip";
            case CHESTPLATE -> "item.mechafauna.mech_chestplate.tooltip";
            case LEGGINGS -> "item.mechafauna.mech_leggings.tooltip";
            case BOOTS -> "item.mechafauna.mech_boots.tooltip";
            default -> null;
        };
        if (key != null) {
            tooltipAdder.accept(Component.translatable(key, Component.keybind("key.mechafauna.fire_support")).withStyle(ChatFormatting.GRAY));
        }
        if (ReinforcementCoreItem.hasArmorCooldownUpgrade(stack)) {
            tooltipAdder.accept(Component.translatable("item.mechafauna.reinforcement_core.armor_upgrade").withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
