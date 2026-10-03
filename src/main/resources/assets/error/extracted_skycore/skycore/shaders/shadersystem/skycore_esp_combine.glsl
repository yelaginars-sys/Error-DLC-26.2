#version 150 core

in vec2 fragCoord;
out vec4 fragColor;

uniform sampler2D maskTexture;
uniform sampler2D glowTexture;

uniform vec4 mainColor;
uniform vec4 friendColor;
uniform vec4 params1;
uniform vec4 params2;
uniform vec4 params3;
uniform vec4 params4;
uniform vec4 params5;

float maxChanRgb(vec3 c) {
    return max(c.r, max(c.g, c.b));
}

float maxChanRgba(vec4 c) {
    return max(c.a, maxChanRgb(c.rgb));
}

// В vanilla/старой обводке пороги для маски находятся заметно ниже 0.5.
// Если держать 0.5, то при разных углах/пиксельном покрытии inside/outside
// может "переключаться", из-за чего внешнее свечение пропадает.
const float SHARP_THRESHOLD = 0.18;

bool isFriendTint(vec3 c) {
    return c.g > c.r * 1.5;
}

float edgeDistance(vec2 texCoord, int w, bool inside) {
    vec2 ts = params1.xy;
    float best = 1e6;
    for (int y = -5; y <= 5; y++) {
        for (int x = -5; x <= 5; x++) {
            if (abs(x) > w || abs(y) > w) {
                continue;
            }
            vec2 off = vec2(float(x), float(y));
            float m = maxChanRgba(texture(maskTexture, texCoord + off * ts));
            bool other = inside ? (m < SHARP_THRESHOLD) : (m >= SHARP_THRESHOLD);
            if (other) {
                best = min(best, length(off));
            }
        }
    }
    return best;
}

void main() {
    vec2 texCoord = vec2(fragCoord.x, 1.0 - fragCoord.y);
    vec4 maskS = texture(maskTexture, texCoord);
    vec3 glowS = texture(glowTexture, texCoord).rgb;

    float sharp = maxChanRgba(maskS);
    float blur = clamp(maxChanRgb(glowS), 0.0, 1.0);
    bool inside = sharp >= SHARP_THRESHOLD;

    if (!inside && blur < 0.0015) {
        discard;
    }

    vec3 tint = mainColor.rgb;
    if (friendColor.a > 0.5 && isFriendTint(inside ? maskS.rgb : glowS)) {
        tint = friendColor.rgb;
    }
    float grey = dot(tint, vec3(0.2126, 0.7152, 0.0722));
    tint = clamp(mix(vec3(grey), tint, params5.z), 0.0, 1.0);

    float pulse = 1.0 + params5.x * sin(params5.y * 6.2831853);

    float halo = 0.0;
    if (params3.z > 0.5 && !inside) {
        halo = pow(blur, params2.y) * params2.x;
    }

    float fill = 0.0;
    if (params3.w > 0.5 && inside) {
        fill = params2.z + pow(clamp(1.0 - blur, 0.0, 1.0), 1.5) * params2.w;
    }

    float rim = 0.0;
    int w = int(params1.z + 0.5);
    if (params5.w > 0.5 && w > 0) {
        int mode = int(params1.w + 0.5);
        if (mode == 2 || (mode == 0 && !inside) || (mode == 1 && inside)) {
            float d = edgeDistance(texCoord, w, inside);
            if (d <= float(w)) {
                rim = (1.0 - d / (float(w) + 1.0)) * params3.x;
            }
        }
    }

    float body = (halo + fill) * pulse;
    vec3 rgb = tint * body;
    rgb += mix(tint, vec3(1.0), clamp(params3.y, 0.0, 1.0)) * rim * pulse;

    if (params4.y > 0.5) {
        float sweep = 1.0 - smoothstep(0.0, params4.z, abs(texCoord.y - params4.x));
        rgb += vec3(sweep * params4.w * max(body, rim));
    }

    rgb *= mainColor.a;
    if (maxChanRgb(rgb) < 0.002) {
        discard;
    }
    fragColor = vec4(rgb, 1.0);
}
