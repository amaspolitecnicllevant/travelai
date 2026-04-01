package com.travelai.domain.legal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Cos de la petició POST /api/my-data/consent.
 * Registra un nou consentiment de l'usuari autenticat.
 */
public record ConsentRequest(

    /** Tipus: PRIVACY_POLICY, TERMS, MARKETING, COOKIES */
    @NotBlank String consentType,

    @NotBlank String consentVersion,

    @NotNull Boolean accepted,

    /** Timestamp del moment en que l'usuari ha clicat "Acceptar" al frontend. */
    Instant acceptedAt
) {}
