import MediaPipeLlm from './MediaPipeLlm';

export enum LlmPreferredBackend {
  DEFAULT = 0,
  GPU = 1,
  CPU = 2,
}

export enum LlmActivationDataType {
  DEFAULT = 0,
  FLOAT32 = 1,
  FLOAT16 = 2,
  INT16 = 3,
  INT8 = 4,
}

export interface LlmModelSettings {
  modelPath: string;
  visionEncoderPath?: string;
  visionAdapterPath?: string;
  cacheDir?: string;
  maxNumTokens?: number;
  maxNumImages?: number;
  preferredBackend?: LlmPreferredBackend;
  activationDataType?: LlmActivationDataType;
  enableAudioModality?: boolean;
}

export interface LlmSessionConfig {
  topK?: number;
  topP?: number;
  temperature?: number;
  randomSeed?: number;
  enableVisionModality?: boolean;
  enableAudioModality?: boolean;
  promptTemplates?: LlmPromptTemplates;
}

export interface LlmPromptTemplates {
  prefix?: string;
  suffix?: string;
  systemInstruction?: string;
}

export interface LlmResponse {
  responses: string[];
  done: boolean;
  tokenCount?: number;
}

export class MediaPipeLLM {
  private nativeInstance: any;

  constructor() {
    this.nativeInstance = new MediaPipeLlm();
  }

  async createEngine(settings: LlmModelSettings): Promise<void> {
    return this.nativeInstance.createEngine(settings);
  }

  async createSession(config?: LlmSessionConfig): Promise<void> {
    return this.nativeInstance.createSession(config || {});
  }

  async addQueryChunk(queryChunk: string): Promise<void> {
    return this.nativeInstance.addQueryChunk(queryChunk);
  }

  async addImage(imageUri: string): Promise<void> {
    return this.nativeInstance.addImage(imageUri);
  }

  async addAudio(audioData: string): Promise<void> {
    return this.nativeInstance.addAudio(audioData);
  }

  async predictSync(): Promise<LlmResponse> {
    return this.nativeInstance.predictSync();
  }

  async predictAsync(callback: (response: LlmResponse) => void): Promise<void> {
    return this.nativeInstance.predictAsync(callback);
  }

  async cloneSession(): Promise<MediaPipeLLM> {
    const clonedNative = await this.nativeInstance.cloneSession();
    const clonedInstance = new MediaPipeLLM();
    clonedInstance.nativeInstance = clonedNative;
    return clonedInstance;
  }

  async sizeInTokens(text: string): Promise<number> {
    return this.nativeInstance.sizeInTokens(text);
  }

  async updateRuntimeConfig(config: Partial<LlmSessionConfig>): Promise<void> {
    return this.nativeInstance.updateRuntimeConfig(config);
  }

  async cleanup(): Promise<void> {
    return this.nativeInstance.cleanup();
  }
}

export default MediaPipeLLM; 