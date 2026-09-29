package com.example.stunslam;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class StunSlamClient implements ClientModInitializer {

    private static boolean enabled = false;
    // Megakadályozza, hogy a mod saját ütései újra kiváltsák az eseményt
    private static boolean busy = false;

    @Override
    public void onInitializeClient() {
        registerCommand();
        registerAttackHandler();
    }

    private void registerCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("stunslam")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Text.literal(
                        "StunSlam: " + (enabled ? "BE" : "KI")));
                    return 1;
                })
                .then(ClientCommandManager.literal("on").executes(ctx -> {
                    enabled = true;
                    ctx.getSource().sendFeedback(Text.literal("StunSlam: BE"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("off").executes(ctx -> {
                    enabled = false;
                    ctx.getSource().sendFeedback(Text.literal("StunSlam: KI"));
                    return 1;
                }))
            )
        );
    }

    private void registerAttackHandler() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() || busy || !enabled || hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            // Csak akkor, ha mace van a kezedben
            if (!player.getMainHandStack().isOf(Items.MACE)) {
                return ActionResult.PASS;
            }
            // Csak pajzzsal védekező játékosra
            if (!(entity instanceof PlayerEntity target) || !target.isBlocking()) {
                return ActionResult.PASS;
            }

            PlayerInventory inv = player.getInventory();
            int axeSlot = -1;
            for (int i = 0; i < 9; i++) {
                if (inv.getStack(i).isIn(ItemTags.AXES)) {
                    axeSlot = i;
                    break;
                }
            }
            if (axeSlot < 0) {
                return ActionResult.PASS; // nincs balta a hotbaron
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.interactionManager == null) {
                return ActionResult.PASS;
            }

            int maceSlot = inv.getSelectedSlot();
            busy = true;
            try {
                // 1. balta: leüti a pajzsot
                inv.setSelectedSlot(axeSlot);
                mc.interactionManager.attackEntity(player, entity);
                // 2. vissza a mace-re, és smash
                inv.setSelectedSlot(maceSlot);
                mc.interactionManager.attackEntity(player, entity);
            } finally {
                busy = false;
            }
            // Az eredeti ütést töröljük, mert helyette már megtörtént a kombó
            return ActionResult.FAIL;
        });
    }
}
