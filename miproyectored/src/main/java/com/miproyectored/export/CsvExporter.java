package com.miproyectored.export;

import com.miproyectored.model.Device;
import com.miproyectored.model.NetworkReport;
import com.miproyectored.util.DataNormalizer; // Asumiendo que lo necesitas para formatear

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CsvExporter {

    private final DataNormalizer dataNormalizer;

    public CsvExporter() {
        this.dataNormalizer = new DataNormalizer(); // Para formatear datos si es necesario
    }

    public void exportReportToFile(NetworkReport report, String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Encabezado del reporte CSV
            writer.println("Reporte de Escaneo de Red");
            writer.println("Objetivo del Escaneo:," + report.getScannedNetworkTarget());
            writer.println("Fecha del Escaneo:," + dataNormalizer.formatTimestamp(report.getScanTimestamp()));
            writer.println("Dispositivos Encontrados:," + report.getDeviceCount());
            writer.println(); // Línea en blanco

            // Encabezados de la tabla de dispositivos
            writer.println("IP,Hostname,MAC,Fabricante,OS,Puertos Abiertos,Servicios,Nivel de Riesgo");

            if (report.getDevices() != null) {
                for (Device device : report.getDevices()) {
                    String ip = device.getIp() != null ? device.getIp() : "N/A";
                    String hostname = device.getHostname() != null ? device.getHostname() : "N/A";
                    String mac = device.getMac() != null ? device.getMac() : "N/A";
                    String manufacturer = device.getManufacturer() != null ? device.getManufacturer() : "N/A";
                    String os = device.getOs() != null ? device.getOs() : "N/A";
                    
                    String openPortsStr = "N/A";
                    if (device.getOpenPorts() != null && !device.getOpenPorts().isEmpty()) {
                        openPortsStr = device.getOpenPorts().stream()
                                             .map(String::valueOf)
                                             .collect(Collectors.joining("; ")); // Puertos separados por ;
                    }

                    String servicesStr = "N/A";
                    if (device.getServices() != null && !device.getServices().isEmpty()) {
                        servicesStr = device.getServices().entrySet().stream()
                                            .map(entry -> entry.getKey() + ":" + entry.getValue().replace(",", ";")) // Reemplazar comas en servicios
                                            .collect(Collectors.joining(" | ")); // Servicios separados por |
                    }
                    
                    String riskLevel = device.getRiskLevel() != null ? device.getRiskLevel() : "N/A";

                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                            ip, hostname, mac, manufacturer, os, openPortsStr, servicesStr, riskLevel);
                }
            }
            System.out.println("Reporte CSV exportado exitosamente a: " + filePath);
        } catch (IOException e) {
            System.err.println("Error al exportar el reporte a CSV (" + filePath + "): " + e.getMessage());
        }
    }
}