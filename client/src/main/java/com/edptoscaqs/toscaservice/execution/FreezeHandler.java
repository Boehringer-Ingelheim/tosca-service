package com.edptoscaqs.toscaservice.execution;

import com.edptoscaqs.toscaservice.ToscaRestAPIHandler;
import com.edptoscaqs.toscaservice.configuration.ToscaConfigParameters;
import com.edptoscaqs.toscaservice.logging.LoggerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static com.edptoscaqs.toscaservice.configuration.Constants.*;

@Service
public class FreezeHandler {
    @Autowired
    private ToscaConfigParameters toscaConfiguration;

    @Autowired
    private LoggerHelper loggerHelper;

    private final ToscaRestAPIHandler toscaRestAPIHandler;

    private Map<String, String> executionListsOwningGroupsBeforeFreeze;
    private Map<String, String> testCasesOwningGroupsBeforeFreeze;
    private boolean isTestEventFrozenAlready;
    private int testCaseCounter;

    @Autowired
    public FreezeHandler(ToscaRestAPIHandler toscaRestAPIHandler,
                         ToscaConfigParameters toscaConfiguration,
                         LoggerHelper loggerHelper) {
        this.toscaRestAPIHandler = toscaRestAPIHandler;
        this.toscaConfiguration = toscaConfiguration;
        this.loggerHelper = loggerHelper;
    }

    public void freezeTestEvent(boolean releaseExecution) throws Exception {
        loggerHelper.logDebug(String.format("[FreezeTestEvent] Process starts - Test Event Name: %s", toscaConfiguration.getTestEventName()));
        List<String> executionLists;
        testCaseCounter = 0;
        try {
            toscaRestAPIHandler.updateAll();
            executionLists = toscaRestAPIHandler.listExecutionLists(toscaConfiguration.getTestEventName());
            if (executionLists.isEmpty()) {
                throw new IllegalArgumentException(String.format("[FreezeTestEvent] Process ends with an error: No execution lists have been found within test event %s", toscaConfiguration.getTestEventName()));
            }
            for (String executionListId : executionLists) {
                testCaseCounter += toscaRestAPIHandler.listTestCasesInExecutionList(executionListId).size();
            }
        } catch (Exception e) {
            throw e;
        }
        isTestEventFrozenAlready = checkTestEventExistsAndIsNotYetFrozen(toscaConfiguration.getTestEventName());
        if (!isTestEventFrozenAlready && releaseExecution) {
            try {
                String testEventId = toscaRestAPIHandler.getTestEventUniqueId(toscaConfiguration.getTestEventName());
                executionListsOwningGroupsBeforeFreeze = new HashMap<>();
                for (String uniqueId : executionLists) {
                    executionListsOwningGroupsBeforeFreeze.put(uniqueId, toscaRestAPIHandler.getObjectProperty(uniqueId, PROPERTY_OWNING_GROUP_NAME));
                    freezeTestCases(uniqueId);
                    changeOwningGroupName(uniqueId, toscaConfiguration.getEdpLockGroupName());
                }
                changeOwningGroupName(testEventId, toscaConfiguration.getEdpLockGroupName());
                toscaRestAPIHandler.checkInAll();
                loggerHelper.logInfo("Test event has been successfully frozen.");
                loggerHelper.logDebug(String.format("[FreezeTestEvent] Process ends successfully - Test Event Name: %s", toscaConfiguration.getTestEventName()));
            } catch (Exception e) {
                toscaRestAPIHandler.revertAll();
                throw e;
            }
        }
        else {
            loggerHelper.logDebug(String.format("[FreezeTestEvent] Process skipped - Test Event Name: %s", toscaConfiguration.getTestEventName()));
        }
    }

    private boolean checkTestEventExistsAndIsNotYetFrozen(String testEventName) throws Exception {
        return Objects.equals(toscaRestAPIHandler.getObjectProperty(toscaRestAPIHandler.getTestEventUniqueId(testEventName), PROPERTY_OWNING_GROUP_NAME), toscaConfiguration.getEdpLockGroupName());
    }

    protected void freezeTestCases(String executionListId) {
        try {
            loggerHelper.logDebug(String.format("[FreezeTestEvent] Process starts - Execution List Id: %s", executionListId));
            List<String> testCaseList = toscaRestAPIHandler.listTestCasesInExecutionList(executionListId);
            if (testCaseList.isEmpty()) {
                loggerHelper.logWarning(String.format("[FreezeTestEvent] Process ends with a warning: No test cases within the execution list - Execution List Id: %s", executionListId));
            }
            testCasesOwningGroupsBeforeFreeze = new HashMap<>();
            for (String uniqueId : testCaseList) {
                testCasesOwningGroupsBeforeFreeze.put(uniqueId, toscaRestAPIHandler.getObjectProperty(uniqueId, PROPERTY_OWNING_GROUP_NAME));
                changeOwningGroupName(uniqueId, toscaConfiguration.getEdpLockGroupName());
            }
        } catch (Exception e) {
            throw e;
        }
    }

    protected void changeOwningGroupName(String uniqueId, String owningGroupName) {
        try {
            loggerHelper.logDebug(String.format("[ChangeOwningGroup] Process starts - Object Id: %s", uniqueId));
            String checkOutStatus = toscaRestAPIHandler.getObjectProperty(uniqueId, PROPERTY_CHECKOUT_STATE);
            if (!Objects.equals(checkOutStatus, CHECKED_IN_STATUS)) {
                throw new IllegalArgumentException(String.format("[ChangeOwningGroup] Process ends with an error: Object has an unexpected status - Object Id: %s - Status: %s ", uniqueId, checkOutStatus));
            }
            toscaRestAPIHandler.checkOutObject(uniqueId);
            loggerHelper.logDebug("Changing owning group to " + owningGroupName);
            toscaRestAPIHandler.changeObjectOwningGroup(uniqueId);
            String owningGroup = toscaRestAPIHandler.getObjectProperty(uniqueId, PROPERTY_OWNING_GROUP_NAME);
            if (!Objects.equals(owningGroup, owningGroupName)) {
                throw new IllegalArgumentException(String.format("[ChangeOwningGroup] Process ends with an error: Object has an unexpected group name - Object Id: %s - Group Name: %s ", uniqueId, owningGroup));
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public int getTestCaseCounter(){
        return testCaseCounter;
    }
}