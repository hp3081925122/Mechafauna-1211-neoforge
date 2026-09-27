package com.hp.mechafauna.gametest;

import com.hp.mechafauna.Mechafauna;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BlockBasedTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.BlockBasedTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

final class GameTestRegistration {
    private GameTestRegistration() {
    }

    static void register(RegisterGameTestsEvent event, String name, String structure, int timeout, int setup,
                         Rotation rotation, Consumer<GameTestHelper> test) {
        var environment = event.registerEnvironment(
            Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "empty_" + name), new TestEnvironmentDefinition.AllOf());
        var info = new TestData<Holder<TestEnvironmentDefinition<?>>>(environment,
            Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, structure), timeout, setup, true, rotation);
        event.registerTest(Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, name), new RegisteredTest(info, test));
    }

    private static final class RegisteredTest extends BlockBasedTestInstance {
        private final Consumer<GameTestHelper> test;

        private RegisteredTest(TestData<Holder<TestEnvironmentDefinition<?>>> info, Consumer<GameTestHelper> test) {
            super(info);
            this.test = test;
        }

        @Override
        public void run(GameTestHelper helper) {
            this.test.accept(helper);
        }

        @Override
        public MapCodec<BlockBasedTestInstance> codec() {
            return BlockBasedTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Mechafauna");
        }
    }
}
