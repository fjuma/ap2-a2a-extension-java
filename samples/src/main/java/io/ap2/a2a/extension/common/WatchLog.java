package io.ap2.a2a.extension.common;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import io.a2a.spec.DataPart;

/**
 * Utility methods related to creating the watch.log file.
 *
 * The watch.log file is a log file meant to be watched in parallel with running a
 * scenario. It will contain all the requests and responses to/from the agent
 * that are sent to/from the client, so engineers can see what is happening
 * between the servers in real time.
 */
public class WatchLog {

    private static final Logger logger = Logger.getLogger(WatchLog.class.getName());
    private static final String LOG_FILE_PATH = ".logs/watch.log";

    // Mandate data keys
    private static final String CART_MANDATE_DATA_KEY = "cart_mandate";
    private static final String INTENT_MANDATE_DATA_KEY = "intent_mandate";
    private static final String PAYMENT_MANDATE_DATA_KEY = "payment_mandate";

    /**
     * Creates a file handler to the logger for watch.log.
     *
     * @return A FileHandler instance configured for 'watch.log'
     * @throws IOException if unable to create the log file
     */
    public static FileHandler createFileHandler() throws IOException {
        // Create directory if it doesn't exist
        File logDir = new File(".logs");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        FileHandler fileHandler = new FileHandler(LOG_FILE_PATH, true);
        fileHandler.setFormatter(new SimpleFormatter());
        return fileHandler;
    }

    /**
     * Logs the A2A message parts to the watch.log file.
     *
     * @param textParts A list of text parts from the request
     * @param dataParts A list of data parts from the request
     */
    public static void logA2aMessageParts(List<String> textParts, List<DataPart> dataParts) {
        loadLogger();
        logRequestInstructions(textParts);
        logMandates(dataParts);
        logExtraData(dataParts);
    }

    /**
     * Logs the A2A extensions activated to the watch.log file.
     *
     * @param requestedExtensions The set of requested extensions
     */
    public static void logA2aRequestExtensions(Set<String> requestedExtensions) {
        if (requestedExtensions == null || requestedExtensions.isEmpty()) {
            return;
        }

        logger.info("\n");
        logger.info("[A2A Extensions Activated in the Request]");

        for (String extension : requestedExtensions) {
            logger.info(extension);
        }
    }

    private static void loadLogger() {
        if (logger.getHandlers().length == 0) {
            try {
                logger.addHandler(createFileHandler());
            } catch (IOException e) {
                System.err.println("Failed to create watch log file handler: " + e.getMessage());
            }
        }
    }

    private static void logRequestInstructions(List<String> textParts) {
        logger.info("\n");
        logger.info("[Request Instructions]");
        logger.info(textParts.toString());
    }

    private static void logMandates(List<DataPart> dataParts) {
        for (DataPart dataPart : dataParts) {
            for (Map.Entry<String, Object> entry : dataPart.getData().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (CART_MANDATE_DATA_KEY.equals(key)) {
                    logger.info("\n");
                    logger.info("[A Cart Mandate was in the request Data]");
                    logger.info(value.toString());
                } else if (INTENT_MANDATE_DATA_KEY.equals(key)) {
                    logger.info("\n");
                    logger.info("[An Intent Mandate was in the request Data]");
                    logger.info(value.toString());
                } else if (PAYMENT_MANDATE_DATA_KEY.equals(key)) {
                    logger.info("\n");
                    logger.info("[A Payment Mandate was in the request Data]");
                    logger.info(value.toString());
                }
            }
        }
    }

    private static void logExtraData(List<DataPart> dataParts) {
        for (DataPart dataPart : dataParts) {
            for (Map.Entry<String, Object> entry : dataPart.getData().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (CART_MANDATE_DATA_KEY.equals(key) ||
                    INTENT_MANDATE_DATA_KEY.equals(key) ||
                    PAYMENT_MANDATE_DATA_KEY.equals(key)) {
                    continue;
                }

                logger.info("\n");
                logger.info(String.format("[Data Part: %s]", key));
                logger.info(value.toString());
            }
        }
    }

    private WatchLog() {
        // Utility class should not be instantiated
    }
}
