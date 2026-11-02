package ar.edu.tpi.misiones.motor;

/** Duracion estimada de una mision (RN-14). */
public record Duracion(int vuelos, int minutos) {

    /** Cantidad de franjas horarias completas que la ventana tiene que cubrir. */
    public int franjasNecesarias() {
        return (minutos + 59) / 60;
    }
}
