package com.miproyectored.util;

import com.miproyectored.model.Device;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DataNormalizer {

    public static final String DEFAULT_UNKNOWN = "unknown"; // Cambiado a public static final
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault());

    /**
     * Normaliza un string: lo convierte a minúsculas y maneja nulos/vacíos.
     * @param input El string a normalizar.
     * @return El string normalizado o DEFAULT_UNKNOWN si es nulo/vacío.
     */
    public String normalizeString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return DEFAULT_UNKNOWN;
        }
        return input.trim().toLowerCase();
    }

    /**
     * Normaliza una dirección MAC: la convierte a mayúsculas y maneja nulos/vacíos.
     * @param mac La dirección MAC a normalizar.
     * @return La dirección MAC normalizada o DEFAULT_UNKNOWN si es nula/vacía.
     */
    public String normalizeMacAddress(String mac) {
        if (mac == null || mac.trim().isEmpty()) {
            return DEFAULT_UNKNOWN;
        }
        return mac.trim().toUpperCase();
    }

    /**
     * Formatea un timestamp (long) a un string de fecha legible.
     * @param timestamp El timestamp en milisegundos.
     * @return El string de fecha formateada.
     */
    public String formatTimestamp(long timestamp) {
        if (timestamp <= 0) {
            return DEFAULT_UNKNOWN;
        }
        return DATE_FORMATTER.format(new Date(timestamp));
    }

}