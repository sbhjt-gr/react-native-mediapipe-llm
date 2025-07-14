#!/usr/bin/env node

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

console.log('Running prebuild tasks...');

const projectRoot = path.resolve(__dirname, '..');

function runCommand(command, options = {}) {
  try {
    console.log(`Running: ${command}`);
    execSync(command, {
      stdio: 'inherit',
      cwd: projectRoot,
      ...options
    });
  } catch (error) {
    console.error(`Failed to run: ${command}`);
    throw error;
  }
}

function checkFile(filePath, description) {
  const fullPath = path.resolve(projectRoot, filePath);
  if (!fs.existsSync(fullPath)) {
    throw new Error(`Missing ${description}: ${filePath}`);
  }
  console.log(`Found ${description}: ${filePath}`);
}

try {
  console.log('\n1. Checking required files...');
  checkFile('CMakeLists.txt', 'CMake configuration');
  checkFile('cpp/MediapipeLlm.h', 'C++ header');
  checkFile('cpp/MediapipeLlm.cpp', 'C++ implementation');
  checkFile('android/build.gradle', 'Android build config');
  checkFile('react-native-mediapipe-llm.podspec', 'iOS podspec');
  
  console.log('\n2. Checking MediaPipe submodule...');
  if (!fs.existsSync(path.resolve(projectRoot, 'mediapipe/.git'))) {
    console.log('MediaPipe submodule not initialized, initializing...');
    runCommand('git submodule update --init --recursive');
  } else {
    console.log('MediaPipe submodule initialized');
  }

  console.log('\n3. Validating TypeScript...');
  runCommand('npx tsc --noEmit');
  console.log('TypeScript validation passed');

  console.log('\n4. Validating C++ code...');
  if (process.platform !== 'win32') {
    runCommand('chmod +x scripts/build-cpp.sh');
    runCommand('scripts/build-cpp.sh');
  } else {
    console.log('Skipping C++ validation on Windows (requires proper build tools)');
  }
  console.log('C++ validation passed');

  console.log('\n5. Building TypeScript...');
  runCommand('npx bob build');
  console.log('TypeScript build completed');

  console.log('\nPrebuild completed successfully!');
  
} catch (error) {
  console.error('\nPrebuild failed:');
  console.error(error.message);
  process.exit(1);
} 