package com.intifix.modules.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.intifix.modules.internal.dto.TecnicoRankeado;
import com.intifix.modules.internal.service.CatalogoInternoService;
import com.intifix.modules.services.dto.response.ServicioResponse;
import com.intifix.modules.users.dto.response.ClienteResponse;
import com.intifix.modules.users.service.ClienteService;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * API interna de catálogo y clientes para ai-service. No es pública: el
 * gateway no enruta {@code /internal/**} y cada llamada exige X-Internal-Api-Key.
 */
@Slf4j
@Hidden
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalCatalogoController {

    private final CatalogoInternoService catalogo;
    private final ClienteService clienteService;
    private final InternalApiKeyVerifier apiKeyVerifier;

    @GetMapping("/catalogo/categorias")
    public List<String> categorias(@RequestHeader(value = InternalApiKeyVerifier.HEADER, required = false) String key) {
        apiKeyVerifier.verificar(key);
        return catalogo.categorias();
    }

    @GetMapping("/catalogo/tecnicos")
    public List<TecnicoRankeado> tecnicos(@RequestHeader(value = InternalApiKeyVerifier.HEADER, required = false) String key,
                                          @RequestParam String categoria) {
        apiKeyVerifier.verificar(key);
        return catalogo.tecnicosPorCategoria(categoria);
    }

    @GetMapping("/catalogo/servicios")
    public List<ServicioResponse> servicios(@RequestHeader(value = InternalApiKeyVerifier.HEADER, required = false) String key,
                                            @RequestParam String titulo) {
        apiKeyVerifier.verificar(key);
        return catalogo.serviciosPorTitulo(titulo);
    }

    @GetMapping("/clientes/{idCliente}")
    public ResponseEntity<ClienteResponse> cliente(@RequestHeader(value = InternalApiKeyVerifier.HEADER, required = false) String key,
                                                   @PathVariable UUID idCliente) {
        apiKeyVerifier.verificar(key);
        try {
            return ResponseEntity.ok(clienteService.obtenerClientePorId(idCliente));
        } catch (RuntimeException e) {
            log.debug("Cliente {} no disponible: {}", idCliente, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/clientes/{idCliente}/servicios")
    public List<ServicioResponse> serviciosDeCliente(
            @RequestHeader(value = InternalApiKeyVerifier.HEADER, required = false) String key,
            @PathVariable UUID idCliente) {
        apiKeyVerifier.verificar(key);
        return catalogo.serviciosDeCliente(idCliente);
    }
}
