package com.edptoscaqs.toscaservice.execution;

import com.edptoscaqs.toscaservice.ToscaExecutionAPIHandler;
import com.edptoscaqs.toscaservice.ToscaRestAPIHandler;
import com.edptoscaqs.toscaservice.configuration.ToscaConfigParameters;
import com.edptoscaqs.toscaservice.factory.WriterFactory;
import com.edptoscaqs.toscaservice.logging.LoggerHelper;
import com.edptoscaqs.toscaservice.utilities.Utilities;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static com.edptoscaqs.toscaservice.configuration.Constants.*;

public class ResultsHandlerTest {
    @Mock
    private ToscaRestAPIHandler toscaRestAPIHandler;
    @Mock
    private ToscaExecutionAPIHandler toscaExecutionAPIHandler;
    @Mock
    private ToscaConfigParameters toscaConfiguration;
    @Mock
    private WriterFactory writerFactory;
    @Mock
    private FileWriter fileWriter;
    @Mock
    private BufferedWriter bufferedWriter;
    @Mock
    private FileOutputStream fileOutputStream;
    @Mock
    private Utilities utilities;
    @InjectMocks
    private ResultsHandler resultsHandler;
    @Mock
    private LoggerHelper loggerHelper;

    @Before
    public void init() throws IOException {
        MockitoAnnotations.initMocks(this);
        doNothing().when(loggerHelper).logInfo(anyString());
        doNothing().when(loggerHelper).logWarning(anyString());
        doNothing().when(loggerHelper).logError(anyString());
        doNothing().when(loggerHelper).logDebug(anyString());
        doNothing().when(loggerHelper).logException(new Exception());
        when(writerFactory.createFileWriter(anyString())).thenReturn(fileWriter);
        when(writerFactory.createBufferedWriter(fileWriter)).thenReturn(bufferedWriter);
        when(writerFactory.createFileOutputStream(anyString())).thenReturn(fileOutputStream);
        doNothing().when(bufferedWriter).write(anyString());
        doNothing().when(bufferedWriter).flush();
        doNothing().when(fileOutputStream).write(any());
        when(toscaConfiguration.getOutputPath()).thenReturn("SampleFilePath");
        when(toscaConfiguration.getTestEventName()).thenReturn("SampleTestEvent");
        doNothing().when(utilities).sleepWithInterruptHandling(anyLong(), any(LoggerHelper.class));
    }

    @Test
    public void testHandleExecutionResultsSuccess() throws Exception {
        // Arrange
        String executionId = "12345";
        String executionResults = "<results>...</results>";
        String decodedXmlString = "<results>...</results>";

        when(toscaExecutionAPIHandler.getExecutionResults(executionId)).thenReturn(executionResults);
        when(toscaExecutionAPIHandler.getExecutionResultsSummary(executionId)).thenReturn(Map.of("failed", 0));
        doNothing().when(bufferedWriter).write(executionResults);
        doNothing().when(bufferedWriter).flush();
        when(utilities.escapeXmlContent(executionResults, loggerHelper)).thenReturn(decodedXmlString);

        // Act
        boolean allTestsPassed = resultsHandler.handleExecutionResults(executionId);

        // Assert
        assertThat(allTestsPassed).isTrue();
        verify(bufferedWriter).write(executionResults);
        verify(bufferedWriter).flush();
    }

    @Test
    public void testHandleExecutionResultsWhenFailedTestsThenTrueResponse() throws Exception {
        // Arrange
        String executionId = "12345";
        String executionResults = "<results>...</results>";
        String decodedXmlString = "<results>...</results>";

        when(toscaExecutionAPIHandler.getExecutionResults(executionId)).thenReturn(executionResults);
        when(toscaExecutionAPIHandler.getExecutionResultsSummary(executionId)).thenReturn(Map.of("failed", 1));
        doNothing().when(bufferedWriter).write(executionResults);
        doNothing().when(bufferedWriter).flush();
        when(utilities.escapeXmlContent(executionResults, loggerHelper)).thenReturn(decodedXmlString);

        // Act
        boolean allTestsPassed = resultsHandler.handleExecutionResults(executionId);

        // Assert
        assertThat(allTestsPassed).isFalse();
        verify(bufferedWriter).write(executionResults);
        verify(bufferedWriter).flush();
    }

    @Test
    public void testHandleExecutionResultsIOException() throws Exception {
        // Arrange
        String executionId = "12345";
        when(writerFactory.createFileWriter(toscaConfiguration.getOutputPath())).thenReturn(fileWriter);
        when(writerFactory.createBufferedWriter(fileWriter)).thenThrow(new IOException());

        // Act & Assert
        assertThatThrownBy(() -> resultsHandler.handleExecutionResults(executionId))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testHandleExecutionResultsOtherException() throws Exception {
        // Arrange
        String executionId = "12345";
        when(toscaExecutionAPIHandler.getExecutionResults(executionId)).thenThrow(new Exception());

        // Act & Assert
        assertThatThrownBy(() -> resultsHandler.handleExecutionResults(executionId))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testSavePDFReportSuccess() throws Exception {
        // Arrange
        List<String> executionLists = Arrays.asList("executionList1", "executionList2");
        when(toscaRestAPIHandler.listExecutionLists(toscaConfiguration.getTestEventName())).thenReturn(executionLists);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq("Name"))).thenReturn("executionListName");
        when(toscaRestAPIHandler.getPdfReport(anyString())).thenReturn("PDF report");
        when(toscaExecutionAPIHandler.isResultImported(anyString())).thenReturn(true);

        // Act
        resultsHandler.savePDFReport("executionId");

        // Assert
        verify(toscaRestAPIHandler).listExecutionLists(toscaConfiguration.getTestEventName());
        verify(toscaRestAPIHandler, times(2)).getObjectProperty(anyString(), eq("Name"));
        verify(fileOutputStream, times(2)).write(any());
    }

    @Test
    public void testSavePDFReportWhenNoExecutionListsThenException() {
        // Arrange
        List<String> executionLists = List.of();
        when(toscaRestAPIHandler.listExecutionLists(toscaConfiguration.getTestEventName())).thenReturn(executionLists);

        // Act & Assert
        assertThatThrownBy(() -> resultsHandler.savePDFReport("executionId"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(String.format("[PDFReport] Process ends with an error: No execution lists in the test event %s", toscaConfiguration.getTestEventName()));
    }

    @Test
    public void testSavePDFReportWhenIOExceptionThenException() throws Exception {
        // Arrange
        List<String> executionLists = Arrays.asList("executionList1");
        when(toscaRestAPIHandler.listExecutionLists(toscaConfiguration.getTestEventName())).thenReturn(executionLists);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq("Name"))).thenReturn("executionListName");
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq("CheckOuState"))).thenReturn(CHECKED_IN_STATUS);
        when(toscaRestAPIHandler.getPdfReport(anyString())).thenReturn("PDF report");
        when(writerFactory.createFileOutputStream(anyString())).thenThrow(new IOException());

        // Act & Assert
        assertThatThrownBy(() -> resultsHandler.savePDFReport(executionLists.get(0)))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testSavePDFReportWhenAPIControllerExceptionThenException() {
        // Arrange
        List<String> executionLists = Arrays.asList("executionList1");
        when(toscaRestAPIHandler.listExecutionLists(toscaConfiguration.getTestEventName())).thenReturn(executionLists);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq("Name"))).thenReturn("executionListName");
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq("CheckOuState"))).thenReturn(CHECKED_IN_STATUS);

        // Act & Assert
        assertThatThrownBy(() -> resultsHandler.savePDFReport(executionLists.get(0)))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testIncludeGitParametersIntoPDFReportExceptionDeletingFile() throws Exception {
        // Arrange
        List<String> filesToDelete = new ArrayList<>();
        filesToDelete.add("file1");
        File realFile = File.createTempFile("temp", null);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(realFile))) {
            writer.write("This is a test line.");
        }
        realFile.deleteOnExit();

        doNothing().when(toscaRestAPIHandler).checkOutTree(anyString());
        doNothing().when(toscaRestAPIHandler).updateAll();
        doNothing().when(toscaRestAPIHandler).checkInAll();
        when(toscaRestAPIHandler.getOwnedFile(anyString())).thenReturn(filesToDelete);
        doThrow(new Exception()).when(toscaRestAPIHandler).deleteAttachment(anyString());

        // Act & Assert
        assertThatThrownBy(() -> resultsHandler.includeGitParametersIntoPDFReport(UNIQUE_ID, realFile))
                .isInstanceOf(Exception.class);
        verify(toscaRestAPIHandler, times(1)).revertAll();

    }

    @Test
    public void testIncludeGitParametersIntoPDFReportExceptionAddingFile() throws Exception {
        // Arrange
        List<String> filesToDelete = new ArrayList<>();
        filesToDelete.add("file1");
        File realFile = File.createTempFile("git_parameters", ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(realFile))) {
            writer.write("This is a test line.");
        }
        realFile.deleteOnExit();

        doNothing().when(toscaRestAPIHandler).checkOutTree(anyString());
        doNothing().when(toscaRestAPIHandler).updateAll();
        doNothing().when(toscaRestAPIHandler).checkInAll();
        when(toscaRestAPIHandler.getOwnedFile(anyString())).thenReturn(filesToDelete);
        doNothing().when(toscaRestAPIHandler).deleteAttachment(anyString());
        doNothing().when(toscaRestAPIHandler).addAttachment(anyString(), eq(realFile));

        // Act & Assert
        assertThatThrownBy(() -> resultsHandler.includeGitParametersIntoPDFReport(UNIQUE_ID, realFile))
                .isInstanceOf(Exception.class);
        verify(toscaRestAPIHandler, times(1)).revertAll();

    }

    @Test
    public void testIncludeGitParametersIntoPDFReportNoFileAttached() throws Exception {
        // Arrange
        List<String> filesToDelete = new ArrayList<>();
        List<String> filesAdded = new ArrayList<>();
        filesToDelete.add("file1");
        File realFile = File.createTempFile("temp", null);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(realFile))) {
            writer.write("This is a test line.");
        }
        realFile.deleteOnExit();

        doNothing().when(toscaRestAPIHandler).checkOutTree(anyString());
        doNothing().when(toscaRestAPIHandler).updateAll();
        doNothing().when(toscaRestAPIHandler).checkInAll();
        when(toscaRestAPIHandler.getOwnedFile(anyString())).thenReturn(filesToDelete, filesAdded);
        doNothing().when(toscaRestAPIHandler).deleteAttachment(anyString());
        doNothing().when(toscaRestAPIHandler).addAttachment(anyString(), eq(realFile));

        // Act & Assert
        assertThatThrownBy(() -> resultsHandler.includeGitParametersIntoPDFReport(UNIQUE_ID, realFile))
                .isInstanceOf(Exception.class)
                .hasMessage("[AddNewGitParameters] Ends with an error: No file attached");
        verify(toscaRestAPIHandler, times(1)).revertAll();

    }

    @Test
    public void testIncludeGitParametersIntoPDFReportMoreThanOneFileAttached() throws Exception {
        // Arrange
        List<String> filesToDelete = new ArrayList<>();
        filesToDelete.add("file1");
        List<String> filesAdded = new ArrayList<>();
        filesAdded.add("file1");
        filesAdded.add("file2");
        File realFile = File.createTempFile("temp", null);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(realFile))) {
            writer.write("This is a test line.");
        }
        realFile.deleteOnExit();

        doNothing().when(toscaRestAPIHandler).checkOutTree(anyString());
        doNothing().when(toscaRestAPIHandler).updateAll();
        doNothing().when(toscaRestAPIHandler).checkInAll();
        when(toscaRestAPIHandler.getOwnedFile(anyString())).thenReturn(filesToDelete,filesAdded);
        doNothing().when(toscaRestAPIHandler).deleteAttachment(anyString());
        doNothing().when(toscaRestAPIHandler).addAttachment(anyString(), eq(realFile));

        // Act & Assert
        assertThatThrownBy(() -> resultsHandler.includeGitParametersIntoPDFReport(UNIQUE_ID, realFile))
                .isInstanceOf(Exception.class)
                .hasMessage("[AddNewGitParameters] Ends with an error: Exists more than one file attached. Files count: 2");
        verify(toscaRestAPIHandler, times(1)).revertAll();

    }

    @Test
    public void testIncludeGitParametersIntoPDFReportCannotGetAttachment() throws Exception {
        // Arrange
        List<String> files = new ArrayList<>();
        files.add("file1");
        File realFile = File.createTempFile("temp", null);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(realFile))) {
            writer.write("This is a test line.");
        }
        realFile.deleteOnExit();

        doNothing().when(toscaRestAPIHandler).checkOutTree(anyString());
        doNothing().when(toscaRestAPIHandler).updateAll();
        doNothing().when(toscaRestAPIHandler).checkInAll();
        when(toscaRestAPIHandler.getOwnedFile(anyString())).thenReturn(files,files);
        doNothing().when(toscaRestAPIHandler).deleteAttachment(anyString());
        doNothing().when(toscaRestAPIHandler).addAttachment(anyString(), eq(realFile));
        when(toscaRestAPIHandler.getAttachment(anyString())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> resultsHandler.includeGitParametersIntoPDFReport(UNIQUE_ID, realFile))
                .isInstanceOf(Exception.class)
                .hasMessage("[AddNewGitParameters] Ends with an error: Cannot get the attachment");
        verify(toscaRestAPIHandler, times(1)).revertAll();

    }

    @Test
    public void testIncludeGitParametersIntoPDFReportSuccess() throws Exception {
        // Arrange
        List<String> files = new ArrayList<>();
        files.add("file1");
        File realFile = File.createTempFile("temp", null);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(realFile))) {
            writer.write("This is a test line.");
        }
        realFile.deleteOnExit();

        doNothing().when(toscaRestAPIHandler).checkOutTree(anyString());
        doNothing().when(toscaRestAPIHandler).updateAll();
        doNothing().when(toscaRestAPIHandler).checkInAll();
        when(toscaRestAPIHandler.getOwnedFile(anyString())).thenReturn(files,files);
        doNothing().when(toscaRestAPIHandler).deleteAttachment(anyString());
        doNothing().when(toscaRestAPIHandler).addAttachment(anyString(), eq(realFile));
        when(toscaRestAPIHandler.getAttachment(anyString())).thenReturn(true);

        // Act
        resultsHandler.includeGitParametersIntoPDFReport(UNIQUE_ID, realFile);

        // Assert
        verify(toscaRestAPIHandler, times(2)).updateAll();
        verify(toscaRestAPIHandler, times(2)).checkInAll();
        verify(toscaRestAPIHandler, times(2)).getOwnedFile(anyString());
        verify(toscaRestAPIHandler, times(1)).deleteAttachment(anyString());
        verify(toscaRestAPIHandler, times(1)).getAttachment(anyString());

    }

}
