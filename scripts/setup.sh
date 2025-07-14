#!/bin/bash

set -e

echo "Setting up react-native-mediapipe-llm..."

if ! command -v git &> /dev/null; then
    echo "Git is not installed. Please install git first."
    exit 1
fi

if ! command -v cmake &> /dev/null; then
    echo "CMake is not installed. Please install CMake 3.13+ first."
    echo "   macOS: brew install cmake"
    echo "   Ubuntu: sudo apt-get install cmake"
    echo "   Windows: Download from https://cmake.org/download/"
    exit 1
fi

CMAKE_VERSION=$(cmake --version | head -n1 | cut -d' ' -f3)
CMAKE_MAJOR=$(echo $CMAKE_VERSION | cut -d. -f1)
CMAKE_MINOR=$(echo $CMAKE_VERSION | cut -d. -f2)

if [ "$CMAKE_MAJOR" -lt 3 ] || ([ "$CMAKE_MAJOR" -eq 3 ] && [ "$CMAKE_MINOR" -lt 13 ]); then
    echo "CMake version $CMAKE_VERSION is too old. Please install CMake 3.13+ first."
    exit 1
fi

echo "CMake version $CMAKE_VERSION is compatible"

echo "Initializing MediaPipe submodule..."
if [ ! -d "mediapipe/.git" ]; then
    git submodule update --init --recursive
    echo "MediaPipe submodule initialized"
else
    echo "MediaPipe submodule already initialized"
fi

echo "Updating MediaPipe to latest version..."
git submodule update --recursive --remote

if [ -f "../../package.json" ]; then
    PROJECT_ROOT="../../"
elif [ -f "../package.json" ]; then
    PROJECT_ROOT="../"
else
    PROJECT_ROOT="."
fi

echo "Setting up platform dependencies..."

if [ -d "${PROJECT_ROOT}ios" ]; then
    echo "Setting up iOS dependencies..."
    if command -v pod &> /dev/null; then
        cd "${PROJECT_ROOT}ios"
        pod install
        cd - > /dev/null
        echo "iOS CocoaPods dependencies installed"
    else
        echo "CocoaPods not found. Install with: sudo gem install cocoapods"
    fi
fi

if [ -d "${PROJECT_ROOT}android" ]; then
    echo "Android setup will happen automatically during build"
    echo "Android CMake build configured"
fi

echo ""
echo "Setup complete!"
echo ""
echo "Next steps:"
echo "1. Add a model file to your app (download from Kaggle or convert your own)"
echo "2. Import and use MediaPipeLLM in your React Native code"
echo "3. Run your app: npx react-native run-ios / npx react-native run-android"
echo ""
echo "Check the README.md for usage examples and API documentation"