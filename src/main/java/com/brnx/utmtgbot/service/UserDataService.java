package com.brnx.utmtgbot.service;

import com.brnx.utmtgbot.model.RegistrationStep;
import com.brnx.utmtgbot.model.UserData;
import com.brnx.utmtgbot.repository.UserDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDataService {
    private final UserDataRepository repository;

    public UserData getOrCreateUser(Long chatId, String utm) {
        return repository.findByChatId(chatId)
                .orElseGet(() -> {
                    UserData userData = new UserData();
                    userData.setChatId(chatId);
                    userData.setUtm(utm);
                    userData.setCurrentStep(RegistrationStep.CONSENT);
                    return repository.save(userData);
                });
    }

    public UserData save(UserData userData) {
        return repository.save(userData);
    }
}
