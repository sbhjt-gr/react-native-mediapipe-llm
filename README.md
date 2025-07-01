# react-native-mediapipe-llm

React Native binding for Google AI Edge Gallery's MediaPipe on-device LLM inference engine.

## Features

- High Performance: Leverages MediaPipe's optimized inference engine
- GPU/NPU Acceleration: Supports GPU acceleration on both platforms and NPU where available
- Cross-Platform: Works on both iOS and Android with unified API
- LLM Support: Supports various LLM models including Gemma, LLaMA, and custom models
- Multimodal: Support for text, image, and audio inputs
- Real-time: Optimized for real-time inference and streaming responses
- Easy Integration: Simple JavaScript API with TypeScript support

## Installation

### Prerequisites

- React Native >= 0.70.0
- iOS 11.0+ / Android API 21+
- CMake 3.13+ (for building)
- Git (for submodules)

### Install the package

```bash
npm install react-native-mediapipe-llm
# or
yarn add react-native-mediapipe-llm
```

### Initialize MediaPipe submodule

The MediaPipe source code is included as a git submodule for easy updates and transparent builds.

```bash
# In your app's root directory
cd node_modules/react-native-mediapipe-llm
git submodule update --init --recursive
```

### iOS Setup

```bash
cd ios && pod install
```

### Android Setup

No additional setup required. The module will be built automatically using CMake.

## Usage

### Quick Start

First, install and initialize the module in your React Native app:

```bash
# Install the package
npm install react-native-mediapipe-llm

# Initialize MediaPipe submodule
cd node_modules/react-native-mediapipe-llm
git submodule update --init --recursive

# iOS setup (if using iOS)
cd ios && pod install
```

### Basic Setup

```typescript
import React, { useEffect, useState } from 'react';
import { 
  MediaPipeLLM, 
  LlmPreferredBackend, 
  LlmActivationDataType,
  LlmResponse 
} from 'react-native-mediapipe-llm';
import { View, Text, TextInput, TouchableOpacity, Alert } from 'react-native';

const ChatScreen = () => {
  const [llm, setLlm] = useState<MediaPipeLLM | null>(null);
  const [isInitialized, setIsInitialized] = useState(false);
  const [input, setInput] = useState('');
  const [response, setResponse] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);

  useEffect(() => {
    initializeLLM();
    return () => {
      llm?.cleanup();
    };
  }, []);

  const initializeLLM = async () => {
    try {
      const llmInstance = new MediaPipeLLM();
      
      // Create engine with optimized settings
      await llmInstance.createEngine({
        modelPath: '/path/to/your/gemma-2b-it-cpu-int8.task', // Download from Kaggle
        maxNumTokens: 2048,
        preferredBackend: LlmPreferredBackend.GPU, // Use GPU for better performance
        activationDataType: LlmActivationDataType.FLOAT16,
        cacheDir: '/data/user/0/your.app.package/cache/mediapipe'
      });

      // Create session with balanced settings
      await llmInstance.createSession({
        topK: 40,
        topP: 0.9,
        temperature: 0.7,
        randomSeed: 42 // For reproducible results
      });

      setLlm(llmInstance);
      setIsInitialized(true);
      console.log('MediaPipe LLM initialized successfully');
    } catch (error) {
      console.error('Failed to initialize LLM:', error);
      Alert.alert('Error', 'Failed to initialize LLM. Please check the model path.');
    }
  };

  const generateResponse = async () => {
    if (!llm || !input.trim()) return;
    
    setIsGenerating(true);
    try {
      // Add the user's input
      await llm.addQueryChunk(input.trim());
      
      // Generate response
      const result = await llm.predictSync();
      setResponse(result.responses[0] || 'No response generated');
      setInput(''); // Clear input
    } catch (error) {
      console.error('Generation error:', error);
      Alert.alert('Error', 'Failed to generate response');
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <View style={{ flex: 1, padding: 20 }}>
      <Text>MediaPipe LLM Status: {isInitialized ? 'Ready' : 'Loading...'}</Text>
      
      <TextInput
        value={input}
        onChangeText={setInput}
        placeholder="Ask me anything..."
        multiline
        style={{ borderWidth: 1, padding: 10, minHeight: 60, marginVertical: 10 }}
        editable={isInitialized && !isGenerating}
      />
      
      <TouchableOpacity 
        onPress={generateResponse}
        disabled={!isInitialized || isGenerating || !input.trim()}
        style={{ 
          backgroundColor: isInitialized && !isGenerating ? '#007AFF' : '#CCC',
          padding: 15,
          borderRadius: 8,
          alignItems: 'center'
        }}
      >
        <Text style={{ color: 'white', fontWeight: 'bold' }}>
          {isGenerating ? 'Generating...' : 'Send'}
        </Text>
      </TouchableOpacity>
      
      {response && (
        <View style={{ marginTop: 20, padding: 15, backgroundColor: '#F5F5F5', borderRadius: 8 }}>
          <Text style={{ fontWeight: 'bold' }}>Response:</Text>
          <Text style={{ marginTop: 5 }}>{response}</Text>
        </View>
      )}
    </View>
  );
};

export default ChatScreen;
```

### Streaming Text Generation

For real-time text generation with streaming responses:

```typescript
import React, { useState, useCallback } from 'react';
import { MediaPipeLLM, LlmResponse } from 'react-native-mediapipe-llm';

const StreamingChatComponent = () => {
  const [llm, setLlm] = useState<MediaPipeLLM | null>(null);
  const [streamingText, setStreamingText] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);

  const handleStreamingGeneration = useCallback(async (prompt: string) => {
    if (!llm) return;

    setIsStreaming(true);
    setStreamingText('');

    try {
      await llm.addQueryChunk(prompt);

      // Streaming prediction with real-time updates
      await llm.predictAsync((response: LlmResponse) => {
        if (response.responses && response.responses.length > 0) {
          // Update UI with partial response
          setStreamingText(response.responses[0]);
        }

        if (response.done) {
          console.log('Streaming complete');
          setIsStreaming(false);
        }
      });
    } catch (error) {
      console.error('Streaming error:', error);
      setIsStreaming(false);
    }
  }, [llm]);

  return (
    <View>
      {/* Streaming text display */}
      <ScrollView style={{ maxHeight: 300, backgroundColor: '#f8f9fa', padding: 15 }}>
        <Text>{streamingText}</Text>
        {isStreaming && <Text style={{ color: '#666' }}>▊</Text>}
      </ScrollView>
      
      <TouchableOpacity 
        onPress={() => handleStreamingGeneration("Explain quantum computing in simple terms")}
        disabled={isStreaming}
      >
        <Text>{isStreaming ? 'Generating...' : 'Start Streaming'}</Text>
      </TouchableOpacity>
    </View>
  );
};
```

### Advanced Text Generation Patterns

```typescript
// Batch processing multiple prompts
const processBatch = async (prompts: string[]) => {
  const results = [];
  
  for (const prompt of prompts) {
    await llm.addQueryChunk(prompt);
    const response = await llm.predictSync();
    results.push({
      prompt,
      response: response.responses[0],
      tokenCount: await llm.sizeInTokens(prompt)
    });
  }
  
  return results;
};

// Conversation context management
class ConversationManager {
  private llm: MediaPipeLLM;
  private conversationHistory: string[] = [];

  constructor(llm: MediaPipeLLM) {
    this.llm = llm;
  }

  async addMessage(role: 'user' | 'assistant', message: string) {
    const formattedMessage = `${role}: ${message}`;
    this.conversationHistory.push(formattedMessage);
    
    // Keep only last 10 messages to manage context
    if (this.conversationHistory.length > 10) {
      this.conversationHistory = this.conversationHistory.slice(-10);
    }
  }

  async generateResponse(userMessage: string): Promise<string> {
    await this.addMessage('user', userMessage);
    
    // Build context from conversation history
    const context = this.conversationHistory.join('\n');
    await this.llm.addQueryChunk(context);
    
    const response = await this.llm.predictSync();
    const assistantResponse = response.responses[0];
    
    await this.addMessage('assistant', assistantResponse);
    return assistantResponse;
  }
}
```

### Model Management

#### Downloading and Setting Up Models

```typescript
import RNFS from 'react-native-fs';
import { Platform } from 'react-native';

class ModelManager {
  static async downloadModel(modelUrl: string, modelName: string): Promise<string> {
    const documentsPath = Platform.OS === 'ios' 
      ? RNFS.DocumentDirectoryPath 
      : RNFS.ExternalDirectoryPath;
    
    const modelPath = `${documentsPath}/models/${modelName}`;
    
    try {
      // Create models directory if it doesn't exist
      await RNFS.mkdir(`${documentsPath}/models`);
      
      // Download model file
      const downloadResult = await RNFS.downloadFile({
        fromUrl: modelUrl,
        toFile: modelPath,
        progress: (res) => {
          const progress = (res.bytesWritten / res.contentLength) * 100;
          console.log(`Download progress: ${progress.toFixed(2)}%`);
        }
      }).promise;
      
      if (downloadResult.statusCode === 200) {
        console.log('Model downloaded successfully');
        return modelPath;
      } else {
        throw new Error(`Download failed with status ${downloadResult.statusCode}`);
      }
    } catch (error) {
      console.error('Model download failed:', error);
      throw error;
    }
  }

  static async getModelPath(modelName: string): Promise<string> {
    const documentsPath = Platform.OS === 'ios' 
      ? RNFS.DocumentDirectoryPath 
      : RNFS.ExternalDirectoryPath;
    
    return `${documentsPath}/models/${modelName}`;
  }

  static async checkModelExists(modelName: string): Promise<boolean> {
    const modelPath = await this.getModelPath(modelName);
    return RNFS.exists(modelPath);
  }
}

// Usage example
const initializeWithModelDownload = async () => {
  const modelName = 'gemma-2b-it-cpu-int8.task';
  const modelUrl = 'https://kaggle.com/models/google/gemma-2/tfLite/gemma2-2b-it-cpu-int8/1/download';
  
  let modelPath;
  
  if (await ModelManager.checkModelExists(modelName)) {
    modelPath = await ModelManager.getModelPath(modelName);
    console.log('Using existing model');
  } else {
    console.log('Downloading model...');
    modelPath = await ModelManager.downloadModel(modelUrl, modelName);
  }
  
  const llm = new MediaPipeLLM();
  await llm.createEngine({ modelPath });
};
```

### Multimodal (Vision) Support

```typescript
import { launchImageLibrary, ImagePickerResponse } from 'react-native-image-picker';

const VisionChatComponent = () => {
  const [llm, setLlm] = useState<MediaPipeLLM | null>(null);
  const [selectedImage, setSelectedImage] = useState<string | null>(null);

  const initializeVisionLLM = async () => {
    const llmInstance = new MediaPipeLLM();
    
    await llmInstance.createEngine({
      modelPath: '/path/to/vision-model.task',
      visionEncoderPath: '/path/to/vision-encoder.task', // Optional vision encoder
      maxNumImages: 5,
      enableVisionModality: true
    });

    await llmInstance.createSession({
      enableVisionModality: true,
      temperature: 0.7
    });

    setLlm(llmInstance);
  };

  const selectImage = () => {
    launchImageLibrary({ mediaType: 'photo' }, (response: ImagePickerResponse) => {
      if (response.assets && response.assets[0]) {
        setSelectedImage(response.assets[0].uri!);
      }
    });
  };

  const analyzeImage = async (question: string) => {
    if (!llm || !selectedImage) return;

    try {
      // Add the selected image
      await llm.addImage(selectedImage);
      
      // Add the question about the image
      await llm.addQueryChunk(question);
      
      // Generate response
      const response = await llm.predictSync();
      return response.responses[0];
    } catch (error) {
      console.error('Image analysis error:', error);
      throw error;
    }
  };

  return (
    <View>
      <TouchableOpacity onPress={selectImage}>
        <Text>Select Image</Text>
      </TouchableOpacity>
      
      {selectedImage && (
        <Image source={{ uri: selectedImage }} style={{ width: 200, height: 200 }} />
      )}
      
      <TouchableOpacity onPress={() => analyzeImage("What do you see in this image?")}>
        <Text>Analyze Image</Text>
      </TouchableOpacity>
    </View>
  );
};

// Batch image analysis
const analyzeMultipleImages = async (llm: MediaPipeLLM, imageUris: string[], question: string) => {
  const results = [];
  
  for (const imageUri of imageUris) {
    await llm.addImage(imageUri);
    await llm.addQueryChunk(`${question} (Image ${imageUris.indexOf(imageUri) + 1})`);
    
    const response = await llm.predictSync();
    results.push({
      imageUri,
      analysis: response.responses[0]
    });
  }
  
  return results;
};
```

### Audio Support

```typescript
// Create session with audio support
await llm.createSession({
  enableAudioModality: true,
  temperature: 0.7
});

// Add audio data (base64 encoded)
await llm.addAudio(base64AudioData);
await llm.addQueryChunk("Transcribe this audio");

const response = await llm.predictSync();
```

### Session Management

```typescript
// Clone session for parallel processing
const clonedLlm = await llm.cloneSession();

// Calculate token count
const tokenCount = await llm.sizeInTokens("Hello world");

// Update runtime configuration
await llm.updateRuntimeConfig({
  temperature: 0.8,
  topK: 50
});

// Cleanup
await llm.cleanup();
```

## API Reference

### Engine Configuration

```typescript
interface LlmModelSettings {
  modelPath: string;                    // Path to .task or .tflite model
  visionEncoderPath?: string;          // Path to vision encoder (optional)
  visionAdapterPath?: string;          // Path to vision adapter (optional)
  cacheDir?: string;                   // Cache directory path
  maxNumTokens?: number;               // Maximum tokens (default: 2048)
  maxNumImages?: number;               // Maximum images for vision
  preferredBackend?: LlmPreferredBackend;
  activationDataType?: LlmActivationDataType;
  enableAudioModality?: boolean;
}
```

### Session Configuration

```typescript
interface LlmSessionConfig {
  topK?: number;                       // Top-K sampling
  topP?: number;                       // Top-P (nucleus) sampling
  temperature?: number;                // Sampling temperature
  randomSeed?: number;                 // Random seed for reproducibility
  enableVisionModality?: boolean;      // Enable image inputs
  enableAudioModality?: boolean;       // Enable audio inputs
  promptTemplates?: LlmPromptTemplates;
}
```

### Backend Options

```typescript
enum LlmPreferredBackend {
  DEFAULT = 0,  // Auto-select best backend
  GPU = 1,      // Force GPU acceleration
  CPU = 2       // Force CPU execution
}

enum LlmActivationDataType {
  DEFAULT = 0,   // Model default
  FLOAT32 = 1,   // 32-bit floating point
  FLOAT16 = 2,   // 16-bit floating point
  INT16 = 3,     // 16-bit integer
  INT8 = 4       // 8-bit integer (quantized)
}
```

## Performance Optimization

### Model Optimization

1. **Use quantized models** for better performance:
   ```typescript
   activationDataType: LlmActivationDataType.INT8
   ```

2. **Enable GPU acceleration**:
   ```typescript
   preferredBackend: LlmPreferredBackend.GPU
   ```

3. **Optimize token limits**:
   ```typescript
   maxNumTokens: 1024  // Reduce for faster inference
   ```

### Platform-Specific Optimizations

#### Android
- GPU acceleration uses OpenGL ES 3.1 compute shaders
- NPU acceleration via Android Neural Networks API (when available)
- Hardware buffer optimization for zero-copy operations

#### iOS
- Metal Performance Shaders for GPU acceleration
- Neural Engine utilization on A12+ devices
- Optimized memory management with ARC

## Model Support

### Supported Models

- **Gemma**: 2B, 7B variants (int4, int8, fp16)
- **Custom Models**: Any TensorFlow Lite compatible model

### Model Conversion

To convert your own models to MediaPipe format:

```bash
# Using MediaPipe Model Maker (Python)
pip install mediapipe-model-maker
# Follow MediaPipe documentation for LLM conversion
```

## Troubleshooting

### Common Issues

1. **Build failures**: Ensure CMake 3.13+ is installed
2. **Model loading errors**: Verify model path and permissions
3. **GPU acceleration not working**: Check device compatibility
4. **Memory issues**: Reduce `maxNumTokens` or use quantized models

### Debug Mode

Enable debug logging:

```typescript
// This will show performance metrics and debug info
const response = await llm.predictSync();
console.log('Performance metrics available in native logs');
```

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Setup

```bash
git clone https://github.com/yourusername/react-native-mediapipe-llm.git
cd react-native-mediapipe-llm
git submodule update --init --recursive
yarn install
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [MediaPipe](https://github.com/google-ai-edge/mediapipe) team for the excellent ML framework
- [Google AI Edge](https://github.com/google-ai-edge/gallery) for LLM inference implementations
- React Native community for the foundational tools and libraries

## Links

- [MediaPipe Documentation](https://developers.google.com/mediapipe)
- [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery)
- [Model Conversion Guide](https://developers.google.com/mediapipe/solutions/tasks/genai/llm_inference)
- [Performance Benchmarks](https://developers.google.com/mediapipe/solutions/tasks/genai/llm_inference/performance) 