/**
 * Chain is a library implementing the chain of responsability pattern.
 *
 * Copyright (C) 2016-2016 Fabien DUMINY (fabien [dot] duminy [at] webmails [dot] com)
 *
 * Chain is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * Chain is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package fr.duminy.components.chain;


import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.internal.InOrderImpl;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@SuppressWarnings({"unchecked", "ThrowableResultOfMethodCallIgnored"})
@RunWith(Theories.class)
public class SimpleChainTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private Command command1;

    @Mock
    private Command command2;

    @Mock
    private Map context;

    @Mock
    private CommandListener listener;

    @Mock
    private SimpleChain.ListenerErrorLogger errorLogger;

    @Before
    public void setUp() {
        initMocks(this);
        doAnswer(logMessage("commandStarted", null)).when(listener).commandStarted(any(Command.class), eq(context));
        doAnswer(logMessage("commandFinished", null)).when(listener).commandFinished(any(Command.class), eq(context), any(Exception.class));
        doAnswer(logMessage("logError", null)).when(errorLogger).logError(anyString(), any(Command.class), eq(context), any(Exception.class));
    }

    @Test
    public void testExecute_runAll() throws Exception {
        SimpleChain chain = new SimpleChain(errorLogger, command1, command2);

        executeAndVerifyRunAll(chain);
    }

    @Theory
    public void testExecute_runAll_errorInLogger(boolean errorInStarted) throws Exception {
        Exception errorInLogger = new Exception("an error");
        thrown.expect(errorInLogger.getClass());
        thrown.expectMessage(errorInLogger.getMessage());
        doThrow(errorInLogger).when(errorLogger).logError(eq(errorInStarted ? "commandStarted" : "commandFinished"), eq(command1), eq(context), eq(errorInLogger));
        SimpleChain chain = new SimpleChain(errorLogger, command1);

        chain.execute(context);

        InOrder inOrder = inOrder(command1, command2, listener, errorLogger);
        if (errorInStarted) {
            inOrder.verify(errorLogger).logError(eq("commandStarted"), eq(command1), eq(context), eq(errorInLogger));
        }
        inOrder.verify(command1).execute(eq(context));
        if (!errorInStarted) {
            inOrder.verify(errorLogger).logError(eq("commandFinished"), eq(command1), eq(context), eq(errorInLogger));
        }
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testExecute_runAll_defaultErrorLogger() throws Exception {
        SimpleChain chain = new SimpleChain(null, command1, command2);

        executeAndVerifyRunAll(chain);
    }

    @Test
    public void testExecute_runAllWithSubChain() throws Exception {
        SimpleChain subChain = new SimpleChain(errorLogger, command1);
        SimpleChain chain = new SimpleChain(errorLogger, subChain, command2);

        executeAndVerifyRunAll(chain);
    }

    @Test
    public void testExecute_runAllWithListener() throws Exception {
        SimpleChain chain = new SimpleChain(errorLogger, listener, command1, command2);

        chain.execute(context);

        ExtInOrder inOrder = inOrder(command1, command2, listener, errorLogger);
        inOrder.verifyExecuteWithListener(command1, null);
        inOrder.verifyExecuteWithListener(command2, null);
        inOrder.verifyNoMoreInteractions();
    }

    @Theory
    public void testExecute_runAllWithErrorInListener(boolean errorInStarted) throws Exception {
        Exception errorInListener = throwExceptionInListener(listener, command1, context, errorInStarted);
        SimpleChain chain = new SimpleChain(errorLogger, listener, command1, command2);

        chain.execute(context);

        ExtInOrder inOrder = inOrder(command1, command2, listener, errorLogger);
        inOrder.verifyExecuteWithListener(command1, null, errorInStarted, !errorInStarted, errorInListener);
        inOrder.verifyExecuteWithListener(command2, null);
        inOrder.verifyNoMoreInteractions();
    }

    @Theory
    public void testExecute_runAllWithErrorInListener_defaultErrorLogger(boolean errorInStarted) throws Exception {
        throwExceptionInListener(listener, command1, context, errorInStarted);
        SimpleChain chain = new SimpleChain(null, listener, command1, command2);

        chain.execute(context);

        ExtInOrder inOrder = inOrder(command1, command2, listener, errorLogger);
        inOrder.verifyExecuteWithListener(command1, null);
        inOrder.verifyExecuteWithListener(command2, null);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testExecute_firstCommandInSubChainFails() throws Exception {
        SimpleChain subChain = spy(new SimpleChain(errorLogger, command1));
        Exception errorInCommand = throwExceptionInCommand(command1);
        SimpleChain chain = new SimpleChain(errorLogger, subChain, command2);

        try {
            chain.execute(context);
        } finally {
            ExtInOrder inOrder = inOrder(command1, command2, listener, errorLogger);
            inOrder.verify(command1).execute(eq(context));
            inOrder.verifyErrorLogger("execute", command1, errorInCommand);
            inOrder.verify(command1).revert(eq(context));
            inOrder.verifyErrorLogger("execute", subChain, errorInCommand);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testExecute_firstCommandFails() throws Exception {
        Exception errorInCommand = throwExceptionInCommand(command1);
        SimpleChain chain = new SimpleChain(errorLogger, command1, command2);

        try {
            chain.execute(context);
        } finally {
            ExtInOrder inOrder = inOrder(command1, command2, listener, errorLogger);
            inOrder.verify(command1).execute(eq(context));
            inOrder.verifyErrorLogger("execute", command1, errorInCommand);
            inOrder.verify(command1).revert(eq(context));
            inOrder.verifyNoMoreInteractions();
        }
    }

    public ExtInOrder inOrder(Object... mocks) {
        return new ExtInOrder(mocks);
    }

    public class ExtInOrder extends InOrderImpl {
        public ExtInOrder(Object... mocks) {
            super(Arrays.asList(mocks));
        }

        public void verifyExecuteWithListener(Command command, Exception errorInCommand) throws Exception {
            verifyExecuteWithListener(command, errorInCommand, false, false, null);
        }

        public void verifyExecuteWithListener(Command command, Exception errorInCommand, boolean errorInStarted, boolean errorInFinished, Exception errorInListener) throws Exception {
            verify(listener).commandStarted(eq(command), eq(context));
            if (errorInStarted) {
                verifyErrorLogger("commandStarted", command, errorInListener);
            }

            verify(command).execute(eq(context));

            verify(listener).commandFinished(eq(command), eq(context), eq(errorInCommand));
            if (errorInFinished) {
                verify(errorLogger).logError(eq("commandFinished"), eq(command), eq(context), eq(errorInListener));
            }
        }

        public void verifyErrorLogger(String method, Command command, Exception errorInListener) {
            verify(errorLogger).logError(eq(method), eq(command), eq(context), eq(errorInListener));
        }
    }

    private Exception throwExceptionInCommand(Command command) throws Exception {
        Exception errorInCommand = new Exception("an error");
        thrown.expect(errorInCommand.getClass());
        thrown.expectMessage(errorInCommand.getMessage());
        doThrow(errorInCommand).when(command).execute(eq(context));
        return errorInCommand;
    }

    private Exception throwExceptionInListener(CommandListener listener, Command command, Map context, boolean errorInStarted) throws Exception {
        final Exception errorInListener = new Exception("an error");
        if (errorInStarted) {
            doAnswer(logMessage("commandStarted", errorInListener)).when(listener).commandStarted(eq(command), eq(context));
        } else {
            doAnswer(logMessage("commandFinished", errorInListener)).when(listener).commandFinished(eq(command), eq(context), isNull(Exception.class));
        }
        return errorInListener;
    }

    static Answer logMessage(final String message, final Exception error) {
        return invocation -> {
            System.out.println(message + ": " + invocation);
            if (error != null) {
                System.out.println("throw error: " + error);
                throw error;
            }
            return null;
        };
    }

    private void executeAndVerifyRunAll(SimpleChain chain) throws Exception {
        chain.execute(context);

        InOrder inOrder = inOrder(command1, command2, listener, errorLogger);
        inOrder.verify(command1).execute(eq(context));
        inOrder.verify(command2).execute(eq(context));
        inOrder.verifyNoMoreInteractions();
    }
}
