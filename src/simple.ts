import { NativeModules } from 'react-native';

export const MediapipeLlm = NativeModules.MediapipeLlm;

export function isMediapipeLlmAvailable(): boolean {
  return !!MediapipeLlm && typeof MediapipeLlm === 'object';
}

export function getMediapipeLlmModule() {
  if (!isMediapipeLlmAvailable()) {
    throw new Error('MediapipeLlm module is not available');
  }
  return MediapipeLlm;
}
