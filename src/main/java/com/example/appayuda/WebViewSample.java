package com.example.appayuda;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;
import netscape.javascript.JSObject;

public class WebViewSample extends Application {
    private Scene scene;

    @Override
    public void start(Stage stage) {
        // Crear la escena
        stage.setTitle("AppAyuda - Navegador Web");
        scene = new Scene(new Browser(stage), 750, 500, Color.web("#666970"));
        // Cargar CSS y manejar posibles errores
        try {
            scene.getStylesheets().add(getClass().getResource("css/BrowserToolbar.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Error al cargar el archivo CSS: " + e.getMessage());
        }

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

class Browser extends Region {
    private HBox toolBar;
    private static String[] imageFiles = new String[]{
            "Images/product.png",
            "Images/blog.png",
            "Images/documentation.png",
            "Images/partners.png",
            "Images/help.png"
    };
    private static String[] captions = new String[]{
            "IES Montecillos",
            "Moodle",
            "Facebook",
            "Twitter",
            "Ayuda"
    };
    private static String[] urls = new String[]{
            "http://www.ieslosmontecillos.es",
            "https://educacionadistancia.juntadeandalucia.es",
            "https://www.facebook.com",
            "https://www.twitter.com",
            WebViewSample.class.getResource("help.html").toExternalForm()
    };

    final ImageView selectedImage = new ImageView();
    final Hyperlink[] hpls = new Hyperlink[captions.length];
    final Image[] images = new Image[imageFiles.length];

    final WebView browser = new WebView();
    final WebEngine webEngine = browser.getEngine();
    final Button toggleHelpTopics = new Button("Toggle Help Topics");
    final WebView smallView = new WebView();
    private boolean needDocumentationButton = false;
    final ComboBox comboBox = new ComboBox();

    public Browser(final Stage stage) {
        getStyleClass().add("browser");

        // Crear enlaces y manejar errores de carga de recursos
        for (int i = 0; i < captions.length; i++) {
            Hyperlink hpl = hpls[i] = new Hyperlink(captions[i]);
            try {
                Image image = images[i] = new Image(getClass().getResourceAsStream(imageFiles[i]));
                hpl.setGraphic(new ImageView(image));
            } catch (Exception e) {
                System.err.println("Error al cargar la imagen: " + e.getMessage());
            }

            final String url = urls[i];
            final boolean addButton = (hpl.getText().equals("Ayuda"));

            // Usar lambda para el manejador de eventos
            hpl.setOnAction(e -> {
                needDocumentationButton = addButton;
                webEngine.load(url);
            });
        }


        // Cargar la página web inicial
        webEngine.load("http://www.ieslosmontecillos.es");

        // Crear barra de herramientas
        toolBar = new HBox();
        toolBar.setAlignment(Pos.CENTER);
        toolBar.getStyleClass().add("browser-toolbar");
        toolBar.getChildren().add(comboBox);
        toolBar.getChildren().addAll(hpls);
        toolBar.getChildren().add(createSpacer());

        // Configurar el botón de ayuda
        toggleHelpTopics.setOnAction(e -> webEngine.executeScript("toggle_visibility('help_topics')"));

        // Manejar ventanas emergentes
        smallView.setPrefSize(120, 80);
        webEngine.setCreatePopupHandler(config -> {
            smallView.setFontScale(0.8);
            if (!toolBar.getChildren().contains(smallView)) {
                toolBar.getChildren().add(smallView);
            }
            return smallView.getEngine();
        });
        //process history
        final WebHistory history = webEngine.getHistory();
        history.getEntries().addListener(
                (ListChangeListener.Change<? extends WebHistory.Entry> c) -> {
                    c.next();
                    c.getRemoved().stream().forEach((e) -> {
                        comboBox.getItems().remove(e.getUrl());
                    });
                    c.getAddedSubList().stream().forEach((e) -> {
                        comboBox.getItems().add(e.getUrl());
                    });
                });

        //set the behavior for the history combobox
        comboBox.setOnAction((Event ev) -> {
            int offset
                    = comboBox.getSelectionModel().getSelectedIndex()
                    - history.getCurrentIndex();
            history.go(offset);
        });

        // Manejar estado de carga de la página web
        webEngine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
            toolBar.getChildren().remove(toggleHelpTopics);
            if (newState == Worker.State.SUCCEEDED) {
                JSObject win = (JSObject) webEngine.executeScript("window");
                win.setMember("app", new JavaApp());
                if (needDocumentationButton) {
                    toolBar.getChildren().add(toggleHelpTopics);
                }
            }
        });
        //adding context menu
        final ContextMenu cm = new ContextMenu();
        MenuItem cmItem1 = new MenuItem("Print");
        cm.getItems().add(cmItem1);
        toolBar.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent e) -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                cm.show(toolBar, e.getScreenX(), e.getScreenY());
            }
        });

        //processing print job
        cmItem1.setOnAction((ActionEvent e) -> {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null) {
                webEngine.print(job);
                job.endJob();
            }
        });


        // Agregar componentes a la región
        getChildren().add(toolBar);
        getChildren().add(browser);

    }

    // Interfaz JavaScript para salir de la aplicación
    public class JavaApp {
        public void exit() {
            Platform.exit();
        }
    }

    // Crear un espaciador
    private Node createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }


    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        double tbHeight = toolBar.prefHeight(w);
        layoutInArea(browser, 0, 0, w, h - tbHeight, 0, HPos.CENTER, VPos.CENTER);
        layoutInArea(toolBar, 0, h - tbHeight, w, tbHeight, 0, HPos.CENTER, VPos.CENTER);
    }

    @Override
    protected double computePrefWidth(double height) {
        return 750;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 500;
    }
}