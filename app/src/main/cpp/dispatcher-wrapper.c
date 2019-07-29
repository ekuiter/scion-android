#include <jni.h>
#include <stdlib.h>
#include <unistd.h>

int main(int, char**);

JNIEXPORT jint JNICALL Java_org_scionlab_endhost_DispatcherService_main(
        JNIEnv *env,          /* interface pointer */
        jobject obj,          /* "this" pointer */
        jstring confFileName, /* argument #1 */
        jstring workingDir)   /* argument #2 */ {
    /* Obtain a C-copy of the Java string */
    const char *str = (*env)->GetStringUTFChars(env, confFileName, NULL);
    setenv("ZLOG_CFG", str, 0);
    (*env)->ReleaseStringUTFChars(env, confFileName, str);
    str = (*env)->GetStringUTFChars(env, workingDir, NULL);
    chdir(str);
    (*env)->ReleaseStringUTFChars(env, workingDir, str);
    jint result = main(0, (char**)NULL);
    return result;
}