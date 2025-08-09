package com.example.simpleeconomy;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class SimpleEconomy implements ModInitializer {
    private static final String CONFIG_FILE = "config/simpleeconomy.json";
    private static final int START_BALANCE = 100000;
    private static Map<UUID, Integer> balances = new HashMap<>();

    @Override
    public void onInitialize() {
        loadBalances();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("balance").executes(ctx -> {
                ServerPlayerEntity player = ctx.getSource().getPlayer();
                int bal = balances.getOrDefault(player.getUuid(), START_BALANCE);
                player.sendMessage(Text.of("Ваш баланс: " + bal + " тугриков"), false);
                return 1;
            }));

            dispatcher.register(literal("pay")
                .then(argument("target", StringArgumentType.word())
                .then(argument("amount", IntegerArgumentType.integer(1))
                .executes(ctx -> {
                    ServerPlayerEntity sender = ctx.getSource().getPlayer();
                    String targetName = StringArgumentType.getString(ctx, "target");
                    int amount = IntegerArgumentType.getInteger(ctx, "amount");

                    ServerPlayerEntity target = ctx.getSource().getServer().getPlayerManager().getPlayer(targetName);

                    if (target == null) {
                        sender.sendMessage(Text.of("Игрок не найден!"), false);
                        return 0;
                    }

                    int senderBal = balances.getOrDefault(sender.getUuid(), START_BALANCE);
                    if (senderBal < amount) {
                        sender.sendMessage(Text.of("Недостаточно средств!"), false);
                        return 0;
                    }

                    balances.put(sender.getUuid(), senderBal - amount);
                    balances.put(target.getUuid(), balances.getOrDefault(target.getUuid(), START_BALANCE) + amount);

                    sender.sendMessage(Text.of("Вы перевели " + amount + " тугриков игроку " + target.getName().getString()), false);
                    target.sendMessage(Text.of("Вы получили " + amount + " тугриков от " + sender.getName().getString()), false);

                    saveBalances();
                    return 1;
                }))));
        });
    }

    private static void loadBalances() {
        try {
            File file = new File(CONFIG_FILE);
            if (!file.exists()) return;
            FileReader reader = new FileReader(file);
            balances = new com.google.gson.Gson().fromJson(reader, balances.getClass());
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveBalances() {
        try {
            File file = new File(CONFIG_FILE);
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file);
            new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(balances, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
