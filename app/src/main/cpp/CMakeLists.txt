cmake_minimum_required(VERSION 3.18.0)
project("terminal-jni")

# Include Android log library
find_library(log-lib log)

# Instructions for user:
# You must clone libvterm into app/src/main/cpp/external/libvterm
# git clone https://github.com/neovim/libvterm.git external/libvterm

# Include directories
include_directories(external/libvterm/include)

# Add source files
file(GLOB LIBVTERM_SOURCES "external/libvterm/src/*.c")

add_library(terminal-jni SHARED
            terminal-jni.cpp
            ${LIBVTERM_SOURCES})

target_link_libraries(terminal-jni
                      ${log-lib})
