package ar.edu.tpi.misiones.dominio;

/**
 * Etapas del cultivo tal como las informa ms-campos en el contexto del lote.
 * Cada servicio define su propio tipo para el contrato: no se comparte en commons
 * porque es un concepto del dominio, no infraestructura.
 */
public enum EtapaCultivo {
    SIEMBRA,
    EMERGENCIA,
    VEGETATIVO,
    FLORACION,
    FRUCTIFICACION,
    MADUREZ
}
