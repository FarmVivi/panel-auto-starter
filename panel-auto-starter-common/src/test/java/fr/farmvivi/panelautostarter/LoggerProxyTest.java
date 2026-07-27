package fr.farmvivi.panelautostarter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.logging.Logger;
import java.util.logging.Level;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour LoggerProxy.
 */
public class LoggerProxyTest {

    @Mock
    private Logger mockDelegateLogger;

    private LoggerProxy loggerProxy;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        loggerProxy = new LoggerProxy(mockDelegateLogger);
    }

    @Test
    public void testLoggerProxyInitialization() {
        assertNotNull("LoggerProxy ne doit pas être null", loggerProxy);
    }

    @Test
    public void testGetName() {
        when(mockDelegateLogger.getName()).thenReturn("test-logger");
        assertEquals("Le nom doit être test-logger", "test-logger", loggerProxy.getName());
    }

    @Test
    public void testGetResourceBundleName() {
        when(mockDelegateLogger.getResourceBundleName()).thenReturn("bundle");
        assertEquals("Le nom du bundle doit être bundle", "bundle", loggerProxy.getResourceBundleName());
    }

    @Test
    public void testInfoLevelWithString() {
        loggerProxy.info("Test message");

        verify(mockDelegateLogger).info("Test message");
    }

    @Test
    public void testWarningLevelWithString() {
        loggerProxy.warning("Warning message");

        verify(mockDelegateLogger).warning("Warning message");
    }

    @Test
    public void testSevereLevelWithString() {
        loggerProxy.severe("Severe message");

        verify(mockDelegateLogger).severe("Severe message");
    }

    @Test
    public void testFineLevelWithString() {
        loggerProxy.fine("Fine message");

        verify(mockDelegateLogger).fine("Fine message");
    }

    @Test
    public void testLogsNotCalledIfNotLoggable() {
        when(mockDelegateLogger.isLoggable(Level.FINE)).thenReturn(false);

        loggerProxy.fine("Not logged message");

        verify(mockDelegateLogger, never()).log(any(Level.class), anyString());
    }

    @Test
    public void testLogWithLevelAndString() {
        when(mockDelegateLogger.isLoggable(Level.INFO)).thenReturn(true);

        loggerProxy.log(Level.INFO, "Message");

        verify(mockDelegateLogger).log(any(Level.class), anyString());
    }

    @Test
    public void testLogWithThrowable() {
        when(mockDelegateLogger.isLoggable(Level.SEVERE)).thenReturn(true);
        Throwable testException = new RuntimeException("Test error");

        loggerProxy.log(Level.SEVERE, "Error occurred", testException);

        verify(mockDelegateLogger).log(Level.SEVERE, "Error occurred", testException);
    }

    @Test
    public void testLogWithStringArrayParams() {
        when(mockDelegateLogger.isLoggable(Level.INFO)).thenReturn(true);
        Object[] params = {"param1", "param2"};

        loggerProxy.log(Level.INFO, "Message with {0} and {1}", params);

        verify(mockDelegateLogger).log(Level.INFO, "Message with {0} and {1}", params);
    }
}
