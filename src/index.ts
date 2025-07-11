import { useCallback, useRef } from 'react';
import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-mediapipe-llm' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'cd ios && pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const MediapipeLlm = NativeModules.MediapipeLlm
  ? NativeModules.MediapipeLlm
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export interface LlmOptions {
  modelPath?: string;
  maxTokens?: number;
  temperature?: number;
  topK?: number;
  topP?: number;
}

export interface LlmInferenceHook {
  generateResponse: (
    prompt: string,
    partialCallback?: (partial: string) => void
  ) => Promise<string>;
  isInitialized: boolean;
  initialize: (options: LlmOptions) => Promise<boolean>;
}

export function useLlmInference(): LlmInferenceHook {
  const isInitializedRef = useRef(false);
  
  const initialize = useCallback(async (opts: LlmOptions): Promise<boolean> => {
    try {
      const success = await MediapipeLlm.initialize(opts);
      isInitializedRef.current = success;
      return success;
    } catch (error) {
      isInitializedRef.current = false;
      return false;
    }
  }, []);

  const generateResponse = useCallback(
    async (
      prompt: string, 
      partialCallback?: (partial: string) => void
    ): Promise<string> => {
      if (!isInitializedRef.current) {
        throw new Error('LLM not initialized. Call initialize() first.');
      }

      if (partialCallback) {
        return new Promise((resolve, reject) => {
          let fullResponse = '';
          
          MediapipeLlm.generateResponseWithCallback(
            prompt,
            (partial: string, done: boolean) => {
              if (partial) {
                fullResponse += partial;
                partialCallback(partial);
              }
              if (done) {
                resolve(fullResponse);
              }
            },
            (error: string) => {
              reject(new Error(error));
            }
          );
        });
      } else {
        return MediapipeLlm.generateResponse(prompt);
      }
    },
    []
  );

  return {
    generateResponse,
    isInitialized: isInitializedRef.current,
    initialize,
  };
}

export default useLlmInference; 