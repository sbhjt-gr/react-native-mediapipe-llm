#!/bin/bash

set -e

echo "Building C++ code for react-native-mediapipe-llm..."

# Ensure we're in the project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

# Check dependencies
if ! command -v cmake &> /dev/null; then
    echo "Error: CMake is not installed"
    exit 1
fi

if ! command -v make &> /dev/null && ! command -v ninja &> /dev/null; then
    echo "Error: Neither make nor ninja build system found"
    exit 1
fi

# Create build directory
BUILD_DIR="$PROJECT_ROOT/build"
mkdir -p "$BUILD_DIR"

echo "Validating MediaPipe submodule..."
if [ ! -d "mediapipe/.git" ]; then
    echo "Error: MediaPipe submodule not initialized. Run 'npm run setup' first."
    exit 1
fi

# Build for validation (not for distribution)
echo "Building C++ code for validation..."
cd "$BUILD_DIR"

# Configure with CMake
cmake .. \
    -DCMAKE_BUILD_TYPE=Release \
    -DMEDIAPIPE_ENABLE_GPU=ON \
    -DMEDIAPIPE_ENABLE_ANDROID=ON \
    -DMEDIAPIPE_ENABLE_IOS=ON

# Build the project
if command -v ninja &> /dev/null; then
    ninja
else
    make -j$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)
fi

echo "C++ validation build completed successfully"

# Clean up build directory to save space in npm package
cd "$PROJECT_ROOT"
rm -rf "$BUILD_DIR"

echo "C++ build validation complete!" 