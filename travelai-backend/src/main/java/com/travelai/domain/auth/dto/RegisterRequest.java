package com.travelai.domain.auth.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegisterRequest(

    @NotBlank(message = "El nom d'usuari és obligatori")
    @Size(min = 3, max = 50, message = "El nom d'usuari ha de tenir entre 3 i 50 caràcters")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "El nom d'usuari només pot contenir lletres, números, punts, guions i guions baixos")
    String username,

    @NotBlank(message = "L'email és obligatori")
    @Email(message = "Format d'email invàlid")
    String email,

    @NotBlank(message = "La contrasenya és obligatòria")
    @Size(min = 8, message = "La contrasenya ha de tenir almenys 8 caràcters")
    String password,

    @NotNull(message = "La data de naixement és obligatòria")
    @Past(message = "La data de naixement ha de ser en el passat")
    LocalDate birthDate,

    // GDPR / LOPD-GDD
    @NotBlank(message = "La versió de la política de privacitat és obligatòria")
    String consentVersion,

    @AssertTrue(message = "Has d'acceptar la política de privacitat")
    Boolean privacyPolicyAccepted,

    @AssertTrue(message = "Has d'acceptar els termes d'ús")
    Boolean termsAccepted,

    @AssertTrue(message = "Has de confirmar que tens 14 anys o més (LOPD-GDD Art. 7)")
    Boolean ageConfirmed,

    // Opcional
    Boolean marketingAccepted

) {}
