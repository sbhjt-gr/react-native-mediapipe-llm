# react-native-mediapipe-llm

React Native binding for Google AI Edge Gallery's MediaPipe on-device LLM inference engine.

## Features

- High Performance: Leverages MediaPipe's optimized inference engine
- GPU/NPU Acceleration: Supports GPU acceleration on both platforms and NPU where available
- Cross-Platform: Works on both iOS and Android with unified API
- LLM Support: Supports various LLM models including Gemma, LLaMA, and custom models
- Real-time: Optimized for real-time inference and streaming responses
- Easy Integration: Simple JavaScript API with TypeScript support

## Installation

### Install the package

```bash
npm install react-native-mediapipe-llm
# or
yarn add react-native-mediapipe-llm
```

### iOS Setup

```bash
cd ios && pod install
```

### Android Setup

No additional setup required. The module will be built automatically.

## Usage

### Quick Start

The primary functionality of this package is accessed through the `useLlmInference()` hook. This hook provides a `generateResponse` function, which you can use to process text prompts.

```typescript
import React, { useState, useEffect } from 'react';
import { View, TextInput, Button, Text } from 'react-native';
import { useLlmInference } from 'react-native-mediapipe-llm';

const App = () => {
  const [prompt, setPrompt] = useState('');
  const [response, setResponse] = useState('');
  const { generateResponse, initialize, isInitialized } = useLlmInference();

  useEffect(() => {
    // Initialize the LLM with your model
    const initLLM = async () => {
      await initialize({
        modelPath: '/path/to/your/model.task', // Download from Kaggle Models
        maxTokens: 512,
        temperature: 0.8,
        topK: 40,
        topP: 0.9,
      });
    };
    
    initLLM();
  }, []);

  const handleGeneratePress = async () => {
    if (!isInitialized) {
      alert('LLM not initialized yet');
      return;
    }
    
    const result = await generateResponse(prompt);
    setResponse(result);
  };

  return (
    <View style={{ padding: 20 }}>
      <TextInput
        style={{
          height: 40,
          borderColor: 'gray',
          borderWidth: 1,
          marginBottom: 10,
        }}
        onChangeText={setPrompt}
        value={prompt}
        placeholder="Enter your prompt here"
      />
      <Button title="Generate Response" onPress={handleGeneratePress} />
      <Text>{response}</Text>
    </View>
  );
};

export default App;
```

### Streaming Responses

You can access partial results by supplying a callback to `generateResponse()`:

```typescript
const handleGeneratePress = async () => {
  if (!isInitialized) return;
  
  let fullResponse = '';
  const result = await generateResponse(
    prompt,
    (partial) => {
      fullResponse += partial;
      setResponse(fullResponse); // Update UI with partial response
    }
  );
  
  setResponse(result); // Final response
};
```

## API Reference

### `useLlmInference()`

Returns an object with the following properties:

- `generateResponse(prompt: string, partialCallback?: (partial: string) => void): Promise<string>` - Generate a response for the given prompt
- `initialize(options: LlmOptions): Promise<boolean>` - Initialize the LLM with the specified options
- `isInitialized: boolean` - Whether the LLM is initialized and ready to use

### `LlmOptions`

- `modelPath: string` - Path to the LLM model file (.task format)
- `maxTokens?: number` - Maximum number of tokens to generate (default: 512)
- `temperature?: number` - Sampling temperature (default: 0.8)
- `topK?: number` - Top-K sampling parameter (default: 40)
- `topP?: number` - Top-P sampling parameter (default: 0.9)

## Model Requirements

Before using this package, you must download or build the LLM model files necessary for its operation. MediaPipe supports various models including:

- Gemma 2B
- Falcon 1B
- StableLM 3B
- Phi-2

Download pre-converted models from [Kaggle Models](https://www.kaggle.com/models) and ensure they are in `.task` format for MediaPipe compatibility.

## Contributing

Contributions are very welcome! If you would like to improve react-native-mediapipe-llm, please feel free to fork the repository, make changes, and submit a pull request.

## License

This project is licensed under the Apache-2.0 License - see the LICENSE file for details.

## Support

For support, feature requests, or any other inquiries, please open an issue on the GitHub project page.