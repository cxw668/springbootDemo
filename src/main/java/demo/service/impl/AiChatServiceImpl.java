package demo.service.impl;

import demo.common.BizCode;
import demo.common.BizException;
import demo.config.AiProperties;
import demo.model.ai.AiChatRequest;
import demo.model.ai.AiChatResponse;
import demo.service.AiChatService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final String NOT_CONFIGURED_API_KEY = "not-configured";

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final AiProperties aiProperties;

    @Value("${spring.ai.openai.api-key:not-configured}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.siliconflow.cn}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:Qwen/Qwen3.5-4B}")
    private String defaultModel;

    public AiChatServiceImpl(ObjectProvider<ChatModel> chatModelProvider, AiProperties aiProperties) {
        this.chatModelProvider = chatModelProvider;
        this.aiProperties = aiProperties;
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        if (!aiProperties.isEnabled()) {
            throw new BizException(BizCode.FORBIDDEN, "AI 对话功能当前未开启");
        }

        if (!StringUtils.hasText(apiKey) || NOT_CONFIGURED_API_KEY.equals(apiKey)) {
            throw new BizException(BizCode.INTERNAL_ERROR, "请先配置环境变量 SILICONFLOW_API_KEY");
        }

        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            throw new BizException(BizCode.INTERNAL_ERROR, "Spring AI 聊天模型未初始化");
        }

        String resolvedModel = StringUtils.hasText(request.getModel()) ? request.getModel() : defaultModel;
        String resolvedSystemPrompt = StringUtils.hasText(request.getSystemPrompt())
                ? request.getSystemPrompt()
                : aiProperties.getDefaultSystemPrompt();

        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(resolvedSystemPrompt)) {
            messages.add(new SystemMessage(resolvedSystemPrompt));
        }
        messages.add(new UserMessage(request.getMessage()));

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(resolvedModel)
                .temperature(request.getTemperature())
                .build();

        ChatResponse response = chatModel.call(new Prompt(messages, options));
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new BizException(BizCode.INTERNAL_ERROR, "硅基流动未返回有效响应");
        }

        return new AiChatResponse(
                aiProperties.getProvider(),
                resolvedModel,
                response.getResult().getOutput().getText()
        );
    }

    @Override
    public Map<String, Object> getConfigSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("enabled", aiProperties.isEnabled());
        summary.put("provider", aiProperties.getProvider());
        summary.put("baseUrl", baseUrl);
        summary.put("defaultModel", defaultModel);
        summary.put("configured", StringUtils.hasText(apiKey) && !NOT_CONFIGURED_API_KEY.equals(apiKey));
        return summary;
    }
}
