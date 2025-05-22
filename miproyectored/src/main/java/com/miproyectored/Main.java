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
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File; // Mantener si se usa en el código original que no se muestra aquí
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main extends Application { // Modificación: Extender Application

    private static final String REPORTS_DIR = "reports"; // Directorio para los reportes

    public static void main(String[] args) {

        launch(args); // Modificación: Llamar a launch para iniciar JavaFX
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Asegúrate que la ruta al FXML es correcta desde la perspectiva del ClassLoader
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/miproyectored/Dashboard.fxml"));
        // O si está en el mismo paquete que el controlador:
        // FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/miproyectored/controller/Dashboard.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("NetScan 2025 - Ventana Principal"); // Título de la ventana
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

}