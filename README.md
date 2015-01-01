UnifiedNlp
==========
The next generation NetworkLocationProvider, based on plugins

Installation
------------
Release builds may be found on the [release page](https://github.com/microg/android_packages_apps_UnifiedNlp/releases).
Installation requires a rooted system.

### Android 4.4 - 5.0 (KitKat / Lollipop)
Download `NetworkLocation.apk`, copy it to `/system/priv-app/NetworkLocation.apk` and reboot. The following shell commands will do the job:

	adb root && adb remount
	adb push path/to/NetworkLocation.apk /system/priv-app/NetworkLocation.apk
	adb reboot

### Android 4.2 - 4.3 (Jelly Bean)
Download `LegacyNetworkLocation.apk`, copy it to `/system/app/NetworkLocation.apk` and reboot. The following shell commands will do the job:

	adb root && adb remount
	adb push path/to/LegacyNetworkLocation.apk /system/app/NetworkLocation.apk
	adb reboot

### Android 2.3 - 4.1 (Gingerbread / Honeycomb / Ice Cream Sandwich / Jelly Bean)
These releases are no longer actively supported and might be unstable. Additionally, most backends require a newer Android version.

However the LegacyNetworkLocation (instructions for Android 4.2) usually works quiet well.

Usage
-----
UnifiedNlp as it does not provide any features, but acts as a middleware for multiple backends.

Here is an open list of backends for geolocation known to me:

-	[AppleWifiNlpBackend](https://github.com/microg/AppleWifiNlpBackend) - backend that uses Apple's service to resolve wifi locations
-	[OpenWlanMapNlpBackend](https://github.com/microg/OpenWlanMapNlpBackend) - backend that uses OpenWlanMap.org to resolve user location.
-	[LocalGSMLocationProvider](https://github.com/rtreffer/LocalGSMLocationProvider) - Local opencellid based location provider backend
-	[LocalGSMBackend](https://github.com/n76/Local-GSM-Backend) - Local location provider for gsm cells with separate database file (lacells.db)
-	[PersonalWifiBackend](https://github.com/n76/wifi_backend) - Local location provider for WiFi APs using on-phone generated database.
-	(...) Create issue or pull request to extend this list :)

The following is an open list of backends for (reverse) geocoding:

-   [NominatimGeocoderBackend](https://github.com/microg/NominatimGeocoderService)

After installing a backend, you can use UnifiedNlp by activating network-based geolocation in Settings->Location. 
Since KitKat, you need to select any mode but "device only", on older Android version this setting is called "Wi-Fi & mobile network location" 
(ignore any misleading texts saying this is for Google's location service, you don't have Google's service installed but UnifiedNlp :smile:) 

As part of a custom ROM
-----------------------
UnifiedNlp can be build as part of Android when building an Android ROM from source.

Add the repo to your (local) manifest.xml and extend the `PRODUCT_PACKAGES` variable with `NetworkLocation` for KitKat and `LegacyNetworkLocation` for Jelly Bean.

Backend-development
-------------------
Take a look at the API documentation in `/api/README.md`. You might also be interested in the sample backends in `/sample/`

Building
--------
To be build with Android Build System using `make UnifiedNlp`, `make LegacyNetworkLocation` or `make NetworkLocation`

Attribution
-----------
Some components: Copyright (C) 2013 The Android Open Source Project

License
-------
    Copyright 2014 μg Project Team

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
