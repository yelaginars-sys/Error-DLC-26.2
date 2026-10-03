package error.util;

import error.util.rotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 */
public final class RotationRegistry {
    private static final Map<String, AuraRotation> ROTATIONS = new LinkedHashMap<>();

    static {
        register(new LinearRotation());
        register(new MatrixRotation());
        register(new SpookyTime());
        ROTATIONS.put("4pookytime", get("SpookyTime"));
        register(new FuntimeRotation());
        register(new BuilderRotation());
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