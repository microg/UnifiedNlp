<img src="http://i.imgur.com/hXY4lcC.png" height="42px" alt="microG" /> Unified Network Location Provider
==========
[![Build Status](https://travis-ci.org/microg/android_packages_apps_UnifiedNlp.svg?branch=master)](https://travis-ci.org/microg/android_packages_apps_UnifiedNlp)

The next generation Network Location Provider, based on plugins. Usually abbreviated as UnifiedNlp.

[![Available on F-Droid](https://f-droid.org/wiki/images/c/c4/F-Droid-button_available-on.png)](https://f-droid.org/repository/browse/?fdid=com.google.android.gms)

Installation
------------
Release builds may be found on the [release page](https://github.com/microg/android_packages_apps_UnifiedNlp/releases).

### Android 4.4–6.0 (KitKat / Lollipop / Marshmallow)
Most modern ROMs come with support for non-Google geolocation providers. On these systems installation is easy:

1. Make sure that no Google geolocation tool is installed (it is usually listed as Google Play Services in Apps)
2. Download and install `NetworkLocation.apk` as a usual app (you may need to enable "Unknown sources" in Settings->Security)
3. Reboot and continue at [Usage](#usage)

Some ROMs, especially those not based on AOSP might have problems using this method. However, if your system has root, you can try installing the hard way:

1. Download `NetworkLocation.apk`
2. Mount `/system` read-write (from your PC, call `adb root && adb remount`)
3. Copy `NetworkLocation.apk` to `/system/priv-app` (from your PC, call `adb push NetworkLocation.apk /system/priv-app/NetworkLocation.apk`)
4. Reboot (from you PC, call `adb reboot`) and continue at [Usage](#usage)

### Android 2.3–4.3 (Gingerbread / Honeycomb / Ice Cream Sandwich / Jelly Bean)
Older Android versions are no longer officially supported. However I still provide a legacy build, that should be compatible with those systems.
It is required to have a rooted system to install on Jelly Bean and older.

1. Download `LegacyNetworkLocation.apk`
2. Mount `/system` read-write (from your PC, call `adb root && adb remount`)
3. Copy `LegacyNetworkLocation.apk` to `/system/app` (from your PC, call `adb push LegacyNetworkLocation.apk /system/priv-app/NetworkLocation.apk`)
4. Reboot (from you PC, call `adb reboot`) and continue at [Usage](#usage)


Usage
-----
UnifiedNlp alone does not provide any features, but acts as a middleware for multiple backends. Most of them can be downloaded and updated using [F-Droid](https://f-droid.org)
Here is a list of actively developed backends for geolocation and (reverse) geocoding known to me:

- [AppleWifiNlpBackend](https://github.com/microg/AppleWifiNlpBackend) uses Apple's service to resolve WiFi locations. It has excellent coverage but the database is proprietary.
- [OpenBmapNlpBackend](https://github.com/wish7code/org.openbmap.unifiedNlpProvider) uses [openBmap](https://radiocells.org/) to resolve user location – community-created, freely licensed database that can optionally be downloaded for offline operation. The coverage [varies from country to country](https://radiocells.org/stats/countries) (it’s best in central europe).
- [MozillaNlpBackend](https://github.com/microg/IchnaeaNlpBackend) uses the Mozilla Location Service to resolve user location. The coverage is OK. Only the cell tower database is free.
- [PersonalWifiBackend](https://github.com/n76/wifi_backend) is a local location provider for WiFi APs using on-phone generated database.
- [LocalGSMBackend](https://github.com/n76/Local-GSM-Backend) is a local location provider for GSM cells. It works offline by downloading freely licensed database files from Mozilla, OpenCellID, or lacells.db.
- [NominatimGeocoderBackend](https://github.com/microg/NominatimGeocoderService) is an address lookup backend.
- (...) Create issue or pull request to extend either list :)

After installing a backend, you can use UnifiedNlp by activating network-based geolocation in Settings->Location.
Since KitKat, you need to select any mode but "device only", on older Android version this setting is called "Wi-Fi & mobile network location"
(ignore any misleading texts saying this is for Google's location service, you don't have Google's service installed but UnifiedNlp :smile:)

There are also a few backends that are not actively maintained:
- [LocalGSMLocationProvider](https://github.com/rtreffer/LocalGSMLocationProvider) has been surpassed by LocalGSMBackend which also has an OpenCellID option.
- [OpenWlanMapNlpBackend](https://github.com/microg/OpenWlanMapNlpBackend) uses OpenWlanMap.org to resolve user location but the NLP backend did not reach release-quality, yet. Users interested in a freely licensed and downloadable database for offline use should stick with openBmap for now.


Backend development
-------------------
The API is available [here](https://github.com/microg/android_external_UnifiedNlpApi). Documentation may be found in the README provided with the API.

You might also take a look into existing backends, to see how they work out.

Building
--------
UnifiedNlp can be built using Gradle. Current builds are done using Gradle 2.2, but other versions might work as well.

To build with Gradle, first download git submodules:

	git submodule update --init

AOSP Build system integration
-----------------------------
UnifiedNlp can be build as part of Android when building an Android ROM from source.
Add this repo to your (local) manifest.xml, as well as the UnifiedNlpApi and extend the `PRODUCT_PACKAGES` variable with `NetworkLocation` for KitKat and `LegacyNetworkLocation` for Jelly Bean.

You can also directly invoke the compilation of UnifiedNlp by calling `make UnifiedNlp` or `make NetworkLocation` (respectively `make LegacyNetworkLocation`) from the build system root.
Note that you need to add [UnifiedNlpApi](https://github.com/microg/android_external_UnifiedNlpApi) and [MicroGUiTools](https://github.com/microg/android_external_MicroGUiTools) to the build system for UnifiedNlp to compile:
```
  <project name="microg/android_packages_apps_UnifiedNlp" path="packages/apps/UnifiedNlp" remote="github" revision="master"/>
  <project name="microg/android_external_UnifiedNlpApi" path="external/UnifiedNlpApi" remote="github" revision="master"/>
  <project name="microg/android_external_MicroGUiTools" path="external/MicroGUiTools" remote="github" revision="master"/>
```

Attribution
-----------
Some components: Copyright © 2013 The Android Open Source Project

`compat`-folder is extracted from different AOSP versions for cross-version compatibility

License
-------
    Copyright © 2013–2016 microG Project Team

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
