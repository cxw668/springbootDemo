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
import reactor.core.publisher.Flux;
import reactor.core.Disposable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final String NOT_CONFIGURED_API_KEY = "not-configured";

    // 专用线程池用于 LLM 调用,避免阻塞主线程池
    private final ExecutorService llmExecutor = Executors.newFixedThreadPool(
            4, 
            r -> {
                Thread thread = new Thread(r);
                thread.setName("llm-chat-worker");
                thread.setDaemon(true);
                return thread;
            }
    );

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final AiProperties aiProperties;

    @Value("${spring.ai.openai.api-key:not-configured}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.siliconflow.cn}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:Qwen3-8B}")
    private String defaultModel;

    @Value("${app.ai.timeout-seconds:10}")
    private int timeoutSeconds;

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

        // Call the chat model with a timeout to avoid hanging if the provider is slow/unreachable
        ChatResponse response;
        try {
            response = CompletableFuture.supplyAsync(
                    () -> chatModel.call(new Prompt(messages, options)), 
                    llmExecutor
            ).get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            throw new BizException(BizCode.INTERNAL_ERROR, "LLM 请求超时(" + timeoutSeconds + "秒),请稍后重试");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
            throw new BizException(BizCode.INTERNAL_ERROR, "LLM 请求被中断");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new BizException(BizCode.INTERNAL_ERROR, "调用 LLM 时发生错误: " + (cause != null ? cause.getMessage() : e.getMessage()));
        }

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
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter stream(AiChatRequest request) {
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

        // 创建 SSE emitter，设置超时时间为 0（不超时）
        SseEmitter emitter = new SseEmitter(0L);
        
        // 使用线程池异步处理流式响应，避免阻塞
        llmExecutor.execute(() -> {
            try {
                // 构建 Prompt
                Prompt prompt = new Prompt(messages, options);
                
                // 调用流式接口
                Flux<String> flux = chatModel.stream(prompt)
                        .map(chatResponse -> {
                            if (chatResponse == null) {
                                return "";
                            }
                            var result = chatResponse.getResult();
                            if (result == null) {
                                return "";
                            }
                            var output = result.getOutput();
                            if (output == null) {
                                return "";
                            }
                            String text = output.getText();
                            return text != null ? text : "";
                        });
                
                // 订阅流式数据
                Disposable disposable = flux.subscribe(
                        chunk -> {
                            try {
                                if (chunk != null && !chunk.isEmpty()) {
                                    // 发送 SSE 格式数据
                                    emitter.send(SseEmitter.event()
                                            .data(chunk)
                                            .name("message"));
                                }
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        emitter::completeWithError,
                        emitter::complete
                );
                
                // 注册清理回调
                emitter.onCompletion(disposable::dispose);
                emitter.onTimeout(() -> {
                    disposable.dispose();
                    emitter.complete();
                });
                emitter.onError(ex -> {
                    disposable.dispose();
                });
                
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
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
