package error.util.display.blur;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.GpuFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

final class BlurTargets implements AutoCloseable {
    private static final int LEVELS = BlurType.MAX_STRENGTH + 1;

    private final TextureTarget[] down = new TextureTarget[LEVELS];
    private final TextureTarget[] temp = new TextureTarget[LEVELS];
    private final Map<BlurKey, TextureTarget> outputs = new HashMap<>();
    private int baseWidth;
    private int baseHeight;

    void ensure(RenderTarget main) {
        if (main.width == baseWidth && main.height == baseHeight) {
            return;
        }
        close();
        baseWidth = main.width;
        baseHeight = main.height;
    }

    TextureTarget down(int level) {
        return obtain(down, level, "down");
    }

    TextureTarget temp(int level) {
        return obtain(temp, level, "temp");
    }

    TextureTarget output(BlurKey key) {
        if (key.type() == BlurType.KAWASE && key.strength() == 1) {
            return down(1);
        }
        return outputs.computeIfAbsent(key, k -> create("out " + k.type() + " " + k.strength(), k.type().outputLevel(k.strength())));
    }

    @Override
    public void close() {
        destroy(down);
        destroy(temp);
        outputs.values().forEach(TextureTarget::destroyBuffers);
        outputs.clear();
    }

    private TextureTarget obtain(TextureTarget[] pool, int level, String label) {
        if (pool[level] == null) {
            pool[level] = create(label, level);
        }
        return pool[level];
    }

    private TextureTarget create(String label, int level) {
        return new TextureTarget("blur " + label + " L" + level, levelSize(baseWidth, level), levelSize(baseHeight, level), false, GpuFormat.RGBA8_UNORM);
    }

    private static int levelSize(int base, int level) {
        int size = base;
        for (int i = 0; i < level; i++) {
            size = Math.max(1, (size + 1) >> 1);
        }
        return size;
    }

    private static void destroy(TextureTarget[] pool) {
        Arrays.stream(pool).filter(target -> target != null).forEach(TextureTarget::destroyBuffers);
        Arrays.fill(pool, null);
    }
}
