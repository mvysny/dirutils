#include <jni.h>
#include <sys/stat.h>
#include <errno.h>
#include <stdio.h>
#include "DirUtils.h"

JNIEXPORT jint JNICALL Java_sk_baka_android_spi_AndroidFS_mkdirInt
  (JNIEnv *env, jobject thisObj, jstring dirname) {
  
   const char *nativeString = (*env)->GetStringUTFChars(env, dirname, 0);
   int result = mkdir(nativeString, 0775);
   (*env)->ReleaseStringUTFChars(env, dirname, nativeString);
   return result == 0 ? 0 : errno;
}

JNIEXPORT jint JNICALL Java_sk_baka_android_spi_AndroidFS_mkdirInt2
  (JNIEnv *env, jobject thisObj, jstring dirname, jint permissions) {

   const char *nativeString = (*env)->GetStringUTFChars(env, dirname, 0);
   int result = mkdir(nativeString, permissions);
   (*env)->ReleaseStringUTFChars(env, dirname, nativeString);
   return result == 0 ? 0 : errno;
}

JNIEXPORT jstring JNICALL Java_sk_baka_android_spi_AndroidFS_strerror
  (JNIEnv *env, jobject thisObj, jint errnum) {
  // cast to char* removes the following warning:  warning: initialization makes pointer from integer without a cast [enabled by default]
  const char *msg = (char*) strerror(errnum);
  return (*env)->NewStringUTF(env, msg);
}

JNIEXPORT jint JNICALL Java_sk_baka_android_spi_AndroidFS_deleteInt
                         (JNIEnv *env, jobject thisObj, jstring path) {
   const char *nativeString = (*env)->GetStringUTFChars(env, path, 0);
    int result = remove(nativeString);
   (*env)->ReleaseStringUTFChars(env, path, nativeString);
   return result == 0 ? 0 : errno;
}

JNIEXPORT jint JNICALL Java_sk_baka_android_spi_AndroidFS_rename
  (JNIEnv *env, jobject thisObj, jstring oldpath, jstring newpath) {
   const char *nativeOldPath = (*env)->GetStringUTFChars(env, oldpath, 0);
   const char *nativeNewPath = (*env)->GetStringUTFChars(env, newpath, 0);
    int result = rename(nativeOldPath, nativeNewPath);
   (*env)->ReleaseStringUTFChars(env, oldpath, nativeOldPath);
   (*env)->ReleaseStringUTFChars(env, newpath, nativeNewPath);
   return result == 0 ? 0 : errno;
}

JNIEXPORT jint JNICALL Java_sk_baka_android_spi_AndroidFS_getmod
  (JNIEnv *env, jobject thisObj, jstring path) {
   const char *nativePath = (*env)->GetStringUTFChars(env, path, 0);
   struct stat fileStat;
   int result = stat(nativePath,&fileStat);
   (*env)->ReleaseStringUTFChars(env, path, nativePath);
   return result == 0 ? (fileStat.st_mode & (~0x80000000)) : (errno | 0x80000000);
}
