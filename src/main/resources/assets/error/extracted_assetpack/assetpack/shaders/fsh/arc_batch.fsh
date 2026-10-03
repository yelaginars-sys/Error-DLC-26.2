#version 330
#moj_import <danq:clip.glsl>

layout(std140) uniform ArcBatchData {
    vec4 clipMeta;
    vec4 clipRect0;
    vec4 clipAxes0;
    vec4 clipRect1;
    vec4 clipAxes1;
};

in vec2 LocalPos;
flat in vec2 HalfSize;
flat in vec4 Color1;
flat in vec4 Color2;
flat in float Radius;
flat in float Thickness;
flat in float StartAngle;
flat in float EndAngle;

out vec4 fragColor;

#define PI 3.141592653589793
#define RAD 0.0174533

void main() {
    if (!clipContains(gl_FragCoord.xy, clipMeta, clipRect0, clipAxes0, clipRect1, clipAxes1)) {
        discard;
    }

    float startAngle = StartAngle * RAD;
    float sweep = min(EndAngle * RAD, PI * 2.0);

    vec2 pixel = fwidth(LocalPos);
    float px = max(max(pixel.x, pixel.y), 0.0001);
    vec2 size = max(HalfSize * 2.0, vec2(0.0001));
    float smoothThresh = 6.0 * px / length(size);

    vec2 centerPos = vec2(LocalPos.x, -LocalPos.y) / max(HalfSize, vec2(0.0001));

    float dist = length(centerPos);
    float bandAlpha = smoothstep(Radius, Radius + smoothThresh, dist)
            * smoothstep(Radius + Thickness, (Radius + Thickness) - smoothThresh, dist);
    float angle = (atan(centerPos.y, centerPos.x) + PI);
    float sweepAngle = mod(angle + PI * 0.5, PI * 2.0);
    float rel = mod(sweepAngle - startAngle + PI * 2.0, PI * 2.0);
    float distStart = rel > PI * 2.0 - smoothThresh ? rel - PI * 2.0 : rel;
    float coverage = min(distStart, sweep - rel);
    float angleAlpha = smoothstep(-smoothThresh, smoothThresh, coverage);
    if (sweep >= PI * 2.0 - smoothThresh) {
        angleAlpha = 1.0;
    }
    if (sweep <= 0.0005) {
        angleAlpha = 0.0;
    }

    float angle2 = (angle / PI * 180.0);
    angle2 = angle2 - 360.0 * floor(angle2 / 360.0);
    if (angle2 >= 180.0) {
        angle2 = (360.0 - angle2) * 2.0;
    } else {
        angle2 = angle2 * 2.0;
    }

    vec4 color = mix(Color1, Color2, angle2 / 360.0) * bandAlpha * angleAlpha;
    if (color.a <= 0.0005) {
        discard;
    }
    fragColor = color;
}
