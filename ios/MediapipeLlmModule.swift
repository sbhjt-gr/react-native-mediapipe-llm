import Foundation
import React
import MediaPipeTasksGenAI

@objc(MediapipeLlm)
class MediapipeLlmModule: RCTEventEmitter {
    private var nextHandle = 1
    private var modelMap = [Int: MediapipeLlmModel]()

    override func supportedEvents() -> [String]! {
        return ["onPartialResponse", "onErrorResponse"]
    }

    override static func requiresMainQueueSetup() -> Bool {
        return true
    }

    @objc(createModelFromAsset:withMaxTokens:withTopK:withTemperature:withRandomSeed:resolver:rejecter:)
    func createModelFromAsset(
        _ modelName: String,
        maxTokens: Int,
        topK: Int,
        temperature: NSNumber,
        randomSeed: Int,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        let modelHandle = nextHandle
        nextHandle += 1

        do {
            guard let modelPath = Bundle.main.path(forResource: modelName, ofType: nil) else {
                throw NSError(domain: "MODEL_NOT_FOUND", code: 0, 
                            userInfo: ["message": "Model \(modelName) not found"])
            }

            let model = try MediapipeLlmModel(
                handle: modelHandle,
                modelPath: modelPath,
                maxTokens: maxTokens,
                topK: topK,
                temperature: temperature.floatValue,
                randomSeed: randomSeed
            )
            
            model.delegate = self
            modelMap[modelHandle] = model
            resolve(modelHandle)
        } catch let error as NSError {
            reject(error.domain, error.localizedDescription, error)
        }
    }

    @objc(createModel:withMaxTokens:withTopK:withTemperature:withRandomSeed:resolver:rejecter:)
    func createModel(
        _ modelPath: String,
        maxTokens: Int,
        topK: Int,
        temperature: NSNumber,
        randomSeed: Int,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        let modelHandle = nextHandle
        nextHandle += 1

        do {
            let model = try MediapipeLlmModel(
                handle: modelHandle,
                modelPath: modelPath,
                maxTokens: maxTokens,
                topK: topK,
                temperature: temperature.floatValue,
                randomSeed: randomSeed
            )
            
            model.delegate = self
            modelMap[modelHandle] = model
            resolve(modelHandle)
        } catch let error as NSError {
            reject(error.domain, error.localizedDescription, error)
        }
    }

    @objc(generateResponse:withRequestId:withPrompt:resolver:rejecter:)
    func generateResponse(
        _ handle: Int,
        requestId: Int,
        prompt: String,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        guard let model = modelMap[handle] else {
            reject("INVALID_HANDLE", "No model found for handle \(handle)", nil)
            return
        }

        model.generateResponse(
            prompt: prompt,
            requestId: requestId,
            resolve: resolve,
            reject: reject
        )
    }

    @objc(releaseModel:resolver:rejecter:)
    func releaseModel(
        _ handle: Int,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        if modelMap.removeValue(forKey: handle) != nil {
            resolve(true)
        } else {
            reject("INVALID_HANDLE", "No model found for handle \(handle)", nil)
        }
    }
}

extension MediapipeLlmModule: MediapipeLlmModelDelegate {
    func onPartialResponse(_ model: MediapipeLlmModel, requestId: Int, response: String) {
        self.sendEvent(withName: "onPartialResponse", body: [
            "handle": model.handle,
            "requestId": requestId,
            "response": response
        ])
    }

    func onErrorResponse(_ model: MediapipeLlmModel, requestId: Int, error: String) {
        self.sendEvent(withName: "onErrorResponse", body: [
            "handle": model.handle,
            "requestId": requestId,
            "error": error
        ])
    }
} 