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

## Rencana
2. Weapon takdir, HUD skill (Z/C/V), Battle Mode, harga tiap dewa.
3. Skill 1-3, Faith + persembahan harian, skill 4-6, hukuman ganti kontrak.
4. Polesan (preview 3D patung di layar kontrak, efek, config JSON, dokumentasi lengkap).
Sistem Saint / 7 kursi: ditunda.
