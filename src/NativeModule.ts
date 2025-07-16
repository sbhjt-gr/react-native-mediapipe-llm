import { NativeModules, Platform } from 'react-native';

const MODULE_NAME = 'MediapipeLlm';

function createLinkingError(module: string): string {
  return `The package 'react-native-mediapipe-llm' doesn't seem to be linked. Make sure: 

${Platform.select({ ios: "- You have run 'cd ios && pod install'", default: '' })}
- You rebuilt the app after installing the package
- You are not using Expo Go
- The native module is properly configured

Module "${module}" is not available.`;
}

export function getMediapipeLlmModule() {
  if (!NativeModules[MODULE_NAME]) {
    throw new Error(createLinkingError(MODULE_NAME));
  }
  return NativeModules[MODULE_NAME];
}

export function isMediapipeLlmAvailable(): boolean {
  return !!NativeModules[MODULE_NAME] && typeof NativeModules[MODULE_NAME] === 'object';
}
