package com.brnx.utmtgbot.controller;

import com.brnx.utmtgbot.service.TelegramUpdateHandlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/telegram")
public class TelegramWebhookController {
    private final TelegramUpdateHandlerService updateHandlerService;

    @PostMapping
    public void onUpdateReceived(@RequestBody Update update) {
        System.out.println("Получено обновление: " + update);
        updateHandlerService.handleUpdate(update);
    }
}

