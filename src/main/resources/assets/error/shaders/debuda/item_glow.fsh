#version 150

in vec2 TexCoord;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

out vec4 OutColor;

vec4 packedItem(vec2 uv) {
    vec4 item = texture(Sampler0, uv);
    float colorCoverage = max(item.a,
            max(item.r, max(item.g, item.b)));

    // The capture FBO is cleared to depth 1.0. Every visible item fragment writes
    // a smaller value even when its texture is pure black, so depth supplies the
    // reliable silhouette that RGB/alpha alone cannot provide.
    float capturedDepth = texture(Sampler1, uv).r;
    float depthCoverage = 1.0 - step(0.999999, capturedDepth);
    float coverage = max(colorCoverage, depthCoverage);
    return vec4(item.rgb, coverage);
}

void main() {
    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));

    // Four bilinear taps reconstruct an isotropic 3x3 tent kernel. Coverage remains
    // continuous: no threshold can rebuild the item's pixel staircase.
    // The destination is the quarter-size bloom mip. These four linear samples
    // cover a wider source footprint before the Gaussian passes.
    vec2 halfTexel = texel;
    vec4 filtered = packedItem(TexCoord + vec2(halfTexel.x, halfTexel.y));
    filtered += packedItem(TexCoord + vec2(-halfTexel.x, halfTexel.y));
    filtered += packedItem(TexCoord + vec2(halfTexel.x, -halfTexel.y));
    filtered += packedItem(TexCoord - halfTexel);
    filtered *= 0.25;

    OutColor = vec4(clamp(filtered.rgb, 0.0, 1.0),
            clamp(filtered.a, 0.0, 1.0));
}
