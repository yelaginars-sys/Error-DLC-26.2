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
        register(new ErrorRotation());
        register(new GrimRotation());
        register(new SnapRotation());
        register(new SmoothRotation());

        // Error Rotations
        register(new ErrorSpookyRotation());
        register(new ErrorReallyWorldRotation());
        register(new ErrorFuntimeRotation());
        register(new ErrorAimAssistRotation());
        register(new ErrorHolyWorldRotation());
        register(new ErrorMLRotation());
        register(new ErrorAresRotation());
        register(new ErrorSnapRotation());

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