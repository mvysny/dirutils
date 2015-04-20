LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := dirutils
LOCAL_SRC_FILES := DirUtils.c

include $(BUILD_SHARED_LIBRARY)

