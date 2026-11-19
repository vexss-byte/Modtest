# Kontrak Dewa (LAPERGODS)

Mod Fabric untuk Minecraft 26.3. **Tahap 1**: blok patung Ra & Thor (model GeckoLib beranimasi) + layar kontrak.

## Yang sudah ada (tahap 1)
- Blok `kontrakdewa:ra_statue` dan `kontrakdewa:thor_statue` (ada di creative tab OP Blocks, atau `/give @s kontrakdewa:ra_statue`).
- Klik kanan patung membuka layar kontrak. Tombol **Sign Contract** mengirim permintaan ke server.
- Server memvalidasi: dewa valid, pemain dalam jarak 8 blok dari patung, belum punya kontrak lain. Hasilnya disimpan permanen per pemain.
- Mengganti kontrak ditolak sementara (hukuman dewa menyusul di tahap 3).
- Alat tes: **Creative + Shift + klik kanan** patung = reset kontrak.

## Dependensi (client & server)
Fabric Loader 0.19.5+, Fabric API 0.165.0+26.3, GeckoLib 5.5.7+, Java 25.

## Build
Push ke `main` -> GitHub Actions membuat artifact `.jar`. Tag `v*` -> Release otomatis.
Lokal: `gradle build` (Gradle 9.5.1, Java 25).

## Mengubah model patung
File Blockbench ada di `tools/`. Setelah edit, konversi ulang:
```
python3 tools/bbmodel_to_geckolib.py tools/ra_sun_god_statue_taller_pedestal.bbmodel ra_statue 8 src/main/resources/assets/kontrakdewa
python3 tools/bbmodel_to_geckolib.py tools/thor_statue_altar_v12_ra_style_thor_motifs.bbmodel thor_statue 0 src/main/resources/assets/kontrakdewa
```
(angka 8 / 0 = titik tengah X/Z model di Blockbench.)

## Tahap 2 (ini)
- Tekan **B** = Battle Mode (hanya jika sudah berkontrak). Z/C/V = skill 1-3 (masih placeholder; semua tombol bisa diganti di Controls > Kontrak Dewa).
- Harga dewa aktif saat Battle Mode: Ra siang cerah +20% / malam -25% damage, Thor armor -50%.
- Menandatangani kontrak memberi weapon takdir (Sun Staff / Thunder Hammer).
- HUD kanan-bawah: status Battle Mode, label harga, 3 slot skill.

## Tahap 3a
- **Faith (1-10):** XP dari Battle Mode (tiap 10 detik), dari tiap skill yang kena target, dan dari persembahan harian. Batas XP battle 600/hari. Naik level otomatis.
- **Persembahan harian** (reset 00.00 WIB): klik kanan patung sambil pegang item. Ra: emas / gandum / bunga matahari. Thor: besi / copper / lightning rod. Streak harian menambah bonus XP.
- **Skill 1-3** (butuh Battle Mode + weapon di tangan utama):
  - Ra: Sunspear (tombak cahaya), Noon Beam (sinar menembus, membakar), Mirage Step (dash + Speed). Damage dikali harga siang/malam.
  - Thor: Thunder Strike (petir ke target), Chain Lightning (loncat antar musuh), Hammer Throw (versi instan: hantaman + petir + dorongan).
  - Cooldown tampil di HUD. Skill 4-6 terkunci sampai Faith 3 / 6 / 9 (isinya menyusul di 3b).
- **Weapon takdir:** hanya 1, tidak jatuh saat mati (diberi lagi saat respawn), bisa diambil lagi lewat tombol "Ambil Weapon" di layar kontrak (cooldown 10 menit).
- **Config:** `config/kontrakdewa.json` (dibuat otomatis): XP, batas harian, damage/cooldown/jarak tiap skill, `allowBlockDamage`. Restart server setelah mengubah. Catatan: HUD client membaca ambang Faith dari config client, jadi samakan file config kalau mengubah `faithThresholds` di server.

## Tahap 3b
- **Skill 4-6** (terbuka di Faith 3 / 6 / 9), tombol X / R / G:
  - Ra: Solar Flare (ledakan cahaya di sekitarmu, membakar + membutakan), Scarab Guard (perisai Absorption + Resistance), Call the Sun (tandai lingkaran, 2 detik kemudian kolom cahaya menghantam, membakar tanah).
  - Thor: Storm Leap (melompat lalu menghantam dengan gelombang petir), Static Field (area listrik 6 detik, melukai + memperlambat), Overcharge (5 stack Charged: tiap pukulan senjata menambah damage petir).
- **Special Magic Thor** (Faith 10, tombol H): Ragnarok Storm, badai petir 8 detik di titik bidikan.
- **Hukuman ganti kontrak:** klik patung dewa lain lalu "Ganti Kontrak": Faith -50%, weapon lama hancur, Weakness + Slowness 30 menit, murka dewa lama (Ra membakar, Thor menyambar), terkunci 24 jam.
- **Weapon di punggung:** pemain berkontrak yang tidak Battle Mode menampilkan weapon di punggung (terlihat pemain lain). Posisi/rotasi/skala ada di konstanta paling atas `BackWeaponLayer.java`.
- Config skill baru: `ra_4..6`, `thor_4..7` di `config/kontrakdewa.json` (`range` = radius/jarak sesuai skill).

## Tahap 4: cutscene klik kanan patung
- Klik kanan patung: kamera menengok ke patung, layar menggelap dengan vignette, bintang berkelap-kelip muncul, konstelasi dewa tergambar perlahan (Ra: cakram matahari + kepala + tongkat; Thor: palu terangkat + simbol X di dada), partikel naik di sekeliling patung, lalu panel kontrak meluncur dari kanan (total sekitar 2,7 detik). Esc menutup; rotasi kamera dikembalikan.
- Data konstelasi (titik + garis) ada di `CosmosFx.java` (RA_NODES/RA_EDGES, THOR_NODES/THOR_EDGES), mudah diubah.
- Waktu tiap tahap ada di konstanta atas `ContractScreen.java`.

## Revisi 3b (aturan weapon & hotbar Battle)
- **Weapon hanya ada saat Battle Mode.** Tekan B: weapon muncul di slot hotbar terpilih (item lama dipindah) dan pemain dipaksa terus memegangnya (tidak bisa ganti slot / pindah tangan). Keluar Battle Mode: weapon dihapus dari inventori. Tidak Battle Mode: weapon hanya terlihat di punggung.
- **Hotbar Battle:** saat Battle Mode, hotbar bawaan disembunyikan dan diganti hotbar Battle (slot weapon + 3 skill Z/C/V dengan cooldown). Kartu Faith tetap di kanan-bawah.
- **Weapon dewa tidak bisa jatuh** (mati, tombol Q, dll): item yang jatuh langsung dihapus. Setelah mati pemain otomatis keluar dari Battle Mode dan tidak membawa weapon.
- **Fokus 3 skill utama:** tombol X/R/G/H dan HUD skill 4-6/Special Magic dinonaktifkan sementara (kode skill-nya masih ada).
- **Weapon di punggung:** posisi diagonal (kepala kanan-atas, gagang kiri-bawah); setelan di konstanta atas `BackWeaponLayer.java`.
- Tombol "Ambil Weapon" di layar kontrak dihapus (weapon tidak lagi dimiliki permanen).

## Source Minecraft untuk referensi
Workflow `sources.yml` (manual): tab Actions > "Minecraft Sources" > Run workflow. Menjalankan `genSources` dan mengunggah source Minecraft 26.3 yang sudah didekompilasi sebagai artifact `minecraft-sources` (ada juga `minecraft-sources-selected.zip` yang lebih kecil).

## Tahap 4b: cutscene sinematik 8 detik (revisi besar)
Satu jam (`CutsceneCamera.elapsed()`) mengatur kamera, efek, konstelasi, dan panel:
| Detik | Adegan |
|---|---|
| 0-1,5 | Pembuka: patung utuh dari depan-kiri, dolly masuk pelan (peralihan mulus dari kamera pemain) |
| 1,5-3,5 | Orbit halus ke depan-kanan sambil naik sedikit |
| 3,5-5,0 | Shot lebar (crane naik lalu turun menetap di depan patung); layar menggelap jadi suasana badai: awan, pulau batu melayang, kabut berlapis, bintang |
| 5,0-6,8 | Manifestasi sosok raksasa: bintang muncul -> garis terbentuk berurutan -> aura nebula menebal dari kaki ke kepala -> mata menyala |
| 6,8-8,0 | Panel kontrak meluncur dari kanan (easing); tombol muncul saat panel sudah ~60% terlihat |
Spasi / Enter melompati cutscene; Esc menutup.

**File:**
- `CutsceneCamera.java`: jalur kamera spline Hermite (tabel `KEYS`), komposisi layar akhir, penempatan sosok raksasa, pembersihan HUD/kamera (`stop()`).
- `CosmosFx.java`: tirai gelap dengan sorotan di patung, gradasi dasar, vignette, bintang.
- `StormFx.java`: awan badai, pulau melayang, kabut berlapis, cahaya kaki patung, petir bercabang (Thor), kilatan, bara.
- `GiantFx.java`: sosok raksasa berlapis (aura nebula, cahaya luar/dalam, garis inti, bintang berdenyut, mata, debu bintang).
- `FxSprites.java`: pembantu gambar sprite/poligon/garis.
- `ContractScreen.java`: timeline, tema panel (navy/cyan untuk Thor, emas untuk Ra), partikel dunia, guntur.
- `CameraMixin.java`: ditambah pembersihan otomatis (layar tertutup / pemain keluar / error).
- Aset: `textures/gui/nebula_blob.png`, `star_glow.png`, `mist.png` (dibuat `tools/make_fx_textures.py`); `constellations/thor.json` dan `ra.json` (dibuat `tools/make_giant.py`; sekarang berisi `blobs` aura dan `dust`).
- Model 3D, texture model, animasi GeckoLib, renderer, jaringan, dan sistem kontrak TIDAK diubah.

## Tahap 4c: shader GLSL cutscene
- **Shader:** `assets/kontrakdewa/shaders/core/storm.vsh` dan `storm.fsh` (GLSL 330). Langit badai kosmik layar penuh: awan berputar (pusaran di atas patung), nebula, bintang berkelap-kelip, berkas cahaya, kabut dasar bergerak, kilat internal di dalam awan, dan kilatan petir yang menerangi awan. Dua palet: Thor (biru) dan Ra (emas, lewat `#define RA`).
- **Lubang siluet patung:** shader membuat area berbentuk badan sempit + alas lebar hampir bening, sehingga patung tetap terlihat jelas, dengan cahaya lembut di tepinya.
- **Integrasi:** `ShaderFx.java` mendaftarkan pipeline OPSIONAL (`RenderPipelines.registerOptional`), jadi kalau shader gagal dikompilasi game tidak crash dan efek bawaan (`CosmosFx`) dipakai. Parameter dikirim lewat warna vertex (x/y/tinggi patung, kegelapan, kilatan); waktu dari `Globals.GameTime`.
- **Opsi mematikan:** `config/kontrakdewa.json` -> `"cutsceneShader": false`.
- **Pratinjau/validasi di luar game:** `tools/preview_shader.py <thor|ra> <out.png>` me-render shader lewat OpenGL headless di atas dunia tiruan; shader divalidasi dengan `glslangValidator`.

## Rencana
2. Weapon takdir, HUD skill (Z/C/V), Battle Mode, harga tiap dewa.
3. Skill 1-3, Faith + persembahan harian, skill 4-6, hukuman ganti kontrak.
4. Polesan (preview 3D patung di layar kontrak, efek, config JSON, dokumentasi lengkap).
Sistem Saint / 7 kursi: ditunda.
