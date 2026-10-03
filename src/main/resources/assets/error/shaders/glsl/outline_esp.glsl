#version 120

uniform sampler2D texture;
uniform vec2 texelSize;
uniform vec4 color;

void main() {
    vec4 center = texture2D(texture, gl_TexCoord[0].xy);

    if (center.a != 0) discard;

    float alpha = 0;
    vec4 endCol = vec4(0);
    for (float x = -1; x <= 1; x++) {
        for (float y = -1; y <= 1; y++) {
            vec4 curColor = texture2D(texture, gl_TexCoord[0].xy + vec2(texelSize.x * x, texelSize.y * y));
            if (curColor.a != 0) {
                alpha += max(0, (2 - sqrt(x * x + y * y)));
            }
            curColor.rgb *= curColor.a;
            endCol += curColor;
        }
    }
    vec3 outRgb = endCol.a > 0.001 ? (endCol.rgb / endCol.a) : color.rgb;
    gl_FragColor = vec4(outRgb, color.a * alpha);
}