package error.voice;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoiceBridge {
    private static final String CLIENT_MANAGER_CLASS = "de.maxhenkel.voicechat.voice.client.ClientManager";
    private static final String VOICECHAT_CLIENT_CLASS = "de.maxhenkel.voicechat.VoicechatClient";
    private static final String MICROPHONE_MANAGER_CLASS = "de.maxhenkel.voicechat.voice.client.microphone.MicrophoneManager";
    private static final String SOUND_MANAGER_CLASS = "de.maxhenkel.voicechat.voice.client.SoundManager";
    private static Boolean presenceCached;
    private static final Map<String, Method> methodCache = new ConcurrentHashMap<>();
    private static final Map<String, Field> fieldCache = new ConcurrentHashMap<>();

    private VoiceBridge() {}

    public static boolean isPresent() {
        if (presenceCached == null) {
            try {
                Class.forName(CLIENT_MANAGER_CLASS);
                presenceCached = Boolean.TRUE;
            } catch (Throwable t) {
                presenceCached = Boolean.FALSE;
            }
        }
        return presenceCached;
    }

    public static boolean hasVoice(UUID uuid) {
        if (uuid == null || !isPresent()) return false;
        try {
            Object stateManager = getPlayerStateManager();
            if (stateManager == null) return false;
            Object state = invokeCached(stateManager, "getState", new Class[]{UUID.class}, uuid);
            if (state == null) return false;
            Object disconnected = invokeCached(stateManager, "isPlayerDisconnected", new Class[]{UUID.class}, uuid);
            return !(disconnected instanceof Boolean b && b);
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object getPlayerStateManager() {
        try {
            Class<?> clazz = Class.forName(CLIENT_MANAGER_CLASS);
            Method method = clazz.getMethod("getPlayerStateManager");
            return method.invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object getClientConfig() {
        try {
            Field field = resolveFieldCached(VOICECHAT_CLIENT_CLASS, "CLIENT_CONFIG");
            return field == null ? null : field.get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object getConfigEntry(String fieldName) {
        try {
            Object config = getClientConfig();
            if (config == null) return null;
            Field field = config.getClass().getField(fieldName);
            return field.get(config);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object getConfigValue(String fieldName) {
        try {
            Object entry = getConfigEntry(fieldName);
            return entry == null ? null : entry.getClass().getMethod("get").invoke(entry);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void setConfigValue(String fieldName, Object value) {
        try {
            Object entry = getConfigEntry(fieldName);
            if (entry == null || value == null) return;
            Method setMethod = findMethodByArgCount(entry.getClass(), "set", 1);
            Method saveMethod = findMethodByArgCount(entry.getClass(), "save", 0);
            if (setMethod == null) return;
            setMethod.setAccessible(true);
            setMethod.invoke(entry, value);
            if (saveMethod != null) {
                saveMethod.setAccessible(true);
                saveMethod.invoke(entry);
            }
        } catch (Throwable t) {}
    }

    public static String getMicrophone() {
        Object v = getConfigValue("microphone");
        return v == null ? "" : v.toString();
    }

    public static void setMicrophone(String name) {
        setConfigValue("microphone", name == null ? "" : name);
    }

    public static String getSpeaker() {
        Object v = getConfigValue("speaker");
        return v == null ? "" : v.toString();
    }

    public static void setSpeaker(String name) {
        setConfigValue("speaker", name == null ? "" : name);
    }

    public static List<String> getMicrophones() {
        return listStaticStrings(MICROPHONE_MANAGER_CLASS, "deviceNames");
    }

    public static List<String> getSpeakers() {
        return listStaticStrings(SOUND_MANAGER_CLASS, "getAllSpeakers");
    }

    private static List<String> listStaticStrings(String className, String staticMethod) {
        List<String> result = new ArrayList<>();
        if (!isPresent()) return result;
        try {
            Class<?> clazz = Class.forName(className);
            Object list = clazz.getMethod(staticMethod).invoke(null);
            if (list instanceof List<?> l) {
                for (Object item : l) {
                    if (item != null) result.add(item.toString());
                }
            }
        } catch (Throwable t) {}
        return result;
    }

    public static double getVoiceChatVolume() {
        return toDoubleOrDefault(getConfigValue("voiceChatVolume"), 1.0);
    }

    public static void setVoiceChatVolume(double v) {
        setConfigValue("voiceChatVolume", v);
    }

    public static double getMicrophoneGain() {
        return toDoubleOrDefault(getConfigValue("microphoneGain"), 1.0);
    }

    public static void setMicrophoneGain(double v) {
        setConfigValue("microphoneGain", v);
    }

    public static boolean isMuted() {
        return toBooleanOrDefault(getConfigValue("muted"), false);
    }

    public static boolean isDisabled() {
        return toBooleanOrDefault(getConfigValue("disabled"), false);
    }

    public static void setMuted(boolean v) {
        if (!tryManagerBooleanSetter("setMuted", v)) {
            setConfigValue("muted", v);
        }
    }

    public static void setDisabled(boolean v) {
        if (!tryManagerBooleanSetter("setDisabled", v)) {
            setConfigValue("disabled", v);
        }
    }

    private static boolean tryManagerBooleanSetter(String method, boolean v) {
        try {
            Object mgr = getPlayerStateManager();
            if (mgr == null) return false;
            Method m = findMethodByArgCount(mgr.getClass(), method, 1);
            if (m == null) return false;
            m.setAccessible(true);
            m.invoke(mgr, v);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isPushToTalk() {
        Object v = getConfigValue("microphoneActivationType");
        return v != null && "PTT".equals(v.toString());
    }

    public static void setPushToTalk(boolean ptt) {
        try {
            Class<?> clazz = Class.forName("de.maxhenkel.voicechat.voice.client.MicrophoneActivationType");
            @SuppressWarnings({"unchecked", "rawtypes"})
            Enum e = Enum.valueOf((Class<Enum>) clazz, ptt ? "PTT" : "VOICE");
            setConfigValue("microphoneActivationType", e);
        } catch (Throwable t) {}
    }

    private static double toDoubleOrDefault(Object o, double def) {
        return o instanceof Number n ? n.doubleValue() : def;
    }

    private static boolean toBooleanOrDefault(Object o, boolean def) {
        return o instanceof Boolean b ? b : def;
    }

    private static Object invokeCached(Object target, String name, Class<?>[] sig, Object... args) {
        try {
            String key = target.getClass().getName() + "#" + name + "/" + sig.length;
            Method m = methodCache.get(key);
            if (m == null) {
                m = target.getClass().getMethod(name, sig);
                m.setAccessible(true);
                methodCache.put(key, m);
            }
            return m.invoke(target, args);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Field resolveFieldCached(String className, String fieldName) {
        String key = className + "#" + fieldName;
        Field f = fieldCache.get(key);
        if (f != null) return f;
        try {
            f = Class.forName(className).getField(fieldName);
            f.setAccessible(true);
            fieldCache.put(key, f);
            return f;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Method findMethodByArgCount(Class<?> c, String name, int argCount) {
        for (Method m : c.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == argCount) return m;
        }
        for (Method m : c.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == argCount) return m;
        }
        return null;
    }
}
