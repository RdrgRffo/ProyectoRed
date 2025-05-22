package com.miproyectored.risk;

import com.miproyectored.model.Device;
import com.miproyectored.util.DataNormalizer; // Para acceder a DEFAULT_UNKNOWN

import java.util.Map;

public class RiskAnalyzer {

    /**
     * Calcula el nivel de riesgo de un dispositivo basado en sus características.
     * Esta es una lógica heurística y puede ser expandida.
     * El método es estático ya que no depende del estado de una instancia de RiskAnalyzer.
     * @param device El dispositivo a analizar.
     * @return Un string indicando el nivel de riesgo ("low", "medium", "high").
     */
    public static String calculateRiskLevel(Device device) {
        if (device == null) {
            return DataNormalizer.DEFAULT_UNKNOWN;
        }

        int openPortsCount = device.getOpenPorts() != null ? device.getOpenPorts().size() : 0;
        Map<Integer, String> services = device.getServices();
        // Normalizar OS aquí o asumir que ya viene normalizado si se prefiere
        // DataNormalizer.normalizeString(device.getOs()) podría ser una opción si DataNormalizer es accesible
        // o si el Device ya tiene el OS normalizado.
        // Por ahora, replicamos la lógica original de DataNormalizer para el OS.
        String os = device.getOs() != null ? device.getOs().toLowerCase() : DataNormalizer.DEFAULT_UNKNOWN;


        int criticalServices = 0;
        boolean hasPotentiallyRiskyService = false;

        if (services != null) {
            for (Map.Entry<Integer, String> entry : services.entrySet()) {
                String serviceName = entry.getValue() != null ? entry.getValue().toLowerCase() : "";
                // Servicios inherentemente riesgosos si están expuestos
                if (serviceName.contains("telnet") || // Puerto 23
                    serviceName.contains("ftp") ||    // Puerto 21
                    serviceName.contains("rlogin") || // Puerto 513
                    serviceName.contains("rsh") ||    // Puerto 514
                    (entry.getKey() == 3389 && serviceName.contains("ms-wbt-server")) || // RDP
                    (entry.getKey() == 5900 && serviceName.contains("vnc")) // VNC
                ) {
                    criticalServices++;
                }
                // Servicios que pueden ser vulnerables si no están actualizados
                if (serviceName.contains("http") && !serviceName.contains("https")) { // HTTP
                     hasPotentiallyRiskyService = true;
                }
                if (serviceName.contains("smb") || serviceName.contains("netbios") || serviceName.contains("microsoft-ds")) { // SMB/NetBIOS
                    hasPotentiallyRiskyService = true;
                }
                if (serviceName.contains("ssh") && (serviceName.contains("openssh 6") || serviceName.contains("openssh 5"))) { // Versiones antiguas de SSH
                    hasPotentiallyRiskyService = true;
                }
            }
        }

        if (criticalServices > 0) {
            return "alto";
        }
        if (openPortsCount > 10 || (openPortsCount > 5 && hasPotentiallyRiskyService)) {
            return "alto";
        }
        if (openPortsCount > 3 || hasPotentiallyRiskyService || os.equals(DataNormalizer.DEFAULT_UNKNOWN) || os.contains("windows xp") || os.contains("windows server 2003")) {
            return "medio";
        }
        if (openPortsCount > 0) {
            return "bajo";
        }

        return "bajo"; // Por defecto, si no hay puertos abiertos o información.
    }
}