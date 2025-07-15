import React, { useState } from 'react';
import { View, TextInput, Button, Text, ScrollView, StyleSheet, KeyboardAvoidingView, Platform, Alert } from 'react-native';
import * as DocumentPicker from 'expo-document-picker';
import { useLlmInference, LlmInferenceConfig } from '@subhajit-gorai/react-native-mediapipe-llm';
import TestComponent from '../TestComponent';

interface Message {
  role: 'user' | 'assistant';
  content: string;
}

export default function App() {
  const [prompt, setPrompt] = useState('');
  const [messages, setMessages] = useState<Message[]>([]);
  const [partialResponse, setPartialResponse] = useState('');
  const [selectedModelPath, setSelectedModelPath] = useState<string | null>(null);
  const [isModelLoading, setIsModelLoading] = useState(false);
  const [llmConfig, setLlmConfig] = useState<LlmInferenceConfig | undefined>();

  // Initialize the LLM hook
  const llm = useLlmInference(llmConfig);

  const handleSelectModel = async () => {
    try {
      setIsModelLoading(true);
      const result = await DocumentPicker.getDocumentAsync({
        type: '*/*',
        copyToCacheDirectory: false,
      });

      if (!result.canceled && result.assets && result.assets.length > 0) {
        const asset = result.assets[0];
        if (asset.name.endsWith('.task')) {
          setSelectedModelPath(asset.uri);
          setMessages([]);
          setPartialResponse('');
          
          // Initialize the LLM with the selected model
          setLlmConfig({
            storageType: 'file',
            modelPath: asset.uri,
            maxTokens: 512,
            topK: 40,
            temperature: 0.8,
            randomSeed: 0
          });
        } else {
          Alert.alert('Invalid File', 'Please select a .task model file.');
        }
      }
    } catch (error) {
      Alert.alert('Error', 'Failed to select model file.');
    } finally {
      setIsModelLoading(false);
    }
  };

  const handleSend = async () => {
    if (!prompt.trim() || !llm.isLoaded) return;

    setMessages((prev: Message[]) => [...prev, { role: 'user', content: prompt }]);
    setPartialResponse('');
    const currentPrompt = prompt;
    setPrompt('');

    try {
      const response = await llm.generateResponse(
        currentPrompt,
        (partial) => setPartialResponse(partial),
        () => setPartialResponse('Failed to generate response.')
      );
      setMessages((prev: Message[]) => [...prev, { role: 'assistant', content: response }]);
      setPartialResponse('');
    } catch (error) {
      setMessages((prev: Message[]) => [...prev, { role: 'assistant', content: 'Failed to generate response.' }]);
      setPartialResponse('');
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === "ios" ? "padding" : "height"}
      keyboardVerticalOffset={Platform.OS === "ios" ? 100 : 0}
    >
      <TestComponent />
      {!selectedModelPath ? (
        <View style={styles.modelSelection}>
          <Text style={styles.modelSelectionText}>
            Select a model file to start chatting
          </Text>
          <Button
            title={isModelLoading ? "Loading..." : "Select Model File"}
            onPress={handleSelectModel}
            disabled={isModelLoading}
          />
        </View>
      ) : (
        <View style={styles.chatContainer}>
          <View style={styles.modelInfo}>
            <Text style={styles.modelInfoText}>
              Model: {selectedModelPath.split('/').pop()}
            </Text>
            <Button
              title="Change Model"
              onPress={handleSelectModel}
              disabled={isModelLoading}
            />
          </View>

          <ScrollView contentContainerStyle={styles.scrollContent}>
            {messages.map((msg: Message, index: number) => (
              <View key={index} style={[styles.message, styles[msg.role]]}>
                <Text style={styles.messageText}>{msg.content}</Text>
              </View>
            ))}
            {partialResponse ? (
              <View style={[styles.message, styles.assistant]}>
                <Text style={styles.messageText}>{partialResponse}</Text>
              </View>
            ) : null}
          </ScrollView>

          <View style={styles.inputContainer}>
            <TextInput
              value={prompt}
              onChangeText={setPrompt}
              placeholder="Enter your prompt..."
              style={styles.input}
              multiline
            />
            <Button
              title="Send"
              onPress={handleSend}
              disabled={!prompt.trim()}
            />
          </View>
        </View>
      )}
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 10,
    backgroundColor: '#f5f5f5',
  },
  chatContainer: {
    flex: 1,
  },
  modelSelection: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  modelSelectionText: {
    fontSize: 18,
    textAlign: 'center',
    marginBottom: 20,
    color: '#333',
  },
  modelInfo: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 10,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#ddd',
  },
  modelInfoText: {
    fontSize: 14,
    color: '#666',
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    justifyContent: 'flex-end',
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 10,
    borderTopWidth: 1,
    borderColor: '#ddd',
    backgroundColor: '#fff',
  },
  input: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 20,
    paddingHorizontal: 15,
    paddingVertical: 10,
    marginRight: 10,
    maxHeight: 100,
  },
  message: {
    padding: 15,
    borderRadius: 20,
    marginBottom: 10,
    maxWidth: '80%',
  },
  user: {
    backgroundColor: '#007AFF',
    alignSelf: 'flex-end',
  },
  assistant: {
    backgroundColor: '#E5E5EA',
    alignSelf: 'flex-start',
  },
  messageText: {
    color: '#000',
  },
}); 