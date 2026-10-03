package error.util.render;

import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public final class ShaderFallback {
    private static final Pattern MOJ_IMPORT = Pattern.compile("^\\s*#moj_import\\s+<(\\w+):([\\w./]+)>\\s*$", Pattern.MULTILINE);
    private static final int MAX_DEPTH = 8;

    private static final String BUILTIN_DYNAMIC_TRANSFORMS = """
            layout(std140) uniform DynamicTransforms {
                mat4 ModelViewMat;
                vec4 ColorModulator;
                vec3 ModelOffset;
                mat4 TextureMat;
            };
            """;

    private static final String BUILTIN_PROJECTION = """
            layout(std140) uniform Projection {
                mat4 ProjMat;
            };
            """;

    private static final String UI_COMMON = """
            const float UI_SIZE_SCALE = 8.0;
            const float UI_RADIUS_SCALE = 16.0;
            const float UI_MSDF_HOLE_BIAS = 0.02;

            float ui_median3(vec3 v) {
                return max(min(v.r, v.g), min(max(v.r, v.g), v.b));
            }

            vec2 ui_unpackU8Pair(int encoded) {
                return vec2(float(encoded & 255), float((encoded >> 8) & 255)) / 255.0;
            }

            vec4 ui_unpackColor(int packedRg, int packedBa) {
                return vec4(ui_unpackU8Pair(packedRg), ui_unpackU8Pair(packedBa));
            }

            float ui_roundedBoxSdfUniform(vec2 point, vec2 halfSize, float radius) {
                vec2 q = abs(point) - halfSize + vec2(radius);
                return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
            }

            float ui_dither(vec2 fragPos) {
                return (fract(sin(dot(fragPos, vec2(12.9898, 78.233))) * 43758.5453) - 0.5) / 255.0;
            }

            float ui_screenPxRange(vec2 uv, vec2 texSize, float pxRange) {
                vec2 unitRange = vec2(pxRange) / texSize;
                vec2 screenTexSize = vec2(1.0) / max(fwidth(uv), vec2(0.00001));
                return max(0.5 * dot(unitRange, screenTexSize), 1.0);
            }

            vec4 ui_compositePremul(vec4 dst, vec4 src) {
                return src + dst * (1.0 - src.a);
            }

            vec4 ui_unpremultiply(vec4 color) {
                return color.a > 0.0001 ? vec4(color.rgb / color.a, color.a) : vec4(0.0);
            }
            """;

    private static final String UI_FRAGMENT = """
            float ui_coverage(float dist) {
                float afwidth = fwidth(dist) * 0.70710678;
                return clamp((0.5 - dist / max(afwidth, 0.0001)), 0.0, 1.0);
            }
            """;

    private ShaderFallback() {}

    public static String load(Identifier shaderId, ShaderType shaderType) {
        if (!shaderId.getNamespace().equals("error")) {
            return null;
        }

        String extension = shaderType == ShaderType.VERTEX ? ".vsh" : ".fsh";
        String source = readClasspath("error", shaderId.getPath() + extension);
        if (source != null) {
            return resolveImports(source, 0);
        }
        return null;
    }

    private static String resolveImports(String source, int depth) {
        if (depth >= MAX_DEPTH) {
            return source;
        }
        Matcher matcher = MOJ_IMPORT.matcher(source);
        StringBuilder result = new StringBuilder(source.length());
        while (matcher.find()) {
            String imported = readInclude(matcher.group(1), matcher.group(2));
            if (imported == null) {
                imported = "";
            }
            imported = stripVersionDirectives(resolveImports(imported, depth + 1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(imported));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String readInclude(String namespace, String path) {
        String fromClasspath = readClasspath(namespace, "include/" + path);
        if (fromClasspath != null) {
            return fromClasspath;
        }
        if (namespace.equals("minecraft")) {
            if (path.equals("dynamictransforms.glsl")) return BUILTIN_DYNAMIC_TRANSFORMS;
            if (path.equals("projection.glsl")) return BUILTIN_PROJECTION;
        }
        if (namespace.equals("error")) {
            if (path.equals("ui_common.glsl")) return UI_COMMON;
            if (path.equals("ui_fragment.glsl")) return UI_FRAGMENT;
        }
        return null;
    }

    private static String stripVersionDirectives(String source) {
        StringBuilder builder = new StringBuilder(source.length());
        for (String line : source.split("\n", -1)) {
            if (!line.stripLeading().startsWith("#version")) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    private static String readClasspath(String namespace, String path) {
        String resourcePath = "/assets/" + namespace + "/shaders/" + path;
        try (InputStream stream = ShaderFallback.class.getResourceAsStream(resourcePath)) {
            return stream != null ? new String(stream.readAllBytes(), StandardCharsets.UTF_8) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}