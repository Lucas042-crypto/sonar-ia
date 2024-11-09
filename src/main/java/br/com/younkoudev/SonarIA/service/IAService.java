package br.com.younkoudev.SonarIA.service;

import lombok.AllArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class IAService {

    private final ChatClient chatClient;

    Map<String, String> getResponseIA(String message) {
        return Map.of(
                "completion",
                chatClient.prompt()
                        .user(message)
                        .call()
                        .content());
    }
}
