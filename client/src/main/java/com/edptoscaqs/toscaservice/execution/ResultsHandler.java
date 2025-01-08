package com.edptoscaqs.toscaservice.execution;

import com.edptoscaqs.toscaservice.ToscaExecutionAPIHandler;
import com.edptoscaqs.toscaservice.ToscaRestAPIHandler;
import com.edptoscaqs.toscaservice.configuration.ToscaConfigParameters;
import com.edptoscaqs.toscaservice.factory.WriterFactory;
import com.edptoscaqs.toscaservice.logging.LoggerHelper;
import com.edptoscaqs.toscaservice.utilities.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.edptoscaqs.toscaservice.configuration.Constants.*;

@Service
public class ResultsHandler {
    @Autowired
    private ToscaConfigParameters toscaConfiguration;

    @Autowired
    private LoggerHelper loggerHelper;

    private final ToscaExecutionAPIHandler toscaExecutionAPIHandler;
    private final ToscaRestAPIHandler toscaRestAPIHandler;
    private final WriterFactory writerFactory;
    private final Utilities utilities;

    @Autowired
    public ResultsHandler(ToscaRestAPIHandler toscaRestAPIHandler,
                          ToscaExecutionAPIHandler toscaExecutionAPIHandler,
                          WriterFactory writerFactory,
                          ToscaConfigParameters toscaConfiguration,
                          Utilities utilities,
                          LoggerHelper loggerHelper) {
        this.toscaRestAPIHandler = toscaRestAPIHandler;
        this.toscaExecutionAPIHandler = toscaExecutionAPIHandler;
        this.writerFactory = writerFactory;
        this.toscaConfiguration = toscaConfiguration;
        this.utilities = utilities;
        this.loggerHelper = loggerHelper;
    }

    public boolean handleExecutionResults(String executionId) throws Exception {
        loggerHelper.logDebug(String.format("[XMLResults] Process starts - Test event: %s - Execution id: %s", toscaConfiguration.getTestEventName(), executionId));
        try (FileWriter fileWriter = writerFactory.createFileWriter(toscaConfiguration.getOutputPath() + toscaConfiguration.getTestEventName() + "_result.xml");
             BufferedWriter bufferedWriter = writerFactory.createBufferedWriter(fileWriter)) {
            String executionResults = toscaExecutionAPIHandler.getExecutionResults(executionId);
            String decodedXmlString = utilities.escapeXmlContent(executionResults, loggerHelper);
            AtomicBoolean allTestsPassed = new AtomicBoolean(true);
            loggerHelper.logDebug(String.format("[XMLResults] Saving results in %s", toscaConfiguration.getOutputPath() + toscaConfiguration.getTestEventName() + "_result.xml"));
            bufferedWriter.write(decodedXmlString.replace("%%", "%"));
            bufferedWriter.flush();
            Map<String, Integer> resultsSummary = toscaExecutionAPIHandler.getExecutionResultsSummary(executionId);
            StringBuilder logText = new StringBuilder();
            logText.append(String.format("%sTest Event Results:", NEW_LINE));
            resultsSummary.forEach((key, value) -> {
                logText.append(String.format("%s  - %-11s: %d", NEW_LINE, key, value));
                if (!key.equals("passed") && value > 0) {
                    allTestsPassed.set(false);
                }
            });
            loggerHelper.logInfo(logText.toString());
            loggerHelper.logDebug(String.format("[XMLResults] Displaying results: %s", decodedXmlString));
            loggerHelper.logDebug(String.format("[XMLResults] Process ends successfully - Test event: %s - Execution id: %s", toscaConfiguration.getTestEventName(), executionId));
            return allTestsPassed.get();
        } catch (Exception e) {
            throw e;
        }
    }

    public void savePDFReport(String executionId) throws Exception {
        loggerHelper.logDebug(String.format("[PDFReport] Process starts - Test event: %s", toscaConfiguration.getTestEventName()));
        File gitParametersFile;
        toscaRestAPIHandler.updateAll();
        List<String> executionLists = toscaRestAPIHandler.listExecutionLists(toscaConfiguration.getTestEventName());
        long endTime;
        if (executionLists.isEmpty()) {
            throw new IllegalArgumentException(String.format("[PDFReport] Process ends with an error: No execution lists in the test event %s", toscaConfiguration.getTestEventName()));
        }
        gitParametersFile = new File(String.format("%s/git_parameters.txt", toscaConfiguration.getOutputPath()).trim());
        for (String uniqueId : executionLists) {
            String executionListName = toscaRestAPIHandler.getObjectProperty(uniqueId, "Name");
            includeGitParametersIntoPDFReport(uniqueId, gitParametersFile);
            endTime = System.currentTimeMillis() + toscaConfiguration.getReportCreationTimeOut() * 60000L;
            do {
                if (System.currentTimeMillis() > endTime) {
                    throw new TimeoutException(String.format("[PDFReport] Process ends with an error: Timeout exceeded - Execution List Id: %s - Timeout: %s", uniqueId, toscaConfiguration.getReportCreationTimeOut()));
                }
                toscaRestAPIHandler.updateAll();
                utilities.sleepWithInterruptHandling(5000, loggerHelper);
            } while (!toscaExecutionAPIHandler.isResultImported(executionId));
            try (FileOutputStream fileOutputStream = writerFactory.createFileOutputStream(toscaConfiguration.getOutputPath() + executionListName + "_report.pdf")) {
                String rawPDF = toscaRestAPIHandler.getPdfReport(uniqueId);
                byte[] pdfBytes = rawPDF.getBytes(StandardCharsets.ISO_8859_1);
                fileOutputStream.write(pdfBytes);
                loggerHelper.logDebug(String.format("[PDFReport] Execution list id: %s - Path: %s", uniqueId, toscaConfiguration.getOutputPath() + executionListName + "_report.pdf"));
            } catch (Exception e) {
                throw e;
            }
        }
        loggerHelper.logDebug(String.format("[PDFReport] Process ends successfully - Test event: %s", toscaConfiguration.getTestEventName()));
    }

    public void includeGitParametersIntoPDFReport(String uniqueId, File gitParametersFile) throws Exception {
        removeOldGitParameters(uniqueId);
        addNewGitParameters(uniqueId, gitParametersFile);
    }

    private void removeOldGitParameters(String uniqueId) throws Exception  {
        loggerHelper.logDebug(String.format("[RemoveOldGitParameters] Process starts - Execution List Id: %s",uniqueId));
        try {
            toscaRestAPIHandler.updateAll();
            toscaRestAPIHandler.checkOutTree(uniqueId);
            List<String> filesToDelete = toscaRestAPIHandler.getOwnedFile(uniqueId);
            for (String fileId:filesToDelete) {
                loggerHelper.logDebug(String.format("File id: %s", fileId));
                toscaRestAPIHandler.deleteAttachment(fileId);
            }
            toscaRestAPIHandler.checkInAll();
            loggerHelper.logDebug(String.format("[RemoveOldGitParameters] Process ends successfully - Execution List Id: %s",uniqueId));
        }
        catch (Exception e) {
            toscaRestAPIHandler.revertAll();
            throw e;
        }
    }

    private void addNewGitParameters(String uniqueId, File gitParametersFile) throws Exception {
        loggerHelper.logDebug(String.format("[AddNewGitParameters] Process starts - Execution List Id: %s",uniqueId));
        try {
            if (gitParametersFile != null && gitParametersFile.isFile() && gitParametersFile.length() > 0) {
                loggerHelper.logDebug(String.format("[AddNewGitParameters] There is a file - Path: %s",gitParametersFile.getAbsolutePath()));
                toscaRestAPIHandler.updateAll();
                toscaRestAPIHandler.checkOutTree(uniqueId);
                toscaRestAPIHandler.addAttachment(uniqueId, gitParametersFile);
                toscaRestAPIHandler.checkInAll();
                List<String> filesAdded = toscaRestAPIHandler.getOwnedFile(uniqueId);
                if (filesAdded == null || filesAdded.isEmpty())
                    throw new Exception("[AddNewGitParameters] Ends with an error: No file attached");
                if (filesAdded.size() != 1)
                    throw new Exception(String.format("[AddNewGitParameters] Ends with an error: Exists more than one file attached. Files count: %d", filesAdded.size()));
                if (!toscaRestAPIHandler.getAttachment(filesAdded.get(0))) {
                    throw new Exception("[AddNewGitParameters] Ends with an error: Cannot get the attachment");
                }
                StringBuilder logText = new StringBuilder();
                logText.append(String.format("%sPDF REPORT PARAMETERS ADDED", NEW_LINE));
                try (BufferedReader reader = new BufferedReader(new FileReader(gitParametersFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logText.append(String.format("%s  %s", NEW_LINE, line));
                    }
                }
                loggerHelper.logInfo(logText.toString());
                loggerHelper.logDebug("[AddNewGitParameters] Process ends successfully");
            }
        }
        catch (Exception e) {
            toscaRestAPIHandler.revertAll();
            throw e;
        }
    }
}
