package com.edptoscaqs.toscaservice;

import com.edptoscaqs.toscaservice.configuration.ToscaConfigParameters;
import com.edptoscaqs.toscaservice.logging.LoggerHelper;
import com.edptoscaqs.toscaservice.utilities.Utilities;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static com.edptoscaqs.toscaservice.configuration.Constants.*;

public class ToscaRestAPIHandlerTest {
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ToscaConfigParameters toscaConfiguration;
    @Mock
    private Utilities utilities;
    @InjectMocks
    private ToscaRestAPIHandler toscaRestAPIHandler;
    @Mock
    private LoggerHelper loggerHelper;

    @Before
    public void init(){
        MockitoAnnotations.initMocks(this);
        when(toscaConfiguration.getToscaServerGateway()).thenReturn("ServerGateway");
        when(toscaConfiguration.getToscaServerPort()).thenReturn(123);
        when(toscaConfiguration.getNonAOSWorkspace()).thenReturn("SampleWorkspace");
        when(toscaConfiguration.getToscaClientId()).thenReturn("ToscaClientId");
        when(toscaConfiguration.getToscaClientSecret()).thenReturn("ToscaClientSecret");
        when(utilities.setClientAuthenticationHttpHeaders(anyString(), anyString())).thenReturn(new HttpHeaders());
        doNothing().when(utilities).writeGitParametersFile(any(), anyString(), any(LoggerHelper.class));
        doNothing().when(loggerHelper).logInfo(anyString());
        doNothing().when(loggerHelper).logWarning(anyString());
        doNothing().when(loggerHelper).logError(anyString());
        doNothing().when(loggerHelper).logDebug(anyString());
        doNothing().when(loggerHelper).logException(new Exception());
    }

    @Test
    public void testWhenRestApiUsernameIsNullThenIllegalArgument() {
        // Arrange
        when(toscaConfiguration.getToscaClientId()).thenReturn(null);
        when(utilities.setClientAuthenticationHttpHeaders(anyString(), anyString())).thenThrow(new IllegalArgumentException("Credentials are null"));

        // Act & Assert
        assertThatThrownBy(() -> utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credentials are null");
    }

    @Test
    public void testWhenRestApiPasswordIsNullThenIllegalArgument() {
        // Arrange
        when(toscaConfiguration.getToscaClientSecret()).thenReturn(null);
        when(utilities.setClientAuthenticationHttpHeaders(anyString(), anyString())).thenThrow(new IllegalArgumentException("Credentials are null"));

        // Act & Assert
        assertThatThrownBy(() -> utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credentials are null");
    }

    @Test
    public void testCheckOutObjectSuccess() {
        String objectID = "1234";
        String checkOutState = CHECKED_IN_STATUS;
        List<Map<String, String>> attributes = new ArrayList<>();
        Map<String, Object> responseBody = new HashMap<>();
        Map<String, String> attribute = new HashMap<>();
        attribute.put("Name", PROPERTY_CHECKOUT_STATE);
        attribute.put("Value", checkOutState);
        attributes.add(attribute);
        responseBody.put("Attributes", attributes);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        // Arrange
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(Map.class))).thenReturn(responseEntity);

        // Act
        toscaRestAPIHandler.checkOutObject(objectID);

        // Assert
        verify(restTemplate).exchange(any(String.class), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(String.class));
    }

    @Test
    public void testCheckOutObjectStatusIsNotCheckedInThenIllegalArgument() {
        String objectID = "1234";
        String checkOutState = PROPERTY_CHECKOUT_STATE;
        List<Map<String, String>> attributes = new ArrayList<>();
        Map<String, Object> responseBody = new HashMap<>();
        Map<String, String> attribute = new HashMap<>();
        attribute.put("Name", PROPERTY_CHECKOUT_STATE);
        attribute.put("Value", checkOutState);
        attributes.add(attribute);
        responseBody.put("Attributes", attributes);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        // Arrange
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(Map.class))).thenReturn(responseEntity);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(String.class)))
                .thenReturn(ResponseEntity.ok("Success"));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.checkOutObject(objectID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(String.format("[CheckOut] Process ends with an error: Object has an unexpected status - Object Id: %s - Status: %s ", objectID, checkOutState));
    }

    @Test
    public void testCheckOutObjectFailWhenRestClientError(){
        // Arrange
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.checkOutObject(anyString()))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testChangeObjectOwningGroupSuccess() {
        // Arrange
        String objectID = "123";
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        ResponseEntity<List<Map<String, Object>>> owningGroupUniqueIdResponse = ResponseEntity.ok(Collections.singletonList(Map.of(UNIQUE_ID, UNIQUE_ID)));
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(String.class)))
                .thenReturn(ResponseEntity.ok("Success"));
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), any(ParameterizedTypeReference.class)))
                .thenReturn(owningGroupUniqueIdResponse);

        // Act
        toscaRestAPIHandler.changeObjectOwningGroup(objectID);

        // Assert
        verify(restTemplate).exchange(any(String.class), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(String.class));
    }

    @Test
    public void testChangeObjectOwningGroupFailWhenRestClientError(){
        // Arrange
        String objectID = "123";
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.changeObjectOwningGroup(objectID))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testCheckInAllSuccess() {
        // Arrange
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(String.class)))
                .thenReturn(ResponseEntity.ok("Success"));

        // Act
        toscaRestAPIHandler.checkInAll();

        // Assert
        verify(restTemplate).exchange(any(String.class), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(String.class));
    }

    @Test
    public void testCheckInAllFailWhenRestClientError(){
        // Arrange
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.checkInAll())
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testUpdateAllSuccess() {
        // Arrange
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(String.class)))
                .thenReturn(ResponseEntity.ok("Success"));

        // Act
        toscaRestAPIHandler.updateAll();

        // Assert
        verify(restTemplate).exchange(any(String.class), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(String.class));
    }

    @Test
    public void testUpdateAllFailWhenRestClientError(){
        // Arrange
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.updateAll())
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testRevertAllSuccess() {
        // Arrange
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(String.class)))
                .thenReturn(ResponseEntity.ok("Success"));

        // Act
        toscaRestAPIHandler.revertAll();

        // Assert
        verify(restTemplate).exchange(any(String.class), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(String.class));
    }

    @Test
    public void testRevertAllFailWhenRestClientError(){
        // Arrange
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.revertAll())
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testListExecutionListsSuccessful() {
        // Arrange
        String testEventName = "testEvent";
        List<Map<String, Object>> responseList = new ArrayList<>();
        responseList.add(Collections.singletonMap(UNIQUE_ID, "id1"));
        responseList.add(Collections.singletonMap(UNIQUE_ID, "id2"));
        ResponseEntity<List<Map<String, Object>>> responseEntity = ResponseEntity.ok(responseList);

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);
        when(utilities.extractUniqueIds(responseList)).thenReturn(Arrays.asList("id1", "id2"));

        // Act
        List<String> result = toscaRestAPIHandler.listExecutionLists(testEventName);

        // Assert
        assertThat(result).containsExactly("id1", "id2");
    }

    @Test
    public void testListExecutionListsRestClientException() {
        // Arrange
        String testEventName = "testEvent";

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.listExecutionLists(testEventName))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testListExecutionListsWhenNullResponseThenException(){
        // Arrange
        String testEventName = "testEvent";
        ResponseEntity<List<Map<String, Object>>> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);
        when(utilities.extractUniqueIds(any())).thenThrow(new IllegalArgumentException("No objects returned"));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.listExecutionLists(testEventName))
                .isInstanceOf(Exception.class)
                .hasMessage("No objects returned");
    }

    @Test
    public void testListExecutionListsWhenNoObjectsReturnedThenException(){
        // Arrange
        String testEventName = "testEvent";
        List<Map<String, Object>> responseList = new ArrayList<>();
        ResponseEntity<List<Map<String, Object>>> responseEntity = ResponseEntity.ok(responseList);

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);
        when(utilities.extractUniqueIds(responseList)).thenThrow(new IllegalArgumentException("No objects returned"));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.listExecutionLists(testEventName))
                .isInstanceOf(Exception.class)
                .hasMessage("No objects returned");
    }

    @Test
    public void testGetObjectPropertyRestClientException() {
        // Arrange
        String uniqueId = UNIQUE_ID;
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(Map.class))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getObjectProperty(uniqueId, PROPERTY_OWNING_GROUP_NAME))
                .isInstanceOf(Exception.class)
                .hasStackTraceContaining("400 BAD_REQUEST");
    }

    @Test
    public void testGetObjectPropertySuccess() {
        // Arrange
        String uniqueId = UNIQUE_ID;
        String expectedOwningGroupName = "EDP_Lock_Group";
        Map<String, Object> responseBody = new HashMap<>();
        List<Map<String, String>> attributes = new ArrayList<>();
        Map<String, String> attribute = new HashMap<>();
        attribute.put("Name", PROPERTY_OWNING_GROUP_NAME);
        attribute.put("Value", expectedOwningGroupName);
        attributes.add(attribute);
        responseBody.put("Attributes", attributes);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(Map.class))).thenReturn(responseEntity);

        // Act
        String owningGroupName = toscaRestAPIHandler.getObjectProperty(uniqueId, PROPERTY_OWNING_GROUP_NAME);

        // Assert
        assertThat(owningGroupName).isEqualTo(expectedOwningGroupName);
    }

    @Test
    public void testGetObjectPropertyNullPointerException() {
        // Arrange
        String uniqueId = UNIQUE_ID;
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(Map.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getObjectProperty(uniqueId, PROPERTY_OWNING_GROUP_NAME))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetObjectPropertyNameWhenNoAttributeThenException() {
        // Arrange
        String uniqueId = UNIQUE_ID;
        Map<String, Object> responseBody = new HashMap<>();
        List<Map<String, String>> attributes = new ArrayList<>();
        Map<String, String> attribute = new HashMap<>();
        attributes.add(attribute);
        responseBody.put("Attributes", attributes);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(Map.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getObjectProperty(uniqueId, PROPERTY_OWNING_GROUP_NAME))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetExecutionListWhenNoOwningGroupThenException() {
        // Arrange
        String uniqueId = UNIQUE_ID;
        Map<String, Object> responseBody = new HashMap<>();
        List<Map<String, String>> attributes = new ArrayList<>();
        Map<String, String> attribute = new HashMap<>();
        attributes.add(attribute);
        responseBody.put("Attributes", attributes);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(Map.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getObjectProperty(uniqueId, PROPERTY_OWNING_GROUP_NAME))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetExecutionListWhenNoAttributesThenException() {
        // Arrange
        String uniqueId = UNIQUE_ID;
        Map<String, Object> responseBody = new HashMap<>();
        List<Map<String, String>> attributes = new ArrayList<>();
        Map<String, String> attribute = new HashMap<>();
        attributes.add(attribute);
        responseBody.put("NoAttributes", attributes);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(Map.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getObjectProperty(uniqueId, PROPERTY_OWNING_GROUP_NAME))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testListTestCasesInExecutionListSuccessful() {
        // Arrange
        String executionListId = "1234";
        List<Map<String, Object>> responseList = new ArrayList<>();
        responseList.add(Collections.singletonMap(UNIQUE_ID, "id1"));
        responseList.add(Collections.singletonMap(UNIQUE_ID, "id2"));
        ResponseEntity<List<Map<String, Object>>> responseEntity = ResponseEntity.ok(responseList);

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);
        when(utilities.extractUniqueIds(responseList)).thenReturn(Arrays.asList("id1", "id2"));

        // Act
        List<String> result = toscaRestAPIHandler.listTestCasesInExecutionList(executionListId);

        // Assert
        assertThat(result).containsExactly("id1", "id2");
    }

    @Test
    public void testListTestCasesInExecutionListRestClientException(){
        // Arrange
        String executionListId = "1234";
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.listTestCasesInExecutionList(executionListId))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testListTestCasesInExecutionListWhenNullResponseThenException(){
        // Arrange
        String executionListId = "1234";
        ResponseEntity<List<Map<String, Object>>> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);
        when(utilities.extractUniqueIds(any())).thenThrow(new IllegalArgumentException("No objects returned"));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.listTestCasesInExecutionList(executionListId))
                .isInstanceOf(Exception.class)
                .hasMessage("No objects returned");
    }

    @Test
    public void testListTestCasesInExecutionListWhenNoObjectsReturnedThenException(){
        // Arrange
        String executionListId = "1234";
        List<Map<String, Object>> responseList = new ArrayList<>();
        ResponseEntity<List<Map<String, Object>>> responseEntity = ResponseEntity.ok(responseList);

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);
        when(utilities.extractUniqueIds(responseList)).thenThrow(new IllegalArgumentException("No objects returned"));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.listTestCasesInExecutionList(executionListId))
                .isInstanceOf(Exception.class)
                .hasMessage("No objects returned");
    }

    @Test
    public void testGetTestEventUniqueIdSuccess() throws Exception {
        // Arrange
        String testEventName = "testEvent";
        String testEventUniqueId = "1234";
        List<Map<String, Object>> responseBody = new ArrayList<>();
        responseBody.add(Collections.singletonMap(UNIQUE_ID, testEventUniqueId));
        ResponseEntity<List<Map<String, Object>>> responseEntity = ResponseEntity.ok(responseBody);

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);

        // Act
        String result = toscaRestAPIHandler.getTestEventUniqueId(testEventName);

        // Assert
        assertThat(result).isEqualTo(testEventUniqueId);
    }

    @Test
    public void testGetTestEventUniqueIdWhenMoreThanOneTestEventsThenException() {
        // Arrange
        String testEventName = "testEvent";
        String testEventUniqueId = "1234";
        List<Map<String, Object>> responseBody = new ArrayList<>();
        responseBody.add(Collections.singletonMap(UNIQUE_ID, testEventUniqueId));
        responseBody.add(Collections.singletonMap(UNIQUE_ID, testEventUniqueId));
        ResponseEntity<List<Map<String, Object>>> responseEntity = ResponseEntity.ok(responseBody);

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getTestEventUniqueId(testEventName))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetTestEventUniqueIdWhenNullResponseThenException(){
        // Arrange
        String testEventName = "testEvent";
        ResponseEntity<List<Map<String, Object>>> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getTestEventUniqueId(testEventName))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetTestEventUniqueIdWhenNoObjectsReturnedThenException(){
        // Arrange
        String testEventName = "testEvent";
        List<Map<String, Object>> responseBody = new ArrayList<>();
        ResponseEntity<List<Map<String, Object>>> responseEntity = ResponseEntity.ok(responseBody);

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getTestEventUniqueId(testEventName))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetTestEventUniqueIdRestClientException() {
        // Arrange
        String testEventName = "testEvent";

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getTestEventUniqueId(testEventName))
                .isInstanceOf(Exception.class)
                .hasStackTraceContaining("400 BAD_REQUEST");
    }

    @Test
    public void testGetOwningGroupUniqueIdSuccess() {
        // Arrange
        String owningGroupName = "owningGroup";
        String owningGroupUniqueId = "1234";
        List<Map<String, Object>> responseBody = new ArrayList<>();
        responseBody.add(Collections.singletonMap(UNIQUE_ID, owningGroupUniqueId));
        ResponseEntity<List<Map<String, Object>>> responseEntity = ResponseEntity.ok(responseBody);

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);

        // Act
        String result = toscaRestAPIHandler.getOwningGroupUniqueId(owningGroupName);

        // Assert
        assertThat(result).isEqualTo(owningGroupUniqueId);
    }

    @Test
    public void testGetOwningGroupUniqueIdWhenMoreThanOneOwningGroupsThenException() {
        // Arrange
        String owningGroupName = "owningGroup";
        String owningGroupUniqueId = "1234";
        List<Map<String, Object>> responseBody = new ArrayList<>();
        responseBody.add(Collections.singletonMap(UNIQUE_ID, owningGroupUniqueId));
        responseBody.add(Collections.singletonMap(UNIQUE_ID, owningGroupUniqueId));
        ResponseEntity<List<Map<String, Object>>> responseEntity = ResponseEntity.ok(responseBody);

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getOwningGroupUniqueId(owningGroupName))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetOwningGroupUniqueIdWhenNullResponseThenException(){
        // Arrange
        String owningGroupName = "owningGroup";
        ResponseEntity<List<Map<String, Object>>> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getOwningGroupUniqueId(owningGroupName))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetOwningGroupUniqueIdWhenNoObjectsReturnedThenException(){
        // Arrange
        String owningGroupName = "owningGroup";
        List<Map<String, Object>> responseBody = new ArrayList<>();
        ResponseEntity<List<Map<String, Object>>> responseEntity = ResponseEntity.ok(responseBody);

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getOwningGroupUniqueId(owningGroupName))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetOwningGroupUniqueIdRestClientException() {
        // Arrange
        String owningGroupName = "owningGroup";

        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getOwningGroupUniqueId(owningGroupName))
                .isInstanceOf(Exception.class)
                .hasStackTraceContaining("400 BAD_REQUEST");
    }

    @Test
    public void testGetObjectPropertyWhenNoNameThenException() {
        // Arrange
        String uniqueId = UNIQUE_ID;
        Map<String, Object> responseBody = new HashMap<>();
        List<Map<String, String>> attributes = new ArrayList<>();
        Map<String, String> attribute = new HashMap<>();
        attributes.add(attribute);
        responseBody.put("Attributes", attributes);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(Map.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getObjectProperty(uniqueId, "Name"))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetObjectsCheckOutStateSuccess() {
        // Arrange
        String uniqueId = UNIQUE_ID;
        String expectedState = "state";
        Map<String, Object> responseBody = new HashMap<>();
        List<Map<String, String>> attributes = new ArrayList<>();
        Map<String, String> attribute = new HashMap<>();
        attribute.put("Name", PROPERTY_CHECKOUT_STATE);
        attribute.put("Value", expectedState);
        attributes.add(attribute);
        responseBody.put("Attributes", attributes);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(Map.class))).thenReturn(responseEntity);

        // Act
        String checkOutState = toscaRestAPIHandler.getObjectProperty(uniqueId, PROPERTY_CHECKOUT_STATE);

        // Assert
        assertThat(checkOutState).isEqualTo(expectedState);
    }

    @Test
    public void testGetObjectsCheckOutStateRestClientException() {
        // Arrange
        String uniqueId = UNIQUE_ID;
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(Map.class))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getObjectProperty(uniqueId, PROPERTY_CHECKOUT_STATE))
                .isInstanceOf(Exception.class)
                .hasStackTraceContaining("400 BAD_REQUEST");
    }

    @Test
    public void testGetObjectsCheckOutStateNullPointerException() {
        // Arrange
        String uniqueId = UNIQUE_ID;
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(Map.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getObjectProperty(uniqueId, PROPERTY_CHECKOUT_STATE))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetObjectPropertyWhenNoAttributeThenException() {
        // Arrange
        String uniqueId = UNIQUE_ID;
        Map<String, Object> responseBody = new HashMap<>();
        List<Map<String, String>> attributes = new ArrayList<>();
        Map<String, String> attribute = new HashMap<>();
        attributes.add(attribute);
        responseBody.put("Attributes", attributes);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(Map.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getObjectProperty(uniqueId, PROPERTY_CHECKOUT_STATE))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetPDFReportSuccess() {
        // Arrange
        String objectId = "1234";
        ResponseEntity<String> responseEntity = ResponseEntity.ok("responseBody");
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(String.class))).thenReturn(responseEntity);

        // Act
        String result = toscaRestAPIHandler.getPdfReport(objectId);

        // Assert
        assertThat(result).isEqualTo("responseBody");
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), eq(entity), eq(String.class));
    }

    @Test
    public void testGetPDFReportWhenEmptyResponseBodyThenException() {
        // Arrange
        String objectId = "1234";
        ResponseEntity<String> responseEntity = ResponseEntity.ok("");
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(String.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getPdfReport(objectId))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetPDFReportWhenNullResponseBodyThenException() {
        // Arrange
        String objectId = "1234";
        ResponseEntity<String> responseEntity = ResponseEntity.noContent().build();
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(String.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getPdfReport(objectId))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetPDFReportWhenHttpServerExceptionThenException() {
        // Arrange
        String objectId = "1234";
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(String.class))).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getPdfReport(objectId))
                .isInstanceOf(Exception.class)
                .hasStackTraceContaining("500 INTERNAL_SERVER_ERROR");
    }

    @Test
    public void testGetPDFReportWhenHttpClientExceptionThenException() {
        // Arrange
        String objectId = "1234";
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(String.class))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getPdfReport(objectId))
                .isInstanceOf(Exception.class)
                .hasStackTraceContaining("400 BAD_REQUEST");
    }

    @Test
    public void testGetOwnedFileSuccess() {
        // Arrange
        String uniqueId = UNIQUE_ID;
        List<Map<String, Object>> responseList = new ArrayList<>();
        responseList.add(Collections.singletonMap(UNIQUE_ID, "id1"));
        responseList.add(Collections.singletonMap(UNIQUE_ID, "id2"));
        ResponseEntity<List<Map<String, Object>>> responseEntity = ResponseEntity.ok(responseList);
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);
        when(utilities.extractUniqueIds(responseList)).thenReturn(Arrays.asList("id1", "id2"));

        // Act
        List<String> files = toscaRestAPIHandler.getOwnedFile(uniqueId);

        // Assert
        assertThat(files).containsExactly("id1", "id2");
    }

    @Test
    public void testGetAttachmentSuccess() {
        // Arrange
        String uniqueId = "1234";
        String responseBody = "file";
        ResponseEntity<String> responseEntity = ResponseEntity.ok(responseBody);
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);

        // Act
        Boolean result = toscaRestAPIHandler.getAttachment(uniqueId);

        // Assert
        assertThat(result).isEqualTo(true);
        verify(loggerHelper, times(3)).logDebug(anyString());
        verify(loggerHelper, never()).logException(any(Exception.class));
    }

    @Test
    public void testGetAttachmentWhenEmptyResponseBodyThenException() {
        // Arrange
        String uniqueId = "1234";
        ResponseEntity<String> responseEntity = ResponseEntity.ok("");
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(entity), eq(String.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaRestAPIHandler.getAttachment(uniqueId))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testDeleteAttachmentSuccess() throws Exception {
        // Arrange
        String uniqueId = "12345";
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());

        when(restTemplate.exchange(any(String.class), eq(HttpMethod.DELETE), eq(new HttpEntity<>(headers)), eq(String.class)))
                .thenReturn(ResponseEntity.ok().build());

        // Act
        toscaRestAPIHandler.deleteAttachment(uniqueId);

        // Assert
        verify(loggerHelper, times(3)).logDebug(any(String.class));
        verify(loggerHelper, times(0)).logException(any(Exception.class));
    }

    @Test
    public void testAddAttachmentSuccess() throws Exception {
        // Arrange
        String uniqueId = "1234";
        File realFile = File.createTempFile("temp", null);
        realFile.deleteOnExit();
        String filePath = realFile.getPath();
        Path path = Paths.get(filePath);
        byte[] fileBytes = Files.readAllBytes(path);
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileBytes, headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.PUT), eq(requestEntity), eq(String.class))).thenReturn(null);

        // Act
        toscaRestAPIHandler.addAttachment(uniqueId, realFile);

        // Assert
        verify(loggerHelper, times(3)).logDebug(anyString());
        verify(loggerHelper, never()).logException(any(Exception.class));
    }

    @Test
    public void testAddAttachmentWithException() throws Exception {
        // Arrange
        String uniqueId = "1234";
        File realFile = File.createTempFile("temp", null);
        realFile.deleteOnExit();
        String filePath = realFile.getPath();
        Path path = Paths.get(filePath);
        byte[] fileBytes = Files.readAllBytes(path);
        HttpHeaders headers = utilities.setClientAuthenticationHttpHeaders(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileBytes, headers);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.PUT), eq(requestEntity), eq(String.class)))
                .thenThrow(new RuntimeException("Exception"));
        // Act
        assertThrows(Exception.class, () -> toscaRestAPIHandler.addAttachment(uniqueId, realFile));

        // Assert
        verify(loggerHelper, times(2)).logDebug(anyString());
    }

}
