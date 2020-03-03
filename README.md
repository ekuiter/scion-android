## SCION for Android

![SCION](scion.png)

<a href='https://play.google.com/store/apps/details?id=org.scionlab.scion&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width='200'/></a>

The SCION app enables you to run a [SCION](https://www.scion-architecture.net/) AS attached to the [SCIONLab](https://scionlab.org) network on an Android smartphone.

### Getting Started

Clone this repository:

```
git clone --recurse-submodules https://github.com/ekuiter/scion-android.git
cd scion-android
```

Then import this project into Android Studio or run the following in a shell to build the app:

```
# build an Android App Bundle (AAB) suitable for publishing in the Play Console
# (also possible from Android Studio with Build > Build Bundle(s) / APK(s) > Build Bundle(s))
./gradlew bundle
cd app/build/outputs/bundle/release

# optionally, inspect the created AAB with bundletool (see https://developer.android.com/studio/command-line/bundletool)
wget https://github.com/google/bundletool/releases/download/0.13.0/bundletool-all-0.13.0.jar

# build an APK set for all device configurations
java -jar bundletool-all-0.13.0.jar build-apks --bundle=app-release.aab --output=all.apks

# build an APK set for the connected device and install
java -jar bundletool-all-0.13.0.jar build-apks --bundle=app-release.aab --output=connected.apks --connected-device
java -jar bundletool-all-0.13.0.jar install-apks --apks=connected.apks

# build universal APK, which can be deployed on any device (see https://stackoverflow.com/q/53040047)
java -jar bundletool-all-0.13.0.jar build-apks --bundle=app-release.aab --output=universal.apks --mode=universal
unzip universal.apks
```

### License

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the [GNU General Public License](LICENSE.md) for more details.

You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.

This software uses the following libraries:

- [SCION](https://github.com/scionproto/scion), used under the conditions of the [Apache License, Version 2.0](https://github.com/scionproto/scion/blob/master/LICENSE)
- [OpenVPN for Android](https://github.com/schwabe/ics-openvpn/), used under the conditions of the [GNU General Public License, Version 2.0](https://github.com/schwabe/ics-openvpn/blob/master/doc/LICENSE.txt)
- [AndroidX](https://developer.android.com/jetpack/androidx), used under the conditions of the [Apache License, Version 2.0](https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/LICENSE.txt)
- [Material Components for Android](https://material.io/develop/android/), used under the conditions of the [Apache License, Version 2.0](https://github.com/material-components/material-components-android/blob/master/LICENSE)
- [android-file-chooser](https://github.com/hedzr/android-file-chooser), used under the conditions of the [Apache License, Version 2.0](https://github.com/hedzr/android-file-chooser/blob/master/LICENSE)
- [toml4j](https://github.com/mwanji/toml4j), used under the conditions of [The MIT License](https://github.com/mwanji/toml4j/blob/master/LICENSE)
- [Timber](https://github.com/JakeWharton/timber), used under the conditions of the [Apache License, Version 2.0](https://github.com/JakeWharton/timber/blob/master/LICENSE.txt)
- [jarchivelib](https://github.com/thrau/jarchivelib), used under the conditions of the [Apache License, Version 2.0](https://github.com/thrau/jarchivelib/blob/master/LICENSE)

For more information on this project, contact the [NetSys group](http://www.netsys.ovgu.de/).

© 2019 Vera Clemens, Tom Kranz<br>
© 2020 Tom Heimbrodt, Elias Kuiter