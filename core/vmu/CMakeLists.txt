# VMU Bridge Module - CMake Configuration
#
# This module provides the VMU bridge layer for exposing VMU display
# data to external consumers (Android JNI).

target_sources(${PROJECT_NAME} PRIVATE
    vmu_bridge.cpp
    vmu_bridge.h
)
