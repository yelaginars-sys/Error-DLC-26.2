#version 330
#moj_import <danq:clip.glsl>

layout(std140) uniform RoundData {
    vec4 color1;
    vec4 color2;
    vec4 color3;
    vec4 color4;
    vec4 outlineColor;
    vec2 size;
    vec2 location;
    vec2 axisX;
    vec2 axisY;
    vec4 radius;
    float thickness;
    float softness;
    vec4 clipMeta;
    vec4 clipRect0;
    vec4 clipAxes0;
    vec4 clipRect1;
    vec4 clipAxes1;
    float squircle;
};

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

vec4 createGradient(vec2 coords, vec4 color1, vec4 color2, vec4 color3, vec4 color4){
    vec4 color = mix(mix(color1, color2, coords.y), mix(color3, color4, coords.y), coords.x);
    color += mix(0.0019607843, -0.0019607843, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
    return color;
}

vec2 localPoint(vec2 point) {
    vec2 p = point - location;
    float det = axisX.x * axisY.y - axisX.y * axisY.x;
    if (abs(det) < 0.0001) {
        return vec2(-100000.0);
    }

    vec2 normalized = vec2(
        (p.x * axisY.y - p.y * axisY.x) / det,
        (axisX.x * p.y - axisX.y * p.x) / det
    );
    return normalized * size;
}

void main() {
    if (!clipContains(gl_FragCoord.xy, clipMeta, clipRect0, clipAxes0, clipRect1, clipAxes1)) {
        discard;
    }

    vec2 local = localPoint(gl_FragCoord.xy);
    float distance = roundedBoxSDF(local - (size / 2.0), size / 2.0, radius);
    vec4 gradient = createGradient(local / size, color1, color2, color3, color4);

    if (thickness > 0.0) {
        vec2 pixel = fwidth(local);
        float edge = max(softness, max(max(pixel.x, pixel.y), 0.0001));

        float halfWidth = thickness * 0.5;
        float ring = clamp((halfWidth - abs(distance + halfWidth)) / edge + 0.5, 0.0, 1.0);
        float outer = clamp(-distance / edge + 0.5, 0.0, 1.0);

        float fillAlpha = gradient.a * outer;
        float outlineAlpha = outlineColor.a * ring;

        float alpha = outlineAlpha + fillAlpha * (1.0 - outlineAlpha);
        vec3 rgb = alpha > 0.0001
            ? (outlineColor.rgb * outlineAlpha + gradient.rgb * fillAlpha * (1.0 - outlineAlpha)) / alpha
            : outlineColor.rgb;

        fragColor = vec4(rgb, alpha);
        return;
    }

    float smoothedAlpha = 1.0 - smoothstep(-1.0, softness + 1.0, distance);
    fragColor = vec4(gradient.rgb, gradient.a * smoothedAlpha);
}
