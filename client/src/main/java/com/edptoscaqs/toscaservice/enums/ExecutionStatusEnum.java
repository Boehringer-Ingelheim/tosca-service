package com.edptoscaqs.toscaservice.enums;

import com.edptoscaqs.toscaservice.logging.LoggerHelper;

import java.util.HashMap;
import java.util.Map;

public enum ExecutionStatusEnum {
    COMPLETED("Completed"),
    ERROR("Error"),
    CANCELLED("Cancelled"),
    IN_PROGRESS("InProgress"),
    COMPLETED_WITH_ERRORS("CompletedWithErrors"),
    COMPLETED_WITH_CANCELLATIONS("CompletedWithCancellations");

    private final String value;
    private static final Map<String, ExecutionStatusEnum> lookup = new HashMap<>();

    static {
        for (ExecutionStatusEnum status : ExecutionStatusEnum.values()) {
            lookup.put(status.getValue(), status);
        }
    }

    ExecutionStatusEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ExecutionStatusEnum get(String value) {
        ExecutionStatusEnum status = lookup.get(value);
        if (status == null) {
            throw new IllegalStateException("[GetExecutionStatus] Error - Unknown status returned: " + value);
        }
        return status;
    }

    public boolean handleStatus(LoggerHelper loggerHelper) {
        boolean continueCheckingStatus = false;
        loggerHelper.logDebug(String.format("[HandleStatus] Process starts - Status: %s", value));
        switch (this) {
            case COMPLETED
                    -> loggerHelper.logDebug("[HandleStatus] Process ends successfully - All test cases completed successfully.");
            case IN_PROGRESS -> {
                loggerHelper.logDebug("[HandleStatus] Status in progress");
                continueCheckingStatus = true;
            }
            default ->
                throw new IllegalStateException(String.format("The execution ends with an unexpected status: %s. No results will be get.", value));
        }
        return continueCheckingStatus;
    }

}
