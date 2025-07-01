# Build Process for react-native-mediapipe-llm

This document explains how C++ compilation is integrated into the npm publish workflow.

## Overview

The module includes C++ code that wraps MediaPipe's LLM inference engine. The build process ensures that:

1. C++ code is validated during development and publishing
2. MediaPipe submodule is properly initialized
3. All native dependencies are correctly configured
4. TypeScript types are generated and built

## Build Scripts

### Development Scripts

```bash
# Initial setup (run once after cloning)
npm run setup

# Validate C++ compilation
npm run build:cpp

# Validate entire build configuration
npm run validate

# Run full prebuild process
npm run prebuild
```

### NPM Lifecycle Scripts

These run automatically during npm operations:

- **`prepare`**: Runs `prebuild` before publishing or after `npm install` in development
- **`prepublishOnly`**: Runs `validate` before `npm publish` to ensure everything is ready
- **`postinstall`**: Runs after users install the package to set up MediaPipe submodule

## Build Process Flow

### During Development

1. **Setup**: `npm run setup`
   - Initializes MediaPipe git submodule
   - Checks CMake and build dependencies
   - Sets up platform-specific dependencies (CocoaPods for iOS)

2. **Validation**: `npm run validate`
   - Validates all required files exist
   - Checks CMake configuration syntax
   - Validates iOS podspec
   - Ensures package.json scripts are correct

3. **C++ Build**: `npm run build:cpp`
   - Compiles C++ code with CMake for validation
   - Tests MediaPipe integration
   - Cleans up build artifacts (keeps source only)

### During npm publish

1. **Prepare phase** (`npm run prebuild`):
   - Checks all required files
   - Initializes MediaPipe submodule if needed
   - Validates TypeScript compilation
   - Validates C++ compilation
   - Builds TypeScript to JavaScript

2. **Pre-publish validation** (`npm run prepublishOnly`):
   - Final validation of build configuration
   - Ensures all files are present
   - Validates CMake and podspec configurations

### After user installs package

1. **Post-install** (`npm run postinstall`):
   - Automatically sets up MediaPipe submodule in user's project
   - Provides guidance for next steps

## Requirements

### Development Requirements

- **Node.js** 18+
- **CMake** 3.13+
- **Git** (for submodules)

### Platform-specific Requirements

#### iOS Development
- **Xcode** 12+
- **CocoaPods** (for dependency management)

#### Android Development
- **Android NDK** 21+
- **CMake** (included in Android SDK)

## Build Configuration

### CMake Configuration

The `CMakeLists.txt` configures:
- Cross-platform C++ compilation
- MediaPipe source integration
- GPU acceleration (OpenGL ES for Android, Metal for iOS)
- TensorFlow Lite linking

### Platform-specific Configuration

#### Android (`android/build.gradle`)
- CMake integration with externalNativeBuild
- GPU acceleration flags
- NDK configuration

#### iOS (`react-native-mediapipe-llm.podspec`)
- Framework dependencies (Metal, Accelerate)
- Header search paths
- Compiler flags for MediaPipe

## Troubleshooting

### Common Issues

1. **MediaPipe submodule not initialized**
   ```bash
   npm run setup
   ```

2. **CMake not found**
   ```bash
   # macOS
   brew install cmake
   
   # Ubuntu
   sudo apt-get install cmake
   ```

3. **C++ compilation fails**
   - Check CMake version (3.13+ required)
   - Ensure MediaPipe submodule is initialized
   - Verify build dependencies are installed

4. **iOS build issues**
   ```bash
   cd ios && pod install
   ```

### Build Validation

Run the full validation suite:
```bash
npm run validate
```

This checks:
- ✓ Project structure
- ✓ C++ source files
- ✓ Platform-specific files
- ✓ TypeScript files
- ✓ MediaPipe submodule
- ✓ CMake configuration
- ✓ Package.json scripts
- ✓ iOS podspec

## Files Included in NPM Package

The published package includes:
- `src/` - TypeScript source
- `lib/` - Compiled JavaScript and TypeScript definitions
- `cpp/` - C++ source files
- `android/` - Android build configuration
- `ios/` - iOS build configuration (via podspec)
- `mediapipe/` - MediaPipe source (as git submodule)
- `CMakeLists.txt` - CMake build configuration
- `scripts/post-install.js` - User setup script

## Development Workflow

1. Make changes to C++ or TypeScript code
2. Run `npm run validate` to check everything
3. Run `npm run prebuild` to test full build process
4. Commit changes
5. Run `npm publish` (automatic validation included)

The build system ensures that published packages are always validated and ready for use. 