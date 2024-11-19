package com.edptoscaqs.toscaservice.logging;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.*;

import static com.edptoscaqs.toscaservice.configuration.Constants.*;

@Component
public class LoggerHelper {

    private Logger CONSOLE_LOGGER;
    private Logger FILE_LOGGER;
    private String separator;
    private String logFilePath;
    private FileHandler fileHandler;
    
    public LoggerHelper() throws IOException {
        CONSOLE_LOGGER = Logger.getAnonymousLogger();
        FILE_LOGGER = Logger.getAnonymousLogger();
        separator = String.format("-----------------------------------%s", NEW_LINE);
        configureConsoleHandler();
        configureFileHandler();
    }

    public void logInfo(String message, Object... args) {
        CONSOLE_LOGGER.log(Level.INFO, String.format(message, args));
        FILE_LOGGER.log(Level.INFO, String.format(message, args));
    }

    public void logWarning(String message, Object... args) {
        CONSOLE_LOGGER.log(Level.WARNING, String.format(message, args));
        FILE_LOGGER.log(Level.WARNING, String.format(message, args));
    }

    public void logError(String message, Object... args) {
        CONSOLE_LOGGER.log(Level.SEVERE, String.format(message, args));
        FILE_LOGGER.log(Level.SEVERE, String.format(message, args));
    }

    public void logDebug(String message, Object... args) {
        CONSOLE_LOGGER.log(Level.FINE, String.format(message, args));
        FILE_LOGGER.log(Level.FINE, String.format(message, args));
    }

    public void logException(Exception e) {
        CONSOLE_LOGGER.log(Level.SEVERE, e.getMessage());
        FILE_LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    private void configureConsoleHandler() {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        consoleHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return new SimpleFormatter().format(record) + separator;
            }
        });        
        CONSOLE_LOGGER.addHandler(consoleHandler);
        CONSOLE_LOGGER.setLevel(Level.INFO);
        CONSOLE_LOGGER.setUseParentHandlers(false);
    }

    private void configureFileHandler() throws IOException {
        try {
            Path testResultsPath = Paths.get("logFiles/");
            if (!Files.exists(testResultsPath)) {
                try {
                    Files.createDirectories(testResultsPath);
                } catch (IOException e) {
                    throw new RuntimeException(String.format("[ConfigureFileHandler] Process ends with an IO exception creating the directory - Path: %s", "build/test-results"), e);
                }
            }
            logFilePath = String.format("logFiles/log_%s.txt", System.currentTimeMillis());
            fileHandler = new FileHandler(logFilePath);
            fileHandler.setLevel(Level.ALL);
            FILE_LOGGER.addHandler(fileHandler);
            FILE_LOGGER.setLevel(Level.ALL);
            FILE_LOGGER.setUseParentHandlers(false);
        } catch (Exception e) {
            throw e;
        }
    }

    public void closeAndCopyLogFile(String destinationDir) {
        try {
            fileHandler.close();
            Path sourcePath = Paths.get(logFilePath);
            Path destinationPath = Paths.get(destinationDir, sourcePath.getFileName().toString());
            Files.copy(sourcePath, destinationPath);
        } catch (IOException e) {
            logException(e);
        }
    }
}