#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec2 LocalPos;
out vec4 VertexColor;
out vec2 ScreenUV;
flat out vec2 HalfSize;
flat out vec4 Radius;
flat out vec4 OutlineColor;
flat out float Thickness;
flat out float Softness;
flat out float IsGlyph;
flat out float AtlasIndex;

int unsign(int value) {
    return value < 0 ? value + 65536 : value;
}

void main() {
    int radiusLow = unsign(UV1.x);
    int radiusHigh = unsign(UV1.y);
    Radius = vec4(
        float(radiusLow & 0xFF), float((radiusLow >> 8) & 0xFF),
        float(radiusHigh & 0xFF), float((radiusHigh >> 8) & 0xFF)) / 4.0;

    int colorLow = unsign(UV2.x);
    int colorHigh = unsign(UV2.y);
    OutlineColor = vec4(
        float(colorHigh & 0xFF),
        float((colorLow >> 8) & 0xFF),
        float(colorLow & 0xFF),
        float((colorHigh >> 8) & 0xFF)) / 255.0;

    IsGlyph = Normal.z < 0.0 ? 1.0 : 0.0;

    AtlasIndex = round(Normal.x * 127.0);

    float thicknessBits = round(Normal.x * 127.0) + round(Normal.y * 127.0) * 128.0;
    float softnessBits = max(round(abs(Normal.z) * 127.0) - 1.0, 0.0);
    float softness = softnessBits / 4.0;
    float margin = (softness + 2.0) * 0.5;

    LocalPos = UV0;
    HalfSize = max(abs(UV0) - vec2(margin), vec2(0.0));
    Thickness = thicknessBits / 64.0;
    Softness = softness;
    VertexColor = Color;

    vec4 clipPos = ProjMat * ModelViewMat * vec4(Position, 1.0);
    gl_Position = clipPos;
    ScreenUV = (clipPos.xy / clipPos.w) * 0.5 + 0.5;
}
