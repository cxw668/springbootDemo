package demo.service;

import demo.model.ai.AiChatRequest;
import demo.model.ai.AiChatResponse;

import java.util.Map;

public interface AiChatService {

    AiChatResponse chat(AiChatRequest request);

    Map<String, Object> getConfigSummary();
}
