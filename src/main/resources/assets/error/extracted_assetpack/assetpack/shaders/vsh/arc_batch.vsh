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
flat out vec2 HalfSize;
flat out vec4 Color1;
flat out vec4 Color2;
flat out float Radius;
flat out float Thickness;
flat out float StartAngle;
flat out float EndAngle;

int unsign(int value) {
    return value < 0 ? value + 65536 : value;
}

void main() {
    Radius = float(unsign(UV1.x)) / 4096.0;
    Thickness = float(unsign(UV1.y)) / 4096.0;

    int colorLow = unsign(UV2.x);
    int colorHigh = unsign(UV2.y);
    Color2 = vec4(
        float(colorHigh & 0xFF),
        float((colorLow >> 8) & 0xFF),
        float(colorLow & 0xFF),
        float((colorHigh >> 8) & 0xFF)) / 255.0;

    EndAngle = (round(Normal.x * 127.0) + round(Normal.y * 127.0) * 128.0) / 45.0;
    StartAngle = max(round(abs(Normal.z) * 127.0) - 1.0, 0.0) * (360.0 / 126.0);

    LocalPos = UV0;
    HalfSize = abs(UV0);
    Color1 = Color;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
