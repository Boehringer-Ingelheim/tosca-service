package com.edptoscaqs.toscaservice;

import com.edptoscaqs.toscaservice.configuration.ToscaConfigParameters;
import com.edptoscaqs.toscaservice.logging.LoggerHelper;
import com.edptoscaqs.toscaservice.utilities.Utilities;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class ToscaExecutionAPIHandler {
    @Autowired
    private ToscaConfigParameters toscaConfiguration;

    @Autowired
    private LoggerHelper loggerHelper;

    @Autowired
    private Utilities utilities;

    private final String EXECUTION_ENQUEUE_URL = "https://{gateway}:{port}/automationobjectservice/api/Execution/Enqueue";
    private final String STATUS_URL = "https://{gateway}:{port}/automationobjectservice/api/Execution/{executionid}/Status";
    private final String RESULTS_URL = "https://{gateway}:{port}/automationobjectservice/api/Execution/{executionid}/Results";
    private final String RESULTS_SUMMARY_URL = "https://{gateway}:{port}/automationobjectservice/api/Execution/{executionid}/Results/Summary";
    private final String PARTIAL_RESULTS_URL = "https://{gateway}:{port}/automationobjectservice/api/Execution/{executionid}/Results?partial=true";
    private final String CANCEL_EXECUTION_URL = "https://{gateway}:{port}/automationobjectservice/api/Execution/{executionid}/Cancel";
    private final String TOKEN_URL = "https://{gateway}:{port}/tua/connect/token";

    private String executionAPIAccessToken;
    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
    }

    public JSONObject triggerEventExecution(String projectName, String executionEnvironment, String eventId, Map<String, String> eventParameters, Map<String, String> executionCharacteristics) throws Exception {
        loggerHelper.logDebug(String.format("[TriggerEventExecution] Process starts - Project Name: %s - Environment: %s - Test Event Id: %s", projectName, executionEnvironment, eventId));
        try {
            executionAPIAccessToken = getExecutionApiAccessToken();
        } catch (Exception e) {
            throw e;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization","Bearer " + executionAPIAccessToken);
        String url = UriComponentsBuilder
                .fromHttpUrl(EXECUTION_ENQUEUE_URL)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort())
                .toUriString();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("projectName", projectName);
        requestBody.put("executionEnvironment", executionEnvironment);
        List<Map<String, Object>> events = new ArrayList<>();
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        Map<String, Object> parameters = new HashMap<>(eventParameters);
        event.put("parameters", parameters);
        Map<String, Object> characteristics = new HashMap<>(executionCharacteristics);
        event.put("characteristics", characteristics);
        events.add(event);
        requestBody.put("events", events);
        requestBody.put("importResult", true);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        loggerHelper.logDebug(String.format("[TriggerEventExecution] URL: %s", url));
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );
            if (response!= null && response.hasBody() && !Objects.requireNonNull(response.getBody()).isEmpty()) {
                loggerHelper.logDebug(String.format("[TriggerEventExecution] Process ends successfully - Project Name: %s - Environment: %s - Test Event Id: %s", projectName, executionEnvironment, eventId));
                return new JSONObject(response.getBody());
            } else {
                throw new IllegalArgumentException(String.format("[TriggerEventExecution] Process ends with an error: Response body is empty - Project Name: %s - Environment: %s - Test Event Id: %s", projectName, executionEnvironment, eventId));
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public String getEventExecutionStatus(String executionId) throws Exception {
        return getEventExecutionData(executionId, "status");
    }

    public boolean isResultImported(String executionId) throws Exception {
        return getEventExecutionData(executionId, "isResultImported") == "true";
    }

    public String getEventExecutionData(String executionId, String dataType) throws Exception {
        loggerHelper.logDebug(String.format("[GetEventExecutionData] Process starts - Execution id: %s", executionId));
        HttpHeaders headers = getHeader();
        String url = UriComponentsBuilder
                .fromHttpUrl(STATUS_URL)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), executionId)
                .toUriString();
        HttpEntity<String> request = new HttpEntity<>(headers);
        loggerHelper.logDebug(String.format("[GetEventExecutionData] URL: %s", url));
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );
            if (response != null && response.hasBody() && !Objects.requireNonNull(response.getBody()).isEmpty()) {
                JSONObject jsonBody = new JSONObject(response.getBody());
                loggerHelper.logDebug(String.format("[GetEventExecutionData] Process ends successfully - Execution id: %s", executionId));
                return jsonBody.getAsString(dataType);
            } else {
                throw new IllegalArgumentException(String.format("[GetEventExecutionData] Process ends with an error: Response body is empty - Execution id: %s", executionId));
            }
        } catch (Exception e) {
            throw e;
        }
    }

    protected HttpHeaders getHeader() throws Exception {
        try {
            executionAPIAccessToken = getExecutionApiAccessToken();
        } catch (Exception e) {
            throw e;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tricentis", "OK");
        headers.set("Authorization","Bearer " + executionAPIAccessToken);
        return headers;
    }

    public String getExecutionResults(String executionId) throws Exception {
        loggerHelper.logDebug(String.format("[GetExecutionResults] Process starts - Execution Id: %s", executionId));
        HttpEntity<String> entity = new HttpEntity<>(getHeader());

        String url = UriComponentsBuilder
                .fromHttpUrl(RESULTS_URL)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), executionId)
                .toUriString();
        loggerHelper.logDebug(String.format("[GetExecutionResults] URL: %s", url));
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response !=null && response.hasBody() && !Objects.requireNonNull(response.getBody()).isEmpty()) {
                loggerHelper.logDebug(String.format("[GetExecutionResults] Process ends successfully - Execution Id: %s", executionId));
                return utilities.extractDataFromEntity(response);
            } else {
                throw new IllegalArgumentException(String.format("[GetExecutionResults] Process ends with an error: Response body is empty - Execution Id: %s", executionId));
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public Map<String, Integer> getExecutionResultsSummary(String executionId) throws Exception {
        loggerHelper.logDebug(String.format("[GetExecutionResultsSummary] Process starts - Execution ID: %s", executionId));
        HttpEntity<String> entity = new HttpEntity<>(getHeader());
        String url = UriComponentsBuilder
                .fromHttpUrl(RESULTS_SUMMARY_URL)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), executionId)
                .toUriString();
        loggerHelper.logDebug(String.format("[GetExecutionResultsSummary] URL: %s", url));
        try {
            ResponseEntity<Map<String, Integer>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
            if (response.getBody() == null || Objects.requireNonNull(response.getBody()).isEmpty()) {
                throw new IllegalArgumentException(String.format("[GetExecutionResultsSummary] Process ends with an error: Response body is null/empty - Execution ID: %s", executionId));
            } else {
                loggerHelper.logDebug(String.format("[GetExecutionResultsSummary] Process ends successfully - Execution ID: %s", executionId));
                return response.getBody();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public void CancelExecution(String executionId) throws Exception {
        loggerHelper.logInfo(String.format("Proceeding to cancel the execution - Execution Id: %s", executionId));
        loggerHelper.logDebug(String.format("[CancelExecution] Process starts - Execution Id: %s", executionId));
        HttpEntity<String> entity = new HttpEntity<>(getHeader());

        String url = UriComponentsBuilder
                .fromHttpUrl(CANCEL_EXECUTION_URL)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), executionId)
                .toUriString();
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                loggerHelper.logInfo(String.format("Execution cancelled successfully - Execution Id: %s", executionId));
                loggerHelper.logDebug(String.format("[CancelExecution] Process ends successfully - Execution Id: %s", executionId));
            } else {
                loggerHelper.logInfo(String.format("An error occurred during the cancellation of the execution - Execution Id: %s", executionId));
                throw new HttpClientErrorException(response.getStatusCode(), String.format("[CancelExecution] Process ends with an error - Execution Id: %s", executionId));
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw e;
        } catch (Exception e) {
            throw new Exception("[CancelExecution] Process ends with an exception - An unexpected error occurred while cancelling execution", e);
        }
    }

    public String getPartialExecutionResults(String executionId) throws Exception {
        loggerHelper.logDebug(String.format("[GetPartialExecutionResult] Process starts - Execution Id: %s", executionId));
        HttpEntity<String> entity = new HttpEntity<>(getHeader());

        String url = UriComponentsBuilder
                .fromHttpUrl(PARTIAL_RESULTS_URL)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort(), executionId)
                .toUriString();
        loggerHelper.logDebug(String.format("[GetPartialExecutionResult] URL: %s", url));
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!(response.getBody() == null || Objects.requireNonNull(response.getBody()).isEmpty())) {
                loggerHelper.logDebug(String.format("[GetPartialExecutionResult] Process ends successfully - Execution Id: %s", executionId));
                return utilities.extractDataFromEntity(response);
            }
        } catch (Exception e) {
        }
        return "";
    }

    protected String getExecutionApiAccessToken() throws Exception{
        loggerHelper.logDebug("[GetToken] Process starts");
        String url = UriComponentsBuilder
                .fromHttpUrl(TOKEN_URL)
                .buildAndExpand(toscaConfiguration.getToscaServerGateway(), toscaConfiguration.getToscaServerPort())
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        if (toscaConfiguration.getToscaClientId() != null && toscaConfiguration.getToscaClientSecret() != null) {
            headers.setBasicAuth(toscaConfiguration.getToscaClientId(), toscaConfiguration.getToscaClientSecret());
        } else {
            throw new IllegalArgumentException("[GetToken] Error - Tosca Client ID and/or Tosca Client Secret cannot be null");
        }
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        loggerHelper.logDebug(String.format("[GetToken] URL: %s", url));
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                if (response.hasBody()) {
                    JSONObject jsonBody = new JSONObject(Objects.requireNonNull(response.getBody()));
                    loggerHelper.logDebug("[GetToken] Process ends successfully");
                    return jsonBody.getAsString("access_token");
                } else {
                    throw new IllegalStateException("[GetToken] Process ends with an error: Response body is empty when requesting access token");
                }
            } else {
                throw new HttpClientErrorException(response.getStatusCode());
            }
        }
        catch (Exception e) {
            throw e;
        }
    }

}