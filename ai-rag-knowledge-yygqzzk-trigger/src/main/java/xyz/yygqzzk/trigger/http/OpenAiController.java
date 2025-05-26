package xyz.yygqzzk.trigger.http;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import xyz.yygqzzk.api.IAiService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zzk
 * @version 1.0
 * @description TODO
 * @since 2025/5/26
 */
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/openai")
public class OpenAiController implements IAiService {

    @Resource
    OpenAiChatClient chatClient;

    @Resource
    PgVectorStore pgVectorStore;

    /**
     * http://localhost:8080/api/v1/openai/generate?model=deepseek-r1:1.5b&message=1+1
     */
    @Override
    @RequestMapping(value = "generate", method = RequestMethod.GET)
    public ChatResponse generate(@RequestParam String model, @RequestParam String message) {
        return chatClient.call(new Prompt(message, OpenAiChatOptions.builder().withModel(model).build()));
    }

    /**
     * http://localhost:8080/api/v1/openai/generate_stream?model=deepseek-r1:1.5b&message=1+1
     */
    @Override
    @RequestMapping(value = "generate_stream", method = RequestMethod.GET)
    public Flux<ChatResponse> generateStream(@RequestParam String model, @RequestParam String message) {
        return chatClient.stream(new Prompt(message, OpenAiChatOptions.builder().withModel(model).build()));
    }

    @Override
    @RequestMapping(value = "generate_stream_rag", method = RequestMethod.GET)
    public Flux<ChatResponse> generateStreamRag(@RequestParam String model, @RequestParam String ragTag, @RequestParam String message) {
        String SYSTEM_PROMPT = """
                Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                If unsure, simply state that you don't know.
                Another thing you need to note is that your reply must be in Chinese!
                DOCUMENTS:
                    {documents}
                """;

        SearchRequest request = SearchRequest.query(message).withTopK(5).withFilterExpression("knowledge == '" + ragTag + "'");

        List<Document> documents = pgVectorStore.similaritySearch(request);

        String documentsCollectors = documents.stream().map(Document::getContent).collect(Collectors.joining());

        Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentsCollectors));

        ArrayList<Message> messages = new ArrayList<>();
        messages.add(ragMessage);
        messages.add(new UserMessage(message));

        return chatClient.stream(new Prompt(messages, OpenAiChatOptions.builder().withModel(model).build()));
    }

}




