#include <jni.h>
#include <string>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <termios.h>
#include <android/log.h>
#include <stdlib.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <errno.h>
#include <vector>
#include <mutex>

// Assuming libvterm is available in include path
// #include "vterm.h"
// To allow compilation without libvterm for now, I will define a dummy VTerm structure if not present?
// No, I'll assume the user has it.
#include "vterm.h"

#define TAG "TerminalJNI"

struct TerminalHandle {
    int pty_fd;
    pid_t pid;
    VTerm *vt;
    VTermScreen *vts;
    std::mutex lock;
};

extern "C" {

JNIEXPORT jlong JNICALL
Java_io_cortex_terminal_engine_TerminalSession_createSession(JNIEnv *env, jobject thiz, jstring cmd, jint rows, jint cols) {
    int master_fd, slave_fd;
    char devname[64];

    const char *cmd_c = env->GetStringUTFChars(cmd, 0);
    std::string cmd_str(cmd_c);
    env->ReleaseStringUTFChars(cmd, cmd_c);

    master_fd = open("/dev/ptmx", O_RDWR | O_NOCTTY);
    if (master_fd < 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to open /dev/ptmx");
        return 0;
    }

    if (grantpt(master_fd) != 0 || unlockpt(master_fd) != 0 || ptsname_r(master_fd, devname, sizeof(devname)) != 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to setup pty");
        close(master_fd);
        return 0;
    }

    slave_fd = open(devname, O_RDWR | O_NOCTTY);
    if (slave_fd < 0) {
        close(master_fd);
        return 0;
    }

    pid_t pid = fork();
    if (pid == 0) {
        // Child
        close(master_fd);
        setsid();
        if (ioctl(slave_fd, TIOCSCTTY, 0) == -1) {}

        struct winsize ws;
        ws.ws_row = (unsigned short)rows;
        ws.ws_col = (unsigned short)cols;
        ws.ws_xpixel = 0;
        ws.ws_ypixel = 0;
        ioctl(slave_fd, TIOCSWINSZ, &ws);

        dup2(slave_fd, 0);
        dup2(slave_fd, 1);
        dup2(slave_fd, 2);
        if (slave_fd > 2) close(slave_fd);

        setenv("TERM", "xterm-256color", 1);
        // Execute shell
        const char *shell = cmd_str.c_str();
        char *args[] = {(char *)shell, NULL};
        execvp(shell, args);
        exit(1);
    }

    // Parent
    close(slave_fd);

    TerminalHandle *handle = new TerminalHandle();
    handle->pty_fd = master_fd;
    handle->pid = pid;

    // Init VTerm
    handle->vt = vterm_new(rows, cols);
    vterm_set_utf8(handle->vt, 1);
    handle->vts = vterm_obtain_screen(handle->vt);
    vterm_screen_reset(handle->vts, 1);

    return (jlong) handle;
}

JNIEXPORT jint JNICALL
Java_io_cortex_terminal_engine_TerminalSession_getPtyFd(JNIEnv *env, jobject thiz, jlong handlePtr) {
    TerminalHandle *handle = (TerminalHandle *)handlePtr;
    return handle->pty_fd;
}

JNIEXPORT void JNICALL
Java_io_cortex_terminal_engine_TerminalSession_resize(JNIEnv *env, jobject thiz, jlong handlePtr, jint rows, jint cols) {
    TerminalHandle *handle = (TerminalHandle *)handlePtr;
    struct winsize ws;
    ws.ws_row = (unsigned short)rows;
    ws.ws_col = (unsigned short)cols;
    ws.ws_xpixel = 0;
    ws.ws_ypixel = 0;
    ioctl(handle->pty_fd, TIOCSWINSZ, &ws);

    std::lock_guard<std::mutex> guard(handle->lock);
    vterm_set_size(handle->vt, rows, cols);
    vterm_screen_flush_damage(handle->vts);
}

JNIEXPORT void JNICALL
Java_io_cortex_terminal_engine_TerminalSession_pushBytes(JNIEnv *env, jobject thiz, jlong handlePtr, jbyteArray data, jint length) {
    TerminalHandle *handle = (TerminalHandle *)handlePtr;
    jbyte *bytes = env->GetByteArrayElements(data, NULL);
    {
        std::lock_guard<std::mutex> guard(handle->lock);
        vterm_input_write(handle->vt, (char *)bytes, length);
    }
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_io_cortex_terminal_engine_TerminalSession_closeSession(JNIEnv *env, jobject thiz, jlong handlePtr) {
    TerminalHandle *handle = (TerminalHandle *)handlePtr;
    // fd is owned by Java ParcelFileDescriptor now, so we don't close it here to avoid double close issues.
    // close(handle->pty_fd);
    kill(handle->pid, SIGKILL);
    {
        std::lock_guard<std::mutex> guard(handle->lock);
        vterm_free(handle->vt);
    }
    delete handle;
}

JNIEXPORT jstring JNICALL
Java_io_cortex_terminal_engine_TerminalSession_getLineInternal(JNIEnv *env, jobject thiz, jlong handlePtr, jint row) {
    TerminalHandle *handle = (TerminalHandle *)handlePtr;

    std::lock_guard<std::mutex> guard(handle->lock);
    int rows, cols;
    vterm_get_size(handle->vt, &rows, &cols);

    if (row >= rows) return env->NewStringUTF("");

    // Build string
    // Simplified ASCII
    std::string line = "";
    VTermScreenCell cell;
    VTermPos pos;
    pos.row = row;

    for (int col = 0; col < cols; col++) {
        pos.col = col;
        vterm_screen_get_cell(handle->vts, pos, &cell);

        if (cell.chars[0] == 0) line += " ";
        else if (cell.chars[0] < 128) line += (char)cell.chars[0];
        else line += "?";
    }

    return env->NewStringUTF(line.c_str());
}

}
