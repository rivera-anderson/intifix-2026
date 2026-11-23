package com.intifix.modules.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.intifix.modules.auth.entity.EstadoUsuario;
import com.intifix.modules.auth.repository.UsuarioAuthRepository;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;

/**
 * API interna para los microservicios. NO es pública: el gateway no enruta
 * {@code /internal/**} y cada llamada exige la cabecera X-Internal-Api-Key.
 *
 * <p>Expone solo lo mínimo que chat-notif-service necesita de los datos que
 * siguen viviendo en el Postgres del monolito.</p>
 */
@Hidden
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalApiController {

    private static final String PARTICIPANTES_SQL = """
            SELECT s.id_servicio, s.id_cliente, a.id_usuario_tecnico
            FROM servicios s
            LEFT JOIN asignaciones_servicio a ON a.id_servicio = s.id_servicio
            WHERE s.id_servicio = ?
            """;

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${intifix.internal.api-key:}")
    private String apiKey;

    public record EstadoUsuarioResponse(boolean existe, boolean activo) {}

    public record ParticipantesResponse(UUID idServicio, UUID idCliente, UUID idTecnico) {}

    /** Si la cuenta existe y puede usar la plataforma (misma regla que el filtro JWT). */
    @GetMapping("/usuarios/{idUsuario}/estado")
    public EstadoUsuarioResponse estadoUsuario(@RequestHeader(value = "X-Internal-Api-Key", required = false) String key,
                                               @PathVariable UUID idUsuario) {
        verificar(key);
        return usuarioAuthRepository.obtenerEstadoConSuspensionPorId(idUsuario)
                .map(info -> new EstadoUsuarioResponse(true, estaActivo(info.getEstado(), info.getSuspensionHasta())))
                .orElse(new EstadoUsuarioResponse(false, false));
    }

    /** Cliente y técnico asignado de un servicio (técnico null si aún no hay asignación). */
    @GetMapping("/servicios/{idServicio}/participantes")
    public ResponseEntity<ParticipantesResponse> participantes(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String key,
            @PathVariable UUID idServicio) {
        verificar(key);
        try {
            return ResponseEntity.ok(jdbcTemplate.queryForObject(PARTICIPANTES_SQL,
                    (rs, rowNum) -> new ParticipantesResponse(
                            rs.getObject("id_servicio", UUID.class),
                            rs.getObject("id_cliente", UUID.class),
                            rs.getObject("id_usuario_tecnico", UUID.class)),
                    idServicio));
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build();
        }
    }

    static boolean estaActivo(EstadoUsuario estado, LocalDateTime suspensionHasta) {
        if (estado == EstadoUsuario.ACTIVO) {
            return true;
        }
        // Suspensión temporal vencida: cuenta como activa (igual que JwtAuthenticationFilter).
        return estado == EstadoUsuario.SUSPENDIDO
                && suspensionHasta != null
                && LocalDateTime.now(ZoneId.systemDefault()).isAfter(suspensionHasta);
    }

    /** Falla cerrado: sin clave configurada, la API interna no responde a nadie. */
    private void verificar(String key) {
        if (apiKey == null || apiKey.isBlank() || key == null
                || !MessageDigest.isEqual(apiKey.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
