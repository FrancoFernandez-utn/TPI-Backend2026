package ar.edu.tpi.misiones.dominio;

/**
 * Estado administrativo del piloto. La vigencia de la licencia no se guarda como estado:
 * RN-01 compara la fecha de vencimiento con el dia del vuelo.
 */
public enum EstadoPiloto {
    ACTIVO,
    SUSPENDIDO
}
