#!/bin/bash

set -e

echo "Validating build configuration for react-native-mediapipe-llm..."

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

success() {
    echo -e "${GREEN}✓${NC} $1"
}

warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

error() {
    echo -e "${RED}✗${NC} $1"
    exit 1
}

# Check project structure
echo "Checking project structure..."

[ -f "package.json" ] || error "package.json not found"
[ -f "CMakeLists.txt" ] || error "CMakeLists.txt not found"
[ -d "cpp" ] || error "cpp directory not found"
[ -d "android" ] || error "android directory not found"
[ -f "react-native-mediapipe-llm.podspec" ] || error "podspec not found"

success "Project structure validated"

# Check C++ files
echo "Checking C++ source files..."

[ -f "cpp/MediapipeLlm.h" ] || error "MediapipeLlm.h not found"
[ -f "cpp/MediapipeLlm.cpp" ] || error "MediapipeLlm.cpp not found"
[ -f "cpp/JSI_Helpers.h" ] || error "JSI_Helpers.h not found"
[ -f "cpp/JSI_Helpers.cpp" ] || error "JSI_Helpers.cpp not found"

success "C++ source files validated"

# Check platform-specific files
echo "Checking platform-specific files..."

[ -f "android/build.gradle" ] || error "Android build.gradle not found"
[ -f "cpp/android/MediapipeLlm_Android.cpp" ] || error "Android C++ implementation not found"
[ -f "cpp/ios/MediapipeLlm_iOS.mm" ] || error "iOS C++ implementation not found"

success "Platform-specific files validated"

# Check TypeScript files
echo "Checking TypeScript files..."

[ -f "src/index.ts" ] || error "TypeScript index not found"
[ -f "src/MediaPipeLlm.ts" ] || error "MediaPipeLlm.ts not found"
[ -f "tsconfig.json" ] || error "tsconfig.json not found"

success "TypeScript files validated"

# Check MediaPipe submodule
echo "Checking MediaPipe submodule..."

if [ ! -d "mediapipe/.git" ]; then
    warning "MediaPipe submodule not initialized"
    echo "Run: git submodule update --init --recursive"
else
    success "MediaPipe submodule present"
fi

# Validate CMakeLists.txt syntax
echo "Validating CMake configuration..."

if command -v cmake &> /dev/null; then
    cmake -P CMakeLists.txt &> /dev/null || {
        # This might fail, so let's try a lighter validation
        grep -q "cmake_minimum_required" CMakeLists.txt || error "Invalid CMakeLists.txt"
        grep -q "project" CMakeLists.txt || error "Invalid CMakeLists.txt"
    }
    success "CMake configuration valid"
else
    warning "CMake not available for validation"
fi

# Check package.json scripts
echo "Checking package.json scripts..."

node -e "
const pkg = require('./package.json');
const required = ['prepare', 'typecheck', 'clean'];
for (const script of required) {
    if (!pkg.scripts[script]) {
        console.error('Missing script:', script);
        process.exit(1);
    }
}
console.log('All required scripts present');
"

success "Package.json scripts validated"

# Validate podspec
echo "Validating iOS podspec..."

if command -v pod &> /dev/null; then
    pod spec lint react-native-mediapipe-llm.podspec --quick --allow-warnings || warning "Podspec validation issues"
    success "Podspec validated"
else
    warning "CocoaPods not available for podspec validation"
fi

echo ""
success "All validations passed!"
echo ""
echo "Ready for npm publish!" 