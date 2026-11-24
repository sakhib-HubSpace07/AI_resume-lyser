package com.resumeai.resumeanalyzer;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final ChatClient chatClient;

    // Spring gives us a ready-made helper that talks to Ollama
    public TestController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // When you open /ask in the browser, this method runs
    @GetMapping("/ask")
    public String ask() {
        return chatClient.prompt()
                .user("Say hello in one short sentence.")
                .call()
                .content();
    }
}