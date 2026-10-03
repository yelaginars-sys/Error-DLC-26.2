#version 120

uniform sampler2D textureIn;
uniform vec4 stripeColor;
uniform vec4 backgroundColor;
uniform float saturation;
uniform float angle;
uniform float time;
uniform vec2 resolution;

vec3 adjustSaturation(vec3 color, float factor) {
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    return mix(vec3(gray), color, factor);
}

void main() {
    vec4 src = texture2D(textureIn, gl_TexCoord[0].st);
    if (src.a < 0.01) {
        discard;
    }

    vec2 uv = gl_FragCoord.xy / resolution.xy;
    float angleRad = radians(angle);
    vec2 rotatedUV = vec2(
        uv.x * cos(angleRad) - uv.y * sin(angleRad),
        uv.x * sin(angleRad) + uv.y * cos(angleRad)
    );

    float stripe = smoothstep(-1.0, 1.0, sin(rotatedUV.x * 35.0 - time * 2.0));

    vec3 stripeRgb = src.rgb * stripeColor.rgb;
    vec3 backgroundRgb = src.rgb * backgroundColor.rgb;
    vec3 color = mix(backgroundRgb, stripeRgb, stripe);
    color = adjustSaturation(color, saturation);

    float alpha = mix(backgroundColor.a, stripeColor.a, stripe) * src.a;
    gl_FragColor = vec4(color, alpha);
}
