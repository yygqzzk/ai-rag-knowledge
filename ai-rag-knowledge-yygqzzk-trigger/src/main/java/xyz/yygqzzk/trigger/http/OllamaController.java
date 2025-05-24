package xyz.yygqzzk.trigger.http;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import xyz.yygqzzk.api.IAiService;

/**
 * @author zzk
 * @version 1.0
 * @description TODO
 * @since 2025/5/24
 */
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/ollama")
public class OllamaController implements IAiService {

    @Resource
    OllamaChatClient chatClient;

    /**
     * http://localhost:8080/api/v1/ollama/generate?model=deepseek-r1:1.5b&message=1+1
     */
    @Override
    @RequestMapping(value = "generate", method = RequestMethod.GET)
    public ChatResponse generate(@RequestParam String model, @RequestParam String message) {
        return chatClient.call(new Prompt(message, OllamaOptions.create().withModel(model)));
    }

    /**
     * http://localhost:8080/api/v1/ollama/generate_stream?model=deepseek-r1:1.5b&message=1+1
     */
    @Override
    @RequestMapping(value = "generate_stream", method = RequestMethod.GET)
    public Flux<ChatResponse> generateStream(@RequestParam String model, @RequestParam String message) {
        return chatClient.stream(new Prompt(message, OllamaOptions.create().withModel(model)));
    }
}




