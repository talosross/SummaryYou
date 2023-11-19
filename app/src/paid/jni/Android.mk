LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE    := api-keys
LOCAL_SRC_FILES := api-keys.c
include $(BUILD_SHARED_LIBRARY)