package com.reon.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    record Body(String name) {}

    @RestController
    static class TestController {
        @GetMapping("/items")
        String item(@RequestParam("id") Long id) {
            return "ok";
        }

        @PostMapping("/items")
        String create(@RequestBody Body body) {
            return "ok";
        }

        @GetMapping("/alias")
        String alias() {
            throw new AliasAlreadyTakenException("Custom alias not available.");
        }
    }

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void missingParameterReturns400() throws Exception {
        mockMvc.perform(get("/items"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'id' is missing"));
    }

    @Test
    void wrongParameterTypeReturns400() throws Exception {
        mockMvc.perform(get("/items").param("id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    @Test
    void invalidJsonReturns400() throws Exception {
        mockMvc.perform(post("/items").contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
    }

    @Test
    void aliasTakenReturns409() throws Exception {
        mockMvc.perform(get("/alias"))
                .andExpect(status().isConflict());
    }
}
