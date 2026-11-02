package ar.edu.tpi.misiones.servicio;

import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Serializacion de los datos de auditoria que se guardan como JSON (contexto, motivos, pronostico, ventanas). */
@Component
public class Json {

    private final JsonMapper mapper;

    public Json(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public String escribir(Object valor) {
        return mapper.writeValueAsString(valor);
    }

    public <T> T leer(String json, TypeReference<T> tipo) {
        return mapper.readValue(json, tipo);
    }
}
