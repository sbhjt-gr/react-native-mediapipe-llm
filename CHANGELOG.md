# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-01-XX

### Added
- Initial release of react-native-mediapipe-llm
- Cross-platform MediaPipe LLM inference engine wrapper
- Support for both iOS and Android platforms
- GPU/NPU acceleration support
- CMake-based build system for optimal performance
- TypeScript definitions and comprehensive API
- Support for text, image, and audio inputs (multimodal)
- Streaming and synchronous inference modes
- Session management and cloning capabilities
- Comprehensive documentation and examples
- MediaPipe as git submodule for easy updates
- Performance optimizations for mobile devices

### Features
- LLM model loading and inference
- Real-time text generation
- Vision-language model support
- Audio processing capabilities
- Token counting and management
- Runtime configuration updates
- Memory-efficient session handling
- Cross-platform image loading
- GPU delegate optimization
- NPU utilization where available

### Platform Support
- iOS 11.0+
- Android API 21+
- React Native 0.70.0+

### Dependencies
- MediaPipe Framework (as submodule)
- TensorFlow Lite
- React Native core libraries
- Platform-specific ML acceleration libraries 