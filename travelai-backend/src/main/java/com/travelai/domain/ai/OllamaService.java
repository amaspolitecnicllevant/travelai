package com.travelai.domain.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OllamaService {

    private final OllamaChatModel chatModel;

    public Flux<String> streamChat(String systemPrompt, String userMessage) {
        var prompt = buildPrompt(systemPrompt, userMessage);
        log.debug("Iniciant stream Ollama — {} chars", userMessage.length());
        return chatModel.stream(prompt)
            .map(res -> res.getResult().getOutput().getContent())
            .filter(chunk -> chunk != null && !chunk.isEmpty())
            .onErrorMap(e -> new AiException("Error en stream: " + e.getMessage()));
    }

    @Retryable(retryFor = AiException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 1500, multiplier = 2))
    public String chat(String systemPrompt, String userMessage) {
        try {
            return chatModel.call(buildPrompt(systemPrompt, userMessage))
                .getResult().getOutput().getContent();
        } catch (Exception e) {
            throw new AiException("Error en crida a Ollama: " + e.getMessage());
        }
    }

    private Prompt buildPrompt(String system, String user) {
        return new Prompt(
            List.of(new SystemMessage(system), new UserMessage(user)),
            OllamaOptions.builder().withFormat("json").withTemperature(0.7f).build()
        );
    }
}
