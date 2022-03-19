<!--
SPDX-FileCopyrightText: 2014, microg Project Team
SPDX-License-Identifier: CC-BY-SA-4.0
-->

<img src="http://i.imgur.com/hXY4lcC.png" height="42px" alt="microG" /> Unified Network Location Provider
==========
[![Build Status](https://travis-ci.com/microg/UnifiedNlp.svg?branch=master)](https://travis-ci.com/microg/UnifiedNlp)

The next generation Network Location Provider, based on plugins. Usually abbreviated as UnifiedNlp.

[![Available on F-Droid](https://f-droid.org/wiki/images/c/c4/F-Droid-button_available-on.png)](https://f-droid.org/repository/browse/?fdid=com.google.android.gms)

Installation
------------
Release builds may be found on the [release page](https://github.com/microg/android_packages_apps_UnifiedNlp/releases).

Unified Network Location Provider is provided in 3 variants:
* NetworkLocation.apk – for the usual configuration of Android 4.4+ without GApps
* LegacyNetworkLocation.apk – for the usual configuration of Android 2.3 - 4.3.1 without GApps
* UnifiedNlp.apk – for Android WITH Gapps

Keep in mind that:
* All three variants are available on F-Droid as well, but they use different apk names (have a look at app description on F-Droid to check which version to use).
* There is another repo containing the [deprecated version](https://github.com/microg/NetworkLocation) of NetworkLocation.apk without the plug-in system.
* [microG GmsCore](https://github.com/microg/android_packages_apps_GmsCore/wiki) project already includes the Unified Network Location Provider.


### Android 4.4 - 7.1.1 (KitKat / Lollipop / Marshmallow / Nougat)
Most modern ROMs come with support for non-Google geolocation providers. On these systems installation is easy:

1. Make sure that no Google geolocation tool is installed (it is usually listed as Google Play Services in Apps)
2. Download and install `NetworkLocation.apk` as a usual app (you may need to enable "Unknown sources" in Settings->Security)
3. Reboot and continue at [Usage](#usage)

Some ROMs, especially those not based on AOSP might have problems using this method. However, if your system has root, you can try installing the hard way:

1. Download `NetworkLocation.apk`
2. Mount `/system` read-write (from your PC, call `adb root && adb remount`)
3. Copy `NetworkLocation.apk` to `/system/priv-app` (from your PC, call `adb push NetworkLocation.apk /system/priv-app/NetworkLocation.apk`)
4. Reboot (from you PC, call `adb reboot`) and continue at [Usage](#usage)

**Note:** On Android 7 (or later) an [additional patch](https://github.com/microg/android_packages_apps_UnifiedNlp/blob/master/patches/android_frameworks_base-N.patch) is needed to make it working, or alternatively, you can install it in `/system/priv-app` as explained above.

### Android 2.3 - 4.3.1 (Gingerbread / Honeycomb / Ice Cream Sandwich / Jelly Bean)
Older Android versions are no longer officially supported. However I still provide a legacy build, that should be compatible with those systems.
It is required to have a rooted system to install on Jelly Bean and older.

1. Download `LegacyNetworkLocation.apk`
2. Mount `/system` read-write (from your PC, call `adb root && adb remount`)
3. Copy `LegacyNetworkLocation.apk` to `/system/app` (from your PC, call `adb push LegacyNetworkLocation.apk /system/app/LegacyNetworkLocation.apk`)
4. Reboot (from you PC, call `adb reboot`) and continue at [Usage](#usage)


Usage
-----
UnifiedNlp alone does not provide any features, but acts as a middleware for multiple backends. Most of them can be downloaded and updated using [F-Droid](https://f-droid.org).
Here is a list of backends known to me.

List of backends for geolocation:
* [AppleWifiNlpBackend](https://github.com/microg/AppleWifiNlpBackend) - Uses Apple's service to resolve Wi-Fi locations. It has excellent coverage but the database is proprietary.
* [OpenWlanMapNlpBackend](https://github.com/microg/OpenWlanMapNlpBackend) - Uses OpenWlanMap.org to resolve user location but the NLP backend did not reach release-quality, yet. Users interested in a freely licensed and downloadable database for offline use should stick with openBmap for now - *Last updated in 2015*
* [OpenBmapNlpBackend](https://github.com/wish7code/org.openbmap.unifiedNlpProvider) - Uses [openBmap](https://radiocells.org/) to resolve user location. Community-created, freely licensed database that can optionally be downloaded for offline operation. The coverage [varies from country to country](https://radiocells.org/stats/countries) (it's best in central Europe).
* [MozillaNlpBackend](https://github.com/microg/IchnaeaNlpBackend) - Uses the Mozilla Location Service to resolve user location. The coverage is OK. Only the cell tower database is free.
* [LocalWifiNlpBackend](https://github.com/n76/wifi_backend) - Local location provider for Wi-Fi APs using on-phone generated database.
* [LocalGSMLocationProvider](https://github.com/rtreffer/LocalGSMLocationProvider) - Local opencellid based location provider backend. Has been surpassed by LocalGSMBackend which also has an OpenCellID option - *Last update in 2014*
* [LocalGSMBackend](https://gitlab.com/deveee/Local-GSM-Backend) - Local location provider for GSM cells. It works offline by downloading freely licensed database files from Mozilla, OpenCellID, or lacells.db.

List of backends for (reverse) geocoding:
* [NominatimGeocoderBackend](https://github.com/microg/NominatimGeocoderService) - Address lookup backend.

(...) Create issue or pull request to extend either list :)

After installing a backend, you can use UnifiedNlp by activating network-based geolocation in Settings->Location. 
Since KitKat, you need to select any mode but "device only", on older Android version this setting is called "Wi-Fi & mobile network location" 
(ignore any misleading texts saying this is for Google's location service, you don't have Google's service installed but UnifiedNlp :smile:) 

Backend development
-------------------
The API is available [here](https://github.com/microg/android_external_UnifiedNlpApi). Documentation may be found in the README provided with the API.

You might also take a look into existing backends, to see how they work out.

Building
--------
UnifiedNlp can be easily built using Gradle.

    git clone https://github.com/microg/UnifiedNlp
    cd UnifiedNlp
    ./gradlew build


Attribution
-----------
Some components: Copyright (C) 2013 The Android Open Source Project

`compat`-folder is extracted from different AOSP versions for cross-version compatibility

License
-------
    Copyright (C) 2013-2022 microG Project Team

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
