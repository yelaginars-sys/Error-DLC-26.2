#version 120

uniform vec4 stripeColor;
uniform vec4 backgroundColor;
uniform float saturation;
uniform float angle;
uniform float time;
uniform vec2 resolution;
uniform float blurWidth;

varying float linePos;

vec3 adjustSaturation(vec3 color, float factor) {
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    return mix(vec3(gray), color, factor);
}

void main() {
    vec2 uv = gl_FragCoord.xy / resolution.xy;

    float angleRad = radians(angle);
    vec2 rotatedUV = vec2(
        uv.x * cos(angleRad) - uv.y * sin(angleRad),
        uv.x * sin(angleRad) + uv.y * cos(angleRad)
    );

    float stripe = smoothstep(-1.0, 1.0, sin(rotatedUV.x * 35.0 - time * 2.0));

    vec3 color = mix(backgroundColor.rgb, stripeColor.rgb, stripe);
    color = adjustSaturation(color, saturation);

    float alpha = mix(backgroundColor.a, stripeColor.a, stripe);
    alpha *= 1.0 - smoothstep(0.0, blurWidth, linePos);
    alpha *= 1.0 - pow(linePos / blurWidth, 2.0);

    gl_FragColor = vec4(color, alpha);
}
