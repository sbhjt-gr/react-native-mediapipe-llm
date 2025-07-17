#!/usr/bin/env node

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const projectRoot = path.resolve(__dirname, '..');

function runCommand(command, options = {}) {
  try {
    execSync(command, {
      stdio: 'inherit',
      cwd: projectRoot,
      ...options
    });
  } catch (error) {
    throw error;
  }
}

function checkFile(filePath, description) {
  const fullPath = path.resolve(projectRoot, filePath);
  if (!fs.existsSync(fullPath)) {
    throw new Error(`Missing ${description}: ${filePath}`);
  }
}

try {
  checkFile('CMakeLists.txt', 'CMake configuration');
  checkFile('cpp/MediapipeLlm.h', 'C++ header');
  checkFile('cpp/MediapipeLlm.cpp', 'C++ implementation');
  checkFile('android/build.gradle', 'Android build config');
  checkFile('react-native-mediapipe-llm.podspec', 'iOS podspec');
  
  if (!fs.existsSync(path.resolve(projectRoot, 'mediapipe/.git'))) {
    runCommand('git submodule update --init --recursive');
  }

  runCommand('npx tsc --noEmit');

  if (process.platform !== 'win32') {
    runCommand('chmod +x scripts/build-cpp.sh');
    runCommand('scripts/build-cpp.sh');
  }

  runCommand('npx bob build');
  
} catch (error) {
  process.exit(1);
} 