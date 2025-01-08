package com.edptoscaqs.toscaservice;

import com.edptoscaqs.toscaservice.configuration.ToscaConfigParameters;
import com.edptoscaqs.toscaservice.logging.LoggerHelper;
import com.edptoscaqs.toscaservice.utilities.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import static com.edptoscaqs.toscaservice.configuration.Constants.*;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;
import java.nio.file.*;
@Service
public class ToscaRestAPIHandler {
    @Autowired
    private ToscaConfigParameters toscaConfiguration;
    
    @Autowired
    private LoggerHelper loggerHelper;
    
    @Autowired
    private Utilities utilities;

    private final String CHECK_OUT_URL = "https://{gateway}:{port}/rest/toscacommander/{workspace}/object/{objectId}/task/CheckOut";
    private final String CHECK_OUT_TREE_URL = "https://{gateway}:{port}/rest/toscacommander/{workspace}/object/{objectId}/task/CheckOutTree";
    private final String CHANGE_OWNING_GROUP_URL = "https://{gateway}:{port}/rest/toscacommander/{workspace}/object/{groupId}/task/AssignOwner?objToDrop={objectId}";
    private final String CHECK_IN_URL = "https://{gateway}:{port}/rest/toscacommander/{workspace}/task/CheckInAll";
    private final String UPDATE_ALL_URL = "https://{gateway}:{port}/rest/toscacommander/{workspace}/task/UpdateAll";
    private final String EXECUTION_LISTS_URL = "https://{gateway}:{port}/rest/toscacommander/{workspace}/object/project/task/Search?tqlString==>SUBPARTS:TestEvent[(Name==\"{testEvent}\")]->UsedOwnedItems:ExecutionList";
    private final String GET_OBJECT_URL = "https://{gateway}:{port}/rest/toscacommander/{workspace}/object/{uniqueId}";
    private final String TEST_EVENTS_URL = "https://{gateway}:{port}/rest/toscacommander/{workspace}/object/project/task/Search?tqlString==>SUBPARTS:TestEvent[(Name==\"{testEvent}\")]";
    private final String PDF_REPORT_URL = "https://{gateway}:{port}/rest/toscacommander/{workspace}/resource?UniqueId={objectId}&reportname={reportName}&source=report&filename={filename}";
    private final String TEST_CASES_IN_EXECUTION_LIST_URL = "https://{gateway}:{port}/rest/toscacommander/{workspace}/object/project/task/Search?tqlString==>SUBPARTS:ExecutionList[(UniqueId==\"{executionListId}\")]->UsedOwnedItems:TestCase";
    private final String REVERT_ALL_URL = "https://{gateway}:{port}/rest/toscacommander/{workspace}/task/RevertAll";
    private final String OWNING_GROUP_URL = "https://{gateway}:{port}/rest/toscacommander/{workspace}/object/project/task/Search?tqlString==>SUBPARTS:TCUserGroup[(Name==\"{owningGroupName}\")]";
    private final String GET_OWNED_FILE = "https://{gateway}:{port}/rest/toscacommander/{workspace}/object/{executionlist}/task/Search?tqlString==>SUBPARTS:OwnedFile";
    private final String GET_ATTACHMENT = "https://{gateway}:{port}/rest/toscacommander/{workspace}/resource?source=object&UniqueId={file}";
    private final String ADD_ATTACHMENT = "https://{gateway}:{port}/rest/toscacommander/{workspace}/object/{executionlist}?name={name}";
    private final String DELETE_ATTACHMENT = "https://{gateway}:{port}/rest/toscacommander/{workspace}/object/{objectId}";

    private RestTemplate restTemplate;
    protected String edpLockOwningGroupId;

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
    }

    public void checkOutObject(String objectID) {
        checkOut(objectID, CHECK_OUT_URL);
    }

    public void checkOutTree(String objectID) {
        checkOut(objectID, CHECK_OUT_TREE_URL);
    }

    private void checkOut(String objectID, String checkOutURL) {
        loggerHelper.logDebug(String.format("Process starts - Object id: %s", objectID));
        String checkOutStatus = getObjectProperty(objectID, PROPERTY_CHECKOUT_STATE);
        if (!Objects.equals(checkOutStatus, CHECKED_IN_STATUS)) {
            throw new IllegalArgumentException(String.format("[CheckOut] Process ends with an error: Object has an unexpected status - Object Id: %s - Status: %s ", objectID, checkOutStatus));
        }
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = UriComponentsBuilder
                .fromHttpUrl(checkOutURL)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), toscaConfiguration.getNonAOSWorkspace(), objectID)
                .toUriString();
        loggerHelper.logDebug(String.format("[CheckOut] URL: %s", url));
        try {
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            loggerHelper.logDebug(String.format("[CheckOut] Process ends successfully - Object id: %s",objectID));
        } catch (Exception e) {
            throw e;
        }
    }

    public void changeObjectOwningGroup(String objectID) {
        edpLockOwningGroupId = getOwningGroupUniqueId(toscaConfiguration.getEdpLockGroupName());
        loggerHelper.logDebug(String.format("[ChangeOwningGroup] Process starts - Object id: %s", objectID));
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = UriComponentsBuilder
                .fromHttpUrl(CHANGE_OWNING_GROUP_URL)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), toscaConfiguration.getNonAOSWorkspace(), edpLockOwningGroupId, objectID)
                .toUriString();
        loggerHelper.logDebug(String.format("[ChangeOwningGroup] URL: %s", url));
        try {
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            loggerHelper.logDebug(String.format("[ChangeOwningGroup] Process ends successfully - Object id: %s", objectID));
        } catch (Exception e) {
            throw e;
        }
    }

    public void checkInAll() {
        loggerHelper.logDebug("[CheckInAll] Process starts");
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = UriComponentsBuilder
                .fromHttpUrl(CHECK_IN_URL)
                .queryParam("checkInComment", "freeze changes")
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), toscaConfiguration.getNonAOSWorkspace())
                .toUriString();
        loggerHelper.logDebug(String.format("[CheckInAll] URL: %s", url));
        try {
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            loggerHelper.logDebug("[CheckInAll] Process ends successfully");
        } catch (Exception e) {
            throw e;
        }
    }

    public void updateAll() {
        loggerHelper.logDebug("[UpdateAll] Process starts");
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = UriComponentsBuilder
                .fromHttpUrl(UPDATE_ALL_URL)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), toscaConfiguration.getNonAOSWorkspace())
                .toUriString();
        loggerHelper.logDebug(String.format("[UpdateAll] URL: %s", url));
        try {
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            loggerHelper.logDebug("[UpdateAll] Process ends successfully");
        } catch (Exception e) {
            throw e;
        }
    }

    public void revertAll() {
        loggerHelper.logWarning("[RevertAll] Process starts");
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = UriComponentsBuilder
                .fromHttpUrl(REVERT_ALL_URL)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), toscaConfiguration.getNonAOSWorkspace())
                .toUriString();
        loggerHelper.logDebug(String.format("[RevertAll] URL: %s", url));
        try {
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            loggerHelper.logDebug("[RevertAll] Process ends successfully");
        } catch (Exception e) {
            loggerHelper.logException(e);
            throw e;
        }
    }

    public List<String> listExecutionLists(String testEventName) {
        loggerHelper.logDebug(String.format("[ListExecutionLists] Process starts - Test Event Name: %s",testEventName));
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = UriComponentsBuilder
                .fromHttpUrl(EXECUTION_LISTS_URL)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), toscaConfiguration.getNonAOSWorkspace(), testEventName)
                .toUriString();
        loggerHelper.logDebug(String.format("[ListExecutionLists] URL: %s", url));
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
            List<Map<String, Object>> responseList = response.getBody();
            loggerHelper.logDebug(String.format("[ListExecutionLists] Process ends successfully - Test Event Name: %s",testEventName));
            return utilities.extractUniqueIds(responseList);
        }
        catch (Exception e) {
            throw e;
        }
    }

    public List<String> listTestCasesInExecutionList(String executionListId) {
        loggerHelper.logDebug(String.format("[ListTestCases] Process starts - Execution list id: %s", executionListId));
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = UriComponentsBuilder
                .fromHttpUrl(TEST_CASES_IN_EXECUTION_LIST_URL)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), toscaConfiguration.getNonAOSWorkspace(), executionListId)
                .toUriString();
        loggerHelper.logDebug(String.format("[ListTestCases] URL: %s", url));
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
            List<Map<String, Object>> responseList = response.getBody();
            loggerHelper.logDebug(String.format("[ListTestCases] Process ends successfully - Execution list id: %s", executionListId));
            return utilities.extractUniqueIds(responseList);
        }
        catch (Exception e) {
            throw e;
        }

    }

    public String getTestEventUniqueId(String testEventName) throws Exception {
        loggerHelper.logDebug(String.format("[GetTestEventId] Process starts - Test Event Name: %s",testEventName));
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = UriComponentsBuilder
                .fromHttpUrl(TEST_EVENTS_URL)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), toscaConfiguration.getNonAOSWorkspace(), testEventName)
                .toUriString();
        loggerHelper.logDebug(String.format("[GetTestEventId] URL: %s", url));
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
            List<Map<String, Object>> responseBody = response.getBody();
            if (!Objects.requireNonNull(responseBody).isEmpty()) {
                loggerHelper.logDebug(String.format("[GetTestEventId] Process ends successfully - Test Event Name: %s",testEventName));
                if (responseBody.size() > 1){
                    throw new Exception(String.format("[GetTestEventId] Process ends with an error: More than one test event found with the same name - Test Event Name: %s",testEventName));
                }
                return responseBody.get(0).get(UNIQUE_ID).toString();
            } else {
                throw new IllegalArgumentException(String.format("[GetTestEventId] Process ends with an error: Test Event not found - Test Event Name: %s",testEventName));
            }
        } catch (Exception e) {
            throw e;
        }
    }

    protected String getOwningGroupUniqueId(String owningGroupName) {
        loggerHelper.logDebug(String.format("[GetOwningGroupId] Process starts - Owning Group Name: %s",owningGroupName));

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = UriComponentsBuilder
                .fromHttpUrl(OWNING_GROUP_URL)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), toscaConfiguration.getNonAOSWorkspace(), owningGroupName)
                .toUriString();
        loggerHelper.logDebug(String.format("[GetOwningGroupId] URL: %s", url));
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() { });
            if (Objects.requireNonNull(response.getBody()).isEmpty()) {
                throw new IllegalArgumentException(String.format("[GetOwningGroupId] Process ends with an error: Owning Group not found - Owning Group Name: %s", owningGroupName));
            }
            if (response.getBody().size() != 1) {
                throw new IllegalArgumentException(String.format("[GetOwningGroupId] Process ends with an error: More than one owning groups found - Owning Group Name: %s", owningGroupName));
            }
            loggerHelper.logDebug(String.format("[GetOwningGroupId] Process ends successfully - Owning Group Name: %s",owningGroupName));
            return response.getBody().get(0).get(UNIQUE_ID).toString();
        } catch (Exception e) {
            throw e;
        }
    }

    public String getObjectProperty(String uniqueId, String propertyName) {
        try {
            loggerHelper.logDebug(String.format("[GetObjectProperty] Process starts - Object id: %s - Property: %s", uniqueId, propertyName));
            List<Map<String, String>> attributes = getObjectAttributes(uniqueId);
            Optional<String> propertyValue = attributes.stream()
                    .filter(map -> map.containsKey("Name") && map.get("Name").equals(propertyName))
                    .map(map -> map.get("Value"))
                    .findFirst();
            if (propertyValue.isPresent()) {
                loggerHelper.logDebug(String.format("[GetObjectProperty] Process ends successfully - Object id: %s - Property: %s - Value: %s",uniqueId, propertyName, propertyValue.get()));
                return propertyValue.get();
            } else {
                throw new NoSuchElementException(String.format("[GetObjectProperty] Process ends with an error: Property not found within object's attributes - Object id: %s - Property: %s", uniqueId, propertyName));
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public String getPdfReport(String objectId) {
        loggerHelper.logDebug(String.format("[GetPDFReport] Process starts - Object id: %s",objectId));
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String fileName = "report.pdf";
        String url = UriComponentsBuilder
                .fromHttpUrl(PDF_REPORT_URL)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), toscaConfiguration.getNonAOSWorkspace(), objectId, toscaConfiguration.getPdfReportName(), fileName)
                .toUriString();
        loggerHelper.logDebug(String.format("[GetPDFReport] URL: %s", url));
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response != null && response.hasBody() && !Objects.requireNonNull(response.getBody()).isEmpty()) {
                loggerHelper.logDebug(String.format("[GetPDFReport] Process ends successfully - Object id: %s",objectId));
                return response.getBody();
            } else {
                throw new IllegalArgumentException(String.format("[GetPDFReport] Process ends with an error: Response body is empty - Object id: %s",objectId));
            }
        } catch (Exception e) {
            throw e;
        }
    }

    protected List<Map<String, String>> getObjectAttributes(String uniqueId) {
        loggerHelper.logDebug(String.format("[GetObjectAttributes] Process starts - Object id: %s",uniqueId));
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = UriComponentsBuilder
                .fromHttpUrl(GET_OBJECT_URL)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), toscaConfiguration.getNonAOSWorkspace(), uniqueId)
                .toUriString();
        loggerHelper.logDebug(String.format("[GetObjectAttributes] URL: %s", url));
        try {
            List<Map<String, String>> attributes = (List<Map<String, String>>) Objects.requireNonNull(restTemplate.exchange(url, HttpMethod.GET, entity, Map.class).getBody()).get("Attributes");
            loggerHelper.logDebug(String.format("URL: %s", url));
            loggerHelper.logDebug(String.format("[GetObjectAttributes] Process ends successfully - Object id: %s",uniqueId));
            return attributes;
        } catch (Exception e) {
            throw e;
        }
    }

    public List<String> getOwnedFile(String uniqueId) {
        loggerHelper.logDebug(String.format("[GetOwnedFile] Process starts - Execution List Id: %s",uniqueId));
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        List<String> files = new ArrayList<>();
        String url = UriComponentsBuilder
                .fromHttpUrl(GET_OWNED_FILE)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), toscaConfiguration.getNonAOSWorkspace(), uniqueId)
                .toUriString();
        loggerHelper.logDebug(String.format("[GetOwnedFile] URL: %s", url));
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
            if (response != null && response.hasBody() && !Objects.requireNonNull(response.getBody()).isEmpty()) {
                files = response.getBody().stream().map(map -> map.get(UNIQUE_ID).toString()).toList();
            }
            loggerHelper.logDebug("[GetOwnedFile] Process ends successfully");
            return files;
        } catch (Exception e) {
            throw e;
        }
    }

    public Boolean getAttachment(String uniqueId) {
        loggerHelper.logDebug(String.format("[GetAttachment] Process starts - File Id: %s",uniqueId));
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = UriComponentsBuilder
            .fromHttpUrl(GET_ATTACHMENT)
            .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), toscaConfiguration.getNonAOSWorkspace(), uniqueId)
            .toUriString();
        loggerHelper.logDebug(String.format("[GetAttachment] URL: %s", url));
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
            if (response != null && response.hasBody() && !Objects.requireNonNull(response.getBody()).isEmpty()) {
                loggerHelper.logDebug("[GetAttachment] Process ends successfully");
                return true;
            } else {
                throw new IllegalArgumentException(String.format("[GetAttachment] Process ends with an error: Response body is empty - Unique id: %s",uniqueId));
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public void deleteAttachment(String uniqueId) throws Exception {
        loggerHelper.logDebug(String.format("[DeleteAttachment] Process starts - Execution list id: %s",uniqueId));
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        String url = UriComponentsBuilder
                .fromHttpUrl(DELETE_ATTACHMENT)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), toscaConfiguration.getNonAOSWorkspace(), uniqueId)
                .toUriString();
        loggerHelper.logDebug(String.format("[DeleteAttachment] URL: %s", url));
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            loggerHelper.logDebug("[DeleteAttachment] Process Process ends successfully");
        } catch (Exception e) {
            throw e;
        }
    }

    public void addAttachment(String uniqueId, File gitParametersFile) throws Exception {
        loggerHelper.logDebug(String.format("[AddAttachment] Process starts - Execution list id: %s",uniqueId));
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        try {
            String filePath = gitParametersFile.getPath();
            Path path = Paths.get(filePath);
            byte[] fileBytes = Files.readAllBytes(path);
            HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileBytes, headers);
            String url = UriComponentsBuilder
                .fromHttpUrl(ADD_ATTACHMENT)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), toscaConfiguration.getNonAOSWorkspace(), uniqueId, gitParametersFile.toPath().getFileName())
                .toUriString();
            loggerHelper.logDebug(String.format("[AddAttachment] URL: %s", url));
            restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);
            loggerHelper.logDebug("[AddAttachment] Process Process ends successfully");
        } catch (Exception e) {
            throw e;
        }
    }

}
