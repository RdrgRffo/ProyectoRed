package com.miproyectored.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class MacManufacturerManager {
    private final Map<String, String> manufacturerDatabase;
    private static final Pattern MAC_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");

    public MacManufacturerManager() {
        this.manufacturerDatabase = new HashMap<>();
        initializeDatabase();
    }

    private void initializeDatabase() {
        // Agregamos algunos fabricantes comunes y sus prefijos MAC
        // Formato: primeros 6 caracteres de la MAC (OUI - Organizationally Unique Identifier)
        manufacturerDatabase.put("00:00:0C", "Cisco Systems");
        manufacturerDatabase.put("00:1A:11", "Google");
        manufacturerDatabase.put("00:23:AB", "Apple Inc."); // Apple
        manufacturerDatabase.put("00:1B:63", "Apple Inc."); // Apple (otro OUI)
        manufacturerDatabase.put("00:50:56", "VMware, Inc.");
        manufacturerDatabase.put("00:0C:29", "VMware, Inc."); // VMware (otro OUI)
        manufacturerDatabase.put("08:00:27", "Oracle VirtualBox");
        manufacturerDatabase.put("DC:A6:32", "Raspberry Pi Foundation"); // Raspberry Pi
        manufacturerDatabase.put("B8:27:EB", "Raspberry Pi Foundation"); // Raspberry Pi (otro OUI)
        manufacturerDatabase.put("00:25:90", "Super Micro Computer, Inc.");
        manufacturerDatabase.put("00:1B:21", "Intel Corporate");
        manufacturerDatabase.put("9C:B6:D0", "Intel Corporate");
        manufacturerDatabase.put("00:16:32", "Samsung Electronics Co.,Ltd");
        manufacturerDatabase.put("00:14:22", "Dell Inc.");
        manufacturerDatabase.put("00:0F:20", "Hewlett Packard");
        manufacturerDatabase.put("3C:D9:2B", "Hewlett Packard Enterprise");
        manufacturerDatabase.put("00:1D:D8", "Microsoft Corporation");
        manufacturerDatabase.put("14:CC:20", "TP-LINK TECHNOLOGIES CO.,LTD.");
        manufacturerDatabase.put("00:09:5B", "NETGEAR");
        manufacturerDatabase.put("00:0C:6E", "ASUSTek COMPUTER INC.");
        manufacturerDatabase.put("00:E0:4C", "Realtek Semiconductor Corp.");
        manufacturerDatabase.put("00:10:18", "Broadcom");
    }

    public String getManufacturer(String macAddress) {
        if (macAddress == null || macAddress.trim().isEmpty()) {
            return "Desconocido";
        }

        // Normalizar la dirección MAC
        macAddress = normalizeMacAddress(macAddress);

        // Validar el formato de la MAC
        if (!isValidMacAddress(macAddress)) {
            return "Formato MAC inválido"; // Esto es un error específico, no "desconocido" genérico
        }

        // Obtener los primeros 8 caracteres (incluyendo los :)
        String oui = macAddress.substring(0, 8).toUpperCase();

        // Buscar en la base de datos
        return manufacturerDatabase.getOrDefault(oui, "Desconocido"); // Cambiado de "Fabricante no identificado"
    }

    public void addManufacturer(String macPrefix, String manufacturer) {
        if (macPrefix != null && manufacturer != null && !macPrefix.trim().isEmpty()) {
            manufacturerDatabase.put(macPrefix.toUpperCase(), manufacturer);
        }
    }

    private String normalizeMacAddress(String macAddress) {
        // Eliminar espacios y convertir a mayúsculas
        return macAddress.trim().toUpperCase()
                // Normalizar separadores a ':'
                .replace("-", ":")
                // Asegurar formato XX:XX:XX
                .replaceAll("([0-9A-F]{2})([0-9A-F]{2})", "$1:$2");
    }

    private boolean isValidMacAddress(String macAddress) {
        return MAC_PATTERN.matcher(macAddress).matches();
    }

    public Map<String, String> getAllManufacturers() {
        return new HashMap<>(manufacturerDatabase);
    }
}