# Native OSD icons

The camera's shooting-screen icons (P/A/S/M, battery, ISO, memory card, AF dot) are not PNGs inside
Sony's PlayMemories apps. They are **system resources**: they live in the camera's framework resources
(package `0x01`), and every Sony app points at them by integer ID. `ui/NativeIcons.kt` loads them the same
way, on the camera only.

Checked on the ILCE-7R (2026-11-25): every ID below resolves to a `BitmapDrawable` of the listed size,
except "battery charging", which is some other 310x80 graphic. If an ID doesn't resolve on another body,
the view shows its text fallback.

## How they were found

1. Pull a Sony app APK (the ones used here are in `open-memories-app-ai/tools/apk/`, e.g.
   `com.sony.imaging.app.digitalfilter-1.apk`).
2. Dump the compiled layout XML with aapt2, which shows the raw resource IDs instead of jadx's guessed
   names:

   ```sh
   aapt2 dump xmltree --file res/layout/icon_exposuremode.xml com.sony.imaging.app.digitalfilter-1.apk
   # A: ...:program_auto_mode(0x7f0101e6)=@0x010803a1
   ```

   Any `@0x0108xxxx` is a drawable in the system package. jadx shows these as random
   `@android:drawable/...` names because it maps them against a stock Android framework, so don't trust
   decompiled names; use aapt2.
3. Useful layouts to dump: `icon_exposuremode.xml`, `icon_iso.xml`, `icon_exposurecompensation.xml`,
   `shooting_parts_sid_basic_info_for_info_on_panel.xml` (top row), `shooting_parts_sid_footer_panel.xml`
   (bottom row), `af_*.xml`, and `drawable-nodpi/icon_battery.xml` (a level list). aapt2 is in
   `~/Library/Android/sdk/build-tools/<version>/aapt2`.
4. For the a7R's 4:3 panel, the focus-mode icons are in `res/layout-notlong/icon_focusmode.xml`, since
   `res/layout/` has no `icon_focusmode.xml`.
5. The IDs were identical in digital-filter, bracket-pro, time-lapse and star-trails, so they look like
   stable platform constants.

## IDs

| Icon | ID | Native size (px) |
|---|---|---|
| Exposure mode P | `0x010803a1` | 70x55 |
| Exposure mode A | `0x010803a5` | 70x55 |
| Exposure mode S | `0x010803a9` | 70x55 |
| Exposure mode M | `0x010803ad` | 70x55 |
| Intelligent auto | `0x010804d5` | 70x55 |
| Battery level 0 to 4 | `0x01080d4e`, `0x01080509`, `0x01080ca2`, `0x01080335`, `0x01080b0a` | 49x32 |
| ~~Battery charging~~ | `0x0108057b` | 310x80, not a battery icon on the a7R |
| Memory card | `0x01080d7f` | 44x32 |
| No card (orange "NO CARD") | `0x01080682` | 100x32 |
| ISO label | `0x0108050b` | 48x34 |
| Exposure compensation mark | `0x010810fc` | 36x34 |
| AF locked dot (green) / AF-C arcs (green) | `0x01080e84` / `0x01080e85` | 44x40 |
| AE lock (asterisk) | `0x01080fe3` | 42x40 |
| Focus mode AF-A | `0x01081369` | 70x60 slot (bitmap size not yet checked on the a7R) |
| Focus mode AF-S | `0x01081345` | 70x60 slot (bitmap size not yet checked on the a7R) |
| Focus mode AF-C | `0x0108138c` | 70x60 slot (bitmap size not yet checked on the a7R) |
| Focus mode MF | `0x010808a9` | 70x60 slot (bitmap size not yet checked on the a7R) |
| Focus mode DMF | `0x010808ab` | 70x60 slot (bitmap size not yet checked on the a7R) |
| Still aspect 3:2, 16:9, 4:3, 1:1 | `0x01081020`, `0x01080974`, `0x0108129d`, `0x01081242` | 47x32 |

Battery level thresholds come from `ScalarProperties.getIntArray("ui.battery.threshold.list")`, default
`{80, 50, 20}`: level 4 above 80%, 3 above 50%, 2 above 20%, 1 above 0, else 0.

## Getting the bitmaps off the camera

There's no framebuffer access over adb on the a7R, but an app can draw each drawable to a `Bitmap` and save
it as a PNG. Two gotchas: the SD card only accepts 8.3 file names (`osd01.png`, not `osd-mode_p.png`; a
folder named `osd-icons` can't be created either), and the PNGs are Sony's artwork, so they stay out of
this repo.

## Off the camera

On the emulator (stock Android) these IDs either don't exist or point at unrelated stock drawables, so
`NativeIcons.drawable` returns null unless `DeviceInfo.isCamera()`, and catches
`Resources.NotFoundException` on the camera too.

## Moving this into OpenMemories-Framework

Once the IDs are confirmed on hardware, this fits the framework as a small `OsdIcons` class: the ID
constants plus `Drawable get(Context, int id)` that returns null off-camera. Apps would then supply their
own fallback art only for the emulator. A firmware dump of `framework-res.apk` would also let us extract
the PNGs for emulator use; none is on this machine.
