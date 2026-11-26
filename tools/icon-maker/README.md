# Icon maker

## What it's for
Making the app's launcher icon in the style of Sony's PlayMemories icons, from any photo.

## How to run it
```
python3 tools/icon-maker/server.py
```
Then open http://127.0.0.1:8765. Options: `--port N`, `--root PATH` (defaults to the repo's git top-level).

## The icon is a square 72×72 PNG
The icon is a square 72×72 PNG, shown as stored: the same width as Sony's 72×96 app cards. The card is 66×66 at (3,2), which leaves room below it for the shadow.

## Where the assets come from
- The badge SVG comes from `plans/icon-maker/make_icon.py`, which draws Univers Condensed as outlines.
- Sony's reference icons were unzipped from the PlayMemories APKs.

## Why the assets stay out of git
They're Sony artwork, and the badge contains outlines from a commercial font, so they live in the gitignored `plans/` folder.

## Installing after "Use as app icon"
That writes `app/src/main/res/drawable-nodpi/ic_launcher.png`. Run `./gradlew :app:assembleDebug`, then follow the camera install steps.
