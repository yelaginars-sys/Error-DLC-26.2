package error.util;

import error.util.rotation.*;
import error.util.rotation.exs.*;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RotationRegistry {
    private static final Map<String, AuraRotation> ROTATIONS = new LinkedHashMap<>();

    static {
        register(new SpookyTime());
        ROTATIONS.put("4pookytime", get("SpookyTime"));
        register(new LinearRotation());
        register(new MatrixRotation());
        register(new FuntimeRotation());
        register(new BuilderRotation());
        register(new LumenRotation());
        register(new GrimRotation());
        register(new SnapRotation());
        register(new SmoothRotation());

        // Energy Rotations
        register(new EnergySpookyRotation());
        register(new EnergyReallyWorldRotation());
        register(new EnergyFuntimeRotation());
        register(new EnergyAimAssistRotation());
        register(new EnergyHolyWorldRotation());
        register(new EnergyMLRotation());
        register(new EnergyAresRotation());
        register(new EnergySnapRotation());

        // Exclusive (exs) Rotations
        register(new FunTimeExsRotation());
        register(new SpookyTimeExsRotation());
        register(new SlimeWorldExsRotation());
        register(new ReallyWorldExsRotation());
        register(new LonyGriefExsRotation());
        register(new HvHExsRotation());
        register(new ShardExsRotation());
        register(new SlothExsRotation());
        register(new LegitExsRotation());
    }

    public static void register(AuraRotation rotation) {
        ROTATIONS.put(rotation.getName().toLowerCase(), rotation);
    }

    public static AuraRotation get(String name) {
        return ROTATIONS.getOrDefault(name.toLowerCase(), ROTATIONS.values().iterator().next());
    }

    public static String[] getNames() {
        return ROTATIONS.values().stream().map(AuraRotation::getName).toArray(String[]::new);
    }
}