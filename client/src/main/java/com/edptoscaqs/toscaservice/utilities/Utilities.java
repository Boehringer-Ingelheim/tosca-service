package com.edptoscaqs.toscaservice.utilities;

import com.edptoscaqs.toscaservice.factory.WriterFactory;
import com.edptoscaqs.toscaservice.logging.LoggerHelper;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpHeaders;
import org.w3c.dom.*;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.apache.commons.text.StringEscapeUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.io.*;
import java.io.StringReader;
import java.io.IOException;

import static com.edptoscaqs.toscaservice.configuration.Constants.*;

@Component
public class Utilities {

    private final WriterFactory writerFactory;

    public Utilities(WriterFactory writerFactory) {
        this.writerFactory = writerFactory;
    }

    public void writeGitParametersFile(StringBuilder content, String path, LoggerHelper loggerHelper) {
        try {
            String folderPath = String.format("%s", path).trim();
            String absolutPath = String.format("%s/git_parameters.txt", path).trim();
            loggerHelper.logDebug(String.format("[WriteToFile] Process starts - Path: %s - Content: %s", absolutPath, content));
            createDirectory(folderPath, loggerHelper);
            FileWriter fileWriter = writerFactory.createFileWriter(absolutPath);
            fileWriter.write(content.toString());
            fileWriter.close();
            loggerHelper.logDebug(String.format("[WriteToFile] Process ends successfully - Path: %s - Content: %s", absolutPath, content));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeGitParametersFile(String path, LoggerHelper loggerHelper) {
        try {
            Path file = Paths.get(String.format("%s/git_parameters.txt", path).trim());
            loggerHelper.logDebug(String.format("[RemoveFile] Process starts - Path: %s", file.toFile().getAbsolutePath()));
            if (!Files.exists(file))
                loggerHelper.logDebug(String.format("[RemoveFile] File does not exist - Path: %s", file.toFile().getAbsolutePath()));
            else {
                Files.delete(file);
                loggerHelper.logDebug(String.format("[RemoveFile] File removed - Path: %s", file.toFile().getAbsolutePath()));
            }
            loggerHelper.logDebug(String.format("[RemoveFile] Process ends successfully - Path: %s", file.toFile().getAbsolutePath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sleepWithInterruptHandling(long duration, LoggerHelper loggerHelper) throws RuntimeException {
        try {
            Thread.sleep(duration);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public List<String> extractUniqueIds(List<Map<String, Object>> responseList) {
        List<String> uniqueIds = new ArrayList<>();
        if (!Objects.requireNonNull(responseList).isEmpty()) {
            for (Map<String, Object> responseItem : responseList) {
                String uniqueId = (String) responseItem.get(UNIQUE_ID);
                uniqueIds.add(uniqueId);
            }
        } else {
            throw new IllegalArgumentException("[Extract Id] Error - No objects returned");
        }
        return uniqueIds;
    }

    public String encodeBasicAuthCredentials(String username, String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("[EncodeAuthenticationCredentials] Error - Some credentials are null");
        }
        String credentials = username + ":" + password;
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    public void createDirectory(String directoryPath, LoggerHelper loggerHelper) {
        Path testResultsPath = Paths.get(directoryPath);
        if (!Files.exists(testResultsPath)) {
            try {
                loggerHelper.logDebug(String.format("[CreateResultsDirectory] Process starts - Path: %s ", directoryPath));
                Files.createDirectories(testResultsPath);
                loggerHelper.logDebug(String.format("[CreateResultsDirectory] Process ends successfully - Path: %s", directoryPath));
            } catch (IOException e) {
                throw new RuntimeException(String.format("[CreateResultsDirectory] Process ends with an IO exception creating the directory - Path: %s", directoryPath), e);
            }
        }
    }

    public HttpHeaders setClientAuthenticationHttpHeaders(String clientId, String clientSecret) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodeBasicAuthCredentials(clientId, clientSecret));
        headers.set("authMode", "clientCredentials");
        return headers;
    }

    public Map<String, Integer> extractPartialTestResults(String xmlContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));

            NodeList testsuiteNodes = document.getElementsByTagName("testsuite");

            Element testsuiteElement = (Element) testsuiteNodes.item(0);
            int totalTests = Integer.parseInt(testsuiteElement.getAttribute("tests"));
            int failedTests = Integer.parseInt(testsuiteElement.getAttribute("failures"));
            int skippedTests = testsuiteElement.getAttribute("skipped").isEmpty() ? 0 : Integer.parseInt(testsuiteElement.getAttribute("skipped"));

            return Map.of("Total Test Cases", totalTests, "Failed Tests", failedTests, "Skipped Tests", skippedTests);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    public static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        if (!path.endsWith("/") && !path.endsWith("\\")) {
            path += Paths.get("").getFileSystem().getSeparator();
        }
        return path;
    }

    public String extractDataFromEntity(ResponseEntity<String> responseEntity) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(responseEntity.getBody().getBytes()), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(line -> line.replace("%", "%%"))
                    .collect(Collectors.joining("\n"));
        }
    }

    public String escapeXmlContent(String xmlString, LoggerHelper loggerHelper){
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlString)));
            String result;
            traverseAndEscape(document.getDocumentElement());
            document.normalizeDocument();
            result = documentToString(document);
            result = result.replace("&amp;apos;", "'")
                    .replace("&amp;quot;", "'")
                    .replace("&amp;lt;", "&lt;")
                    .replace("&amp;gt;", "&gt;")
                    .replace("&amp;amp;", "&amp;")
                    .replace("&#13;", "\r")
                    .replace("&#10;", "\n");
            loggerHelper.logInfo("[XMLResults] XML content is well-formed");
            return result;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            loggerHelper.logWarning("[XMLResults] The XML is not well-formed and could not be corrected. It may not be interpreted correctly.");
            return xmlString;
        }
    }

    protected void traverseAndEscape(Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attr = attributes.item(i);
                attr.setNodeValue(escapeContent(attr.getNodeValue()));
            }
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.TEXT_NODE) {
                    child.setNodeValue(escapeContent(child.getNodeValue()));
                } else {
                    traverseAndEscape(child);
                }
            }
        }
    }

    protected String escapeContent(String content) {
        return StringEscapeUtils.escapeXml11(content);
    }

    protected String documentToString(Document document) {
        try {
            // Set the encoding to UTF-8
            DOMImplementationLS domImplementation = (DOMImplementationLS) document.getImplementation();
            LSSerializer lsSerializer = domImplementation.createLSSerializer();
            LSOutput lsOutput = domImplementation.createLSOutput();
            lsOutput.setEncoding("UTF-8");

            // Add the required namespaces to the root element
            Element root = document.getDocumentElement();
            root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            root.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");

            // Configure the serializer to not format the output
            lsSerializer.getDomConfig().setParameter("format-pretty-print", false);

            StringWriter stringWriter = new StringWriter();
            lsOutput.setCharacterStream(stringWriter);
            lsSerializer.write(document, lsOutput);

            // Insert a newline character between the XML declaration and the root element
            String result = stringWriter.toString().replaceFirst("\\?>", "?>\n");

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error converting document to string", e);
        }
    }
}
