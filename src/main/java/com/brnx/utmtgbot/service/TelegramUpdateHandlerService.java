package com.brnx.utmtgbot.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.brnx.utmtgbot.model.RegistrationStep;
import com.brnx.utmtgbot.model.UserData;
import lombok.RequiredArgsConstructor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Base64;

import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URL;

@Service
@RequiredArgsConstructor
public class TelegramUpdateHandlerService {

    private final UserDataService userDataService;
    private final TelegramService telegramService;

    public void handleUpdate(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        } else if (update.hasMessage() && update.getMessage().hasPhoto()) {
            handlePhoto(update.getMessage());
        }
    }

    private void handleMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText();
        String utm = null;

        if (text.startsWith("/start")) {
            String[] parts = text.split(" ");
            if (parts.length > 1) {
                utm = parts[1];
            }

            UserData userData = userDataService.getOrCreateUser(chatId, utm);

            // Устанавливаем начальный шаг для пользователя
            userData.setCurrentStep(RegistrationStep.CONSENT);
            userDataService.save(userData);

            // Отправляем сообщение с кнопками для согласия
            telegramService.sendConsentMessage(chatId);
            return; // Выходим из метода, чтобы не обрабатывать дальше
        }

        UserData userData = userDataService.getOrCreateUser(chatId, utm);

        switch (userData.getCurrentStep()) {
            case FULL_NAME -> {
                if (text.trim().split(" ").length >= 2) {
                    userData.setFullName(text.trim());
                    userData.setCurrentStep(RegistrationStep.BIRTH_DATE);
                    userDataService.save(userData);
                    telegramService.sendMessage(chatId, "Введите дату рождения (в формате dd.MM.yyyy):");
                } else {
                    telegramService.sendMessage(chatId, "Введите корректные ФИО (минимум имя и фамилия).");
                }
            }
            case BIRTH_DATE -> {
                try {
                    LocalDate date = LocalDate.parse(text, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                    userData.setBirthDate(date);
                    userData.setCurrentStep(RegistrationStep.GENDER);
                    userDataService.save(userData);
                    telegramService.sendGenderChoice(chatId);
                } catch (DateTimeParseException e) {
                    telegramService.sendMessage(chatId, "Некорректная дата. Введите в формате dd.MM.yyyy");
                }
            }
            default -> telegramService.sendMessage(chatId, "Пожалуйста, используйте кнопки или дождитесь инструкции.");
        }
    }

    private void handleCallbackQuery(CallbackQuery query) {
        Long chatId = query.getMessage().getChatId();
        String data = query.getData();

        UserData userData = userDataService.getOrCreateUser(chatId, null);

        switch (userData.getCurrentStep()) {
            case CONSENT -> {
                if ("agree".equals(data)) {
                    userData.setConsentGiven(true);
                    userData.setCurrentStep(RegistrationStep.FULL_NAME);
                    userDataService.save(userData);
                    telegramService.sendMessage(chatId, "Введите ФИО:");
                } else if ("disagree".equals(data)) {
                    telegramService.sendMessage(chatId, "Спасибо за ваше время! Желаем вам хорошего дня!");
                    telegramService.sendMessage(chatId, "Если хотите начать заново, отправьте /start.");
                } else if ("link".equals(data)) {
                    telegramService.sendMessage(chatId, "Ознакомиться с политикой: https://example.com");
                }
            }
            case GENDER -> {
                if ("male".equals(data) || "female".equals(data)) {
                    userData.setGender("male".equals(data) ? "Мужской" : "Женский");
                    userData.setCurrentStep(RegistrationStep.PHOTO);
                    userDataService.save(userData);
                    telegramService.sendMessage(chatId, "Пожалуйста, отправьте фотографию.");
                }
            }
        }
    }

    private void handlePhoto(Message message) {
        Long chatId = message.getChatId();
        UserData userData = userDataService.getOrCreateUser(chatId, null);

        if (userData.getCurrentStep() == RegistrationStep.PHOTO) {
            // Получаем fileId фотографии
            String fileId = message.getPhoto().get(message.getPhoto().size() - 1).getFileId(); // Берем самое большое изображение
            userData.setPhotoPath(fileId); // Сохраняем fileId как путь к фото
            userData.setCurrentStep(RegistrationStep.COMPLETED);
            userDataService.save(userData);

            try {
                // Скачиваем файл через TelegramService
                File photoFile = telegramService.downloadFile(fileId);

                // Генерируем Word-документ
                File document = generateWordDocumentWithImage(userData, photoFile);

                // Отправляем документ пользователю
                telegramService.sendDocument(chatId, document);

                // Удаляем временные файлы
                photoFile.delete();
                document.delete();

                // Отправляем сообщение пользователю
                telegramService.sendMessage(chatId, "Спасибо! Ваши данные сохранены и отправлены вам в виде документа.");
            } catch (Exception e) {
                e.printStackTrace();
                telegramService.sendMessage(chatId, "Произошла ошибка при создании документа.");
            }
        } else {
            telegramService.sendMessage(chatId, "Ожидается другой шаг. Пожалуйста, следуйте инструкциям.");
        }
    }

    public File generateWordDocumentWithImage(UserData userData, File photoFile) throws IOException, InvalidFormatException {
        XWPFDocument document = new XWPFDocument();

        // Создаем заголовок
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setText("Данные пользователя");
        titleRun.setBold(true);
        titleRun.setFontSize(16);

        // Добавляем данные пользователя
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText("ФИО: " + userData.getFullName());
        run.addBreak();
        run.setText("Дата рождения: " + userData.getBirthDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        run.addBreak();
        run.setText("Пол: " + userData.getGender());
        run.addBreak();

        // Добавляем фото
        if (photoFile != null && photoFile.exists()) {
            try (FileInputStream photoStream = new FileInputStream(photoFile)) {
                int pictureType = getImageType(photoFile.getName());
                int widthEMU = pxToEMU(200); // Ширина 200 пикселей
                int heightEMU = pxToEMU(200); // Высота 200 пикселей

                // Создаем параграф для изображения
                XWPFParagraph imageParagraph = document.createParagraph();
                XWPFRun imageRun = imageParagraph.createRun();
                imageRun.addPicture(photoStream, pictureType, photoFile.getName(), widthEMU, heightEMU);
            }
        } else {
            run.setText("Фото не предоставлено.");
        }

        // Сохраняем документ во временный файл
        File file = File.createTempFile("user_data_", ".docx");
        try (FileOutputStream out = new FileOutputStream(file)) {
            document.write(out);
        }
        document.close();
        return file;
    }

    // Метод для преобразования пикселей в EMU
    private int pxToEMU(int pixels) {
        return pixels * 9525; // 1EMU = 1/9525 дюйма
    }

    // Метод для определения типа изображения
    private int getImageType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "png":
                return XWPFDocument.PICTURE_TYPE_PNG;
            case "jpeg":
            case "jpg":
                return XWPFDocument.PICTURE_TYPE_JPEG;
            case "gif":
                return XWPFDocument.PICTURE_TYPE_GIF;
            default:
                throw new IllegalArgumentException("Unsupported image format: " + extension);
        }
    }



}
