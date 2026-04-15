package demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Unified response format tests")
    class UnifiedResponseTests {

        @Test
        @DisplayName("GET /user/page returns Result wrapper with code 200")
        void testPageReturnsResult() throws Exception {
            mockMvc.perform(get("/user/page")
                            .param("pageNo", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("POST /user returns Result with success data")
        void testCreateReturnsResult() throws Exception {
            User user = new User();
            user.setName("ResultTest");
            user.setAge(25);
            user.setPhone("12800000000");

            mockMvc.perform(post("/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        @DisplayName("DELETE /user/{id} returns Result with success data")
        void testDeleteReturnsResult() throws Exception {
            // First create a user
            User user = new User();
            user.setName("DeleteResultTest");
            user.setAge(30);
            user.setPhone("13100000000");

            MvcResult createResult = mockMvc.perform(post("/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andReturn();

            // Now delete via search to find the ID
            MvcResult searchResult = mockMvc.perform(get("/user/search")
                            .param("name", "DeleteResultTest"))
                    .andReturn();

            String searchJson = searchResult.getResponse().getContentAsString();
            // Extract ID from response - parse the JSON
            var response = objectMapper.readTree(searchJson);
            Long id = response.get("data").get(0).get("id").asLong();

            mockMvc.perform(delete("/user/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(true));
        }
    }

    @Nested
    @DisplayName("Validation error tests")
    class ValidationTests {

        @Test
        @DisplayName("POST /user - empty name returns 422 validation error")
        void testCreateWithEmptyName() throws Exception {
            User user = new User();
            user.setName("");
            user.setAge(25);
            user.setPhone("12510000000");

            mockMvc.perform(post("/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(422))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("POST /user - missing age returns 422 validation error")
        void testCreateWithMissingAge() throws Exception {
            User user = new User();
            user.setName("NoAge");
            // age is null
            user.setPhone("10000000000");
            mockMvc.perform(post("/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(422))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("POST /user - negative age returns 422 validation error")
        void testCreateWithNegativeAge() throws Exception {
            User user = new User();
            user.setName("NegativeAge");
            user.setAge(-5);
            user.setPhone("150000000000");

            mockMvc.perform(post("/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(422))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("POST /user - age over 150 returns 422 validation error")
        void testCreateWithAgeOver150() throws Exception {
            User user = new User();
            user.setName("TooOld");
            user.setAge(200);
            user.setPhone("12100000000");

            mockMvc.perform(post("/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(422))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("PUT /user - missing ID returns 422 validation error")
        void testUpdateWithMissingId() throws Exception {
            User user = new User();
            user.setName("NoId");
            user.setAge(25);
            user.setPhone("125100000000");
            // id is null

            mockMvc.perform(put("/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(422))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("Error handling tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("DELETE /user/{id} - non-existent ID returns error result")
        void testDeleteNonExistentUser() throws Exception {
            mockMvc.perform(delete("/user/{id}", 9999))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("用户不存在"));
        }

        @Test
        @DisplayName("PUT /user - non-existent ID returns error result")
        void testUpdateNonExistentUser() throws Exception {
            User user = new User();
            user.setId(9999L);
            user.setName("Ghost");
            user.setAge(30);
            user.setPhone("15000000000");
            user.setVersion(1);

            mockMvc.perform(put("/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").exists());
        }
    }
}
