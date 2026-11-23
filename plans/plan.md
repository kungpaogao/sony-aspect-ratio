prototype
1. import the interfaces for sony scalar camera
```
Step 1. Add it in your settings.gradle.kts at the end of repositories:

	dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url = uri("https://jitpack.io") }
		}
	}

Step 2. Add the dependency

	dependencies {
	        implementation("com.github.kungpaogao:OpenMemories-Framework:Tag")
	}
```

2. get some simple overlay that shows a 65:24 crop box over the camera preview
   3. display lens info over the image for now 
4. allow user to enter and exit the app without it locking up

prototype 2 - take real pictures
1. look into: `getAspectRatio() / set	String	Aspect ratio: "3:2", "16:9", "4:3", "1:1".`
   4. does this allow arbitrary aspect ratios?
   5. if not, manually crop
2. allow to switch between 3:2, 16:9, 65:24, 17:6, 1:1, 5:4
   3. controls: press ok button, then rotate any dial (start with top back dial by thumb)
3. allow to take picture and save to sdcard

real build
1. mirror the native camera ui
   2. bottom: SS, f-stop, exposure comp, iso
   3. top: mode, battery, storage left
2. allow switching between shooting modes: P, A, S, M are allowable 
   5. allow user to change any shooting settings
   5. look at the sony sample apps to see how apps like digital filter support this

for the future
6. make sure raw is saved without crop
1. eventually allow jpg color customization - what makes "leica" or "hasselblad" colors? are luts
   sufficient?
2. 