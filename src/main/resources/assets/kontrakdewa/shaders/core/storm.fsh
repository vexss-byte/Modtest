#version 330
#extension GL_ARB_separate_shader_objects : require

// Langit badai kosmik untuk cutscene patung (layar penuh).
// Parameter dikirim lewat warna vertex: R = x sorotan, G = y sorotan (0..1 dari kiri/atas), B = radius sorotan,
// A = (kegelapan 5 bit << 3) | (kilatan 3 bit). Waktu dari Globals.GameTime. Palet: THOR (default) atau RA.

layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    float GlintAlpha;
    vec3 CameraOffset;
    float GameTime;
    vec2 ScreenSize;
    int MenuBlurRadius;
    int UseRgss;
};

uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

// ---------------------------------------------------------------- derau
float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 r = mat2(0.80, 0.60, -0.60, 0.80);
    for (int i = 0; i < 5; i++) {
        v += a * vnoise(p);
        p = r * p * 2.03 + 11.7;
        a *= 0.5;
    }
    return v;
}

float ridged(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        float n = 1.0 - abs(vnoise(p) * 2.0 - 1.0);
        v += a * n * n;
        p = p * 2.1 + 5.3;
        a *= 0.5;
    }
    return v;
}

// bintang: sel acak dengan bintang kecil berkelap-kelip
float stars(vec2 uv, float scale, float t) {
    vec2 g = uv * scale;
    vec2 id = floor(g);
    vec2 f = fract(g) - 0.5;
    float h = hash21(id);
    if (h < 0.86) return 0.0;
    vec2 off = vec2(hash21(id + 3.1), hash21(id + 7.7)) - 0.5;
    float d = length(f - off * 0.6);
    float tw = 0.55 + 0.45 * sin(t * (1.5 + h * 3.0) + h * 40.0);
    return smoothstep(0.09, 0.0, d) * tw * (0.5 + 0.5 * h);
}

void main() {
    vec2 uv = texCoord0;
    float aspect = ScreenSize.x / max(ScreenSize.y, 1.0);

    // ---- parameter dari warna vertex ----
    vec2 spot = vertexColor.rg;
    float spotR = vertexColor.b * 0.8;
    float packedA = floor(vertexColor.a * 255.0 + 0.5);
    float dark = floor(packedA / 8.0) / 31.0;
    float flash = mod(packedA, 8.0) / 7.0;

    float t = GameTime * 1200.0;      // detik (berulang tiap 20 menit)

    // koordinat tinggi-layar: x dikali aspek supaya pola tidak melar
    vec2 q = vec2(uv.x * aspect, uv.y);
    vec2 sp = vec2(spot.x * aspect, spot.y);

    // ---- sorotan mengikuti siluet patung: badan sempit + alas lebar (spotR = setengah tinggi patung di layar) ----
    float hh = max(spotR, 0.02);
    vec2 pq = q - sp;
    vec2 bd = abs(pq) - vec2(0.42 * hh, 1.0 * hh);                       // badan
    float sdBody = length(max(bd, 0.0)) + min(max(bd.x, bd.y), 0.0);
    vec2 pd = abs(pq - vec2(0.0, 0.78 * hh)) - vec2(0.60 * hh, 0.24 * hh); // alas
    float sdPed = length(max(pd, 0.0)) + min(max(pd.x, pd.y), 0.0);
    float sdStatue = min(sdBody, sdPed) / hh;                             // jarak ke siluet (satuan tinggi patung)
    float hole = 1.0 - smoothstep(0.0, 0.14, sdStatue);                   // 1 di atas patung, 0 di luar
    float aura = exp(-max(sdStatue, 0.0) * 5.0);                          // cahaya lembut tepat di luar siluet

    // ---- pusaran badai di atas patung ----
    vec2 eye = vec2(sp.x, max(sp.y - 0.62, -0.1));
    vec2 d = q - eye;
    float r = length(d);
    float ang = atan(d.y, d.x) + 1.15 / (0.25 + r) + t * 0.035;
    vec2 swirl = eye + r * vec2(cos(ang), sin(ang));

    // ---- awan badai berlapis ----
    vec2 warp = vec2(fbm(swirl * 1.6 + t * 0.03), fbm(swirl * 1.6 + 7.3 - t * 0.025));
    float c1 = fbm(swirl * 2.2 + warp * 1.8 + vec2(t * 0.015, 0.0));
    float c2 = fbm(swirl * 4.5 - warp * 1.2 + vec2(-t * 0.022, t * 0.01));
    float density = smoothstep(0.34, 0.74, c1 * 0.7 + c2 * 0.45);

    // ---- warna langit ----
#ifdef RA
    vec3 skyTop = vec3(0.045, 0.020, 0.012);
    vec3 skyLow = vec3(0.16, 0.07, 0.035);
    vec3 cloudDark = vec3(0.060, 0.032, 0.022);
    vec3 cloudLit = vec3(0.62, 0.34, 0.12);
    vec3 nebA = vec3(0.55, 0.20, 0.08);
    vec3 nebB = vec3(0.60, 0.42, 0.10);
    vec3 glow = vec3(1.0, 0.72, 0.30);
    vec3 fogCol = vec3(0.55, 0.36, 0.20);
#else
    vec3 skyTop = vec3(0.012, 0.022, 0.065);
    vec3 skyLow = vec3(0.045, 0.085, 0.17);
    vec3 cloudDark = vec3(0.020, 0.030, 0.055);
    vec3 cloudLit = vec3(0.28, 0.50, 0.85);
    vec3 nebA = vec3(0.10, 0.22, 0.60);
    vec3 nebB = vec3(0.30, 0.16, 0.55);
    vec3 glow = vec3(0.55, 0.78, 1.0);
    vec3 fogCol = vec3(0.30, 0.42, 0.62);
#endif

    vec3 sky = mix(skyTop, skyLow, smoothstep(0.0, 1.0, uv.y));

    // nebula kosmik di sela-sela awan
    float neb = ridged(q * 1.3 + warp * 0.8 + vec2(t * 0.006, 0.0));
    float nebMask = smoothstep(0.35, 0.95, neb) * (1.0 - uv.y * 0.55);
    sky += mix(nebA, nebB, fbm(q * 2.2 + 3.0)) * nebMask * 0.55;

    // bintang (hanya tampak di celah awan)
    float st = 1.2 * stars(q, 34.0, t) + 0.9 * stars(q + 4.2, 66.0, t * 1.3) + 0.6 * stars(q + 9.1, 110.0, t * 0.8);
    sky += vec3(0.85, 0.92, 1.0) * st * (1.0 - density) * (1.0 - hole * 0.9) * (1.6 - uv.y);

    // awan: sisi terang menghadap cahaya petir/kilatan
    float lit = clamp(c1 - fbm(swirl * 2.2 + warp * 1.8 + vec2(0.06, -0.09) + vec2(t * 0.015, 0.0)) + 0.5, 0.0, 1.0);
    float flicker = 0.22 + 0.16 * (0.5 + 0.5 * sin(t * 0.7 + c2 * 6.0)) + flash * 0.85;
    vec3 cloud = mix(cloudDark, cloudLit, clamp(lit * lit * flicker * 2.4 + flash * 0.25, 0.0, 1.0));
    vec3 col = mix(sky, cloud, density * 0.95);

    // kilat internal di dalam awan (denyut lokal acak)
    float slot = floor(t * 2.2);
    float region = floor(q.x * 1.6 + hash21(vec2(slot, 3.0)) * 5.0);
    float burst = step(0.80, hash21(vec2(slot, region))) * exp(-fract(t * 2.2) * 6.0);
    col += glow * burst * density * (0.25 + 0.5 * lit) * (1.0 - uv.y * 0.6) * dark;

    // kilatan menerangi seluruh langit
    col += glow * flash * (0.10 + 0.25 * density);

    // ---- berkas cahaya dari celah awan (miring) ----
    vec2 rd = vec2(q.x * 0.9 + q.y * 0.6, q.y);
    float shafts = smoothstep(0.55, 1.0, fbm(vec2(rd.x * 5.0 + t * 0.02, 0.3))) * (1.0 - density) * (1.0 - uv.y);
    col += glow * shafts * 0.10 * dark;

    // ---- kabut dasar bergerak ----
    float fogLine = smoothstep(0.52, 1.0, uv.y);
    float fogN = fbm(vec2(q.x * 2.4 + t * 0.045, uv.y * 5.0 - t * 0.01));
    float fog = fogLine * smoothstep(0.30, 0.85, fogN);
    col = mix(col, fogCol * (0.5 + 0.5 * fogN), fog * 0.55);

    // ---- kabut tipis tepat di kaki patung ----
    float base = exp(-pow((uv.y - (spot.y + hh * 0.95)) * 9.0, 2.0)) * exp(-pow((uv.x - spot.x) * aspect * 1.6, 2.0));
    col += fogCol * base * (0.35 + 0.35 * fbm(q * 6.0 + t * 0.05)) * 0.5;

    // ---- vignette ----
    float vig = smoothstep(1.25, 0.35, length((uv - 0.5) * vec2(1.0, 0.85) * 1.5));
    col *= mix(0.55, 1.0, vig);

    // ---- cahaya biru/emas di tepi siluet patung (kesan patung disinari badai) ----
    col += glow * aura * (0.20 + 0.25 * flash) * (1.0 - hole);

    // ---- alpha: gelap di luar siluet, patung hampir bening ----
    float outer = mix(0.80, 0.93, density);
    float alpha = dark * mix(outer, 0.10, hole);
    alpha = max(alpha, dark * aura * 0.35 * (1.0 - hole));
    alpha = max(alpha, dark * (base * 0.30 + fog * 0.34) * (1.0 - hole * 0.7));
    alpha = clamp(alpha + flash * 0.05, 0.0, 0.95);

    vec4 junk = texture(Sampler0, texCoord0) * 0.0;   // menjaga Sampler0 tetap terpakai
    fragColor = vec4(col, alpha) + junk;
}
