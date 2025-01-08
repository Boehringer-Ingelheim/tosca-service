package com.edptoscaqs.toscaservice.utilities;

import com.edptoscaqs.toscaservice.factory.WriterFactory;
import com.edptoscaqs.toscaservice.logging.LoggerHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;
import static com.edptoscaqs.toscaservice.configuration.Constants.*;

public class UtilitiesTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    @Mock
    private WriterFactory writerFactory;
    @Mock
    private FileWriter fileWriter;
    @InjectMocks
    private Utilities utilities;
    @Mock
    private LoggerHelper loggerHelper;

    @Before
    public void init() throws IOException {
        MockitoAnnotations.initMocks(this);
        when(writerFactory.createFileWriter(anyString())).thenReturn(fileWriter);
        doNothing().when(fileWriter).write(anyString());
        doNothing().when(fileWriter).close();
    }

    @Test
    public void testWriteGitParametersFileSuccess() throws IOException {
        // Arrange
        StringBuilder content = new StringBuilder("sample");
        String parametersFilePath = "parametersFilePath";

        // Act
        utilities.writeGitParametersFile(content, parametersFilePath, loggerHelper);

        // Assert
        verify(fileWriter).write(content.toString());
        verify(fileWriter).close();
    }

    @Test
    public void testWriteGitParametersFileIOException() throws IOException {
        // Arrange
        StringBuilder content = new StringBuilder("sample");
        String parametersFilePath = "parametersFilePath";
        doThrow(new IOException()).when(fileWriter).write(anyString());

        // Act & Assert
        assertThatThrownBy(() -> utilities.writeGitParametersFile(content, parametersFilePath, loggerHelper))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testRemoveGitParametersFileFileExists() throws IOException {
        // Arrange
        Path tempFile = tempFolder.newFile("git_parameters.txt").toPath();
        String path = tempFile.getParent().toString();

        // Act
        utilities.removeGitParametersFile(path, loggerHelper);

        // Assert
        verify(loggerHelper).logDebug(contains("[RemoveFile] Process starts"));
        verify(loggerHelper).logDebug(contains("[RemoveFile] File removed"));
        verify(loggerHelper).logDebug(contains("[RemoveFile] Process ends successfully"));
        assertFalse(Files.exists(tempFile));
    }

    @Test
    public void testRemoveGitParametersFileFileDoesNotExist() {
        // Arrange
        String path = tempFolder.getRoot().getAbsolutePath();

        // Act
        utilities.removeGitParametersFile(path, loggerHelper);

        // Assert
        verify(loggerHelper).logDebug(contains("[RemoveFile] Process starts"));
        verify(loggerHelper).logDebug(contains("[RemoveFile] File does not exist"));
        verify(loggerHelper).logDebug(contains("[RemoveFile] Process ends successfully"));
    }

    @Test
    public void testSleepWithInterruptHandlingSuccess() {
        // Arrange
        long duration = 1000L;

        // Act
        utilities.sleepWithInterruptHandling(duration, loggerHelper);

        // Assert
        // No exception should be thrown
    }

    @Test
    public void testSleepWithInterruptHandling_Interrupted() {
        // Arrange
        long duration = 1000L;
        Thread.currentThread().interrupt(); // Interrupt the current thread

        // Act & Assert
        assertThatThrownBy(() -> utilities.sleepWithInterruptHandling(duration, loggerHelper))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testExtractUniqueIdsSuccess() {
        // Arrange
        List<Map<String, Object>> responseList = Collections.singletonList(Map.of(UNIQUE_ID, "id1"));

        // Act
        List<String> uniqueIds = utilities.extractUniqueIds(responseList);

        // Assert
        assertThat(uniqueIds).containsExactly("id1");
    }

    @Test
    public void testExtractUniqueIdsWhenNullListThenException() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> utilities.extractUniqueIds(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testExtractUniqueIdsWhenEmptyListThenException() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> utilities.extractUniqueIds(new ArrayList<>()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("[Extract Id] Error - No objects returned");
    }

    @Test
    public void testEncodeBasicAuthCredentialsSuccess() {
        // Arrange
        String username = "username";
        String password = "password";

        // Act
        utilities.encodeBasicAuthCredentials(username, password);

        // Assert
        assertThatCode(() -> utilities.encodeBasicAuthCredentials(username, password)).doesNotThrowAnyException();
    }

    @Test
    public void testEncodeBasicAuthCredentialsWhenUsernameIsNullThenException() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> utilities.encodeBasicAuthCredentials(null, "password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("[EncodeAuthenticationCredentials] Error - Some credentials are null");
    }

    @Test
    public void testEncodeBasicAuthCredentialsWhenPasswordIsNullThenException() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> utilities.encodeBasicAuthCredentials("username", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("[EncodeAuthenticationCredentials] Error - Some credentials are null");
    }

    @Test
    public void testCreateDirectoryDirectoryDoesNotExist() throws IOException {
        // Arrange
        Path tempDir = tempFolder.newFolder("testDir").toPath();
        String directoryPath = tempDir.toString();
        Files.delete(tempDir); // Ensure the directory does not exist

        // Act
        utilities.createDirectory(directoryPath, loggerHelper);

        // Assert
        verify(loggerHelper).logDebug(contains("[CreateResultsDirectory] Process starts"));
        verify(loggerHelper).logDebug(contains("[CreateResultsDirectory] Process ends successfully"));
        assertTrue(Files.exists(tempDir));
    }

    @Test
    public void testCreateDirectoryDirectoryExists() throws IOException {
        // Arrange
        Path tempDir = tempFolder.newFolder("testDir").toPath();
        String directoryPath = tempDir.toString();

        // Act
        utilities.createDirectory(directoryPath, loggerHelper);

        // Assert
        verify(loggerHelper, never()).logDebug(contains("[CreateResultsDirectory] Process starts"));
        verify(loggerHelper, never()).logDebug(contains("[CreateResultsDirectory] Process ends successfully"));
    }

    @Test
    public void testSetClientAuthenticationHttpHeadersSuccess() {
        // Arrange
        String clientId = "testClientId";
        String clientSecret = "testClientSecret";

        // Act
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(clientId, clientSecret);

        // Assert
        assertThat(headers).isNotNull();
        assertThat(headers.get("Authorization")).containsExactly("Basic " + utilities.encodeBasicAuthCredentials(clientId, clientSecret));
        assertThat(headers.get("authMode")).containsExactly("clientCredentials");
    }

    @Test
    public void testSetClientAuthenticationHttpHeadersWithNullClientId() {
        // Arrange
        String clientId = null;
        String clientSecret = "testClientSecret";

        // Act & Assert
        assertThatThrownBy(() -> utilities.setClientAuthenticationHttpHeaders(clientId, clientSecret))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("[EncodeAuthenticationCredentials] Error - Some credentials are null");
    }

    @Test
    public void testSetClientAuthenticationHttpHeadersWithNullClientSecret() {
        // Arrange
        String clientId = "testClientId";
        String clientSecret = null;

        // Act & Assert
        assertThatThrownBy(() -> utilities.setClientAuthenticationHttpHeaders(clientId, clientSecret))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("[EncodeAuthenticationCredentials] Error - Some credentials are null");
    }

    @Test
    public void testExtractPartialTestResultsSuccess() {
        // Arrange
        String xmlContent = "<testsuite tests=\"10\" failures=\"2\" skipped=\"1\"></testsuite>";

        // Act
        Map<String, Integer> result = utilities.extractPartialTestResults(xmlContent);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("Total Test Cases")).isEqualTo(10);
        assertThat(result.get("Failed Tests")).isEqualTo(2);
        assertThat(result.get("Skipped Tests")).isEqualTo(1);
    }

    @Test
    public void testExtractPartialTestResultsNoSkippedAttribute() {
        // Arrange
        String xmlContent = "<testsuite tests=\"10\" failures=\"2\"></testsuite>";

        // Act
        Map<String, Integer> result = utilities.extractPartialTestResults(xmlContent);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("Total Test Cases")).isEqualTo(10);
        assertThat(result.get("Failed Tests")).isEqualTo(2);
        assertThat(result.get("Skipped Tests")).isEqualTo(0);
    }

    @Test
    public void testExtractPartialTestResultsInvalidXml() {
        // Arrange
        String xmlContent = "<testsuite tests=\"10\" failures=\"2\" skipped=\"1\">";

        // Act
        Map<String, Integer> result = utilities.extractPartialTestResults(xmlContent);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    public void testExtractPartialTestResultsEmptyXml() {
        // Arrange
        String xmlContent = "";

        // Act
        Map<String, Integer> result = utilities.extractPartialTestResults(xmlContent);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    public void testNormalizePathNull() {
        // Act
        String result = Utilities.normalizePath(null);

        // Assert
        assertNull(result);
    }

    @Test
    public void testNormalizePathEmpty() {
        // Act
        String result = Utilities.normalizePath("");

        // Assert
        assertEquals("", result);
    }

    @Test
    public void testNormalizePathWithTrailingSlash() {
        // Act
        String result = Utilities.normalizePath("some/path/");

        // Assert
        assertEquals("some/path/", result);
    }

    @Test
    public void testNormalizePathWithTrailingBackslash() {
        // Act
        String result = Utilities.normalizePath("some\\path\\");

        // Assert
        assertEquals("some\\path\\", result);
    }

    @Test
    public void testExtractDataFromEntitySuccess() throws IOException {
        // Arrange
        String responseBody = "line1\nline2%line3";
        ResponseEntity<String> responseEntity = ResponseEntity.ok(responseBody);

        // Act
        String result = utilities.extractDataFromEntity(responseEntity);

        // Assert
        assertThat(result).isEqualTo("line1\nline2%%line3");
    }

    @Test
    public void testExtractDataFromEntityEmptyBody() throws IOException {
        // Arrange
        ResponseEntity<String> responseEntity = ResponseEntity.ok("");

        // Act
        String result = utilities.extractDataFromEntity(responseEntity);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    public void testExtractDataFromEntityNullBody() {
        // Arrange
        ResponseEntity<String> responseEntity = ResponseEntity.ok(null);

        // Act & Assert
        assertThatThrownBy(() -> utilities.extractDataFromEntity(responseEntity))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testEscapeXmlContentMalformedXml() {
        // Arrange
        String xmlString = "<root><element>value</root>";

        // Act
        String result = utilities.escapeXmlContent(xmlString, loggerHelper);

        // Assert
        assertThat(result).isEqualTo(xmlString);
        verify(loggerHelper).logWarning("[XMLResults] The XML is not well-formed and could not be corrected. It may not be interpreted correctly.");
    }

    @Test
    public void testEscapeXmlContentEmptyString() {
        // Arrange
        String xmlString = "";

        // Act
        String result = utilities.escapeXmlContent(xmlString, loggerHelper);

        // Assert
        assertThat(result).isEqualTo(xmlString);
        verify(loggerHelper, never()).logInfo(anyString());
    }

    @Test
    public void testTraverseAndEscapeElementNode() throws Exception {
        // Arrange
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        Element element = document.createElement("root");
        element.setAttribute("attr", "value & < >");
        document.appendChild(element);

        // Act
        utilities.traverseAndEscape(element);

        // Assert
        NamedNodeMap attributes = element.getAttributes();
        Node attr = attributes.getNamedItem("attr");
        assertEquals("value &amp; &lt; &gt;", attr.getNodeValue());
    }

    @Test
    public void testTraverseAndEscapeTextNode() throws Exception {
        // Arrange
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        Element element = document.createElement("root");
        element.setTextContent("value & < >");
        document.appendChild(element);

        // Act
        utilities.traverseAndEscape(element);

        // Assert
        assertEquals("value &amp; &lt; &gt;", element.getTextContent());
    }

    @Test
    public void testTraverseAndEscapeNestedElements() throws Exception {
        // Arrange
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        Element root = document.createElement("root");
        Element child = document.createElement("child");
        child.setTextContent("value & < >");
        root.appendChild(child);
        document.appendChild(root);

        // Act
        utilities.traverseAndEscape(root);

        // Assert
        assertEquals("value &amp; &lt; &gt;", child.getTextContent());
    }

    @Test
    public void testEscapeContentWithSpecialCharacters() {
        // Arrange
        String content = "value & \" ' < >";
        String expected = "value &amp; &quot; &apos; &lt; &gt;";

        // Act
        String result = utilities.escapeContent(content);

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testEscapeContentWithoutSpecialCharacters() {
        // Arrange
        String content = "value";
        String expected = "value";

        // Act
        String result = utilities.escapeContent(content);

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testEscapeContentEmptyString() {
        // Arrange
        String content = "";
        String expected = "";

        // Act
        String result = utilities.escapeContent(content);

        // Assert
        assertThat(result).isEqualTo(expected);
    }

}
