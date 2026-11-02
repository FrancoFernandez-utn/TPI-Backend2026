package ar.edu.tpi.commons.error;

import java.time.Instant;
import java.util.List;

/**
 * Formato estandar de error que devuelven todos los microservicios.
 *
 * @param timestamp momento en que se produjo el error
 * @param status    codigo HTTP
 * @param error     descripcion corta del codigo HTTP (por ejemplo "Not Found")
 * @param codigo    codigo funcional estable, pensado para que lo lea un cliente (por ejemplo "RECURSO_NO_ENCONTRADO")
 * @param mensaje   explicacion legible para una persona
 * @param path      ruta de la peticion
 * @param detalles  detalles adicionales, por ejemplo un error por cada campo invalido
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String codigo,
        String mensaje,
        String path,
        List<String> detalles) {

    public ErrorResponse {
        detalles = detalles == null ? List.of() : List.copyOf(detalles);
    }

    /** Serializa el error a JSON sin depender de una version concreta de Jackson. */
    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"timestamp\":").append(texto(timestamp.toString())).append(',');
        sb.append("\"status\":").append(status).append(',');
        sb.append("\"error\":").append(texto(error)).append(',');
        sb.append("\"codigo\":").append(texto(codigo)).append(',');
        sb.append("\"mensaje\":").append(texto(mensaje)).append(',');
        sb.append("\"path\":").append(texto(path)).append(',');
        sb.append("\"detalles\":[");
        for (int i = 0; i < detalles.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(texto(detalles.get(i)));
        }
        return sb.append("]}").toString();
    }

    private static String texto(String valor) {
        if (valor == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
