package demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.common.AppProperties;
import demo.common.GlobalExceptionHandler;
import demo.model.ai.AiChatResponse;
import demo.service.AiChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import({GlobalExceptionHandler.class, AppProperties.class})
class AiControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiChatService aiChatService;

    @Test
    @DisplayName("POST /ai/chat returns Result wrapper with AI reply")
    void testChatReturnsResult() throws Exception {
        AiChatResponse response = new AiChatResponse("SiliconFlow", "Qwen/Qwen3.5-4B", "你好，我是一个 AI 助手。");
        when(aiChatService.chat(org.mockito.ArgumentMatchers.any())).thenReturn(response);

        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "message", "你好，请介绍一下你自己"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("调用成功"))
                .andExpect(jsonPath("$.data.provider").value("SiliconFlow"))
                .andExpect(jsonPath("$.data.model").value("Qwen/Qwen3.5-4B"))
                .andExpect(jsonPath("$.data.reply").value("你好，我是一个 AI 助手。"));
    }

    @Test
    @DisplayName("POST /ai/chat with blank message returns validation error")
    void testChatValidation() throws Exception {
        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "message", ""
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("message: 消息内容不能为空"));
    }

    @Test
    @DisplayName("GET /ai/config returns configuration summary")
    void testGetConfigReturnsResult() throws Exception {
        when(aiChatService.getConfigSummary()).thenReturn(Map.of(
                "enabled", true,
                "provider", "SiliconFlow",
                "baseUrl", "https://api.siliconflow.cn",
                "defaultModel", "Qwen/Qwen3.5-4B",
                "configured", false
        ));

        mockMvc.perform(get("/ai/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.provider").value("SiliconFlow"))
                .andExpect(jsonPath("$.data.defaultModel").value("Qwen/Qwen3.5-4B"))
                .andExpect(jsonPath("$.data.configured").value(false));
    }
}
