package ar.edu.tpi.misiones.dominio;

/** Motivo de rechazo o advertencia de una decision, con un codigo estable y un detalle legible. */
public record Motivo(String codigo, String detalle) {

    public static final String ETAPA_PROHIBIDA = "ETAPA_PROHIBIDA";
    public static final String INTERVALO_MINIMO = "INTERVALO_MINIMO";
    public static final String SIN_DRON_APTO = "SIN_DRON_APTO";
    public static final String SIN_VENTANA_CLIMATICA = "SIN_VENTANA_CLIMATICA";
    public static final String SIN_DRON_DISPONIBLE = "SIN_DRON_DISPONIBLE";
    public static final String SIN_PILOTO_DISPONIBLE = "SIN_PILOTO_DISPONIBLE";
}
