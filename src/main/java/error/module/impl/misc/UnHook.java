package error.module.impl.misc;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import error.Client;
import error.event.EventTarget;
import error.event.list.KeyboardInputEvent;
import error.module.Category;
import error.module.Module;
import error.module.impl.render.ClickGui;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UnHook extends Module {

    private String getPath() {
        return Minecraft.getInstance().gameDirectory.getAbsolutePath();
    }

    @Getter
    private static String code = "1";
    public static boolean unhooked = false;

    public static File resourcePackFolder;

    private final List<Module> savedModules = new ArrayList<>();
    private final Map<Module, Integer> savedKeys = new HashMap<>();

    private static PrintStream originalOut;
    private static PrintStream originalErr;
    private static PrintStream filteredOut;
    private static PrintStream filteredErr;

    private static final String[] LEAK_PATTERNS = {
            "Error DLC",
            "error.module",
            "error.Client",
            "ErrorClient",
            "Lumen",
            "Ancient",
            "ancient",
            "[Ancient",
            "Ancient »",
            "[Config]",
            "[Manager]",
            "[Lang]",
            "[Ancient Logs]",
            "- white",
            "mod white",
            "white,",
            "Execution",
            "FunTime",
            "funtime",
            "Litka",
            "Sloth",
            "dynamic_fps",
            "Dynamic FPS",
            "FontRenderer",
            "FontInitializer",
            "FontAtlas",
            "client/Font",
            "client/Penguin",
            "PenguinCompanion",
            "Animate cancelled",
            "#animate {",
            "Вход",
            "Logs sanitized",
            "ModuleManager",
            "ConfigManager",
            "EventBus",
            "ShaderStore",
            "RenderUtil",
            "ColorUtil",
            "EnemySense",
            "AutoCrystal",
            "CrystalAura",
            "AttackAura",
            "SpearSwap",
            "TriggerBot",
            "ElytraTarget",
            "ClickGui",
            "InterFace",
            "ModuleName",
            "ru.white",
    };

    public UnHook() {
        super("UnHook", "Убирает сторонние хуки и инжекты (Паник-кнопка)", Category.MISC);
    }

    private static boolean shouldFilter(String line) {
        if (line == null || line.isEmpty()) return false;
        for (String p : LEAK_PATTERNS) {
            if (line.contains(p)) return true;
        }
        return false;
    }

    private static class FilteredPrintStream extends PrintStream {
        private final PrintStream delegate;

        public FilteredPrintStream(PrintStream delegate) {
            super(delegate);
            this.delegate = delegate;
        }

        @Override
        public void println(String s) {
            if (!shouldFilter(s)) {
                delegate.println(s);
            }
        }

        @Override
        public void print(String s) {
            if (!shouldFilter(s)) {
                delegate.print(s);
            }
        }

        @Override
        public void println(Object obj) {
            String s = String.valueOf(obj);
            if (!shouldFilter(s)) {
                delegate.println(obj);
            }
        }

        @Override
        public void print(Object obj) {
            String s = String.valueOf(obj);
            if (!shouldFilter(s)) {
                delegate.print(obj);
            }
        }

        @Override
        public void write(byte[] b) {
            String s = new String(b);
            if (!shouldFilter(s)) {
                try { delegate.write(b); } catch (Exception ignored) {}
            }
        }

        @Override
        public void write(byte[] b, int off, int len) {
            String s = new String(b, off, len);
            if (!shouldFilter(s)) {
                try { delegate.write(b, off, len); } catch (Exception ignored) {}
            }
        }

        @Override
        public void write(int b) {
            delegate.write(b);
        }

        @Override
        public void flush() {
            delegate.flush();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        unhooked = true;

        installLogFilter();

        Minecraft mc = Minecraft.getInstance();
        error.ui.mainmenu.PanelRefractions.close(mc);
        if (screen() != null && !(screen() instanceof net.minecraft.client.gui.screens.ChatScreen)) {
            mc.setScreenAndShow(null);
        }

        if (mc.player != null) {
            Client.getInstance().getCommandManager().setPrefix("!");
            if (mc.gui != null && mc.gui.hud != null && mc.gui.hud.getChat() != null) {
                mc.gui.hud.getChat().clearMessages(false);
            }
        }

        String minecraftPath = getPath();
        if (minecraftPath != null && !minecraftPath.isEmpty()) {
            resourcePackFolder = new File(minecraftPath, "resourcepacks");
        }

        savedModules.clear();
        savedKeys.clear();
        for (Module m : Client.getInstance().getModuleManager().getModules()) {
            if (m == this) continue;

            if (m.isEnabled()) {
                savedModules.add(m);
                m.setState(false);
            }

            if (m.getBind() != null && m.getBind().isBound()) {
                if (!m.getBind().getValue().isEmpty()) {
                    int primaryKey = m.getBind().getValue().get(0);
                    savedKeys.put(m, primaryKey);
                    m.getBind().clear();
                }
            }
        }

        sanitizeLogs();
        removeModFiles();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        restoreAll();
        restoreLogFilter();
    }

    private void restoreAll() {
        unhooked = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            Client.getInstance().getCommandManager().setPrefix(".");
        }

        for (Map.Entry<Module, Integer> entry : savedKeys.entrySet()) {
            entry.getKey().getBind().setSingle(entry.getValue());
        }
        savedKeys.clear();

        for (Module m : savedModules) {
            if (!m.isEnabled()) {
                m.setState(true);
            }
        }
        savedModules.clear();
    }

    private void installLogFilter() {
        try {
            originalOut = System.out;
            originalErr = System.err;
            filteredOut = new FilteredPrintStream(originalOut);
            filteredErr = new FilteredPrintStream(originalErr);
            System.setOut(filteredOut);
            System.setErr(filteredErr);
        } catch (Exception ignored) {}
    }

    private void restoreLogFilter() {
        try {
            if (originalOut != null) System.setOut(originalOut);
            if (originalErr != null) System.setErr(originalErr);
        } catch (Exception ignored) {}
    }

    private void sanitizeLogs() {
        try {
            Minecraft mc = Minecraft.getInstance();
            Path sourceLog = mc.gameDirectory.toPath().resolve("logs/latest.log");

            if (!Files.exists(sourceLog)) {
                return;
            }

            String targetDirStr = getPath();
            Path targetDir = Paths.get(targetDirStr, "logs");
            Path targetLog = targetDir.resolve("latest.log");

            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            List<String> lines = Files.readAllLines(sourceLog, StandardCharsets.UTF_8);

            List<String> cleanedLines = lines.stream()
                    .filter(line -> !shouldFilter(line))
                    .collect(Collectors.toList());

            Files.write(targetLog, cleanedLines, StandardCharsets.UTF_8);
            Files.write(sourceLog, new byte[0]);

        } catch (Exception ignored) {}
    }

    private void removeModFiles() {
        try {
            List<Path> targets = new ArrayList<>();
            collectModJars(targets);
            collectUpdatedFiles(targets);
            if (targets.isEmpty()) return;

            Thread cleaner = new Thread(() -> {
                boolean released = false;
                for (int attempt = 0; attempt < 20; attempt++) {
                    boolean allGone = true;
                    for (Path target : targets) {
                        if (!Files.exists(target)) continue;
                        try {
                            Files.delete(target);
                            target.toFile().deleteOnExit();
                        } catch (Exception ignored) {
                            allGone = false;
                        }
                    }
                    if (allGone) return;
                    if (!released) {
                        released = true;
                        releaseLoadedJarHandles(targets);
                        continue;
                    }
                    try { Thread.sleep(1500); } catch (InterruptedException ie) { return; }
                }
            }, "unhook-mod-cleaner");
            cleaner.setDaemon(true);
            cleaner.start();
        } catch (Exception ignored) {}
    }

    private static List<Path> findModsDirs() {
        List<Path> modsDirs = new ArrayList<>();
        addUnique(modsDirs, Minecraft.getInstance().gameDirectory.toPath().resolve("mods"));
        java.net.URL codeSource = UnHook.class.getProtectionDomain().getCodeSource().getLocation();
        if (codeSource != null) {
            try {
                Path jar = Paths.get(codeSource.toURI()).toRealPath();
                Path node = jar.getParent();
                while (node != null) {
                    if (node.getFileName() != null && ".fabric".equals(node.getFileName().toString())) {
                        Path gameDir = node.getParent();
                        if (gameDir != null) addUnique(modsDirs, gameDir.resolve("mods"));
                        break;
                    }
                    node = node.getParent();
                }
            } catch (Exception ignored) {}
        }
        return modsDirs;
    }

    private static boolean isModJar(Path jar) {
        for (Path modsDir : findModsDirs()) {
            try {
                if (jar.toRealPath().startsWith(modsDir.toRealPath())) return true;
            } catch (Exception ignored) {
                if (jar.startsWith(modsDir)) return true;
            }
        }
        return false;
    }

    private void collectModJars(List<Path> targets) {
        java.net.URL codeSource = UnHook.class.getProtectionDomain().getCodeSource().getLocation();
        if (codeSource != null) {
            try {
                Path jar = Paths.get(codeSource.toURI());
                if (Files.isRegularFile(jar) && isModJar(jar)) addUnique(targets, jar);
            } catch (Exception ignored) {}
        }
        for (Path modsDir : findModsDirs()) {
            if (!Files.isDirectory(modsDir)) continue;
            try (var stream = Files.list(modsDir)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().startsWith("dynamic-fps"))
                        .forEach(p -> addUnique(targets, p));
            } catch (Exception ignored) {}
        }
    }

    private void collectUpdatedFiles(List<Path> targets) {
        for (Path modsDir : findModsDirs()) {
            if (!Files.isDirectory(modsDir)) continue;
            try (var stream = Files.list(modsDir)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".updated"))
                        .forEach(p -> addUnique(targets, p));
            } catch (Exception ignored) {}
        }
    }

    private static void addUnique(List<Path> targets, Path candidate) {
        if (!targets.contains(candidate)) targets.add(candidate);
    }

    private static void releaseLoadedJarHandles(List<Path> targets) {
        try {
            List<Path> realTargets = new ArrayList<>(targets.size());
            for (Path t : targets) {
                try { realTargets.add(t.toRealPath()); } catch (Exception e) { realTargets.add(t.toAbsolutePath().normalize()); }
            }

            ClassLoader knot = UnHook.class.getClassLoader();
            while (knot != null && !knot.getClass().getName().startsWith("net.fabricmc.loader.impl.launch.knot.KnotClassLoader")) {
                knot = knot.getParent();
            }
            if (knot == null) return;

            Field urlLoaderField = findField(knot.getClass(), "urlLoader");
            if (urlLoaderField == null) return;
            urlLoaderField.setAccessible(true);
            Object old = urlLoaderField.get(knot);
            if (!(old instanceof URLClassLoader)) return;

            URL[] urls = ((URLClassLoader) old).getURLs();
            List<URL> keep = new ArrayList<>();
            boolean dropped = false;
            for (URL url : urls) {
                if (isTargetUrl(url, realTargets)) {
                    dropped = true;
                } else {
                    keep.add(url);
                }
            }
            if (!dropped) return;

            Class<?> dynCls = Class.forName("net.fabricmc.loader.impl.launch.knot.KnotClassLoader$DynamicURLClassLoader");
            Constructor<?> ctor = dynCls.getDeclaredConstructor(URL[].class);
            ctor.setAccessible(true);
            Object replacement = ctor.newInstance((Object) keep.toArray(new URL[0]));

            urlLoaderField.set(knot, replacement);
            ((URLClassLoader) old).close();
        } catch (Exception ignored) {}
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> c = clazz;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {}
            c = c.getSuperclass();
        }
        return null;
    }

    private static boolean isTargetUrl(URL url, List<Path> realTargets) {
        String ext = url.toExternalForm();
        if (ext.length() > 2 && ext.endsWith("!/")) ext = ext.substring(0, ext.length() - 2);
        try {
            URL base = new URL(ext);
            if ("jar".equals(base.getProtocol())) base = new URL(base.getPath());
            if (!"file".equals(base.getProtocol())) return false;
            Path real = Paths.get(base.toURI()).toRealPath();
            for (Path target : realTargets) {
                if (Files.isSameFile(real, target)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
}
