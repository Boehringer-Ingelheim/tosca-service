package com.edptoscaqs.toscaservice.execution;

import com.edptoscaqs.toscaservice.ToscaRestAPIHandler;
import com.edptoscaqs.toscaservice.configuration.ToscaConfigParameters;
import com.edptoscaqs.toscaservice.logging.LoggerHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static com.edptoscaqs.toscaservice.configuration.Constants.*;

public class FreezeHandlerTest {
    @Mock
    private ToscaRestAPIHandler toscaRestAPIHandler;
    @Mock
    private ToscaConfigParameters toscaConfiguration;
    @Mock
    private LoggerHelper loggerHelper;
    @InjectMocks
    private FreezeHandler freezeHandler;
   
    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        doNothing().when(loggerHelper).logInfo(anyString());
        doNothing().when(loggerHelper).logWarning(anyString());
        doNothing().when(loggerHelper).logError(anyString());
        doNothing().when(loggerHelper).logDebug(anyString());
        doNothing().when(loggerHelper).logException(new Exception());
        when(toscaConfiguration.getTestEventName()).thenReturn("SampleTestEvent");
        when(toscaConfiguration.getEdpLockGroupName()).thenReturn("EDP_Lock_Group");
    }

    @Test
    public void testFreezeTestEventSuccess() throws Exception {
        // Arrange
        String testEventUniqueId = "1234";
        List<String> executionLists = Arrays.asList("executionList1", "executionList2");
        List<String> testCases = Arrays.asList("testCase1", "testCase2");
        boolean releaseExecution = true;
        String edpLockGroup = "EDP_LOCK_GROUP";

        when(toscaConfiguration.getEdpLockGroupName()).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.listExecutionLists(toscaConfiguration.getTestEventName())).thenReturn(executionLists);
        when(toscaRestAPIHandler.getTestEventUniqueId(toscaConfiguration.getTestEventName())).thenReturn(testEventUniqueId);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_OWNING_GROUP_NAME))).thenReturn(OWNING_GROUP_ALL_USERS).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.listTestCasesInExecutionList(anyString())).thenReturn(testCases);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_CHECKOUT_STATE))).thenReturn(CHECKED_IN_STATUS);
        doNothing().when(toscaRestAPIHandler).checkOutObject(anyString());
        doNothing().when(toscaRestAPIHandler).changeObjectOwningGroup(anyString());
        doNothing().when(toscaRestAPIHandler).checkInAll();

        // Act
        freezeHandler.freezeTestEvent(releaseExecution);

        // Assert
        verify(toscaRestAPIHandler, times(1)).listExecutionLists(toscaConfiguration.getTestEventName());
        verify(toscaRestAPIHandler, times(7)).checkOutObject(anyString());
        verify(toscaRestAPIHandler, times(7)).changeObjectOwningGroup(anyString());
        verify(toscaRestAPIHandler, times(14)).getObjectProperty(anyString(), eq(PROPERTY_OWNING_GROUP_NAME));
        verify(toscaRestAPIHandler, times(7)).getObjectProperty(anyString(), eq(PROPERTY_CHECKOUT_STATE));
        assertThatCode(() -> freezeHandler.freezeTestEvent(releaseExecution)).doesNotThrowAnyException();
    }

    @Test
    public void testFreezeTestEventWhenNoReleaseNoFreeze() throws Exception{
        // Arrange
        String testEventUniqueId = "1234";
        List<String> executionLists = List.of("executionList1");
        boolean releaseExecution = false;

        when(toscaRestAPIHandler.getTestEventUniqueId(toscaConfiguration.getTestEventName())).thenReturn(testEventUniqueId);
        when(toscaRestAPIHandler.getObjectProperty(testEventUniqueId,  PROPERTY_OWNING_GROUP_NAME)).thenReturn(OWNING_GROUP_ALL_USERS);
        when(toscaRestAPIHandler.listExecutionLists(toscaConfiguration.getTestEventName())).thenReturn(executionLists);

        // Act
        freezeHandler.freezeTestEvent(releaseExecution);

        // Assert
        verify(toscaRestAPIHandler, times(1)).listExecutionLists(anyString());
        verify(toscaRestAPIHandler, times(0)).checkOutObject(anyString());
        verify(toscaRestAPIHandler, times(0)).changeObjectOwningGroup(anyString());
        verify(toscaRestAPIHandler, times(0)).checkInAll();
        verify(toscaRestAPIHandler, times(1)).getObjectProperty(anyString(),  eq(PROPERTY_OWNING_GROUP_NAME));
        verify(toscaRestAPIHandler, times(0)).getObjectProperty(anyString(), eq(PROPERTY_CHECKOUT_STATE));
        verify(toscaRestAPIHandler, times(0)).getObjectProperty(anyString(),  eq(PROPERTY_REVISION));
    }

    @Test
    public void testFreezeTestEventWhenOwningGroupAlreadyChangedNoFreeze() throws Exception{
        // Arrange
        String testEventUniqueId = "1234";
        List<String> executionLists = List.of("executionList1");
        boolean releaseExecution = true;
        String testEventName = toscaConfiguration.getTestEventName();
        String edpLockGroup = toscaConfiguration.getEdpLockGroupName();

        when(toscaRestAPIHandler.getTestEventUniqueId(testEventName)).thenReturn(testEventUniqueId);
        when(toscaRestAPIHandler.getObjectProperty(testEventUniqueId,  PROPERTY_OWNING_GROUP_NAME)).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.listExecutionLists(testEventName)).thenReturn(executionLists);

        // Act
        freezeHandler.freezeTestEvent(releaseExecution);

        // Assert
        verify(toscaRestAPIHandler, times(1)).listExecutionLists(anyString());
        verify(toscaRestAPIHandler, times(0)).checkOutObject(anyString());
        verify(toscaRestAPIHandler, times(0)).changeObjectOwningGroup(anyString());
        verify(toscaRestAPIHandler, times(0)).checkInAll();
        verify(toscaRestAPIHandler, times(1)).getObjectProperty(anyString(), eq(PROPERTY_OWNING_GROUP_NAME));
        verify(toscaRestAPIHandler, times(0)).getObjectProperty(anyString(), eq(PROPERTY_CHECKOUT_STATE));
        verify(toscaRestAPIHandler, times(0)).getObjectProperty(anyString(),  eq(PROPERTY_REVISION));
    }

    @Test
    public void testFreezeTestEventEmptyExecutionLists() throws Exception {
        // Arrange
        String testEventUniqueId = "1234";
        List<String> executionLists = Collections.emptyList();
        boolean releaseExecution = true;

        String edpLockGroup = toscaConfiguration.getEdpLockGroupName();
        when(toscaRestAPIHandler.listExecutionLists(toscaConfiguration.getTestEventName())).thenReturn(executionLists);
        when(toscaRestAPIHandler.getTestEventUniqueId(toscaConfiguration.getTestEventName())).thenReturn(testEventUniqueId);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_OWNING_GROUP_NAME))).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.getObjectProperty(testEventUniqueId, PROPERTY_OWNING_GROUP_NAME)).thenReturn(OWNING_GROUP_ALL_USERS).thenReturn(edpLockGroup);
        doNothing().when(toscaRestAPIHandler).revertAll();

        // Act & Assert
        assertThatThrownBy(() -> freezeHandler.freezeTestEvent(releaseExecution))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testFreezeTestEventWhenOwningGroupIsNotCorrectThenException() throws Exception {
        // Arrange
        String testEventUniqueId = "1234";
        List<String> executionLists = List.of("executionList1");
        boolean releaseExecution = true;

        when(toscaRestAPIHandler.listExecutionLists(toscaConfiguration.getTestEventName())).thenReturn(executionLists);
        when(toscaRestAPIHandler.getTestEventUniqueId(toscaConfiguration.getTestEventName())).thenReturn(testEventUniqueId);
        when(toscaRestAPIHandler.getObjectProperty(anyString(),  eq(PROPERTY_OWNING_GROUP_NAME))).thenReturn("incorrectOwningGroup");
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_CHECKOUT_STATE))).thenReturn(CHECKED_IN_STATUS);
        doNothing().when(toscaRestAPIHandler).checkOutObject(anyString());
        doNothing().when(toscaRestAPIHandler).changeObjectOwningGroup(anyString());
        doNothing().when(toscaRestAPIHandler).checkInAll();
        doNothing().when(toscaRestAPIHandler).revertAll();

        // Act & Assert
        assertThatThrownBy(() -> freezeHandler.freezeTestEvent(releaseExecution))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testFreezeTestEventWhenListExecutionListsRestClientExceptionThenException() throws Exception {
        // Arrange
        String testEventUniqueId = "1234";
        boolean releaseExecution = true;

        when(toscaRestAPIHandler.listExecutionLists(toscaConfiguration.getTestEventName())).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        when(toscaRestAPIHandler.getTestEventUniqueId(toscaConfiguration.getTestEventName())).thenReturn(testEventUniqueId);
        when(toscaRestAPIHandler.getObjectProperty(testEventUniqueId,  PROPERTY_OWNING_GROUP_NAME)).thenReturn(OWNING_GROUP_ALL_USERS);

        // Act & Assert
        assertThatThrownBy(() -> freezeHandler.freezeTestEvent(releaseExecution))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testFreezeTestEventWhenCheckoutTreeRestClientExceptionThenException() throws Exception {
        // Arrange
        String testEventUniqueId = "1234";
        List<String> executionLists = List.of("executionList1");
        boolean releaseExecution = true;

        String testEventName = toscaConfiguration.getTestEventName();
        String edpLockGroup = toscaConfiguration.getEdpLockGroupName();
        when(toscaRestAPIHandler.listExecutionLists(testEventName)).thenReturn(executionLists);
        when(toscaRestAPIHandler.getTestEventUniqueId(testEventName)).thenReturn(testEventUniqueId);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_OWNING_GROUP_NAME))).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.getObjectProperty(testEventUniqueId, PROPERTY_OWNING_GROUP_NAME)).thenReturn(OWNING_GROUP_ALL_USERS).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_CHECKOUT_STATE))).thenReturn(CHECKED_IN_STATUS);
        doNothing().when(toscaRestAPIHandler).revertAll();
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST))
                .when(toscaRestAPIHandler)
                .checkOutObject(anyString());

        // Act & Assert
        assertThatThrownBy(() -> freezeHandler.freezeTestEvent(releaseExecution))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testFreezeTestEventWhenRevertAllExceptionThenExceptionAndSpecialLog() throws Exception {
        // Arrange
        String testEventUniqueId = "1234";
        List<String> executionLists = List.of("executionList1");
        boolean releaseExecution = true;
        String testEventName = toscaConfiguration.getTestEventName();
        String edpLockGroup = toscaConfiguration.getEdpLockGroupName();

        when(toscaRestAPIHandler.listExecutionLists(testEventName)).thenReturn(executionLists);
        when(toscaRestAPIHandler.getTestEventUniqueId(testEventName)).thenReturn(testEventUniqueId);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_OWNING_GROUP_NAME))).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.getObjectProperty(testEventUniqueId, PROPERTY_OWNING_GROUP_NAME)).thenReturn(OWNING_GROUP_ALL_USERS).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_CHECKOUT_STATE))).thenReturn(CHECKED_IN_STATUS);
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST))
                .when(toscaRestAPIHandler)
                .revertAll();
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST))
                .when(toscaRestAPIHandler)
                .checkOutObject(anyString());

        // Act & Assert
        assertThatThrownBy(() -> freezeHandler.freezeTestEvent(releaseExecution))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    public void testFreezeTestEventWhenChangeOwningGroupRestClientExceptionThenException() throws Exception {
        // Arrange
        String testEventUniqueId = "1234";
        List<String> executionLists = List.of("executionList1");
        boolean releaseExecution = true;
        String testEventName = toscaConfiguration.getTestEventName();
        String edpLockGroup = toscaConfiguration.getEdpLockGroupName();

        when(toscaRestAPIHandler.listExecutionLists(testEventName)).thenReturn(executionLists);
        when(toscaRestAPIHandler.getTestEventUniqueId(testEventName)).thenReturn(testEventUniqueId);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_OWNING_GROUP_NAME))).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.getObjectProperty(testEventUniqueId, PROPERTY_OWNING_GROUP_NAME)).thenReturn(OWNING_GROUP_ALL_USERS).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_CHECKOUT_STATE))).thenReturn(CHECKED_IN_STATUS);
        doNothing().when(toscaRestAPIHandler).checkOutObject(anyString());
        doNothing().when(toscaRestAPIHandler).revertAll();
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST))
                .when(toscaRestAPIHandler)
                .changeObjectOwningGroup(anyString());

        // Act & Assert
        assertThatThrownBy(() -> freezeHandler.freezeTestEvent(releaseExecution))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testFreezeTestEventWhenCheckInAllRestClientExceptionThenException() throws Exception {
        // Arrange
        String testEventUniqueId = "1234";
        List<String> executionLists = List.of("executionList1");
        List<String> testCases = Arrays.asList("testCase1", "testCase2");
        boolean releaseExecution = true;
        String testEventName = toscaConfiguration.getTestEventName();
        String edpLockGroup = toscaConfiguration.getEdpLockGroupName();
        when(toscaRestAPIHandler.listExecutionLists(testEventName)).thenReturn(executionLists);
        when(toscaRestAPIHandler.getTestEventUniqueId(testEventName)).thenReturn(testEventUniqueId);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_OWNING_GROUP_NAME))).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.getObjectProperty(testEventUniqueId, PROPERTY_OWNING_GROUP_NAME)).thenReturn(OWNING_GROUP_ALL_USERS).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.listTestCasesInExecutionList(anyString())).thenReturn(testCases);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_CHECKOUT_STATE))).thenReturn(CHECKED_IN_STATUS);
        doNothing().when(toscaRestAPIHandler).checkOutObject(anyString());
        doNothing().when(toscaRestAPIHandler).changeObjectOwningGroup(anyString());
        doNothing().when(toscaRestAPIHandler).revertAll();
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST))
                .when(toscaRestAPIHandler)
                .checkInAll();

        // Act & Assert
        assertThatThrownBy(() -> freezeHandler.freezeTestEvent(releaseExecution))
                .isInstanceOf(Exception.class)
                .hasStackTraceContaining("400 BAD_REQUEST");
    }

    @Test
    public void testFreezeTestEventWhenUpdateAllRestClientExceptionThenException() throws Exception {
        // Arrange
        String testEventUniqueId = "1234";
        List<String> executionLists = List.of("executionList1");
        boolean releaseExecution = true;
        String testEventName = toscaConfiguration.getTestEventName();
        String edpLockGroup = toscaConfiguration.getEdpLockGroupName();

        when(toscaRestAPIHandler.listExecutionLists(testEventName)).thenReturn(executionLists);
        when(toscaRestAPIHandler.getTestEventUniqueId(testEventName)).thenReturn(testEventUniqueId);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_OWNING_GROUP_NAME))).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.getObjectProperty(testEventUniqueId, PROPERTY_OWNING_GROUP_NAME)).thenReturn(OWNING_GROUP_ALL_USERS).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_CHECKOUT_STATE))).thenReturn(CHECKED_IN_STATUS);
        doNothing().when(toscaRestAPIHandler).checkOutObject(anyString());
        doNothing().when(toscaRestAPIHandler).changeObjectOwningGroup(anyString());
        doNothing().when(toscaRestAPIHandler).checkInAll();
        doNothing().when(toscaRestAPIHandler).revertAll();
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST))
                .when(toscaRestAPIHandler)
                .updateAll();

        // Act & Assert
        assertThatThrownBy(() -> freezeHandler.freezeTestEvent(releaseExecution))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testFreezeTestEventWhenGetExecutionListInfoRestClientExceptionThenException() throws Exception {
        // Arrange
        String testEventUniqueId = "1234";
        List<String> executionLists = List.of("executionList1");
        boolean releaseExecution = true;
        String testEventName = toscaConfiguration.getTestEventName();
        String edpLockGroup = toscaConfiguration.getEdpLockGroupName();
        when(toscaRestAPIHandler.listExecutionLists(testEventName)).thenReturn(executionLists);
        when(toscaRestAPIHandler.getTestEventUniqueId(testEventName)).thenReturn(testEventUniqueId);
        when(toscaRestAPIHandler.getObjectProperty(anyString(),  eq(PROPERTY_OWNING_GROUP_NAME))).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.getObjectProperty(testEventUniqueId,  PROPERTY_OWNING_GROUP_NAME)).thenReturn(OWNING_GROUP_ALL_USERS).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.getObjectProperty(anyString(),  eq(PROPERTY_OWNING_GROUP_NAME))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_CHECKOUT_STATE))).thenReturn(CHECKED_IN_STATUS);
        doNothing().when(toscaRestAPIHandler).checkOutObject(anyString());
        doNothing().when(toscaRestAPIHandler).changeObjectOwningGroup(anyString());
        doNothing().when(toscaRestAPIHandler).checkInAll();
        doNothing().when(toscaRestAPIHandler).updateAll();
        doNothing().when(toscaRestAPIHandler).revertAll();

        // Act & Assert
        assertThatThrownBy(() -> freezeHandler.freezeTestEvent(releaseExecution))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    public void testFreezeTestCasesSuccess() throws Exception {
        // Arrange
        String executionListId = "1234";
        List<String> testCases = Arrays.asList("testCase1", "testCase2");
        String edpLockGroup = "EDP_LOCK_GROUP";

        when(toscaConfiguration.getEdpLockGroupName()).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.listTestCasesInExecutionList(executionListId)).thenReturn(testCases);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_OWNING_GROUP_NAME))).thenReturn(edpLockGroup);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_CHECKOUT_STATE))).thenReturn(CHECKED_IN_STATUS);
        doNothing().when(toscaRestAPIHandler).checkOutObject(anyString());
        doNothing().when(toscaRestAPIHandler).changeObjectOwningGroup(anyString());

        // Act
        freezeHandler.freezeTestCases(executionListId);

        // Assert
        verify(toscaRestAPIHandler, times(1)).listTestCasesInExecutionList(executionListId);
        verify(toscaRestAPIHandler, times(2)).checkOutObject(anyString());
        verify(toscaRestAPIHandler, times(2)).changeObjectOwningGroup(anyString());
        verify(toscaRestAPIHandler, times(4)).getObjectProperty(anyString(), eq(PROPERTY_OWNING_GROUP_NAME));
        verify(toscaRestAPIHandler, times(2)).getObjectProperty(anyString(), eq(PROPERTY_CHECKOUT_STATE));
        assertThatCode(() -> freezeHandler.freezeTestCases(executionListId)).doesNotThrowAnyException();
    }

    @Test
    public void testFreezeTestCasesEmptyTestCases() {
        // Arrange
        String executionListId = "1234";
        List<String> testCases = Collections.emptyList();
        when(toscaRestAPIHandler.listTestCasesInExecutionList(anyString())).thenReturn(testCases);
        doNothing().when(toscaRestAPIHandler).checkOutObject(anyString());
        doNothing().when(toscaRestAPIHandler).changeObjectOwningGroup(anyString());

        // Act
        freezeHandler.freezeTestCases(executionListId);

        // Assert
        verify(loggerHelper, times(1)).logWarning(anyString());
    }

    @Test
    public void testFreezeTestCasesWhenOwningGroupIsNotCorrectThenException() {
        // Arrange
        String executionListId = "1234";
        List<String> testCases = List.of("testCase1");
        when(toscaRestAPIHandler.listTestCasesInExecutionList(executionListId)).thenReturn(testCases);
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_OWNING_GROUP_NAME))).thenReturn("incorrectOwningGroup");
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_CHECKOUT_STATE))).thenReturn(CHECKED_IN_STATUS);
        doNothing().when(toscaRestAPIHandler).checkOutObject(anyString());
        doNothing().when(toscaRestAPIHandler).changeObjectOwningGroup(anyString());

        // Act & Assert
        assertThatThrownBy(() -> freezeHandler.freezeTestCases(executionListId))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testFreezeTestCasesWhenListTestCasesRestClientExceptionThenException() {
        // Arrange
        String executionListId = "1234";
        when(toscaRestAPIHandler.listTestCasesInExecutionList(executionListId)).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThatThrownBy(() -> freezeHandler.freezeTestCases(executionListId))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testChangeOwningGroupNameWhenCheckOutStateIsNotCheckedInThenException() {
        // Arrange
        String uniqueId = "1234";
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_OWNING_GROUP_NAME))).thenReturn("incorrectOwningGroup");
        when(toscaRestAPIHandler.getObjectProperty(anyString(), eq(PROPERTY_CHECKOUT_STATE))).thenReturn("CheckedOut");
        doNothing().when(toscaRestAPIHandler).checkOutObject(anyString());
        doNothing().when(toscaRestAPIHandler).changeObjectOwningGroup(anyString());

        // Act & Assert
        assertThatThrownBy(() -> freezeHandler.changeOwningGroupName(uniqueId, anyString()))
                .isInstanceOf(Exception.class);
    }
}
