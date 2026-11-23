package com.intifix.modules.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Valida la cabecera X-Internal-Api-Key de la API interna. Falla cerrado: si
 * no hay clave configurada, la API interna no responde a nadie.
 */
@Component
public class InternalApiKeyVerifier {

    public static final String HEADER = "X-Internal-Api-Key";

    private final byte[] apiKey;

    public InternalApiKeyVerifier(@Value("${intifix.internal.api-key:}") String apiKey) {
        this.apiKey = apiKey == null ? new byte[0] : apiKey.getBytes(StandardCharsets.UTF_8);
    }

    public void verificar(String key) {
        if (apiKey.length == 0 || key == null
                || !MessageDigest.isEqual(apiKey, key.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
