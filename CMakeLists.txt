# This CMakeLists.txt is kept for potential future use if C++ components are needed
# Currently the module uses standard React Native native modules (Kotlin for Android, Objective-C for iOS)
# instead of C++ JSI bridge for better maintainability and compatibility

cmake_minimum_required(VERSION 3.13)
project(react-native-mediapipe-llm)

# This file is intentionally minimal as the current implementation
# uses platform-specific native modules rather than C++ JSI bridge 