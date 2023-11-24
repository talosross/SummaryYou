#include <jni.h>

JNIEXPORT jstring JNICALL
Java_com_talosross_summaryyou_APIKeyLibrary_00024Companion_getAPIKey(JNIEnv *env, jobject thiz) {
    return (*env)->  NewStringUTF(env, "");
}
