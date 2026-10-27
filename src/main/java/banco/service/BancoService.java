package banco.service;

import banco.repository.CuentaRepository;
import banco.repository.TransferenciaRepository;
import banco.repository.IncidenciaRepository;
import banco.dto.CuentaRespuesta;
import banco.dto.SolicitudTransferencia;
import banco.model.Cuenta;
import banco.model.Transferencia;
import banco.model.Incidencia;
import banco.model.CambioIncidencia;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BancoService {
    private final ServicioTransferencias transferencias;
    private final CuentaRepository cuentas;
    private final TransferenciaRepository historial;
    private final IncidenciaRepository incidencias;
    private final ReglaImporteElevado reglaImporte;

    public BancoService(ServicioTransferencias transferencias, CuentaRepository cuentas,
                        TransferenciaRepository historial, IncidenciaRepository incidencias,
                        ReglaImporteElevado reglaImporte) {
        this.transferencias = transferencias;
        this.cuentas = cuentas;
        this.historial = historial;
        this.incidencias = incidencias;
        this.reglaImporte = reglaImporte;
    }

    @Transactional(readOnly = true)
    public List<CuentaRespuesta> listarCuentas() {
        return cuentas.listar().stream().map(CuentaRespuesta::de).toList();
    }

    @Transactional(readOnly = true)
    public List<Transferencia> listarTransferencias() {
        return historial.listar();
    }

    @Transactional(readOnly = true)
    public List<Incidencia> listarIncidencias() {
        return incidencias.listar();
    }

    @Transactional(readOnly = true)
    public Incidencia consultarIncidencia(long id) {
        return incidencias.buscar(id);
    }

    @Transactional(readOnly = true)
    public List<CambioIncidencia> consultarHistorialIncidencia(long id) {
        incidencias.buscar(id);
        return incidencias.listarHistorial(id);
    }

    @Transactional
    public Incidencia iniciarRevision(long id) {
        incidencias.buscar(id);
        if (!incidencias.iniciarRevision(id)) {
            throw new EstadoIncidenciaException("Solo se puede iniciar la revisión de una incidencia PENDIENTE.");
        }
        incidencias.registrarInicioRevision(id);
        return incidencias.buscar(id);
    }

    @Transactional
    public Incidencia resolverIncidencia(long id, String comentario) {
        if (comentario == null || comentario.isBlank() || comentario.length() > 1000) {
            throw new IllegalArgumentException("Indica un comentario de resolución de entre 1 y 1000 caracteres, no solo espacios.");
        }
        incidencias.buscar(id);
        if (!incidencias.resolver(id)) {
            throw new EstadoIncidenciaException("Solo se puede resolver una incidencia EN_REVISION.");
        }
        incidencias.registrarResolucion(id, comentario.strip());
        return incidencias.buscar(id);
    }

    // Saldos, transferencia e incidencia (si procede) se confirman juntos.
    // Si falla alguna, Spring revierte las escrituras de esta transacción.
    @Transactional
    public List<CuentaRespuesta> transferir(SolicitudTransferencia solicitud) {
        BigDecimal importe;
        try {
            importe = solicitud.importe().setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("El importe debe tener como máximo dos decimales.");
        }
        if (importe.precision() > 17) {
            throw new IllegalArgumentException("El importe excede el límite permitido.");
        }
        long origenId = solicitud.cuentaOrigenId();
        long destinoId = solicitud.cuentaDestinoId();
        if (origenId == destinoId) {
            throw new IllegalArgumentException("Las cuentas deben ser distintas.");
        }

        // Bloquear siempre por ID ascendente evita bloqueos mutuos en envíos inversos.
        Cuenta primera = cuentas.buscarParaActualizar(Math.min(origenId, destinoId));
        Cuenta segunda = cuentas.buscarParaActualizar(Math.max(origenId, destinoId));
        Cuenta origen = primera.getId() == origenId ? primera : segunda;
        Cuenta destino = primera.getId() == destinoId ? primera : segunda;
        if (destino.getSaldo().add(importe).compareTo(new BigDecimal("99999999999999999.99")) > 0) {
            throw new IllegalArgumentException("El saldo de destino excedería el límite permitido.");
        }
        transferencias.transferir(origen, destino, importe);
        cuentas.guardarSaldo(origen);
        cuentas.guardarSaldo(destino);
        long transferenciaId = historial.registrar(origen, destino, importe);
        if (reglaImporte.seCumple(importe, origen.getMoneda().getCurrencyCode())) {
            incidencias.registrar(transferenciaId, ReglaImporteElevado.CODIGO, reglaImporte.motivo(importe));
        }
        return List.of(CuentaRespuesta.de(origen), CuentaRespuesta.de(destino));
    }
}
