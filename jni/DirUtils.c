#include <jni.h>
#include <sys/stat.h>
#include <errno.h>
#include <stdio.h>
#include "DirUtils.h"

JNIEXPORT jint JNICALL Java_sk_baka_android_DirUtils_mkdirInt
  (JNIEnv *env, jobject thisObj, jstring dirname) {
  
   const char *nativeString = (*env)->GetStringUTFChars(env, dirname, 0);
   int result = mkdir(nativeString, 0775);  // 0x775 NEFUNGUJE, JA SOM DEBIL: http://stackoverflow.com/questions/10147990/how-to-create-directory-with-right-permissons-using-c-on-posix
   (*env)->ReleaseStringUTFChars(env, dirname, nativeString);
   return result == 0 ? 0 : errno;
}

JNIEXPORT jstring JNICALL Java_sk_baka_android_DirUtils_strerror
  (JNIEnv *env, jobject thisObj, jint errnum) {
  const char *msg = strerror(errnum);
  return (*env)->NewStringUTF(env, msg);
}

JNIEXPORT jint JNICALL Java_sk_baka_android_DirUtils_deleteInt
                         (JNIEnv *env, jobject thisObj, jstring path) {
   const char *nativeString = (*env)->GetStringUTFChars(env, path, 0);
    int result = remove(nativeString);
   (*env)->ReleaseStringUTFChars(env, path, nativeString);
   return result == 0 ? 0 : errno;
}
