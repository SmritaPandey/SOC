package com.qsdpdp.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qsdpdp.QSDPDPApplication;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * JavaFX Desktop Window with rounded corners and Google-branded splash.
 * Embeds the web dashboard inside a native window using WebView.
 */
public class QSDPDPLauncher extends Application {

    private static final Logger logger = LoggerFactory.getLogger(QSDPDPLauncher.class);
    private static final String APP_TITLE = "QS-DPDP Enterprise — Compliance OS v1.0.0";
    private static final String APP_URL = "http://localhost:8443";
    private static final int WIDTH = 1400;
    private static final int HEIGHT = 900;

    @Override
    public void init() {
        logger.info("Initializing QS-DPDP Desktop Window...");
    }

    @Override
    public void start(Stage stage) {
        // Use transparent stage for rounded window effect
        stage.initStyle(StageStyle.DECORATED);

        // ── Build rounded splash screen ──
        VBox splashBox = new VBox(20);
        splashBox.setAlignment(Pos.CENTER);
        splashBox.setPadding(new Insets(60));
        splashBox.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 24;" +
            "-fx-border-radius: 24;" +
            "-fx-border-color: #e2e8f0;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 5);"
        );

        // App icon
        try {
            javafx.scene.image.ImageView iconView = new javafx.scene.image.ImageView(
                new Image(getClass().getResourceAsStream("/images/app-icon.png"))
            );
            iconView.setFitWidth(96);
            iconView.setFitHeight(96);
            iconView.setPreserveRatio(true);
            splashBox.getChildren().add(iconView);
        } catch (Exception e) {
            logger.debug("Splash icon not found");
        }

        // Brand label using Google colors
        Label brandLabel = new Label("QS-DPDP Enterprise");
        brandLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 28));
        brandLabel.setTextFill(Color.web("#202124")); // Google dark gray

        Label subtitleLabel = new Label("Compliance OS — DPDP Act 2023");
        subtitleLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        subtitleLabel.setTextFill(Color.web("#5f6368")); // Google gray

        // Google-colored accent bar
        HBox colorBar = new HBox(0);
        colorBar.setAlignment(Pos.CENTER);
        colorBar.setMaxWidth(200);
        String[] googleColors = {"#4285F4", "#EA4335", "#FBBC05", "#34A853"};
        for (String color : googleColors) {
            Region bar = new Region();
            bar.setPrefSize(50, 4);
            bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");
            colorBar.getChildren().add(bar);
        }

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        progressBar.setStyle(
            "-fx-accent: #4285F4;"  // Google blue
        );

        Label statusLabel = new Label("Starting application server...");
        statusLabel.setTextFill(Color.web("#80868b"));
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));

        Label versionLabel = new Label("v1.0.0 — QualityShield Technologies");
        versionLabel.setTextFill(Color.web("#bdc1c6"));
        versionLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));

        splashBox.getChildren().addAll(brandLabel, subtitleLabel, colorBar, progressBar, statusLabel, versionLabel);

        // Wrap in a container with padding for the rounded effect
        StackPane splashContainer = new StackPane(splashBox);
        splashContainer.setStyle("-fx-background-color: #f8f9fa;"); // Light gray background
        splashContainer.setPadding(new Insets(20));

        Scene splashScene = new Scene(splashContainer, WIDTH, HEIGHT);
        stage.setTitle(APP_TITLE);
        stage.setScene(splashScene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);

        // Load app icon for taskbar
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/app-icon.png")));
        } catch (Exception ignored) {}

        stage.show();

        // ── Wait for server, then switch to WebView ──
        Thread serverWaiter = new Thread(() -> {
            logger.info("Waiting for Spring Boot server at {}...", APP_URL);
            boolean ready = false;
            int attempts = 0;
            while (!ready && attempts < 60) {
                try {
                    Thread.sleep(1000);
                    attempts++;
                    int finalAttempts = attempts;
                    Platform.runLater(() -> {
                        statusLabel.setText("Connecting to server... (" + finalAttempts + "s)");
                        progressBar.setProgress(Math.min(0.95, finalAttempts / 25.0));
                    });
                    HttpURLConnection conn = (HttpURLConnection) new URL(APP_URL).openConnection();
                    conn.setRequestMethod("HEAD");
                    conn.setConnectTimeout(2000);
                    conn.setReadTimeout(2000);
                    int code = conn.getResponseCode();
                    if (code >= 200 && code < 500) {
                        ready = true;
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    // Server not ready yet
                }
            }

            if (ready) {
                logger.info("Server ready! Loading desktop UI...");
                Platform.runLater(() -> loadWebView(stage));
            } else {
                logger.error("Server did not start within 60 seconds");
                Platform.runLater(() -> statusLabel.setText("ERROR: Server did not start."));
            }
        });
        serverWaiter.setDaemon(true);
        serverWaiter.setName("qsdpdp-server-waiter");
        serverWaiter.start();
    }

    private void loadWebView(Stage stage) {
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.setUserAgent("QS-DPDP-Desktop/1.0.0");

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                logger.info("Dashboard loaded in desktop window");
            }
        });

        engine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
            if (newTitle != null && !newTitle.isEmpty()) {
                stage.setTitle("QS-DPDP Enterprise — " + newTitle);
            }
        });

        engine.load(APP_URL);

        // WebView in a rounded container
        StackPane root = new StackPane(webView);
        root.setStyle("-fx-background-color: #f8f9fa;");

        Scene webScene = new Scene(root, WIDTH, HEIGHT);
        stage.setScene(webScene);
    }

    @Override
    public void stop() {
        logger.info("Shutting down QS-DPDP Desktop Window...");
        try { QSDPDPApplication.shutdown(); } catch (Exception ignored) {}
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
