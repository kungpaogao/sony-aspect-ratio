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


real build

1. setup MVVM without importing the newer VM dependencies
2. need some way to mock all the changes in emulator - make a fake so can tweak params and see
   them change as expected on real camera. this should be included with openmemories-framework
3. look into: `getAspectRatio() / set	String	Aspect ratio: "3:2", "16:9", "4:3", "1:1".`
   4. does this allow arbitrary aspect ratios?
   5. if not, manually crop
4. does the raw capture without the aspect ratio crop?
5. preview ui
   6. show the crop box + area around it should be grayed out (but still visible)
   5. should display the same info as normal camera (all settings, shots left, etc.)


for the future

1. eventually allow jpg color customization - what makes "leica" or "hasselblad" colors? are luts
   sufficient?
2. 