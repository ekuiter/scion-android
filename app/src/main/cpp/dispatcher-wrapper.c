/**
 * Copyright (C) 2019  Vera Clemens, Tom Kranz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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