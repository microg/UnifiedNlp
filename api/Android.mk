LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := UnifiedNlpApi
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += src/org/microg/nlp/api/LocationBackend.aidl \
                   src/org/microg/nlp/api/GeocoderBackend.aidl \
                   src/org/microg/nlp/api/LocationCallback.aidl

include $(BUILD_STATIC_JAVA_LIBRARY)
