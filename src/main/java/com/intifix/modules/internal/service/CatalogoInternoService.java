package com.intifix.modules.internal.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.intifix.modules.internal.dto.TecnicoRankeado;
import com.intifix.modules.services.dto.response.ServicioResponse;
import com.intifix.modules.services.service.ServicioService;
import com.intifix.modules.technicians.dto.response.EspecialidadResponse;
import com.intifix.modules.technicians.dto.response.ReputacionResponse;
import com.intifix.modules.technicians.dto.response.TecnicoResponse;
import com.intifix.modules.technicians.entity.Especialidad;
import com.intifix.modules.technicians.enums.EstadoAprobacionTecnico;
import com.intifix.modules.technicians.repository.EspecialidadRepository;
import com.intifix.modules.technicians.service.EspecialidadService;
import com.intifix.modules.technicians.service.ReputacionTecnicoService;
import com.intifix.modules.technicians.service.TecnicoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consultas de catálogo para ai-service. Es la lógica que antes vivía en las
 * tools del módulo ai: se queda en el monolito porque los técnicos, las
 * especialidades y la reputación son datos suyos.
 *
 * <p>Todas las consultas son tolerantes a fallos: devuelven lista vacía.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogoInternoService {

    private static final int LIMITE_BUSQUEDA = 10;

    private final TecnicoService tecnicoService;
    private final EspecialidadService especialidadService;
    private final EspecialidadRepository especialidadRepository;
    private final ReputacionTecnicoService reputacionService;
    private final ServicioService servicioService;

    /** Categorías con técnicos aprobados; si no hay, todas. */
    public List<String> categorias() {
        try {
            List<String> conTecnicos = especialidadRepository.findConTecnicosAprobados()
                    .stream()
                    .map(Especialidad::getNombre)
                    .toList();
            if (!conTecnicos.isEmpty()) {
                return conTecnicos;
            }
            return especialidadRepository.findAll()
                    .stream()
                    .map(Especialidad::getNombre)
                    .sorted()
                    .toList();
        } catch (RuntimeException e) {
            log.warn("No se pudieron listar las categorías con técnicos: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<ServicioResponse> serviciosPorTitulo(String titulo) {
        try {
            return servicioService
                    .buscarServiciosPorTitulo(titulo, PageRequest.of(0, LIMITE_BUSQUEDA))
                    .getContent();
        } catch (RuntimeException e) {
            log.warn("No se pudieron obtener servicios para '{}': {}", titulo, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<ServicioResponse> serviciosDeCliente(UUID idCliente) {
        try {
            return servicioService
                    .obtenerServiciosPorCliente(idCliente, PageRequest.of(0, LIMITE_BUSQUEDA))
                    .getContent();
        } catch (RuntimeException e) {
            log.warn("No se pudo obtener el historial del cliente {}: {}", idCliente, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Técnicos aprobados de la especialidad más parecida, con su reputación. */
    public List<TecnicoRankeado> tecnicosPorCategoria(String categoria) {
        UUID idEspecialidad = resolverEspecialidad(categoria);
        if (idEspecialidad == null) {
            log.warn("No se encontró especialidad para '{}' ni por coincidencia parcial", categoria);
            return Collections.emptyList();
        }
        try {
            return tecnicoService
                    .buscarTecnicosPorEspecialidad(idEspecialidad, PageRequest.of(0, LIMITE_BUSQUEDA))
                    .getContent()
                    .stream()
                    .filter(t -> t.getEstadoAprobacion() == EstadoAprobacionTecnico.APROBADO)
                    .map(this::enriquecerConReputacion)
                    .toList();
        } catch (RuntimeException e) {
            log.warn("Error al buscar técnicos para especialidad {}: {}", idEspecialidad, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Búsqueda tolerante de la especialidad:
     * 1. match exacto, 2. LIKE con la palabra clave, 3. LIKE con cada palabra significativa.
     */
    private UUID resolverEspecialidad(String keyword) {
        try {
            EspecialidadResponse exacta = especialidadService.obtenerEspecialidadPorNombre(keyword);
            return exacta.getIdEspecialidad();
        } catch (RuntimeException ignored) {
            // sin match exacto: sigue con la búsqueda parcial
        }

        List<Especialidad> candidatas = especialidadRepository.buscarPorNombre(keyword);
        if (!candidatas.isEmpty()) {
            Especialidad mejor = candidatas.get(0);
            log.info("Match parcial: '{}' → '{}'", keyword, mejor.getNombre());
            return mejor.getIdEspecialidad();
        }

        String[] palabras = keyword.toLowerCase().split("[\\s,y]+");
        for (String palabra : palabras) {
            if (palabra.length() < 3) {
                continue;
            }
            List<Especialidad> sub = especialidadRepository.buscarPorNombre(palabra);
            if (!sub.isEmpty()) {
                log.info("Match por token '{}': '{}'", palabra, sub.get(0).getNombre());
                return sub.get(0).getIdEspecialidad();
            }
        }
        return null;
    }

    private TecnicoRankeado enriquecerConReputacion(TecnicoResponse tecnico) {
        try {
            if (reputacionService.existeReputacion(tecnico.getIdUsuario())) {
                ReputacionResponse rep = reputacionService.obtenerReputacion(tecnico.getIdUsuario());
                return TecnicoRankeado.of(tecnico, rep);
            }
        } catch (RuntimeException e) {
            log.warn("No se pudo obtener reputación del técnico {}: {}", tecnico.getIdUsuario(), e.getMessage());
        }
        return TecnicoRankeado.sinReputacion(tecnico);
    }
}
