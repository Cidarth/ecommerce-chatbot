package com.chatbot.controller;

import com.chatbot.service.ChatbotService;
import com.chatbot.model.ChatRequest;
import com.chatbot.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String response = chatbotService.processQuery(request.getQuestion());
        return new ChatResponse(response);
    }
}