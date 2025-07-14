import Foundation
import MediaPipeTasksGenAI
import React

protocol MediapipeLlmModelDelegate: AnyObject {
    func onPartialResponse(_ model: MediapipeLlmModel, requestId: Int, response: String)
    func onErrorResponse(_ model: MediapipeLlmModel, requestId: Int, error: String)
}

final class MediapipeLlmModel {
    weak var delegate: MediapipeLlmModelDelegate?
    let handle: Int
    
    private lazy var inference: LlmInference! = {
        let options = LlmInference.Options(modelPath: self.modelPath)
        options.maxTokens = self.maxTokens
        options.topk = self.topK
        options.temperature = self.temperature
        options.randomSeed = self.randomSeed
        return try? LlmInference(options: options)
    }()

    private let modelPath: String
    private let maxTokens: Int
    private let topK: Int
    private let temperature: Float
    private let randomSeed: Int

    init(
        handle: Int,
        modelPath: String,
        maxTokens: Int,
        topK: Int,
        temperature: Float,
        randomSeed: Int
    ) throws {
        self.handle = handle
        self.modelPath = modelPath
        self.maxTokens = maxTokens
        self.topK = topK
        self.temperature = temperature
        self.randomSeed = randomSeed
    }

    func generateResponse(
        prompt: String,
        requestId: Int,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        var result = ""
        
        do {
            try self.inference.generateResponseAsync(
                inputText: prompt,
                progress: { [weak self] partialResponse, error in
                    guard let self = self else { return }
                    
                    if let error = error {
                        self.delegate?.onErrorResponse(self, requestId: requestId, error: error.localizedDescription)
                        reject("GENERATE_RESPONSE_ERROR", error.localizedDescription, error)
                    } else if let partialResponse = partialResponse {
                        self.delegate?.onPartialResponse(self, requestId: requestId, response: partialResponse)
                        result += partialResponse
                    }
                },
                completion: {
                    resolve(result)
                }
            )
        } catch {
            reject("INIT_GENERATE_RESPONSE_ERROR", "Failed to generate response: \(error)", error)
        }
    }
} 