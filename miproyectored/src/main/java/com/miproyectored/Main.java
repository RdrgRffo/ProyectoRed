package com.miproyectored;

import com.miproyectored.export.CsvExporter; // Nueva importación
import com.miproyectored.export.HtmlExporter; // Nueva importación
import com.miproyectored.export.JsonExporter;
import com.miproyectored.inventory.InventoryManager;
import com.miproyectored.model.Device;
import com.miproyectored.model.NetworkReport;
import com.miproyectored.scanner.NmapScanner;
import com.miproyectored.util.DataNormalizer;
import com.miproyectored.util.NetworkUtils;
import java.io.File; // Nueva importación
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {

    private static final String REPORTS_DIR = "reports"; // Directorio para los reportes

    public static void main(String[] args) {
        System.out.println("Iniciando MiProyectoRed...");

        // Crear directorio de reportes si no existe
        File reportsDir = new File(REPORTS_DIR);
        if (!reportsDir.exists()) {
            if (reportsDir.mkdirs()) {
                System.out.println("Directorio de reportes creado en: " + reportsDir.getAbsolutePath());
            } else {
                System.err.println("No se pudo crear el directorio de reportes: " + reportsDir.getAbsolutePath());
                // Considerar si continuar o salir si el directorio es crucial
            }
        }


        // 1. Instanciar componentes principales
        NmapScanner scanner = new NmapScanner(); 
        JsonExporter jsonExporter = new JsonExporter();
        CsvExporter csvExporter = new CsvExporter(); // Nuevo
        HtmlExporter htmlExporter = new HtmlExporter(); // Nuevo
        InventoryManager inventoryManager = new InventoryManager();
        DataNormalizer dataNormalizer = new DataNormalizer(); 

        // 2. Detectar redes locales para escanear
        List<String> networksToScan = NetworkUtils.detectLocalNetworks();

        if (networksToScan == null || networksToScan.isEmpty()) {
            System.out.println("No se pudieron detectar redes locales automáticamente.");
            networksToScan = new ArrayList<>();
            networksToScan.add("scanme.nmap.org"); 
            System.out.println("Se escaneará '" + networksToScan.get(0) + "' como objetivo por defecto.");
        }

        System.out.println("Se escanearán las siguientes redes/objetivos: " + networksToScan);
        int reportFileCounter = 1; 

        for (String targetNetwork : networksToScan) {
            System.out.println("\n========================================================");
            System.out.println("Iniciando escaneo para el objetivo: " + targetNetwork);
            System.out.println("========================================================");

            List<Device> detectedDevices = scanner.scan(targetNetwork);

            NetworkReport report = new NetworkReport();
            report.setScannedNetworkTarget(targetNetwork);
            // El timestamp se establece automáticamente en el constructor de NetworkReport
            // report.setScanEngineInfo(scanner.getNmapVersion()); // Si NmapScanner proveyera esta info

            if (detectedDevices != null) {
                for (Device device : detectedDevices) {
                    // El nivel de riesgo y la normalización ya se aplicaron en NmapScanner
                    report.addDevice(device);
                }
            }

            System.out.println("\n--- Guardando Reporte en Base de Datos ---");
            long reportId = inventoryManager.saveReport(report); 
            if (reportId != -1) {
                System.out.println("Reporte para " + targetNetwork + " guardado en BD con ID: " + reportId);
            } else {
                System.err.println("Fallo al guardar el reporte para " + targetNetwork + " en la BD.");
            }

            // Preparar nombre base para los archivos de reporte
            String safeTargetNetworkName = targetNetwork.replaceAll("[^a-zA-Z0-9.-]", "_");
            String baseReportFileName = "reporte_escaneo_" + safeTargetNetworkName + "_" + reportFileCounter;

            // Exportar a JSON
            System.out.println("\n--- Exportando Reporte a Archivo JSON ---");
            String jsonReportFileName = REPORTS_DIR + File.separator + baseReportFileName + ".json";
            jsonExporter.exportReportToFile(report, jsonReportFileName);
            // System.out.println("Reporte JSON exportado a: " + jsonReportFileName); // Mensaje ya en JsonExporter

            // Exportar a CSV
            System.out.println("\n--- Exportando Reporte a Archivo CSV ---");
            String csvReportFileName = REPORTS_DIR + File.separator + baseReportFileName + ".csv";
            csvExporter.exportReportToFile(report, csvReportFileName);
            // System.out.println("Reporte CSV exportado a: " + csvReportFileName); // Mensaje ya en CsvExporter

            // Exportar a HTML
            System.out.println("\n--- Exportando Reporte a Archivo HTML ---");
            String htmlReportFileName = REPORTS_DIR + File.separator + baseReportFileName + ".html";
            htmlExporter.exportReportToFile(report, htmlReportFileName);
            // System.out.println("Reporte HTML exportado a: " + htmlReportFileName); // Mensaje ya en HtmlExporter
            
            reportFileCounter++;

            System.out.println("\n--- Reporte del Escaneo para: " + report.getScannedNetworkTarget() + " ---");
            // Usar DataNormalizer para formatear la fecha
            System.out.println("Fecha del escaneo: " + dataNormalizer.formatTimestamp(report.getScanTimestamp()));
            System.out.println("Objetivo: " + report.getScannedNetworkTarget());
            System.out.println("Dispositivos encontrados: " + report.getDeviceCount());

            if (report.getDevices() != null && !report.getDevices().isEmpty()) {
                System.out.println("\nDetalles de los dispositivos:");
                for (Device device : report.getDevices()) {
                    System.out.println("------------------------------------");
                    System.out.println("  IP: " + device.getIp());
                    // Los datos ya vienen normalizados desde NmapScanner
                    if (device.getHostname() != null && !device.getHostname().isEmpty() && !device.getHostname().equals(device.getIp().toLowerCase()) && !device.getHostname().equals("unknown")) {
                        System.out.println("  Hostname: " + device.getHostname());
                    }
                    if (device.getMac() != null && !device.getMac().isEmpty() && !device.getMac().equals("UNKNOWN")) {
                        System.out.print("  MAC: " + device.getMac());
                        if (device.getManufacturer() != null && !device.getManufacturer().isEmpty() && !device.getManufacturer().equals("unknown")) {
                            System.out.print(" (" + device.getManufacturer() + ")");
                        }
                        System.out.println();
                    }
                    if (device.getOs() != null && !device.getOs().isEmpty() && !device.getOs().equals("unknown")) {
                        System.out.println("  OS: " + device.getOs());
                    }
                    if (device.getOpenPorts() != null && !device.getOpenPorts().isEmpty()) {
                        System.out.println("  Puertos abiertos: " + device.getOpenPorts().size() + " " + device.getOpenPorts());
                        if (device.getServices() != null && !device.getServices().isEmpty()) {
                            System.out.println("  Servicios detectados: ");
                            for (Map.Entry<Integer, String> entry : device.getServices().entrySet()) {
                                System.out.println("    - Puerto " + entry.getKey() + "/tcp: " + entry.getValue());
                            }
                        }
                    } else {
                        System.out.println("  No se detectaron puertos abiertos.");
                    }
                    // Mostrar el nivel de riesgo
                    if (device.getRiskLevel() != null && !device.getRiskLevel().isEmpty()){
                        System.out.println("  Nivel de Riesgo: " + device.getRiskLevel().toUpperCase()); // Mostrar en mayúsculas para destacar
                    }
                }
                System.out.println("------------------------------------");
            } else {
                System.out.println("No se encontraron dispositivos activos o con información relevante para " + targetNetwork + ".");
            }
            System.out.println("\nEscaneo para " + targetNetwork + " finalizado.");
        }

        System.out.println("\n========================================================");
        System.out.println("Todos los escaneos han finalizado.");
        System.out.println("Se ha generado la base de datos 'network_inventory.db' en el directorio del proyecto.");
        System.out.println("========================================================");
    }
}