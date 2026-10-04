package error.util.display.batch;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.jspecify.annotations.Nullable;

public final class BatchRenderState implements GuiElementRenderState {
    private static final int MAX_MEMBERS = 4096;

    private final List<GuiElementRenderState> members = new ArrayList<>(32);
    private int[] rects = new int[128];
    private RenderPipeline pipeline;
    private TextureSetup textureSetup;
    private @Nullable ScreenRectangle scissor;
    private @Nullable ScreenRectangle bounds;
    private int minX;
    private int minY;
    private int maxX;
    private int maxY;

    void open(RenderPipeline pipeline, TextureSetup textureSetup, @Nullable ScreenRectangle scissor) {
        this.pipeline = pipeline;
        this.textureSetup = textureSetup;
        this.scissor = scissor;
        this.bounds = null;
        this.minX = Integer.MAX_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.maxY = Integer.MIN_VALUE;
    }

    void reset() {
        members.clear();
        pipeline = null;
        textureSetup = null;
        scissor = null;
        bounds = null;
    }

    boolean accepts(RenderPipeline pipeline, TextureSetup textureSetup, @Nullable ScreenRectangle scissor) {
        return members.size() < MAX_MEMBERS
                && this.pipeline == pipeline
                && Objects.equals(this.scissor, scissor)
                && this.textureSetup.equals(textureSetup);
    }

    boolean overlaps(int x0, int y0, int x1, int y1) {
        if (x0 >= maxX || x1 <= minX || y0 >= maxY || y1 <= minY) {
            return false;
        }
        int[] rects = this.rects;
        for (int i = 0, end = members.size() << 2; i < end; i += 4) {
            if (x0 < rects[i + 2] && x1 > rects[i] && y0 < rects[i + 3] && y1 > rects[i + 1]) {
                return true;
            }
        }
        return false;
    }

    void add(GuiElementRenderState element, int x0, int y0, int x1, int y1) {
        int offset = members.size() << 2;
        if (offset + 4 > rects.length) {
            rects = Arrays.copyOf(rects, rects.length << 1);
        }
        rects[offset] = x0;
        rects[offset + 1] = y0;
        rects[offset + 2] = x1;
        rects[offset + 3] = y1;
        members.add(element);
        minX = Math.min(minX, x0);
        minY = Math.min(minY, y0);
        maxX = Math.max(maxX, x1);
        maxY = Math.max(maxY, y1);
    }

    void seal() {
        bounds = new ScreenRectangle(minX, minY, maxX - minX, maxY - minY);
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        List<GuiElementRenderState> members = this.members;
        for (int i = 0, size = members.size(); i < size; i++) {
            members.get(i).buildVertices(consumer);
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return pipeline;
    }

    @Override
    public TextureSetup textureSetup() {
        return textureSetup;
    }

    @Override
    public @Nullable ScreenRectangle scissorArea() {
        return scissor;
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return bounds;
    }
}
