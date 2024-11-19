package com.edptoscaqs.toscaservice;

import com.edptoscaqs.toscaservice.configuration.ToscaConfigParameters;
import com.edptoscaqs.toscaservice.logging.LoggerHelper;
import com.edptoscaqs.toscaservice.utilities.Utilities;
import net.minidev.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ToscaExecutionAPIHandlerTest {
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ToscaConfigParameters toscaConfiguration;
    @InjectMocks
    private ToscaExecutionAPIHandler toscaExecutionAPIHandler;
    @Mock
    private LoggerHelper loggerHelper;
    @Mock
    private Utilities utilities;

    @Before
    public void init(){
        MockitoAnnotations.initMocks(this);
        when(toscaConfiguration.getToscaClientId()).thenReturn("clientId");
        when(toscaConfiguration.getToscaClientSecret()).thenReturn("clientSecret");
        when(toscaConfiguration.getToscaServerGateway()).thenReturn("serverGateway");
        when(toscaConfiguration.getToscaServerPort()).thenReturn(123);
        doNothing().when(loggerHelper).logInfo(anyString());
        doNothing().when(loggerHelper).logWarning(anyString());
        doNothing().when(loggerHelper).logError(anyString());
        doNothing().when(loggerHelper).logDebug(anyString());
        doNothing().when(loggerHelper).logException(new Exception());
    }

    @Test
    public void testGetExecutionApiAccessTokenSuccess() throws Exception {
        // Arrange
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("access_token", "test_access_token");
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(ResponseEntity.ok(responseBody));

        // Act
        String accessToken = toscaExecutionAPIHandler.getExecutionApiAccessToken();

        // Assert
        assertThat(accessToken).isEqualTo("test_access_token");
    }

    @Test
    public void testGetExecutionApiAccessTokenUnauthorized() {
        // Arrange
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getExecutionApiAccessToken())
                .isInstanceOf(Exception.class)
                .hasStackTraceContaining("401 UNAUTHORIZED");
    }

    @Test
    public void testGetExecutionApiAccessTokenWithNullClientId() {
        // Arrange
        when(toscaConfiguration.getToscaClientId()).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getExecutionApiAccessToken())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("[GetToken] Error - Tosca Client ID and/or Tosca Client Secret cannot be null");
    }

    @Test
    public void testGetExecutionApiAccessTokenWithNullClientSecret() {
        // Arrange
        when(toscaConfiguration.getToscaClientSecret()).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getExecutionApiAccessToken())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("[GetToken] Error - Tosca Client ID and/or Tosca Client Secret cannot be null");
    }

    @Test
    public void testGetExecutionApiAccessTokenWithIncorrectClientId() {
        // Arrange
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getExecutionApiAccessToken())
                .isInstanceOf(Exception.class)
                .hasStackTraceContaining("400 BAD_REQUEST");
    }

    @Test
    public void testGetExecutionApiAccessTokenResponseWithEmptyBody() {
        // Arrange
        ResponseEntity<Map> responseEntity = ResponseEntity.noContent().build();
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getExecutionApiAccessToken())
                .isInstanceOf(Exception.class)
                .hasStackTraceContaining("Response body is empty when requesting access token");
    }

    @Test
    public void testGetExecutionApiAccessTokenWithNonSuccessfulCode() {
        // Arrange
        ResponseEntity<Map> responseEntity = ResponseEntity.notFound().build();
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getExecutionApiAccessToken())
                .isInstanceOf(Exception.class)
                .hasStackTraceContaining("404 NOT_FOUND");
    }

    @Test
    public void testTriggerEventExecutionSuccess() throws Exception {
        // Arrange
        String projectName = "testProject";
        String executionEnvironment = "testEnvironment";
        String eventId = "testEventId";
        Map<String, String> eventParameters = new HashMap<>();
        eventParameters.put("param1", "value1");
        eventParameters.put("param2", "value2");
        Map<String, String> characteristics = new HashMap<>();
        characteristics.put("key1", "value1");
        characteristics.put("key2", "value2");

        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders expectedHeaders = new HttpHeaders();
        expectedHeaders.setContentType(MediaType.APPLICATION_JSON);
        expectedHeaders.set("X-Tricentis", "OK");
        expectedHeaders.set("Authorization", "Bearer " + accessToken);

        Map<String, Object> expectedRequestBody = new HashMap<>();
        expectedRequestBody.put("projectName", projectName);
        expectedRequestBody.put("executionEnvironment", executionEnvironment);
        List<Map<String, Object>> events = new ArrayList<>();
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        Map<String, Object> parameters = new HashMap<>(eventParameters);
        event.put("parameters", parameters);
        Map<String, Object> executionCharacteristics = new HashMap<>(characteristics);
        event.put("characteristics", new HashMap<>(executionCharacteristics));
        events.add(event);
        expectedRequestBody.put("events", events);
        expectedRequestBody.put("importResult", true);

        HttpEntity<Map<String, Object>> expectedRequest = new HttpEntity<>(expectedRequestBody, expectedHeaders);

        ResponseEntity<Map> mockResponse = mock(ResponseEntity.class);
        when(mockResponse.getBody()).thenReturn(Collections.singletonMap("result", "success"));
        when(mockResponse.hasBody()).thenReturn(true);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(expectedRequest), eq(Map.class))).thenReturn(mockResponse);

        // Act
        JSONObject result = toscaExecutionAPIHandler.triggerEventExecution(projectName, executionEnvironment, eventId, eventParameters, characteristics);

        // Assert
        assertThat(result).containsEntry("result", "success").isNotNull();
    }

    @Test
    public void testTriggerEventExecutionGetAccessTokenFailure() {
        // Arrange
        String projectName = "testProject";
        String executionEnvironment = "testEnvironment";
        String eventId = "testEventId";
        Map<String, String> eventParameters = new HashMap<>();
        eventParameters.put("param1", "value1");
        eventParameters.put("param2", "value2");
        Map<String, String> characteristics = new HashMap<>();
        characteristics.put("key1", "value1");
        characteristics.put("key2", "value2");

        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act and Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.triggerEventExecution(projectName, executionEnvironment, eventId, eventParameters, characteristics))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testTriggerEventExecutionFailedRequest() {
        // Arrange
        String projectName = "testProject";
        String executionEnvironment = "testEnvironment";
        String eventId = "testEventId";
        Map<String, String> eventParameters = new HashMap<>();
        eventParameters.put("param1", "value1");
        eventParameters.put("param2", "value2");
        Map<String, String> characteristics = new HashMap<>();
        characteristics.put("key1", "value1");
        characteristics.put("key2", "value2");

        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders expectedHeaders = new HttpHeaders();
        expectedHeaders.setContentType(MediaType.APPLICATION_JSON);
        expectedHeaders.set("X-Tricentis", "OK");
        expectedHeaders.set("Authorization", "Bearer " + accessToken);

        Map<String, Object> expectedRequestBody = new HashMap<>();
        expectedRequestBody.put("projectName", projectName);
        expectedRequestBody.put("executionEnvironment", executionEnvironment);
        List<Map<String, Object>> events = new ArrayList<>();
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        Map<String, Object> parameters = new HashMap<>(eventParameters);
        event.put("parameters", parameters);
        Map<String, Object> executionCharacteristics = new HashMap<>(characteristics);
        event.put("characteristics", executionCharacteristics);
        events.add(event);
        expectedRequestBody.put("events", events);
        expectedRequestBody.put("importResult", true);

        HttpEntity<Map<String, Object>> expectedRequest = new HttpEntity<>(expectedRequestBody, expectedHeaders);

        ResponseEntity<Map> mockResponse = mock(ResponseEntity.class);
        when(mockResponse.getBody()).thenReturn(Collections.singletonMap("result", "success"));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(expectedRequest), eq(Map.class))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.triggerEventExecution(projectName, executionEnvironment, eventId, eventParameters, characteristics))
                .isInstanceOf(Exception.class)
                .hasStackTraceContaining("400 BAD_REQUEST");
    }

    @Test
    public void testTriggerEventExecutionEmptyResponse() {
        // Arrange
        String projectName = "testProject";
        String executionEnvironment = "testEnvironment";
        String eventId = "testEventId";
        Map<String, String> eventParameters = new HashMap<>();
        eventParameters.put("param1", "value1");
        eventParameters.put("param2", "value2");
        Map<String, String> characteristics = new HashMap<>();
        characteristics.put("key1", "value1");
        characteristics.put("key2", "value2");

        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders expectedHeaders = new HttpHeaders();
        expectedHeaders.setContentType(MediaType.APPLICATION_JSON);
        expectedHeaders.set("X-Tricentis", "OK");
        expectedHeaders.set("Authorization", "Bearer " + accessToken);

        Map<String, Object> expectedRequestBody = new HashMap<>();
        expectedRequestBody.put("projectName", projectName);
        expectedRequestBody.put("executionEnvironment", executionEnvironment);
        List<Map<String, Object>> events = new ArrayList<>();
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        Map<String, Object> parameters = new HashMap<>(eventParameters);
        event.put("parameters", parameters);
        Map<String, Object> executionCharacteristics = new HashMap<>(characteristics);
        event.put("characteristics", executionCharacteristics);
        events.add(event);
        expectedRequestBody.put("events", events);
        expectedRequestBody.put("importResult", true);

        HttpEntity<Map<String, Object>> expectedRequest = new HttpEntity<>(expectedRequestBody, expectedHeaders);

        ResponseEntity<Map> responseEntity = ResponseEntity.noContent().build();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(expectedRequest), eq(Map.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.triggerEventExecution(projectName, executionEnvironment, eventId, eventParameters, characteristics))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetEventExecutionDataSuccess() throws Exception {
        // Arrange
        String executionId = "12345";
        String expectedStatus = "SUCCESS";
        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Collections.singletonMap("status", "SUCCESS"), HttpStatus.OK));

        // Act
        String status = toscaExecutionAPIHandler.getEventExecutionStatus(executionId);

        // Assert
        assertThat(status).isEqualTo(expectedStatus);
    }

    @Test
    public void testGetEventExecutionDataRestClientException() {
        // Arrange
        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);
        String executionId = "12345";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("Rest client exception"));

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getEventExecutionStatus(executionId))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetEventExecutionDataNullResponse() {
        // Arrange
        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);
        String executionId = "12345";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getEventExecutionStatus(executionId))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetEventExecutionDataEmptyResponse() {
        // Arrange
        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);
        String executionId = "12345";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Collections.emptyMap(), HttpStatus.OK));

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getEventExecutionStatus(executionId))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetEventExecutionDataExceptionInAccessToken() {
        // Arrange
        String executionId = "12345";
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenThrow(new RestClientException("Failed to get access token"));

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getEventExecutionStatus(executionId))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetExecutionResultsSuccess() throws Exception {
        // Arrange
        String executionId = "1234";

        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/automationobjectservice/api/Execution/" + executionId + "/Results";

        ResponseEntity<String> response = ResponseEntity.ok("responseBody");
        when(restTemplate.exchange(url, HttpMethod.GET, entity, String.class)).thenReturn(response);
        when(utilities.extractDataFromEntity(response)).thenReturn(response.getBody());

        // Act
        String result = toscaExecutionAPIHandler.getExecutionResults(executionId);

        // Assert
        assertThat(result).isEqualTo("responseBody");
    }

    @Test
    public void testGetExecutionResultsEmptyResponseBody() {
        // Arrange
        String executionId = "1234";

        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/automationobjectservice/api/Execution/" + executionId + "/Results";

        ResponseEntity<String> response = ResponseEntity.ok("");
        when(restTemplate.exchange(url, HttpMethod.GET, entity, String.class)).thenReturn(response);

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getExecutionResults(executionId))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetExecutionResultsNullResponseBody() {
        // Arrange
        String executionId = "1234";

        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/automationobjectservice/api/Execution/" + executionId + "/Results";

        ResponseEntity<String> response = ResponseEntity.noContent().build();
        when(restTemplate.exchange(url, HttpMethod.GET, entity, String.class)).thenReturn(response);

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getExecutionResults(executionId))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetExecutionResultsHttpClientException() {
        // Arrange
        String executionId = "1234";

        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/automationobjectservice/api/Execution/" + executionId + "/Results";

        when(restTemplate.exchange(url, HttpMethod.GET, entity, String.class)).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getExecutionResults(executionId))
                .isInstanceOf(Exception.class)
                .hasStackTraceContaining("500 INTERNAL_SERVER_ERROR");
    }

    @Test
    public void testGetExecutionResultsHttpServerException() {
        // Arrange
        String executionId = "1234";

        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/automationobjectservice/api/Execution/" + executionId + "/Results";

        when(restTemplate.exchange(url, HttpMethod.GET, entity, String.class)).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getExecutionResults(executionId))
                .isInstanceOf(Exception.class)
                .hasStackTraceContaining("400 BAD_REQUEST");
    }

    @Test
    public void testGetExecutionResultsFailedToGetAccessToken() {
        // Arrange
        String executionId = "1234";
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act and Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getExecutionResults(executionId))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetExecutionResultsSummarySuccess() throws Exception {
        // Arrange
        String executionId = "1234";

        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/automationobjectservice/api/Execution/" + executionId + "/Results/Summary";

        ResponseEntity<Map<String, Integer>> response = ResponseEntity.ok(Map.of("response", 1));
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(response);

        // Act
        Map<String, Integer> result = toscaExecutionAPIHandler.getExecutionResultsSummary(executionId);

        // Assert
        assertThat(result).isEqualTo(Map.of("response", 1));
    }

    @Test
    public void testGetExecutionResultsSummaryEmptyResponseBody() {
        // Arrange
        String executionId = "1234";

        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/automationobjectservice/api/Execution/" + executionId + "/Results/Summary";

        ResponseEntity<Map<String, Integer>> response = ResponseEntity.ok(new HashMap<>());
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(response);

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getExecutionResultsSummary(executionId))
                .isInstanceOf(Exception.class)
                .hasMessage(String.format("[GetExecutionResultsSummary] Process ends with an error: Response body is null/empty - Execution ID: %s", executionId));
    }

    @Test
    public void testGetExecutionResultsSummaryNullResponseBody() {
        // Arrange
        String executionId = "1234";

        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/automationobjectservice/api/Execution/" + executionId + "/Results/Summary";

        ResponseEntity<String> response = ResponseEntity.noContent().build();
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenReturn(response);

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getExecutionResultsSummary(executionId))
                .isInstanceOf(Exception.class)
                .hasMessage(String.format("[GetExecutionResultsSummary] Process ends with an error: Response body is null/empty - Execution ID: %s", executionId));
    }

    @Test
    public void testGetExecutionResultsSummaryHttpClientException() {
        // Arrange
        String executionId = "1234";

        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/automationobjectservice/api/Execution/" + executionId + "/Results/Summary";

        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getExecutionResultsSummary(executionId))
                .isInstanceOf(HttpServerErrorException.class)
                .hasStackTraceContaining("500 INTERNAL_SERVER_ERROR");
    }

    @Test
    public void testGetExecutionResultsSummaryHttpServerException() {
        // Arrange
        String executionId = "1234";
        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/automationobjectservice/api/Execution/" + executionId + "/Results/Summary";

        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), eq(entity), any(ParameterizedTypeReference.class))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getExecutionResultsSummary(executionId))
                .isInstanceOf(HttpClientErrorException.class)
                .hasStackTraceContaining("400 BAD_REQUEST");
    }

    @Test
    public void testGetPartialExecutionResultsSuccess() throws Exception {
        // Arrange
        String executionId = "1234";

        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/automationobjectservice/api/Execution/" + executionId + "/Results?partial=true";

        ResponseEntity<String> response = ResponseEntity.ok("responseBody");
        when(restTemplate.exchange(url, HttpMethod.GET, entity, String.class)).thenReturn(response);
        when(utilities.extractDataFromEntity(response)).thenReturn(response.getBody());

        // Act
        String result = toscaExecutionAPIHandler.getPartialExecutionResults(executionId);

        // Assert
        assertThat(result).isEqualTo("responseBody");
    }

    @Test
    public void testGetPartialExecutionResultsEmptyResponseBody() throws Exception {
        // Arrange
        String executionId = "1234";

        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/automationobjectservice/api/Execution/" + executionId + "/Results?partial=true";

        ResponseEntity<String> response = ResponseEntity.ok("");
        when(restTemplate.exchange(url, HttpMethod.GET, entity, String.class)).thenReturn(response);

        // Act
        String result = toscaExecutionAPIHandler.getPartialExecutionResults(executionId);

        // Assert
        assertThat(result).isEqualTo("");
    }

    @Test
    public void testGetPartialExecutionResultsNullResponseBody() throws Exception {
        // Arrange
        String executionId = "1234";

        String accessToken = "test_access_token";
        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/automationobjectservice/api/Execution/" + executionId + "/Results?partial=true";

        when(restTemplate.exchange(url, HttpMethod.GET, entity, String.class)).thenReturn(null);

        // Act
        String result = toscaExecutionAPIHandler.getPartialExecutionResults(executionId);

        // Assert
        assertThat(result).isEqualTo("");
    }

    @Test
    public void testGetPartialExecutionResultsFailedToGetAccessToken() {
        // Arrange
        String executionId = "1234";
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act and Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.getPartialExecutionResults(executionId))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testCancelExecutionSuccess() throws Exception {
        // Arrange
        String executionId = "12345";
        String accessToken = "test_access_token";
        String responseBody = "Execution cancelled successfully";

        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/automationobjectservice/api/Execution/" + executionId + "/Cancel";
        when(restTemplate.exchange(url, HttpMethod.PUT, entity, String.class)).thenReturn(ResponseEntity.ok(responseBody));

        // Act
        toscaExecutionAPIHandler.CancelExecution(executionId);

        // Assert
        verify(restTemplate).exchange(url, HttpMethod.PUT, entity, String.class);
    }

    @Test
    public void testCancelExecutionHttpClientException() throws Exception {
        // Arrange
        String executionId = "12345";
        String accessToken = "test_access_token";

        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://serverGateway:123/automationobjectservice/api/Execution/" + executionId + "/Cancel";
        when(restTemplate.exchange(url, HttpMethod.PUT, entity, String.class)).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.CancelExecution(executionId))
                .isInstanceOf(HttpClientErrorException.class);
    }

    @Test
    public void testCancelExecutionHttpServerException() throws Exception {
        // Arrange
        String executionId = "12345";
        String accessToken = "test_access_token";

        Map<String, String> responseBodyToken = new HashMap<>();
        responseBodyToken.put("access_token", accessToken);
        ResponseEntity<Map> responseEntityToken = ResponseEntity.ok(responseBodyToken);
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenReturn(responseEntityToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://serverGateway:123/automationobjectservice/api/Execution/" + executionId + "/Cancel";
        when(restTemplate.exchange(url, HttpMethod.PUT, entity, String.class)).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.CancelExecution(executionId))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    public void testCancelExecutionFailedToGetAccessToken() throws Exception {
        // Arrange
        String executionId = "12345";
        when(restTemplate.exchange(
                "https://" + toscaConfiguration.getToscaServerGateway() + ":" + toscaConfiguration.getToscaServerPort() + "/tua/connect/token",
                HttpMethod.POST,
                createRequestEntityAccessToken(),
                Map.class
        )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> toscaExecutionAPIHandler.CancelExecution(executionId))
                .isInstanceOf(HttpClientErrorException.class)
                .hasStackTraceContaining("400 BAD_REQUEST");
    }

    private HttpEntity<MultiValueMap<String, String>> createRequestEntityAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        return new HttpEntity<>(body, headers);
    }

}