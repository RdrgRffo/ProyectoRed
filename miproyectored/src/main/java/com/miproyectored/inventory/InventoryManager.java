package com.miproyectored.inventory;

import com.miproyectored.model.Device;
import com.miproyectored.model.NetworkReport;

import java.sql.*;
import java.util.ArrayList; // Added
import java.util.List;      // Added
import java.util.Map;

public class InventoryManager {
    private static final String DATABASE_URL = "jdbc:sqlite:network_inventory.db"; // Nombre del archivo de la BD

    // Clase interna para representar un ScanReport de la BD
    public static class ScanReport {
        private long reportId;
        private String target;
        private long timestamp;
        private String engineInfo;

        public ScanReport(long reportId, String target, long timestamp, String engineInfo) {
            this.reportId = reportId;
            this.target = target;
            this.timestamp = timestamp;
            this.engineInfo = engineInfo;
        }

        public long getReportId() { return reportId; }
        public String getTarget() { return target; }
        public long getTimestamp() { return timestamp; }
        public String getEngineInfo() { return engineInfo; }
    }

    // Clase interna para representar un DevicePort de la BD
    public static class DevicePort {
        private long portId;
        private long deviceId;
        private int portNumber;
        private String serviceName;
        private String protocol;

        public DevicePort(long portId, long deviceId, int portNumber, String serviceName, String protocol) {
            this.portId = portId;
            this.deviceId = deviceId;
            this.portNumber = portNumber;
            this.serviceName = serviceName;
            this.protocol = protocol;
        }

        public long getPortId() { return portId; }
        public long getDeviceId() { return deviceId; }
        public int getPortNumber() { return portNumber; }
        public String getServiceName() { return serviceName; }
        public String getProtocol() { return protocol; }
    }

    public InventoryManager() {
        createTables();
    }

    private Connection connect() throws SQLException {
        // SQLite creará el archivo si no existe al intentar conectar
        return DriverManager.getConnection(DATABASE_URL);
    }

    private void createTables() {
        // Sentencia SQL para crear la tabla ScanReports
        String sqlScanReports = "CREATE TABLE IF NOT EXISTS ScanReports (" +
                                "report_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "scan_target TEXT NOT NULL," +
                                "scan_timestamp BIGINT NOT NULL," + // Almacenado como milisegundos desde la época
                                "scan_engine_info TEXT" +
                                ");";

        // Sentencia SQL para crear la tabla Devices
        String sqlDevices = "CREATE TABLE IF NOT EXISTS Devices (" +
                            "device_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "report_id INTEGER NOT NULL," +
                            "ip_address TEXT NOT NULL," +
                            "hostname TEXT," +
                            "mac_address TEXT," +
                            "manufacturer TEXT," +
                            "os_details TEXT," +
                            "risk_level TEXT," +
                            "snmp_system_name TEXT," +
                            "snmp_system_description TEXT," +
                            "snmp_system_location TEXT," +
                            "snmp_system_contact TEXT," +
                            "snmp_system_uptime TEXT," +
                            "snmp_interface_description TEXT," +
                            "snmp_interface_speed TEXT," +
                            "snmp_interface_status TEXT," +
                            "FOREIGN KEY (report_id) REFERENCES ScanReports(report_id) ON DELETE CASCADE," +
                            "UNIQUE (report_id, ip_address)" + // Un dispositivo (IP) es único dentro de un reporte
                            ");";

        // Sentencia SQL para crear la tabla DevicePorts
        String sqlDevicePorts = "CREATE TABLE IF NOT EXISTS DevicePorts (" +
                                "port_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "device_id INTEGER NOT NULL," + // Clave foránea a Devices
                                "port_number INTEGER NOT NULL," +
                                "service_name TEXT," +
                                "protocol TEXT DEFAULT 'tcp'," +
                                "FOREIGN KEY (device_id) REFERENCES Devices(device_id) ON DELETE CASCADE," + // Opcional: ON DELETE CASCADE
                                "UNIQUE (device_id, port_number, protocol)" + // Un puerto es único para un dispositivo y protocolo
                                ");";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlScanReports);
            stmt.execute(sqlDevices);
            stmt.execute(sqlDevicePorts);
            System.out.println("Tablas de la base de datos SQLite verificadas/creadas en: " + DATABASE_URL);
        } catch (SQLException e) {
            System.err.println("Error al crear/verificar las tablas: " + e.getMessage());
            // Considerar lanzar una RuntimeException aquí si la creación de tablas es crítica para el inicio
        }
    }

    public long saveReport(NetworkReport report) {
        if (report == null) {
            System.err.println("El reporte es nulo, no se puede guardar en la base de datos.");
            return -1; // Indicar fallo
        }

        String sqlInsertReport = "INSERT INTO ScanReports(scan_target, scan_timestamp, scan_engine_info) VALUES(?,?,?)";
        String sqlInsertDevice = "INSERT INTO Devices(report_id, ip_address, hostname, mac_address, manufacturer, " +
                               "os_details, risk_level, snmp_system_name, snmp_system_description, snmp_system_location, " +
                               "snmp_system_contact, snmp_system_uptime, snmp_interface_description, snmp_interface_speed, " +
                               "snmp_interface_status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        String sqlInsertPort = "INSERT INTO DevicePorts(device_id, port_number, service_name, protocol) VALUES(?,?,?,?)";

        Connection conn = null;
        long reportId = -1;

        try {
            conn = connect();
            conn.setAutoCommit(false); // Iniciar transacción

            // 1. Insertar el ScanReport
            try (PreparedStatement pstmtReport = conn.prepareStatement(sqlInsertReport, Statement.RETURN_GENERATED_KEYS)) {
                pstmtReport.setString(1, report.getScannedNetworkTarget());
                pstmtReport.setLong(2, report.getScanTimestamp());
                pstmtReport.setString(3, report.getScanEngineInfo()); // Puede ser null
                
                int affectedRows = pstmtReport.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Fallo al crear el reporte, no se insertaron filas.");
                }

                try (ResultSet generatedKeys = pstmtReport.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        reportId = generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Fallo al crear el reporte, no se obtuvo ID.");
                    }
                }
            }

            // 2. Insertar cada Device y sus Ports
            if (report.getDevices() != null) {
                for (Device device : report.getDevices()) {
                    long deviceId = -1;
                    try (PreparedStatement pstmtDevice = conn.prepareStatement(sqlInsertDevice, Statement.RETURN_GENERATED_KEYS)) {
                        pstmtDevice.setLong(1, reportId);
                        pstmtDevice.setString(2, device.getIp());
                        pstmtDevice.setString(3, device.getHostname());
                        pstmtDevice.setString(4, device.getMac());
                        pstmtDevice.setString(5, device.getManufacturer());
                        pstmtDevice.setString(6, device.getOs());
                        pstmtDevice.setString(7, device.getRiskLevel());
                        
                        Map<String, String> snmpInfo = device.getSnmpInfo();
                        pstmtDevice.setString(8, snmpInfo.getOrDefault("systemName", null));
                        pstmtDevice.setString(9, snmpInfo.getOrDefault("systemDescription", null));
                        pstmtDevice.setString(10, snmpInfo.getOrDefault("systemLocation", null));
                        pstmtDevice.setString(11, snmpInfo.getOrDefault("systemContact", null));
                        pstmtDevice.setString(12, snmpInfo.getOrDefault("systemUptime", null));
                        pstmtDevice.setString(13, snmpInfo.getOrDefault("interfaceDescription", null));
                        pstmtDevice.setString(14, snmpInfo.getOrDefault("interfaceSpeed", null));
                        pstmtDevice.setString(15, snmpInfo.getOrDefault("interfaceStatus", null));
                        
                        int affectedDeviceRows = pstmtDevice.executeUpdate();
                        if (affectedDeviceRows == 0) {
                            // Podríamos decidir si continuar con otros dispositivos o hacer rollback total
                            System.err.println("Fallo al insertar el dispositivo " + device.getIp() + ". Se omite este dispositivo.");
                            continue; // Saltar al siguiente dispositivo
                        }

                        try (ResultSet generatedDeviceKeys = pstmtDevice.getGeneratedKeys()) {
                            if (generatedDeviceKeys.next()) {
                                deviceId = generatedDeviceKeys.getLong(1);
                            } else {
                                System.err.println("Fallo al obtener ID para el dispositivo " + device.getIp() + ". Se omiten sus puertos.");
                                continue; // Saltar al siguiente dispositivo
                            }
                        }
                    }

                    if (deviceId != -1 && device.getOpenPorts() != null && !device.getOpenPorts().isEmpty()) {
                        try (PreparedStatement pstmtPort = conn.prepareStatement(sqlInsertPort)) {
                            for (Integer portNumber : device.getOpenPorts()) {
                                pstmtPort.setLong(1, deviceId);
                                pstmtPort.setInt(2, portNumber);
                                String serviceName = (device.getServices() != null && device.getServices().get(portNumber) != null) 
                                                     ? device.getServices().get(portNumber) 
                                                     : "Desconocido"; // Cambiado de "Unknown"
                                pstmtPort.setString(3, serviceName);
                                pstmtPort.setString(4, "tcp"); // Asumimos TCP por defecto
                                pstmtPort.addBatch();
                            }
                            pstmtPort.executeBatch();
                        }
                    }
                }
            }

            conn.commit(); // Confirmar todos los cambios si todo fue bien
            System.out.println("Reporte y dispositivos guardados exitosamente en la base de datos. Report ID: " + reportId);

        } catch (SQLException e) {
            System.err.println("Error transaccional al guardar el reporte en la base de datos: " + e.getMessage());
            // e.printStackTrace(); // Para depuración
            if (conn != null) {
                try {
                    System.err.println("Intentando rollback de la transacción.");
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error durante el rollback: " + ex.getMessage());
                }
            }
            reportId = -1; // Indicar fallo
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Restaurar auto-commit
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error al cerrar la conexión: " + e.getMessage());
                }
            }
        }
        return reportId;
    }

    // Aquí podrías añadir métodos para leer datos, ej:
    // public NetworkReport getReportById(long reportId) { ... }
    // public List<Device> getDevicesForReport(long reportId) { ... }
    // public List<NetworkReport> getAllReports() { ... }

    public List<ScanReport> getAllScanReports() {
        List<ScanReport> reports = new ArrayList<>();
        String sql = "SELECT report_id, scan_target, scan_timestamp, scan_engine_info FROM ScanReports ORDER BY scan_timestamp DESC";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                reports.add(new ScanReport(
                        rs.getLong("report_id"),
                        rs.getString("scan_target"),
                        rs.getLong("scan_timestamp"),
                        rs.getString("scan_engine_info")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener todos los reportes de escaneo: " + e.getMessage());
        }
        return reports;
    }

    public List<Device> getAllDevices() {
        List<Device> devices = new ArrayList<>();
        String sql = "SELECT device_id, report_id, ip_address, hostname, mac_address, manufacturer, os_details, " +
                    "risk_level, snmp_system_name, snmp_system_description, snmp_system_location, snmp_system_contact, " +
                    "snmp_system_uptime, snmp_interface_description, snmp_interface_speed, snmp_interface_status " +
                    "FROM Devices ORDER BY device_id";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Device device = new Device(rs.getString("ip_address"));
                device.setHostname(rs.getString("hostname"));
                device.setMacAndManufacturer(rs.getString("mac_address"));
                device.setOs(rs.getString("os_details"));
                device.setRiskLevel(rs.getString("risk_level"));
                
                // Agregar información SNMP
                device.addSnmpInfo("systemName", rs.getString("snmp_system_name"));
                device.addSnmpInfo("systemDescription", rs.getString("snmp_system_description"));
                device.addSnmpInfo("systemLocation", rs.getString("snmp_system_location"));
                device.addSnmpInfo("systemContact", rs.getString("snmp_system_contact"));
                device.addSnmpInfo("systemUptime", rs.getString("snmp_system_uptime"));
                device.addSnmpInfo("interfaceDescription", rs.getString("snmp_interface_description"));
                device.addSnmpInfo("interfaceSpeed", rs.getString("snmp_interface_speed"));
                device.addSnmpInfo("interfaceStatus", rs.getString("snmp_interface_status"));
                
                devices.add(device);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener todos los dispositivos: " + e.getMessage());
        }
        return devices;
    }

    public List<DevicePort> getAllDevicePorts() {
        List<DevicePort> ports = new ArrayList<>();
        String sql = "SELECT port_id, device_id, port_number, service_name, protocol FROM DevicePorts ORDER BY device_id, port_number";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ports.add(new DevicePort(
                        rs.getLong("port_id"),
                        rs.getLong("device_id"),
                        rs.getInt("port_number"),
                        rs.getString("service_name"),
                        rs.getString("protocol")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener todos los puertos de dispositivos: " + e.getMessage());
        }
        return ports;
    }
}