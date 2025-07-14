#!/usr/bin/env node

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

console.log('react-native-mediapipe-llm: Running post-install setup...');

const isInNodeModules = __dirname.includes('node_modules');
const projectRoot = isInNodeModules 
  ? path.resolve(__dirname, '..') 
  : path.resolve(__dirname, '..');

function runCommand(command, options = {}) {
  try {
    execSync(command, {
      stdio: 'inherit',
      cwd: projectRoot,
      ...options
    });
  } catch (error) {
    console.warn(`Warning: Failed to run: ${command}`);
    console.warn('You may need to run setup manually.');
  }
}

try {
  if (isInNodeModules) {
    console.log('Setting up MediaPipe submodule...');
    
    const mediapipeDir = path.resolve(projectRoot, 'mediapipe');
    const mediapipeGitDir = path.resolve(mediapipeDir, '.git');
    
    if (!fs.existsSync(mediapipeGitDir)) {
      console.log('Initializing MediaPipe submodule...');
      runCommand('git submodule update --init --recursive');
    } else {
      console.log('MediaPipe submodule already initialized');
    }
    
    console.log('Post-install setup completed');
    console.log('');
    console.log('Next steps:');
    console.log('1. Download a compatible LLM model (.task file)');
    console.log('2. Import MediaPipeLLM in your React Native app');
    console.log('3. See README.md for usage examples');
  } else {
    console.log('Development mode detected, skipping post-install setup');
  }
} catch (error) {
  console.warn('Post-install setup failed:', error.message);
  console.warn('You may need to run setup manually with: npm run setup');
} 