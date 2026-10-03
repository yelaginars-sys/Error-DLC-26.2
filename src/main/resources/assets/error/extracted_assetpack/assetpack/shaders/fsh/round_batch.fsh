#version 330
#moj_import <danq:clip.glsl>

layout(std140) uniform RoundBatchData {
    vec4 clipMeta;
    vec4 clipRect0;
    vec4 clipAxes0;
    vec4 clipRect1;
    vec4 clipAxes1;
    float squircle;
};

in vec2 LocalPos;
in vec4 VertexColor;
flat in vec2 HalfSize;
flat in vec4 Radius;
flat in vec4 OutlineColor;
flat in float Thickness;
flat in float Softness;

out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 size, vec4 radius) {
    radius.xy = (center.x > 0.0) ? radius.xy : radius.zw;
    float r = (center.y > 0.0) ? radius.x : radius.y;
    r *= 2.0;
    r = max(r, 0.1);
    vec2 q = abs(center) - (size) + r;
    float exponent = squircle > 0.5 ? 4.0 : 2.0;
    vec2 max_q = max(q, 0.0);
    float dist = pow(pow(max_q.x, exponent) + pow(max_q.y, exponent), 1.0 / exponent) - r;
    return dist + min(max(q.x, q.y), 0.0);
}

void main() {
    if (!clipContains(gl_FragCoord.xy, clipMeta, clipRect0, clipAxes0, clipRect1, clipAxes1)) {
        discard;
    }

    vec2 p = vec2(LocalPos.x, -LocalPos.y);
    vec2 size = max(HalfSize * 2.0, vec2(0.0001));
    float dist = roundedBoxSDF(p, HalfSize, Radius);

    vec2 pixel = fwidth(LocalPos);
    float px = max(max(pixel.x, pixel.y), 0.0001);
    float softLocal = Softness * px;

    vec2 coords = p / size + 0.5;
    vec4 gradient = VertexColor;
    gradient += mix(0.0019607843, -0.0019607843, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));

    if (Thickness > 0.0) {
        float edge = max(softLocal, px);
        float halfWidth = Thickness * px * 0.5;
        float ring = clamp((halfWidth - abs(dist + halfWidth)) / edge + 0.5, 0.0, 1.0);
        float outer = clamp(-dist / edge + 0.5, 0.0, 1.0);

        float fillAlpha = gradient.a * outer;
        float outlineAlpha = OutlineColor.a * ring;

        float alpha = outlineAlpha + fillAlpha * (1.0 - outlineAlpha);
        if (alpha <= 0.0005) {
            discard;
        }

        vec3 rgb = alpha > 0.0001
            ? (OutlineColor.rgb * outlineAlpha + gradient.rgb * fillAlpha * (1.0 - outlineAlpha)) / alpha
            : OutlineColor.rgb;

        fragColor = vec4(rgb, alpha);
        return;
    }

    float alpha = gradient.a * (1.0 - smoothstep(-px, softLocal + px, dist));
    if (alpha <= 0.0005) {
        discard;
    }

    fragColor = vec4(gradient.rgb, alpha);
}
