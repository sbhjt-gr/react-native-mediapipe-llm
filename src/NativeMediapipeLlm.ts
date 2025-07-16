import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  createModelFromAsset(
    modelName: string,
    maxTokens: number,
    topK: number,
    temperature: number,
    randomSeed: number
  ): Promise<number>;
  
  createModel(
    modelPath: string,
    maxTokens: number,
    topK: number,
    temperature: number,
    randomSeed: number
  ): Promise<number>;
  
  generateResponse(
    modelHandle: number,
    prompt: string,
    requestId: string
  ): Promise<string>;
  
  releaseModel(modelHandle: number): Promise<void>;
  
  multiply(a: number, b: number): number;
  
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('MediapipeLlm');
