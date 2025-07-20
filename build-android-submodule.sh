#!/bin/bash

echo "=== React Native MediaPipe LLM - Android Submodule Build Guide ==="
echo ""

# Check if we're in the right directory
if [ ! -f "package.json" ]; then
    echo "Error: Run this script from the react-native-mediapipe-llm root directory"
    exit 1
fi

echo "Step 1: Prerequisites Check"
echo "=========================="

# Check for required tools
MISSING_TOOLS=()

if ! command -v git &> /dev/null; then
    MISSING_TOOLS+=("git")
fi

if ! command -v cmake &> /dev/null; then
    MISSING_TOOLS+=("cmake")
fi

if ! command -v node &> /dev/null; then
    MISSING_TOOLS+=("node")
fi

if ! command -v npm &> /dev/null && ! command -v yarn &> /dev/null; then
    MISSING_TOOLS+=("npm or yarn")
fi

if [ ${#MISSING_TOOLS[@]} -gt 0 ]; then
    echo "Missing required tools: ${MISSING_TOOLS[*]}"
    echo "Please install them first."
    exit 1
fi

echo "✓ All required tools found"

# Check CMake version
CMAKE_VERSION=$(cmake --version | head -n1 | cut -d' ' -f3)
echo "✓ CMake version: $CMAKE_VERSION"

echo ""
echo "Step 2: Environment Setup"
echo "========================="

# Check for Android environment
if [ -z "$ANDROID_HOME" ]; then
    echo "⚠️  ANDROID_HOME not set. Make sure Android SDK is installed."
    echo "   Set ANDROID_HOME to your Android SDK path"
else
    echo "✓ ANDROID_HOME: $ANDROID_HOME"
fi

if [ -z "$ANDROID_NDK" ] && [ -z "$ANDROID_NDK_HOME" ]; then
    echo "⚠️  Android NDK path not found."
    echo "   Make sure Android NDK is installed via Android Studio"
else
    echo "✓ Android NDK found"
fi

echo ""
echo "Step 3: MediaPipe Submodule Setup"
echo "================================="

# Initialize MediaPipe submodule
if [ ! -d "mediapipe/.git" ]; then
    echo "Initializing MediaPipe submodule..."
    git submodule update --init --recursive
    echo "✓ MediaPipe submodule initialized"
else
    echo "✓ MediaPipe submodule already initialized"
    
    # Update to latest
    echo "Updating MediaPipe submodule..."
    git submodule update --recursive --remote
    echo "✓ MediaPipe submodule updated"
fi

# Verify critical files exist
CRITICAL_FILES=(
    "mediapipe/mediapipe/tasks/cc/genai/inference/c/llm_inference_engine.h"
    "mediapipe/mediapipe/tasks/cc/genai/inference/c/llm_inference_engine_cpu.cc"
)

for file in "${CRITICAL_FILES[@]}"; do
    if [ ! -f "$file" ]; then
        echo "❌ Missing critical file: $file"
        echo "   MediaPipe submodule may be incomplete"
        exit 1
    fi
done

echo "✓ Critical MediaPipe files verified"

echo ""
echo "Step 4: Install Dependencies"
echo "============================"

# Install npm dependencies
if [ -f "package-lock.json" ]; then
    npm ci
elif [ -f "yarn.lock" ]; then
    yarn install --frozen-lockfile
else
    npm install
fi

echo "✓ Dependencies installed"

echo ""
echo "Step 5: Build Validation"
echo "========================"

# Run setup script
if [ -f "scripts/setup.sh" ]; then
    echo "Running setup script..."
    bash scripts/setup.sh
    echo "✓ Setup script completed"
fi

# Validate C++ build (optional, but recommended)
echo "Validating C++ compilation..."
if [ -f "scripts/build-cpp.sh" ]; then
    bash scripts/build-cpp.sh
    if [ $? -eq 0 ]; then
        echo "✓ C++ validation build successful"
    else
        echo "⚠️  C++ validation build failed, but this may be normal"
        echo "   The actual build will happen during Android compilation"
    fi
else
    echo "⚠️  C++ validation script not found"
fi

echo ""
echo "=== BUILD READY ==="
echo ""
echo "Your module is now configured to use MediaPipe submodule for Android."
echo ""
echo "Next steps:"
echo "1. Navigate to your React Native app directory"
echo "2. Install this module: npm install /path/to/react-native-mediapipe-llm"
echo "3. For Android: ./gradlew assembleDebug (or run through Android Studio)"
echo "4. For iOS: cd ios && pod install && cd .. && npx react-native run-ios"
echo ""
echo "Build times will be significantly longer due to MediaPipe compilation from source."
echo ""
echo "Troubleshooting:"
echo "- If build fails, ensure you have enough RAM (8GB+ recommended)"
echo "- Check that all Android SDK components are installed"
echo "- For detailed build logs: ./gradlew assembleDebug --info --stacktrace"
echo ""
