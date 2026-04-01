package com.travelai.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

    @NotBlank(message = "L'email és obligatori")
    @Email(message = "Format d'email invàlid")
    String email,

    @NotBlank(message = "La contrasenya és obligatòria")
    String password

) {}
