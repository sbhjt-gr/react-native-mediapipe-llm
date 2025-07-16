import { NativeModules, TurboModuleRegistry } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-mediapipe-llm' doesn't seem to be linked. Make sure: \n\n` +
  '- You have run \'pod install\' in the ios directory\n' +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

// First try to get the TurboModule (New Architecture)
const MediapipeLlmTurboModule = TurboModuleRegistry.get('MediapipeLlm');

// Fall back to legacy NativeModules (Old Architecture)
export const MediapipeLlm = MediapipeLlmTurboModule ?? NativeModules.MediapipeLlm ?? new Proxy(
  {},
  {
    get() {
      throw new Error(LINKING_ERROR);
    },
  }
);

export function isMediapipeLlmAvailable(): boolean {
  return !!MediapipeLlm && typeof MediapipeLlm === 'object';
}

export function getMediapipeLlmModule() {
  if (!isMediapipeLlmAvailable()) {
    throw new Error('MediapipeLlm module is not available');
  }
  return MediapipeLlm;
}
