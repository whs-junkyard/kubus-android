# KUSmartBus for Android

<a href="https://play.google.com/store/apps/details?id=th.in.whs.ku.bus"><img src="https://developer.android.com/images/brand/en_generic_rgb_wo_60.png" alt="Get it on Google Play Store"></a>

(Thai store only)

## Using the source

1. `git clone https://github.com/whs/kubus-android.git kubus`
2. `cd kubus; git submodule init; git submodule update`
3. Import into Eclipse (Import > Existing Projects into Workspace)
4. Add [Support Library appcompat v7](https://developer.android.com/tools/projects/projects-eclipse.html#ReferencingLibraryProject) and Google Play Services into workspace
5. Import libraries projects into Eclipse (Import > Existing Android Code into Workspace) select libs folder and import all projects.
6. Edit sentry project properties, add libs/android-async-http.jar to Java Build Path

## License

KUSmartBus for Android is licensed under [GNU Affero General Public License, version 3](https://www.gnu.org/licenses/agpl-3.0.html) or later.

The name "KUSmartBus", "KU Smart Bus" may not be used without permission.

- [android-async-http](https://github.com/koush/android-websockets) *(libs/android-async-http.jar)* is licensed under Apache license v2.0
- [android-websockets](https://github.com/koush/android-websockets) *(libs/android-websockets)* is licensed under MIT license
- [AppRater](https://github.com/delight-im/AppRater) *(libs/AppRater)* is licensed under Apache License v2.0
- [Sentry-Android](https://github.com/whs/Sentry-Android) *(libs/sentry)* is licensed under MIT license
- [Wire](https://github.com/square/wire) *(libs/wire-runtime.jar wire-compiler.jar)* is licensed under Apache License v2.0