package com.brnx.utmtgbot.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
public class UserData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long chatId;
    private String utm;
    private String fullName;
    private LocalDate birthDate;
    private String gender;
    private String photoPath;
    private boolean consentGiven;
    @Enumerated(EnumType.STRING)
    private RegistrationStep currentStep;
}
