# Icon maker

## What it's for
Making the app's launcher icon in the style of Sony's PlayMemories icons, from any photo.

## How to run it
```
python3 tools/icon-maker/server.py
```
Then open http://127.0.0.1:8765. Options: `--port N`, `--root PATH` (defaults to the repo's git top-level).

## Why the file is 72×96 and squeezed
The camera's app menu stretches the 72×96 file to a square, so the art is designed square and then squeezed to 75% of its width.

## Where the assets come from
- The badge SVG comes from `plans/icon-maker/make_icon.py`, which draws Univers Condensed as outlines.
- Sony's reference icons were unzipped from the PlayMemories APKs.

## Why the assets stay out of git
They're Sony artwork, and the badge contains outlines from a commercial font, so they live in the gitignored `plans/` folder.

## Installing after "Use as app icon"
That writes `app/src/main/res/drawable-nodpi/ic_launcher.png`. Run `./gradlew :app:assembleDebug`, then follow the camera install steps.
