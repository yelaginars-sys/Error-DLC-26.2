#version 330
#moj_import <danq:clip.glsl>

uniform sampler2D Sampler0;

layout(std140) uniform EffectData {
    vec4 color;
    vec4 color2;
    vec4 screen;
    vec4 params0;
    vec4 params1;
    vec4 tex;
    vec4 halfPixel;
    vec4 clipMeta;
    vec4 clipRect0;
    vec4 clipAxes0;
    vec4 clipRect1;
    vec4 clipAxes1;
};

in vec2 TexCoord;
in vec4 VertexColor;
in float LocalX;
out vec4 OutColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

float screenPxRange() {
    vec2 textureSizePx = vec2(textureSize(Sampler0, 0));
    vec2 unitRange = vec2(params0.x) / textureSizePx;
    vec2 screenTexSize = vec2(1.0) / max(fwidth(TexCoord), vec2(0.000001));
    return max(0.5 * dot(unitRange, screenTexSize), 1.0);
}

float fadeMask() {
    float mode = params0.y;

    if (mode < 0.5 && params1.z > 0.5) {
        float t = clamp((LocalX - params1.x) / max(params1.y, 0.0001), 0.0, 1.0);
        return pow(1.0 - t, 1.5);
    }

    float mask = 1.0;

    if ((mode >= 0.5 && mode < 1.5) || mode >= 2.5) {
        if (params1.y > 0.0) {
            float t = clamp((LocalX - params1.x) / max(params1.y, 0.0001), 0.0, 1.0);
            mask *= pow(t, 1.5);
        }
    }

    if ((mode >= 1.5 && mode < 2.5) || mode >= 2.5) {
        if (params1.w > 0.0) {
            float t = clamp((LocalX - params1.z) / max(params1.w, 0.0001), 0.0, 1.0);
            mask *= pow(1.0 - t, 1.5);
        }
    }

    return mask;
}

float fadeMaskMarquee() {
    float mask = 1.0;

    if (params1.y > 0.0) {
        float t = clamp((LocalX - params1.x) / max(params1.y, 0.0001), 0.0, 1.0);
        mask *= pow(1.0 - t, 1.5);
    }

    if (params1.w > 0.0) {
        float t = clamp((LocalX - params1.z) / max(params1.w, 0.0001), 0.0, 1.0);
        mask *= pow(t, 1.5);
    }

    return mask;
}

void main() {
    if (!clipContains(gl_FragCoord.xy, clipMeta, clipRect0, clipAxes0, clipRect1, clipAxes1)) {
        discard;
    }

    vec3 msd = texture(Sampler0, TexCoord).rgb;
    float signedDistance = median(msd.r, msd.g, msd.b) - 0.5;
    float alpha = clamp(screenPxRange() * signedDistance + 0.5, 0.0, 1.0);

    float mask = params0.y >= 3.5 ? fadeMaskMarquee() : fadeMask();

    OutColor = vec4(VertexColor.rgb, VertexColor.a * alpha * mask);
    if (OutColor.a <= 0.001) {
        discard;
    }
}
