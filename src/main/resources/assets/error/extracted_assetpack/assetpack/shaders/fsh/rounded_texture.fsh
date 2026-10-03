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
out vec4 OutColor;

float roundedBoxSDF(vec2 center, vec2 size, float radius) {
    float r = max(radius * 2.0, 0.1);
    vec2 q = abs(center) - size + r;
    float exponent = params0.z > 0.5 ? 4.0 : 2.0;
    vec2 maxQ = max(q, 0.0);
    float dist = pow(pow(maxQ.x, exponent) + pow(maxQ.y, exponent), 1.0 / exponent) - r;
    return dist + min(max(q.x, q.y), 0.0);
}

vec2 localPoint(vec2 point) {
    vec2 location = screen.zw;
    vec2 axisX = params1.xy;
    vec2 axisY = params1.zw;
    vec2 p = point - location;
    float det = axisX.x * axisY.y - axisX.y * axisY.x;
    if (abs(det) < 0.0001) {
        return vec2(-100000.0);
    }

    vec2 normalized = vec2(
        (p.x * axisY.y - p.y * axisY.x) / det,
        (axisX.x * p.y - axisX.y * p.x) / det
    );
    return normalized * screen.xy;
}

void main() {
    if (!clipContains(gl_FragCoord.xy, clipMeta, clipRect0, clipAxes0, clipRect1, clipAxes1)) {
        discard;
    }

    vec2 size = screen.xy;
    vec2 local = localPoint(gl_FragCoord.xy);
    float distance = roundedBoxSDF(local - (size / 2.0), size / 2.0, params0.x);
    float softness = max(params0.y, 0.5);
    float mask = 1.0 - smoothstep(-softness, softness, distance);
    vec4 base = texture(Sampler0, TexCoord);
    if (tex.w > 0.5) {
        vec4 overlay = texture(Sampler0, TexCoord + vec2(tex.x, 0.0));
        float a = overlay.a + base.a * (1.0 - overlay.a);
        vec3 rgb = a > 0.0001 ? (overlay.rgb * overlay.a + base.rgb * base.a * (1.0 - overlay.a)) / a : base.rgb;
        base = vec4(rgb, a);
    }
    vec4 textureColor = base * VertexColor;

    OutColor = vec4(textureColor.rgb, textureColor.a * mask);
    if (OutColor.a <= 0.001) {
        discard;
    }
}
