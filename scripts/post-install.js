#!/usr/bin/env node

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

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
  }
}

try {
  if (isInNodeModules) {
    const mediapipeDir = path.resolve(projectRoot, 'mediapipe');
    const mediapipeGitDir = path.resolve(mediapipeDir, '.git');
    
    if (!fs.existsSync(mediapipeGitDir)) {
      runCommand('git submodule update --init --recursive');
    }
  }
} catch (error) {
} 