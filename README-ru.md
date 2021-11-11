<!--
SPDX-FileCopyrightText: 2014, microg Project Team
SPDX-License-Identifier: CC-BY-SA-4.0
-->

<img src="http://i.imgur.com/hXY4lcC.png" height="42px" alt="microG" /> Объединённый провайдер сетей местоположения
==========

[English](https://github.com/microG/UnifiedNlp/tree/master/README.md) | [Русский](https://github.com/microG/UnifiedNlp/tree/master/README-ru.md)

[![Build Status](https://travis-ci.com/microg/UnifiedNlp.svg?branch=master)](https://travis-ci.com/microg/UnifiedNlp)

Провайдер сетей местоположения нового поколения, основанный на модулях. Также известен как UnifiedNlp.

[![Available on F-Droid](https://f-droid.org/wiki/images/c/c4/F-Droid-button_available-on.png)](https://f-droid.org/repository/browse/?fdid=com.google.android.gms)

Установка
---------
Финальные сборки могут быть найдены на [странице релизов](https://github.com/microg/android_packages_apps_UnifiedNlp/releases).

Объединённый провайдер сетей местоположения предоставляется в 3 вариантах:
* NetworkLocation.apk – обычная конфигурация для Android 4.4+ без GApps
* LegacyNetworkLocation.apk – обычная конфигурация для Android 2.3 - 4.3.1 без GApps
* UnifiedNlp.apk – для систем с Сервисами Google Play

Имейте в виду:
* Все три варианта также доступны на F-Droid, но apk названы иначе (сверьтесь с описанием приложения в F-Droid, чтобы удостовериться, что вы находитесь на странице нужного вам варианта).
* Есть ещё один репозиторий, в котором находится [устаревшая версия](https://github.com/microg/NetworkLocation) NetworkLocation.apk без системы модулей.
* [microG GmsCore](https://github.com/microg/android_packages_apps_GmsCore/wiki) уже содержит встроенный Объединённый провайдер сетей местоположения.


### Android 4.4 - 7.1.1 (KitKat / Lollipop / Marshmallow / Nougat)
Большинство современных прошивок сразу поддерживает установку провайдеров местоположения не от Google. Установка на таких системах проста:

1. Убедитесь, что инструменты определения местоположения от Google не установлены (проверьте наличие Сервисов Google Play в списке приложений)
2. Скачайте и установите `NetworkLocation.apk` как обычное приложение (возможно, вам понадобится разрешить установку из неизвестных источников: Настройки->Безопасность)
3. Перезагрузите устройство и вернитесь к разделу [Использование](#Использование)

На некоторых прошивках, особенно не основанных на AOSP, при использовании вышеописанного метода могут возникнуть проблемы. Впрочем, если на вашей системе установлен root, можно поступить немного сложнее:

1. Скачайте `NetworkLocation.apk`
2. Смонтируйте раздел `/system` как read-write (на компьютере нужно выполнить команду `adb root && adb remount`)
3. Скопируйте `NetworkLocation.apk` в `/system/priv-app` (команда `adb push NetworkLocation.apk /system/priv-app/NetworkLocation.apk`)
4. Перезагрузите устройство (можно выполнить `adb reboot`) и вернитесь к разделу [Использование](#Использование)

**Важно:** На Android 7 (и выше) необходимо доустановить [дополнительный патч](https://github.com/microg/android_packages_apps_UnifiedNlp/blob/master/patches/android_frameworks_base-N.patch), либо установку нужно выполнять сразу в `/system/priv-app` согласно инструкции выше.

### Android 2.3 - 4.3.1 (Gingerbread / Honeycomb / Ice Cream Sandwich / Jelly Bean)
Старые версии Android больше официально не поддерживаются. Тем не менее, устаревшая сборка всё ещё прилагается, она должна быть совместима с такими системами.
Настоятельно рекомендуется наличие root для установки на Jelly Bean и старее.

1. Скачайте `LegacyNetworkLocation.apk`
2. Смонтируйте раздел `/system` как read-write (на компьютере нужно выполнить команду `adb root && adb remount`)
3. Скопируйте `LegacyNetworkLocation.apk` в `/system/app` (команда `adb push LegacyNetworkLocation.apk /system/app/LegacyNetworkLocation.apk`)
4. Перезагрузите устройство (можно выполнить `adb reboot`) и вернитесь к разделу [Использование](#Использование)


Использование
-------------
Сам по себе UnifiedNlp не предоставляет никаких функций, а лишь служит посредником для ряда backend-модулей. Большинство из них можно загрузить и обновлять через [F-Droid](https://f-droid.org).
Вот список наиболее известных backend-модулей.

Backend-модули для определения местоположения:
* [AppleWifiNlpBackend](https://github.com/microg/AppleWifiNlpBackend) - Использует сервисы Apple для определения местоположения по точкам доступа Wi-Fi. Имеет отличное покрытие, но база данных проприетарная.
* [OpenWlanMapNlpBackend](https://github.com/microg/OpenWlanMapNlpBackend) - Использует OpenWlanMap.org для определения местоположения, но сам по себе модуль до сих пор нестабилен. Заинтересованным в свободно лицензируемых и скачиваемых базах данных для использования без интернета стоит пока что обратить внимание на openBmap - *Последнее обновление было в 2015*
* [OpenBmapNlpBackend](https://github.com/wish7code/org.openbmap.unifiedNlpProvider) - Использует [openBmap](https://radiocells.org/) для определения местоположения. Народная и свободно лицензируемая база данных также может быть скачана для использования без интернета. Качество покрытия [варьируется в каждой стране](https://radiocells.org/stats/countries) (лучше всего дела с ним обстоят в Центральной Европе). 
* [MozillaNlpBackend](https://github.com/microg/IchnaeaNlpBackend) - Использует Сервисы местоположения Mozilla. Покрытие удовлетворительное. Только база данных сотовых вышек распространяется свободно.
* [LocalWifiNlpBackend](https://github.com/n76/wifi_backend) - Локальный провайдер местоположения по точкам доступа Wi-Fi с базой данных, генерирующейся на телефоне.
* [LocalGSMLocationProvider](https://github.com/rtreffer/LocalGSMLocationProvider) - Локальный провайдер местоположения по сотовым вышкам. Был превзойдён модулем LocalGSMBackend, который также поддерживает OpenCellID - *Последнее обновление было в 2014*
* [LocalGSMBackend](https://github.com/n76/Local-GSM-Backend) - Локальный провайдер местоположения по сотовым вышкам. Может работать без интернета после загрузки свободна лицензируемых баз данных Mozilla, OpenCellID или lacells.db.

Список backend-модулей для (реверсивного) определения адресов:
* [NominatimGeocoderBackend](https://github.com/microg/NominatimGeocoderService) - Backend-модуль определения адресов.

(...) Откройте issue или pull request, если хотите дополнить какой-либо из списков :)

После установки модуля, включите UnifiedNlp пройдя в настройки более точного определения местоположения (Настройки->Местоположение).
Начиная с KitKat, нужно выбрать режим кроме "Только GPS", на более старых версиях Android этот пункт может называться "Использовать Wi-Fi и сотовые вышки"
(не обращайте внимания на фразы о Сервисах местоположения Google — у вас ведь установлен UnifiedNlp, а не они :smile:) 

Разработка backend-модулей
--------------------------
API доступен [здесь](https://github.com/microg/android_external_UnifiedNlpApi). Документация находится в README репозитория.

Вы можете посмотреть и на то, как работают уже разработанные backend-модули.

Сборка
------
UnifiedNlp можно легко собрать с помощью Gradle.

    git clone https://github.com/microg/UnifiedNlp
    cd UnifiedNlp
    ./gradlew build


Источники
---------
Некоторые компоненты: Copyright (C) 2013 The Android Open Source Project

Папка `compat` была собрана из различных версий AOSP для совместимости с разными версиями

Лицензия
--------
    Copyright (C) 2013-2019 microG Project Team

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
