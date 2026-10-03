#version 120

uniform sampler2D image;
uniform float offset;
uniform vec2 resolution;

void main() {
    vec2 uv = gl_TexCoord[0].xy / 2.0;
    vec2 halfpixel = resolution / 2.0 * offset;

    vec3 sum = vec3(0.0);
    sum += texture2D(image, uv + vec2(-halfpixel.x * 2.0, 0.0)).rgb;
    sum += texture2D(image, uv + vec2(-halfpixel.x, halfpixel.y)).rgb * 2.0;
    sum += texture2D(image, uv + vec2(0.0, halfpixel.y * 2.0)).rgb;
    sum += texture2D(image, uv + vec2(halfpixel.x, halfpixel.y)).rgb * 2.0;
    sum += texture2D(image, uv + vec2(halfpixel.x * 2.0, 0.0)).rgb;
    sum += texture2D(image, uv + vec2(halfpixel.x, -halfpixel.y)).rgb * 2.0;
    sum += texture2D(image, uv + vec2(0.0, -halfpixel.y * 2.0)).rgb;
    sum += texture2D(image, uv + vec2(-halfpixel.x, -halfpixel.y)).rgb * 2.0;

    gl_FragColor = vec4(sum / 12.0, 1.0);
}