package com.brnx.utmtgbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelegramService {

    private final RestTemplate restTemplate;

    @Value("${telegram.bot.token}")
    private String botToken;

    private String getApiUrl(String method) {
        return "https://api.telegram.org/bot" + botToken + "/" + method;
    }

    public void sendConsentMessage(Long chatId) {
        String url = getApiUrl("sendMessage");

        // Создаем клавиатуру
        List<List<Map<String, String>>> keyboard = new ArrayList<>();
        keyboard.add(Arrays.asList(
                Map.of("text", "Согласен", "callback_data", "agree"),
                Map.of("text", "Не согласен", "callback_data", "disagree")
        ));
        keyboard.add(List.of(
                Map.of("text", "Ознакомиться с политикой", "url", "https://example.com")
        ));

        // Формируем payload
        Map<String, Object> payload = Map.of(
                "chat_id", chatId,
                "text", "Пожалуйста, ознакомьтесь с политикой конфиденциальности и дайте согласие на обработку данных.",
                "reply_markup", Map.of("inline_keyboard", keyboard)
        );

        // Отправляем запрос
        restTemplate.postForObject(url, payload, String.class);
    }

    public File downloadFile(String fileId) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        String fileInfoUrl = "https://api.telegram.org/bot" + botToken + "/getFile?file_id=" + fileId;
        Map<String, Object> fileInfoResponse = restTemplate.getForObject(fileInfoUrl, Map.class);

        if (Boolean.TRUE.equals(fileInfoResponse.get("ok"))) {
            Map<String, Object> result = (Map<String, Object>) fileInfoResponse.get("result");
            String filePath = (String) result.get("file_path");

            String fileDownloadUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;
            InputStream inputStream = new URL(fileDownloadUrl).openStream();

            File tempFile = File.createTempFile("telegram_photo_", "." + extractFileExtension(filePath));
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            return tempFile;
        } else {
            throw new Exception("Не удалось получить информацию о файле: " + fileInfoResponse.get("description"));
        }
    }

    private String extractFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filePath.substring(lastDotIndex + 1);
    }

    public void sendMessage(Long chatId, String text) {
        String url = getApiUrl("sendMessage");
        Map<String, Object> payload = Map.of(
                "chat_id", chatId,
                "text", text
        );
        restTemplate.postForObject(url, payload, String.class);
    }

    public void sendGenderChoice(Long chatId) {
        String url = getApiUrl("sendMessage");
        Map<String, Object> replyMarkup = Map.of(
                "inline_keyboard", List.of(
                        List.of(Map.of("text", "Мужской", "callback_data", "male")),
                        List.of(Map.of("text", "Женский", "callback_data", "female"))
                )
        );

        Map<String, Object> payload = Map.of(
                "chat_id", chatId,
                "text", "Выберите пол:",
                "reply_markup", replyMarkup
        );
        restTemplate.postForObject(url, payload, String.class);
    }

    public void sendDocument(Long chatId, File file) {
        String url = getApiUrl("sendDocument");

        // Создаем запрос с файлом
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        body.add("document", new FileSystemResource(file));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Отправляем запрос
        restTemplate.postForObject(url, requestEntity, String.class);
    }
}
