# Copyright (c) 2014 μg Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
appcompat_dir := ../../../frameworks/support/v7/appcompat
res_dir := res $(appcompat_dir)/res

LOCAL_MODULE_TAGS := optional

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dir))
LOCAL_SRC_FILES := $(call all-java-files-under, src)

# For some reason framework has to be added here else GeocoderParams is not found, 
# this way everything else is duplicated, but atleast compiles...
LOCAL_JAVA_LIBRARIES := framework com.android.location.provider

# Include compat v9 files if necassary
ifeq ($(shell [ $(PLATFORM_SDK_VERSION) -ge 17 ] && echo true), true)
LOCAL_JAVA_LIBRARIES += UnifiedNlpCompatV9
endif

LOCAL_STATIC_JAVA_LIBRARIES := UnifiedNlpApi android-support-v4 android-support-v7-appcompat
LOCAL_PACKAGE_NAME := UnifiedNlp
LOCAL_SDK_VERSION := current
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_AAPT_FLAGS := --auto-add-overlay \
    --extra-packages android.support.v7.appcompat \

include $(BUILD_PACKAGE)

##
# NetworkLocation
# Target using com.google.android.gms as package name

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dir))
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES := framework com.android.location.provider

# Include compat v9 files if necassary
ifeq ($(shell [ $(PLATFORM_SDK_VERSION) -ge 17 ] && echo true), true)
LOCAL_JAVA_LIBRARIES += UnifiedNlpCompatV9
endif

LOCAL_STATIC_JAVA_LIBRARIES := UnifiedNlpApi android-support-v4 android-support-v7-appcompat
LOCAL_PACKAGE_NAME := NetworkLocation
LOCAL_SDK_VERSION := current
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_AAPT_FLAGS := --auto-add-overlay \
    --rename-manifest-package com.google.android.gms \
    --extra-packages android.support.v7.appcompat \

include $(BUILD_PACKAGE)

##
# NetworkLocation
# Target using com.google.android.location as package name

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dir))
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES := framework com.android.location.provider

# Build against compat v9 files if necessary
ifeq ($(shell [ $(PLATFORM_SDK_VERSION) -ge 17 ] && echo true), true)
LOCAL_JAVA_LIBRARIES += UnifiedNlpCompatV9
endif

LOCAL_STATIC_JAVA_LIBRARIES := UnifiedNlpApi android-support-v4 android-support-v7-appcompat
LOCAL_PACKAGE_NAME := LegacyNetworkLocation
LOCAL_SDK_VERSION := current
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags


LOCAL_AAPT_FLAGS := --auto-add-overlay \
    --rename-manifest-package com.google.android.location \
    --extra-packages android.support.v7.appcompat \

include $(BUILD_PACKAGE)

include $(LOCAL_PATH)/compat/v9/Android.mk

