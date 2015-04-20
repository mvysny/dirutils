#include <jni.h>
#include "DirUtils.h"
#include <sys/stat.h>
#include <errno.h>
 
JNIEXPORT jint JNICALL Java_sk_baka_android_DirUtils_mkdirInt
  (JNIEnv *env, jobject thisObj, jstring dirname) {
  
   const char *nativeString = (*env)->GetStringUTFChars(env, dirname, 0);
   int result = mkdir(nativeString, 0x755);
   (*env)->ReleaseStringUTFChars(env, dirname, nativeString);
   return result == 0 ? 0 : errno;
}

JNIEXPORT jstring JNICALL Java_sk_baka_android_DirUtils_strerror
  (JNIEnv *env, jobject thisObj, jint errnum) {
  const char *msg = strerror(errnum);
  return (*env)->NewStringUTF(env, msg);
}

JNIEXPORT jint JNICALL Java_sk_baka_android_DirUtils_deleteInt
  (JNIEnv *env, jobject thisObj, jstring dirname) {
// @TODO mvy implement
  return 0;
}

