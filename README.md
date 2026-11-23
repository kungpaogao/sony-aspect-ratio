# Sony aspect ratio app

## Context
I'm going to attempt to create an app that mirrors the functionality of the [aspect ratio dial on the Fujifilm GFX100RF](https://fujifilm-dsc.com/en/manual/gfx100rf/about_this_camera/parts/#aspect-ratio_dial). 
This is going to be built on top of the amazing [Sony-PMCA-RE](https://github.com/ma1co/Sony-PMCA-RE) project and its derivatives.
I forked [OpenMemories-Framework](https://github.com/kungpaogao/OpenMemories-Framework) and will be extending that with APIs I need as I go. 

I will be using AI to help build this, but I will do my best to review all the code and make it actually readable.

## Build & install
Install older version of adb to better match adb protocol on camera and prevent random crashes:
- Android SDK Platform-Tools r25.0.3 (December 2016)
- Android Debug Bridge version 1.0.36

I created an alias `adb-camera` and made sure that it always runs on port 5038.
```
./gradlew :app:assembleDebug
adb-camera connect <camera-ip> && adb-camera -s <camera-ip> install -r app/build/outputs/apk/debug/app-debug.apk
```