package com.miproyectored.export;

import com.miproyectored.model.Device;
import com.miproyectored.model.NetworkReport;
import com.miproyectored.util.DataNormalizer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HtmlExporter {

    private final DataNormalizer dataNormalizer;

    public HtmlExporter() {
        this.dataNormalizer = new DataNormalizer();
    }

    public void exportReportToFile(NetworkReport report, String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html lang=\"es\">");
            writer.println("<head>");
            writer.println("    <meta charset=\"UTF-8\">");
            writer.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
            writer.println("    <title>Reporte de Escaneo de Red</title>");
            writer.println("    <style>");
            writer.println("        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f4f4f4; color: #333; }");
            writer.println("        .container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }");
            writer.println("        h1, h2 { color: #333; }");
            writer.println("        table { width: 100%; border-collapse: collapse; margin-top: 20px; }");
            writer.println("        th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }");
            writer.println("        th { background-color: #007bff; color: white; }");
            writer.println("        tr:nth-child(even) { background-color: #f2f2f2; }");
            writer.println("        .device-details { margin-bottom: 30px; }");
            writer.println("        .summary p { margin: 5px 0; }");
            writer.println("    </style>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("    <div class=\"container\">");
            writer.println("        <h1>Reporte de Escaneo de Red</h1>");
            writer.println("        <div class=\"summary\">");
            writer.println("            <p><strong>Objetivo del Escaneo:</strong> " + escapeHtml(report.getScannedNetworkTarget()) + "</p>");
            writer.println("            <p><strong>Fecha del Escaneo:</strong> " + escapeHtml(dataNormalizer.formatTimestamp(report.getScanTimestamp())) + "</p>");
            writer.println("            <p><strong>Dispositivos Encontrados:</strong> " + report.getDeviceCount() + "</p>");
            writer.println("        </div>");

            if (report.getDevices() != null && !report.getDevices().isEmpty()) {
                writer.println("        <h2>Detalles de los Dispositivos</h2>");
                writer.println("        <table>");
                writer.println("            <thead>");
                writer.println("                <tr>");
                writer.println("                    <th>IP</th>");
                writer.println("                    <th>Hostname</th>");
                writer.println("                    <th>MAC</th>");
                writer.println("                    <th>Fabricante</th>");
                writer.println("                    <th>OS</th>");
                writer.println("                    <th>Puertos Abiertos</th>");
                writer.println("                    <th>Servicios</th>");
                writer.println("                    <th>Nivel de Riesgo</th>");
                writer.println("                </tr>");
                writer.println("            </thead>");
                writer.println("            <tbody>");

                for (Device device : report.getDevices()) {
                    writer.println("                <tr>");
                    writer.println("                    <td>" + escapeHtml(device.getIp()) + "</td>");
                    writer.println("                    <td>" + escapeHtml(device.getHostname()) + "</td>");
                    writer.println("                    <td>" + escapeHtml(device.getMac()) + "</td>");
                    writer.println("                    <td>" + escapeHtml(device.getManufacturer()) + "</td>");
                    writer.println("                    <td>" + escapeHtml(device.getOs()) + "</td>");
                    
                    String openPortsStr = "N/A";
                    if (device.getOpenPorts() != null && !device.getOpenPorts().isEmpty()) {
                        openPortsStr = device.getOpenPorts().stream().map(String::valueOf).collect(Collectors.joining(", "));
                    }
                    writer.println("                    <td>" + escapeHtml(openPortsStr) + "</td>");

                    String servicesStr = "N/A";
                    if (device.getServices() != null && !device.getServices().isEmpty()) {
                        servicesStr = device.getServices().entrySet().stream()
                                            .map(entry -> "Puerto " + entry.getKey() + ": " + escapeHtml(entry.getValue()))
                                            .collect(Collectors.joining("<br>"));
                    }
                    writer.println("                    <td>" + servicesStr + "</td>");
                    writer.println("                    <td>" + escapeHtml(device.getRiskLevel()) + "</td>");
                    writer.println("                </tr>");
                }
                writer.println("            </tbody>");
                writer.println("        </table>");
            } else {
                writer.println("        <p>No se encontraron dispositivos activos o con información relevante.</p>");
            }

            writer.println("    </div>");
            writer.println("</body>");
            writer.println("</html>");
            System.out.println("Reporte HTML exportado exitosamente a: " + filePath);
        } catch (IOException e) {
            System.err.println("Error al exportar el reporte a HTML (" + filePath + "): " + e.getMessage());
        }
    }

    // Método simple para escapar caracteres HTML
    private String escapeHtml(String text) {
        if (text == null) return "N/A";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}