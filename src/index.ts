import { useState, useEffect, useCallback } from 'react';
import { NativeEventEmitter, NativeModules } from 'react-native';

const { MediapipeLlm } = NativeModules;

let eventEmitter: NativeEventEmitter | null = null;
if (MediapipeLlm) {
  try {
    eventEmitter = new NativeEventEmitter(MediapipeLlm);
  } catch (error) {
    
  }
}

export type LlmInferenceConfig = {
  storageType: 'asset' | 'file';
  modelName?: string;
  modelPath?: string;
  maxTokens?: number;
  topK?: number;
  temperature?: number;
  randomSeed?: number;
};

export interface LlmInferenceHook {
  generateResponse: (
    prompt: string,
    onPartial?: (partial: string) => void,
    onError?: (error: string) => void
  ) => Promise<string>;
  isLoaded: boolean;
}

export function useLlmInference(config?: LlmInferenceConfig): LlmInferenceHook {
  const [modelHandle, setModelHandle] = useState<number | undefined>();
  
  useEffect(() => {
    if (!config) {
      setModelHandle(undefined);
      return;
    }

    const createPromise = config.storageType === 'asset'
      ? MediapipeLlm.createModelFromAsset(
          config.modelName,
          config.maxTokens ?? 512,
          config.topK ?? 40,
          config.temperature ?? 0.8,
          config.randomSeed ?? 0
        )
      : MediapipeLlm.createModel(
          config.modelPath,
          config.maxTokens ?? 512,
          config.topK ?? 40,
          config.temperature ?? 0.8,
          config.randomSeed ?? 0
        );

    createPromise
      .then(setModelHandle)
      .catch(console.error);

    return () => {
      if (modelHandle) {
        MediapipeLlm.releaseModel(modelHandle);
      }
    };
  }, [config?.storageType, config?.modelName, config?.modelPath, config?.maxTokens, config?.topK, config?.temperature, config?.randomSeed]);

  const generateResponse = useCallback(
    async (
      prompt: string,
      onPartial?: (partial: string) => void,
      onError?: (error: string) => void
    ): Promise<string> => {
      if (!modelHandle) throw new Error('Model not loaded');
      
      const requestId = Date.now();
      
      const partialSub = eventEmitter?.addListener(
        'onPartialResponse',
        (ev) => {
          if (ev.requestId === requestId && onPartial) {
            onPartial(ev.response);
          }
        }
      );
      
      const errorSub = eventEmitter?.addListener(
        'onErrorResponse',
        (ev) => {
          if (ev.requestId === requestId && onError) {
            onError(ev.error);
          }
        }
      );

      try {
        return await MediapipeLlm.generateResponse(
          modelHandle,
          requestId,
          prompt
        );
      } finally {
        partialSub?.remove();
        errorSub?.remove();
      }
    },
    [modelHandle]
  );

  return {
    generateResponse,
    isLoaded: modelHandle !== undefined
  };
} 