#version 150 core
in vec2 fragCoord;
uniform sampler2D uTex;
uniform vec2 size;
uniform float radius;
uniform float hurt_time;
uniform float alpha;
uniform float texXSize;
uniform float texYSize;
out vec4 fragColor;
float signedDistanceField(vec2 p, vec2 b, float r) {
    return length(max(abs(p) - b, 0.0)) - r;
}
void main() {
    vec2 tex = fragCoord;
    vec2 pixel = tex * size;
    vec2 centre = 0.5 * size;
    float roundMask = 1.0 - smoothstep(0.0, 1.0, signedDistanceField(centre - pixel, centre - radius - 1.0, radius));
    vec2 baseSkinCoord = vec2(
        mix(8.0 / texXSize, 16.0 / texXSize, tex.x),
        mix(8.0 / texYSize, 16.0 / texYSize, tex.y)
    );
    vec4 baseSkin = texture(uTex, baseSkinCoord);
    vec2 overlayCoord = vec2(
        mix(40.0 / texXSize, 48.0 / texXSize, tex.x),
        mix(8.0 / texYSize, 16.0 / texYSize, tex.y)
    );
    vec4 overlay = texture(uTex, overlayCoord);
    vec3 finalColor = mix(baseSkin.rgb, overlay.rgb, overlay.a);
    float finalAlpha = max(baseSkin.a, overlay.a);
    finalColor = mix(finalColor, vec3(1.0, 0.0, 0.0), hurt_time);
    finalAlpha = finalAlpha * roundMask * alpha;
    fragColor = vec4(finalColor, finalAlpha);
}
