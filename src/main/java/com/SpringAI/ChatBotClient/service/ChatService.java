package com.SpringAI.ChatBotClient.service;

import com.SpringAI.ChatBotClient.entity.ChatHistory;
import com.SpringAI.ChatBotClient.repository.ChatHistoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatHistoryRepository chatHistoryRepository;

    public ChatService(ChatClient.Builder builder, VectorStore vectorStore,ChatHistoryRepository chatHistoryRepository) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
        this.chatHistoryRepository = chatHistoryRepository;
    }

    public Map<String, Object> chat(String userQuestion) {

        String normalizedQuestion = normalizeQuery(userQuestion);


        // 1. Retrieve relevant chunks from vector store
        List<Document> relevantDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userQuestion)
                        .topK(4)
                        .build()
        );

        // 2. Build context string
        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // 3. Call LLM with context + question
        String answer = chatClient.prompt()
                .system(s -> s.text("""
                    You are a helpful assistant.
                    Use the context below to answer the user's question.
                
                    Important rules:
                      - If the context contains relevant information even partially, use it to answer
                      - Do NOT say "I don't know" if the context has any related information
                      - Only say "I don't know" if the context is completely unrelated to the question
                      - Keep your answer clear, detailed and well structured

                Context:
                {context}
                """)
                        .param("context", context))
                .user(userQuestion)
                .call()
                .content();

        // Return answer + source chunks
        List<String> sources = relevantDocs.stream()
                .map(Document::getText)
                .toList();

        return Map.of(
                "answer", answer,
                "sources", sources
        );
    }

    private String normalizeQuery(String query) {
        // Expand short or vague queries
        Map<String, String> shortQueryMap = Map.of(
                "spring", "What is Spring Boot and what are its core features",
                "boot", "What is Spring Boot and what are its core features",
                "jpa", "What is Spring Data JPA and how does it work",
                "security", "How does Spring Security work",
                "rest", "How to build REST APIs with Spring Boot",
                "database", "How to connect database in Spring Boot",
                "cache", "How does caching work in Spring Boot",
                "test", "How to write tests in Spring Boot"
        );

        String trimmed = query.trim().toLowerCase();
        // If query matches a short keyword, expand it
        if (shortQueryMap.containsKey(trimmed)) {
            System.out.println("🔍 Expanding short query: " + query + " → " + shortQueryMap.get(trimmed));
            return shortQueryMap.get(trimmed);
        }
        // List of known Spring annotations
        List<String> springAnnotations = List.of(
                "Transactional", "SpringBootApplication", "RestController",
                "Controller", "Service", "Repository", "Component",
                "Autowired", "Bean", "Configuration", "Entity",
                "Table", "Column", "Id", "GeneratedValue",
                "PathVariable", "RequestParam", "RequestBody", "RequestMapping",
                "GetMapping", "PostMapping", "PutMapping", "DeleteMapping",
                "EnableAutoConfiguration", "ComponentScan", "Profile",
                "Value", "Async", "Cacheable", "CacheEvict", "CachePut",
                "ExceptionHandler", "ControllerAdvice", "ResponseBody"
        );

        String normalizedQuery = query;
        for (String annotation : springAnnotations) {
            // If user typed annotation name without @, add it
            normalizedQuery = normalizedQuery.replaceAll(
                    "(?<![\\w@])" + annotation + "(?![\\w])",
                    "@" + annotation
            );
        }

        return normalizedQuery;
    }




}
