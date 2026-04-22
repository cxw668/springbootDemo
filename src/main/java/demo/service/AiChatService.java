package demo.service;

import demo.model.ai.AiChatRequest;
import demo.model.ai.AiChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

public interface AiChatService {

    AiChatResponse chat(AiChatRequest request);

    SseEmitter stream(AiChatRequest request);

    Map<String, Object> getConfigSummary();
}
