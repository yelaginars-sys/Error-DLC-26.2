#version 150 core

in vec2 fragCoord;
out vec4 fragColor;

uniform sampler2D image;
uniform float offset;
uniform vec2 resolution;

void main() {
    // FBO color attachments are GL bottom-origin; fragCoord.y=0 is screen top.
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    vec2 halfpixel = resolution * max(offset, 0.5);

    // 13-tap dual-filter downsample — smoother than classic 5-tap kawase.
    vec4 sum = texture(image, uv) * 4.0;
    sum += texture(image, uv - halfpixel);
    sum += texture(image, uv + halfpixel);
    sum += texture(image, uv + vec2(halfpixel.x, -halfpixel.y));
    sum += texture(image, uv - vec2(halfpixel.x, -halfpixel.y));

    vec2 d = halfpixel * 2.0;
    sum += texture(image, uv + vec2(-d.x, 0.0));
    sum += texture(image, uv + vec2(d.x, 0.0));
    sum += texture(image, uv + vec2(0.0, -d.y));
    sum += texture(image, uv + vec2(0.0, d.y));

    fragColor = sum * (1.0 / 12.0);
}
