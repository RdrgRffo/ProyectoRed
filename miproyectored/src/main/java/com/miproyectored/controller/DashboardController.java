package com.miproyectored.controller; // Asegúrate que este es el paquete correcto según tu estructura

import com.miproyectored.export.*;
import com.miproyectored.inventory.InventoryManager;
import com.miproyectored.model.Device;
import com.miproyectored.model.NetworkReport;
import com.miproyectored.scanner.NmapScanner;
import com.miproyectored.util.DataNormalizer;
import com.miproyectored.util.NetworkUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.TableCell;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
// import javafx.scene.layout.VBox; // No parece estar usado directamente
// import javafx.stage.FileChooser; // No se usará más para exportar individualmente

import java.awt.Desktop; // Necesario para abrir carpetas
import java.io.File;
import java.io.IOException; // Necesario para manejar excepciones de Desktop.open
import java.util.List;
import java.util.Map;

public class DashboardController {
    private static final String REPORTS_DIR = "reports";
    private final NmapScanner scanner = new NmapScanner();
    // Los exportadores individuales ya no se usarán para botones directos
    // private final JsonExporter jsonExporter = new JsonExporter();
    // private final CsvExporter csvExporter = new CsvExporter();
    // private final HtmlExporter htmlExporter = new HtmlExporter();
    private final InventoryManager inventoryManager = new InventoryManager();
    private final DataNormalizer dataNormalizer = new DataNormalizer();

    @FXML
    private Label mainTitleLabel; // Asume que tienes un Label con fx:id="mainTitleLabel" en tu FXML
    @FXML
    private TextArea outputArea;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Button scanButton;
    @FXML
    private Label targetNetworkLabel; // Reemplaza al ComboBox
    @FXML
    private Label statusLabel;
    @FXML
    private TabPane databaseTabPane;
    @FXML
    private Tab scanReportsTab;
    @FXML
    private Tab devicesTab;
    @FXML
    private Tab devicePortsTab;
    @FXML
    private TableView<InventoryManager.ScanReport> scanReportsTable; // Calificado si es necesario aquí
    @FXML
    private TableView<Device> devicesTable;
    @FXML
    private TableView<InventoryManager.DevicePort> devicePortsTable; // Calificado si es necesario aquí
    
    private NetworkReport currentReport; // Se sigue usando para el escaneo actual y la salida en outputArea


    @FXML
    public void initialize() {
        // Cambiar el título principal de la UI (si existe el Label)
        if (mainTitleLabel != null) {
            mainTitleLabel.setText("NetScan 2025"); // Modificado: "v1" eliminado
        } else {
            System.out.println("Advertencia: mainTitleLabel no está inyectado. El título no se cambiará desde el controlador.");
            // El título de la ventana (Stage) se establece generalmente en la clase Application.
        }

        // Detectar y mostrar la red local
        List<String> networks = NetworkUtils.detectLocalNetworks();
        String networkToDisplay;
        if (networks != null && !networks.isEmpty()) {
            networkToDisplay = networks.get(0); // Usar la primera red detectada
            if (networks.size() > 1) {
                statusLabel.setText("Múltiples redes detectadas. Usando: " + networkToDisplay);
            }
        } else {
            networkToDisplay = "scanme.nmap.org"; // Valor por defecto si no se detectan redes
            statusLabel.setText("No se detectaron redes locales. Usando: " + networkToDisplay);
        }
        targetNetworkLabel.setText(networkToDisplay);

        // Inicializar tablas de la base de datos
        initializeDatabaseTables();
        loadDatabaseData();
    }

    private void initializeDatabaseTables() {
        // Configurar tabla de ScanReports
        TableColumn<InventoryManager.ScanReport, Long> reportIdCol = new TableColumn<>("ID"); 
        reportIdCol.setCellValueFactory(new PropertyValueFactory<>("reportId"));

        TableColumn<InventoryManager.ScanReport, String> targetCol = new TableColumn<>("Target"); 
        targetCol.setCellValueFactory(new PropertyValueFactory<>("target"));

        TableColumn<InventoryManager.ScanReport, String> dateCol = new TableColumn<>("Date"); 
        dateCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(dataNormalizer.formatTimestamp(cellData.getValue().getTimestamp())));

        TableColumn<InventoryManager.ScanReport, String> engineCol = new TableColumn<>("Engine Info"); 
        engineCol.setCellValueFactory(new PropertyValueFactory<>("engineInfo"));

        scanReportsTable.getColumns().setAll(reportIdCol, targetCol, dateCol, engineCol);

        // Configurar tabla de Devices
        TableColumn<Device, String> deviceIpCol = new TableColumn<>("IP");
        deviceIpCol.setCellValueFactory(new PropertyValueFactory<>("ip"));

        TableColumn<Device, String> hostnameCol = new TableColumn<>("Hostname");
        hostnameCol.setCellValueFactory(new PropertyValueFactory<>("hostname"));

        TableColumn<Device, String> macCol = new TableColumn<>("MAC");
        macCol.setCellValueFactory(new PropertyValueFactory<>("mac"));

        TableColumn<Device, String> manufacturerCol = new TableColumn<>("Manufacturer");
        manufacturerCol.setCellValueFactory(new PropertyValueFactory<>("manufacturer"));

        TableColumn<Device, String> osCol = new TableColumn<>("OS");
        osCol.setCellValueFactory(new PropertyValueFactory<>("os"));

        TableColumn<Device, String> riskCol = new TableColumn<>("Risk Level");
        riskCol.setCellValueFactory(new PropertyValueFactory<>("riskLevel"));

        TableColumn<Device, String> snmpInfoCol = new TableColumn<>("Información SNMP");
        snmpInfoCol.setPrefWidth(200); // Hacer la columna más ancha
        snmpInfoCol.setCellValueFactory(cellData -> {
            Map<String, String> snmpInfo = cellData.getValue().getSnmpInfo();
            if (snmpInfo == null || snmpInfo.isEmpty()) {
                return new SimpleStringProperty("No hay información SNMP");
            }
            StringBuilder info = new StringBuilder();
            snmpInfo.forEach((key, value) -> {
                info.append(key).append(": ").append(value).append("\n");
            });
            return new SimpleStringProperty(info.toString());
        });
        
        // Configurar el formato de celda para mostrar múltiples líneas
        snmpInfoCol.setCellFactory(tc -> {
            TableCell<Device, String> cell = new TableCell<Device, String>() {
                private Text text = new Text();
                private VBox box = new VBox(text);
                {
                    box.setAlignment(Pos.CENTER_LEFT);
                    text.wrappingWidthProperty().bind(tc.widthProperty());
                    setPrefHeight(Control.USE_COMPUTED_SIZE);
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        text.setText(item);
                        setGraphic(box);
                    }
                }
            };
            return cell;
        });

        devicesTable.getColumns().setAll(deviceIpCol, hostnameCol, macCol, manufacturerCol, osCol, riskCol, snmpInfoCol);

        // Configurar tabla de DevicePorts
        TableColumn<InventoryManager.DevicePort, Long> portIdCol = new TableColumn<>("Port ID"); 
        portIdCol.setCellValueFactory(new PropertyValueFactory<>("portId"));

        TableColumn<InventoryManager.DevicePort, Long> deviceIdCol = new TableColumn<>("Device ID"); 
        deviceIdCol.setCellValueFactory(new PropertyValueFactory<>("deviceId"));

        TableColumn<InventoryManager.DevicePort, Integer> portNumCol = new TableColumn<>("Port"); 
        portNumCol.setCellValueFactory(new PropertyValueFactory<>("portNumber"));

        TableColumn<InventoryManager.DevicePort, String> serviceCol = new TableColumn<>("Service"); 
        serviceCol.setCellValueFactory(new PropertyValueFactory<>("serviceName"));

        TableColumn<InventoryManager.DevicePort, String> protocolCol = new TableColumn<>("Protocol"); 
        protocolCol.setCellValueFactory(new PropertyValueFactory<>("protocol"));

        devicePortsTable.getColumns().setAll(portIdCol, deviceIdCol, portNumCol, serviceCol, protocolCol);
    }

    private void loadDatabaseData() {
        // Asegúrate que los siguientes métodos existen y son públicos en InventoryManager:
        // - public List<ScanReport> getAllScanReports()
        // - public List<Device> getAllDevices()
        // - public List<DevicePort> getAllDevicePorts()
        // (o los tipos de lista que correspondan si ScanReport y DevicePort son clases internas)
        scanReportsTable.setItems(FXCollections.observableArrayList(inventoryManager.getAllScanReports()));
        devicesTable.setItems(FXCollections.observableArrayList(inventoryManager.getAllDevices()));
        devicePortsTable.setItems(FXCollections.observableArrayList(inventoryManager.getAllDevicePorts()));
    }

    @FXML
    private void handleScan() {
        String targetNetwork = targetNetworkLabel.getText(); // Obtener la red del Label
        if (targetNetwork == null || targetNetwork.isEmpty()) {
            statusLabel.setText("No hay una red de destino especificada.");
            return;
        }

        scanButton.setDisable(true); // Deshabilitar botón durante el escaneo

        Task<Void> scanTask = new Task<>() {
            @Override
            protected Void call() {
                updateMessage("Escaneando: " + targetNetwork + "...");
                List<Device> detectedDevices = scanner.scan(targetNetwork);

                currentReport = new NetworkReport();
                currentReport.setScannedNetworkTarget(targetNetwork);

                if (detectedDevices != null) {
                    for (Device device : detectedDevices) {
                        currentReport.addDevice(device);
                    }
                }

                long reportId = inventoryManager.saveReport(currentReport);
                if (reportId != -1) {
                    updateMessage("Reporte guardado en BD con ID: " + reportId);
                } else {
                    updateMessage("Fallo al guardar el reporte en la BD.");
                }

                // Actualizar las tablas después del escaneo
                javafx.application.Platform.runLater(() -> loadDatabaseData());

                String safeName = targetNetwork.replaceAll("[^a-zA-Z0-9.-]", "_");
                File dir = new File(REPORTS_DIR);
                if (!dir.exists()) dir.mkdirs();

                String baseName = REPORTS_DIR + File.separator + "reporte_" + safeName + "_" + System.currentTimeMillis();
                
                JsonExporter jsonExporter = new JsonExporter();
                CsvExporter csvExporter = new CsvExporter();
                HtmlExporter htmlExporter = new HtmlExporter();

                jsonExporter.exportReportToFile(currentReport, baseName + ".json");
                csvExporter.exportReportToFile(currentReport, baseName + ".csv");
                htmlExporter.exportReportToFile(currentReport, baseName + ".html");
                updateMessage("Reportes generados. Reporte BD ID: " + reportId);


                StringBuilder output = new StringBuilder();
                output.append("Escaneo finalizado para: ").append(targetNetwork).append("\n");
                output.append("Fecha: ").append(dataNormalizer.formatTimestamp(currentReport.getScanTimestamp())).append("\n");
                output.append("Dispositivos encontrados: ").append(currentReport.getDeviceCount()).append("\n\n");

                if (currentReport.getDevices() != null) {
                    for (Device device : currentReport.getDevices()) {
                        output.append("=== Información del Dispositivo ===\n");
                        output.append("IP: ").append(device.getIp()).append("\n");
                        
                        if (device.getHostname() != null && !device.getHostname().equals("unknown"))
                            output.append("Hostname: ").append(device.getHostname()).append("\n");
                        
                        if (device.getMac() != null && !device.getMac().equals("UNKNOWN")) {
                            output.append("MAC: ").append(device.getMac());
                            if (device.getManufacturer() != null && !device.getManufacturer().equals("unknown"))
                                output.append(" (").append(device.getManufacturer()).append(")");
                            output.append("\n");
                        }
                        
                        if (device.getOs() != null && !device.getOs().equals("unknown"))
                            output.append("Sistema Operativo: ").append(device.getOs()).append("\n");
                        
                        // Información SNMP
                        Map<String, String> snmpInfo = device.getSnmpInfo();
                        if (!snmpInfo.isEmpty()) {
                            output.append("\n=== Información SNMP ===\n");
                            output.append("Nombre del Sistema: ").append(snmpInfo.getOrDefault("systemName", "Desconocido")).append("\n");
                            output.append("Descripción: ").append(snmpInfo.getOrDefault("systemDescription", "Desconocido")).append("\n");
                            output.append("Ubicación: ").append(snmpInfo.getOrDefault("systemLocation", "Desconocido")).append("\n");
                            output.append("Contacto: ").append(snmpInfo.getOrDefault("systemContact", "Desconocido")).append("\n");
                            output.append("Tiempo Activo: ").append(snmpInfo.getOrDefault("systemUptime", "Desconocido")).append("\n");
                            output.append("Interfaz: ").append(snmpInfo.getOrDefault("interfaceDescription", "Desconocido")).append("\n");
                            output.append("Velocidad: ").append(snmpInfo.getOrDefault("interfaceSpeed", "Desconocido")).append("\n");
                            output.append("Estado: ").append(snmpInfo.getOrDefault("interfaceStatus", "Desconocido")).append("\n");
                        }
                        
                        // Información de puertos
                        if (device.getOpenPorts() != null && !device.getOpenPorts().isEmpty()) {
                            output.append("\n=== Puertos y Servicios ===\n");
                            output.append("Puertos abiertos: ").append(device.getOpenPorts().size()).append("\n");
                            if (device.getServices() != null && !device.getServices().isEmpty()) {
                                for (Map.Entry<Integer, String> entry : device.getServices().entrySet()) {
                                    output.append("  Puerto ").append(entry.getKey())
                                          .append("/tcp: ").append(entry.getValue()).append("\n");
                                }
                            }
                        }
                        
                        if (device.getRiskLevel() != null && !device.getRiskLevel().isEmpty())
                            output.append("\nNivel de Riesgo: ").append(device.getRiskLevel().toUpperCase()).append("\n");
                        
                        output.append("\n=============================\n\n");
                    }
                } else {
                    output.append("No se encontraron dispositivos activos.\n");
                }

                final String finalOutput = output.toString();
                javafx.application.Platform.runLater(() -> outputArea.setText(finalOutput));

                return null;
            }
        };

        progressBar.progressProperty().bind(scanTask.progressProperty());
        statusLabel.textProperty().bind(scanTask.messageProperty());

        scanTask.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(1.0); // Indica 100% completado
            statusLabel.textProperty().unbind(); // Desvincular para establecer mensaje final
            statusLabel.setText("Escaneo completado. Reportes generados.");
            scanButton.setDisable(false); // Rehabilitar botón
            // Opcional: resetear la barra después de un tiempo
            // new Timeline(new KeyFrame(Duration.seconds(2), ae -> progressBar.setProgress(0.0))).play();
        });

        scanTask.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0.0); // Indica fallo o resetea
            statusLabel.textProperty().unbind();
            Throwable ex = scanTask.getException();
            statusLabel.setText("Error durante el escaneo: " + (ex != null ? ex.getMessage() : "Error desconocido"));
            if (ex != null) {
                ex.printStackTrace();
            }
            scanButton.setDisable(false); // Rehabilitar botón
        });

        new Thread(scanTask).start();
    }

    // Se elimina el siguiente método ya que scanButton.onAction llama directamente a handleScan
    /*
    @FXML
    private void handleScanButtonAction() {
    // Lógica para iniciar el escaneo
    handleScan();
    // Aquí es donde probablemente se dispararía la generación automática de reportes
    }
    */

    @FXML
    private void handleShowReportsButtonAction() {
        File reportsDirFile = new File(REPORTS_DIR);
        if (Desktop.isDesktopSupported()) {
            try {
                // Ya no se intenta crear el directorio aquí.
                // Se asume que el directorio es creado por el proceso de escaneo al guardar reportes.

                // Asegurarse de que el directorio exista antes de intentar abrirlo
                if (reportsDirFile.exists() && reportsDirFile.isDirectory()) {
                    Desktop.getDesktop().open(reportsDirFile);
                } else {
                    // Mensaje actualizado para ser más informativo si la carpeta no existe.
                    statusLabel.setText("La carpeta de reportes '" + reportsDirFile.getName() + "' no existe. Genere reportes mediante un escaneo primero.");
                }
            } catch (IOException e) {
                statusLabel.setText("Error al abrir la carpeta de reportes: " + e.getMessage());
                System.err.println("Error al abrir la carpeta de reportes: " + e);
                // e.printStackTrace(); // Considera usar esto para depuración
            } catch (SecurityException se) {
                statusLabel.setText("Error de seguridad al acceder a la carpeta de reportes.");
                System.err.println("Error de seguridad al acceder a la carpeta de reportes: " + se);
            }
        } else {
            statusLabel.setText("No se puede abrir la carpeta de reportes en este sistema.");
        }
    }
    } // Cierre de la clase DashboardController

