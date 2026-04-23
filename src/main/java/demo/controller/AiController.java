package demo.controller;

import demo.common.Result;
import demo.model.ai.AiChatRequest;
import demo.model.ai.AiChatResponse;
import demo.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "AI 对话", description = "通过 Spring AI 调用硅基流动兼容 OpenAI 的聊天接口")
@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiChatService aiChatService;

    public AiController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @Operation(summary = "AI 对话", description = "默认走硅基流动的 Qwen3-8B，可按请求覆盖模型")
    @PostMapping("/chat")
    public Object chat(@Valid @RequestBody AiChatRequest request, @RequestHeader(value = "Accept", required = false) String accept) {
        // If client expects server-sent events, return an SseEmitter streaming the model output
        if (accept != null && accept.contains("text/event-stream")) {
            return aiChatService.stream(request);
        }
        return Result.success("调用成功", aiChatService.chat(request));
    }

    @Operation(summary = "获取 AI 配置摘要", description = "返回当前 Spring AI 与硅基流动的脱敏配置")
    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        return Result.success(aiChatService.getConfigSummary());
    }
}
