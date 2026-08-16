#include <jni.h>
#include <sys/types.h>
#include <stdbool.h>
#include <unistd.h>
#include <pthread.h>
#include <stdio.h>
#include <fcntl.h>
#include <string.h>
#include <errno.h>
#include <stdlib.h>
#include <time.h>

#include "stdio_is.h"

//
// Created by maks on 17.02.21.
//

#define LOG_FLUSH_INTERVAL_MS 500
#define LOG_FLUSH_BYTES (64 * 1024)
#define FILTER_TEXT "Session ID is"

static volatile jobject exitTrap_ctx;
static volatile jclass exitTrap_exitClass;
static volatile jmethodID exitTrap_staticMethod;
static JavaVM *exitTrap_jvm;

static int pfd[2] = {-1, -1};
static pthread_t logger;
static jmethodID logger_onEventLogged;
static volatile jobject logListener = NULL;
static int latestlog_fd = -1;
static size_t pending_flush_bytes = 0;
static struct timespec last_flush_time;
static pthread_mutex_t log_mutex = PTHREAD_MUTEX_INITIALIZER;

static bool contains_bytes(const char *buf, size_t len, const char *needle) {
    size_t needle_len = strlen(needle);
    if (needle_len == 0 || len < needle_len) return false;

    for (size_t i = 0; i <= len - needle_len; ++i) {
        if (memcmp(buf + i, needle, needle_len) == 0) return true;
    }
    return false;
}

static bool write_all(int fd, const char *buf, size_t len) {
    size_t written = 0;
    while (written < len) {
        ssize_t result = write(fd, buf + written, len - written);
        if (result > 0) {
            written += (size_t) result;
        } else if (result < 0 && errno == EINTR) {
            continue;
        } else {
            return false;
        }
    }
    return true;
}

static long elapsed_ms(const struct timespec *start, const struct timespec *end) {
    time_t seconds = end->tv_sec - start->tv_sec;
    long nanoseconds = end->tv_nsec - start->tv_nsec;
    return (long) (seconds * 1000L + nanoseconds / 1000000L);
}

static void flush_log_locked(bool force) {
    if (latestlog_fd == -1 || pending_flush_bytes == 0) return;

    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    if (force || elapsed_ms(&last_flush_time, &now) >= LOG_FLUSH_INTERVAL_MS ||
            pending_flush_bytes >= LOG_FLUSH_BYTES) {
        // Do not force storage synchronization for every small log block.
        // A final flush is still performed when the logger thread exits or a
        // new log file is opened.
        fdatasync(latestlog_fd);
        pending_flush_bytes = 0;
        last_flush_time = now;
    }
}

static bool recordBuffer(const char *buf, size_t len) {
    // Keep the existing privacy filter, but inspect only the bytes read from
    // the pipe. The old strstr() call could read past a non-NUL-terminated
    // pipe buffer.
    if (contains_bytes(buf, len, FILTER_TEXT)) return false;

    pthread_mutex_lock(&log_mutex);
    if (latestlog_fd != -1) {
        if (write_all(latestlog_fd, buf, len)) {
            pending_flush_bytes += len;
            flush_log_locked(false);
        }
    }
    pthread_mutex_unlock(&log_mutex);
    return true;
}

static void *logger_thread(void* param) {
    JNIEnv *env;
    jstring writeString;
    JavaVM* dvm = (JavaVM*) param;
    (*dvm)->AttachCurrentThread(dvm, &env, NULL);
    ssize_t rsize;
    char buf[2050];
    while ((rsize = read(pfd[0], buf, sizeof(buf) - 1)) > 0) {
        bool shouldRecordString = recordBuffer(buf, (size_t) rsize);
        if (buf[rsize - 1] == '\n') {
            rsize = rsize - 1; // truncate
        }
        buf[rsize] = 0x00;
        if (shouldRecordString && logListener != NULL) {
            writeString = (*env)->NewStringUTF(env, buf); // send to app without newline
            (*env)->CallVoidMethod(env, logListener, logger_onEventLogged, writeString);
            (*env)->DeleteLocalRef(env, writeString);
        }
    }

    pthread_mutex_lock(&log_mutex);
    flush_log_locked(true);
    pthread_mutex_unlock(&log_mutex);

    (*dvm)->DetachCurrentThread(dvm);
    return NULL;
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_Logger_begin(JNIEnv *env, __attribute((unused)) jclass clazz, jstring logPath) {
    pthread_mutex_lock(&log_mutex);
    if (latestlog_fd != -1) {
        flush_log_locked(true);
        int localfd = latestlog_fd;
        latestlog_fd = -1;
        close(localfd);
    }
    pending_flush_bytes = 0;
    pthread_mutex_unlock(&log_mutex);

    if (logger_onEventLogged == NULL) {
        jclass eventLogListener = (*env)->FindClass(env, "net/kdt/pojavlaunch/Logger$eventLogListener");
        logger_onEventLogged = (*env)->GetMethodID(env, eventLogListener, "onEventLogged", "(Ljava/lang/String;)V");
    }
    jclass ioeClass = (*env)->FindClass(env, "java/io/IOException");

    setvbuf(stdout, 0, _IOLBF, 0); // make stdout line-buffered
    setvbuf(stderr, 0, _IONBF, 0); // make stderr unbuffered

    /* create the pipe and redirect stdout and stderr */
    if (pipe(pfd) != 0) {
        (*env)->ThrowNew(env, ioeClass, strerror(errno));
        return;
    }
    dup2(pfd[1], 1);
    dup2(pfd[1], 2);

    /* open latestlog.txt for writing */
    const char* logFilePath = (*env)->GetStringUTFChars(env, logPath, NULL);
    latestlog_fd = open(logFilePath, O_WRONLY | O_TRUNC | O_CLOEXEC);
    if (latestlog_fd == -1) {
        (*env)->ReleaseStringUTFChars(env, logPath, logFilePath);
        close(pfd[0]);
        close(pfd[1]);
        pfd[0] = pfd[1] = -1;
        (*env)->ThrowNew(env, ioeClass, strerror(errno));
        return;
    }
    (*env)->ReleaseStringUTFChars(env, logPath, logFilePath);

    clock_gettime(CLOCK_MONOTONIC, &last_flush_time);

    JavaVM* vm = NULL;
    (*env)->GetJavaVM(env, &vm);

    /* spawn the logging thread */
    int result = pthread_create(&logger, 0, logger_thread, vm);
    if (result != 0) {
        close(latestlog_fd);
        latestlog_fd = -1;
        close(pfd[0]);
        close(pfd[1]);
        pfd[0] = pfd[1] = -1;
        (*env)->ThrowNew(env, ioeClass, strerror(result));
        return;
    }
    pthread_detach(logger);
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_Logger_appendToLog(JNIEnv *env, __attribute((unused)) jclass clazz, jstring text) {
    jsize appendStringLength = (*env)->GetStringUTFLength(env, text);
    char newChars[appendStringLength + 2];
    (*env)->GetStringUTFRegion(env, text, 0, (*env)->GetStringLength(env, text), newChars);
    newChars[appendStringLength] = '\n';
    newChars[appendStringLength + 1] = 0;
    if (recordBuffer(newChars, (size_t) appendStringLength + 1) && logListener != NULL) {
        (*env)->CallVoidMethod(env, logListener, logger_onEventLogged, text);
    }
}

JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_Logger_setLogListener(JNIEnv *env, __attribute((unused)) jclass clazz, jobject log_listener) {
    jobject logListenerLocal = logListener;
    if (log_listener == NULL) {
        logListener = NULL;
    } else {
        logListener = (*env)->NewGlobalRef(env, log_listener);
    }
    if (logListenerLocal != NULL) (*env)->DeleteGlobalRef(env, logListenerLocal);
}
