package ar.edu.tpi.campos.dominio;

/**
 * Catalogo cerrado de etapas del cultivo (decision D-12 del equipo, no un requisito del dominio).
 *
 * <p>El dominio exige conocer la "etapa actual" para aplicar RN-09 (etapas prohibidas por producto)
 * y menciona FLORACION como ejemplo. Un valor cerrado permite comparar etapas sin ambiguedad.
 * Se eligieron seis etapas genericas que recorren el ciclo de cualquier cultivo, desde la siembra
 * hasta la madurez, sin depender de escalas especificas de cada especie.
 */
public enum EtapaCultivo {
    SIEMBRA,
    EMERGENCIA,
    VEGETATIVO,
    FLORACION,
    FRUCTIFICACION,
    MADUREZ
}
