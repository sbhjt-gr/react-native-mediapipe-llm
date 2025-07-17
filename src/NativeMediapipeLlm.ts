import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface MemoryConfiguration {
  // Universal device profile
  totalRamGB: number;
  totalRamMB: number;
  availableRamMB: number;
  currentHeapMB: number;
  potentialHeapMB: number;
  deviceCategory: 'flagship_ultra' | 'flagship_pro' | 'flagship' | 'premium' | 'mid_range' | 'budget_plus' | 'budget';
  isLargeHeapEnabled: boolean;
  
  // Universal model limits
  maxModelSizeMB: number;
  warningThresholdMB: number;
  recommendation: string;
  potentialAfterOptimization: number;
  
  // Configuration recommendations
  recommendations: string[];
  warnings: string[];
  info: string[];
  gradleRecommendations: string[];
  manifestRecommendations: string[];
  
  // Memory status
  memoryStatus: 'excellent' | 'good' | 'moderate' | 'limited';
}

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
    requestId: number,
    prompt: string
  ): Promise<string>;
  
  releaseModel(modelHandle: number): Promise<void>;
  
  getMemoryConfiguration(): Promise<MemoryConfiguration>;
  
  multiply(a: number, b: number): number;
  
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('MediapipeLlm');
