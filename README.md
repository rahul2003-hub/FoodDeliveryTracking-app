# Food Delivery Tracking

## Build APK

### Debug APK

On Windows, open a terminal in the project root and run:

```bat
gradlew.bat assembleDebug
```

The debug APK will be created at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

### Release APK

To build the release APK, run:

```bat
gradlew.bat assembleRelease
```

The release APK will be created at:

```text
app\build\outputs\apk\release\app-release.apk
```

### Install the Debug APK

With a device connected through USB debugging or an Android emulator running, install the debug APK with:

```bat
gradlew.bat installDebug
```

If Gradle is not recognized, use the included `gradlew.bat` wrapper from the project root. An Android SDK and Java 11 or newer are required.