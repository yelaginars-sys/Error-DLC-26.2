package error.command.impl;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import error.Client;
import error.command.Command;
import error.ui.hud.impl.GpsHud;
import error.util.client.persiki.ChatUtil;

import java.util.List;

public class GpsCommand extends Command {

    public GpsCommand() {
        super("gps", "Установка метки навигатора в HUD");
    }

    @Override
    public List<String> aliases() {
        return List.of("waypoint", "point", "nav");
    }

    private String getPrefix() {
        if (Client.INSTANCE != null && Client.INSTANCE.commandManager != null) {
            return Client.INSTANCE.commandManager.getPrefix();
        }
        return ".";
    }

    @Override
    public void build(LiteralArgumentBuilder<Object> builder) {
        builder.executes(context -> {
            String prefix = getPrefix();
            ChatUtil.info("Управление GPS меткой в HUD:");
            ChatUtil.entry(prefix + "gps x z", "Поставить метку на X Z");
            ChatUtil.entry(prefix + "gps x y z", "Поставить метку на X Y Z");
            ChatUtil.entry(prefix + "gps название x z", "Поставить именованную метку");
            ChatUtil.entry(prefix + "gps off / clear", "Отключить GPS");
            return 1;
        });

        LiteralArgumentBuilder<Object> offArg = LiteralArgumentBuilder.literal("off");
        offArg.executes(context -> clearGps());
        builder.then(offArg);

        LiteralArgumentBuilder<Object> clearArg = LiteralArgumentBuilder.literal("clear");
        clearArg.executes(context -> clearGps());
        builder.then(clearArg);

        LiteralArgumentBuilder<Object> stopArg = LiteralArgumentBuilder.literal("stop");
        stopArg.executes(context -> clearGps());
        builder.then(stopArg);

        // .gps <x> <z>  OR  .gps <x> <y> <z>
        builder.then(RequiredArgumentBuilder.<Object, Double>argument("x", DoubleArgumentType.doubleArg())
                .then(RequiredArgumentBuilder.<Object, Double>argument("z", DoubleArgumentType.doubleArg())
                        .executes(context -> {
                            double x = DoubleArgumentType.getDouble(context, "x");
                            double z = DoubleArgumentType.getDouble(context, "z");
                            return setGps(null, x, Double.NaN, z);
                        })
                        .then(RequiredArgumentBuilder.<Object, String>argument("name", StringArgumentType.greedyString())
                                .executes(context -> {
                                    double x = DoubleArgumentType.getDouble(context, "x");
                                    double z = DoubleArgumentType.getDouble(context, "z");
                                    String name = StringArgumentType.getString(context, "name");
                                    return setGps(name, x, Double.NaN, z);
                                }))
                )
                .then(RequiredArgumentBuilder.<Object, Double>argument("y", DoubleArgumentType.doubleArg())
                        .then(RequiredArgumentBuilder.<Object, Double>argument("z", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    double x = DoubleArgumentType.getDouble(context, "x");
                                    double y = DoubleArgumentType.getDouble(context, "y");
                                    double z = DoubleArgumentType.getDouble(context, "z");
                                    return setGps(null, x, y, z);
                                })
                                .then(RequiredArgumentBuilder.<Object, String>argument("name", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            double x = DoubleArgumentType.getDouble(context, "x");
                                            double y = DoubleArgumentType.getDouble(context, "y");
                                            double z = DoubleArgumentType.getDouble(context, "z");
                                            String name = StringArgumentType.getString(context, "name");
                                            return setGps(name, x, y, z);
                                        }))
                        )
                )
        );

        // .gps <name> <x> <z>
        builder.then(RequiredArgumentBuilder.<Object, String>argument("name", StringArgumentType.word())
                .then(RequiredArgumentBuilder.<Object, Double>argument("x", DoubleArgumentType.doubleArg())
                        .then(RequiredArgumentBuilder.<Object, Double>argument("z", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    double x = DoubleArgumentType.getDouble(context, "x");
                                    double z = DoubleArgumentType.getDouble(context, "z");
                                    return setGps(name, x, Double.NaN, z);
                                })
                                .then(RequiredArgumentBuilder.<Object, Double>argument("y", DoubleArgumentType.doubleArg())
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "name");
                                            double x = DoubleArgumentType.getDouble(context, "x");
                                            double z = DoubleArgumentType.getDouble(context, "z");
                                            double y = DoubleArgumentType.getDouble(context, "y");
                                            return setGps(name, x, y, z);
                                        }))
                        )
                )
        );
    }

    private static int setGps(String name, double x, double y, double z) {
        GpsHud.setTarget(name, x, y, z);
        if (name != null && !name.isEmpty()) {
            if (!Double.isNaN(y)) {
                ChatUtil.success("GPS навигирует к '" + name + "' (X: " + (int) x + ", Y: " + (int) y + ", Z: " + (int) z + ")");
            } else {
                ChatUtil.success("GPS навигирует к '" + name + "' (X: " + (int) x + ", Z: " + (int) z + ")");
            }
        } else {
            if (!Double.isNaN(y)) {
                ChatUtil.success("GPS установлен на (X: " + (int) x + ", Y: " + (int) y + ", Z: " + (int) z + ")");
            } else {
                ChatUtil.success("GPS установлен на (X: " + (int) x + ", Z: " + (int) z + ")");
            }
        }
        return 1;
    }

    private static int clearGps() {
        GpsHud.clearGps();
        ChatUtil.info("GPS навигатор отключен.");
        return 1;
    }
}