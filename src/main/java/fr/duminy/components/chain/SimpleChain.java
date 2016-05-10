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

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple implementation of the chain of responsibility pattern.
 *
 * @param <C> The type of context.
 * @param <T> The type of {@link Command}.
 */
public class SimpleChain<C, T extends Command<C>> implements Chain<C> {
    private final List<? extends T> commands;
    private final CommandListener listener;
    private final ListenerErrorLogger listenerErrorLogger;

    public SimpleChain(ListenerErrorLogger listenerErrorLogger, T... commands) {
        this(listenerErrorLogger, null, commands);
    }

    public SimpleChain(ListenerErrorLogger listenerErrorLogger, CommandListener listener, T... commands) {
        this.commands = Arrays.asList(commands);
        this.listener = listener;
        this.listenerErrorLogger = (listenerErrorLogger == null) ? DefaultListenerErrorLogger.INSTANCE : listenerErrorLogger;
    }

    @Override
    public void execute(C context) throws CommandException {
        executeCommands(commands, context);
    }

    private void executeCommands(List<? extends T> commands, C context) throws CommandException {
        if (commands == null) {
            return;
        }

        for (T command : commands) {
            notifyCommandStarted(context, command);

            Exception errorInCommand = null;
            try {
                command.execute(context);
            } catch (Exception e) {
                logError("execute", command, context, e);
                errorInCommand = e;
                throw e;
            } finally {
                notifyCommandFinished(context, command, errorInCommand);
                if (errorInCommand != null) {
                    command.revert(context);
                }
            }
        }
    }

    private void notifyCommandStarted(C context, T command) {
        if (listener != null) {
            try {
                listener.commandStarted(command, context);
            } catch (Exception errorInListener) {
                logError("commandStarted", command, context, errorInListener);
            }
        }
    }

    private void notifyCommandFinished(C context, T command, Exception errorInCommand) {
        if (listener != null) {
            try {
                listener.commandFinished(command, context, errorInCommand);
            } catch (Exception errorInListener) {
                logError("commandFinished", command, context, errorInListener);
            }
        }
    }

    private void logError(String method, Command command, C context, Exception errorInListener) {
        listenerErrorLogger.logError(method, command, context, errorInListener);
    }

    @FunctionalInterface
    public interface ListenerErrorLogger<D> {
        void logError(String method, Command command, D context, Exception errorInListener);
    }

    static class DefaultListenerErrorLogger<D> implements ListenerErrorLogger<D> {
        static final DefaultListenerErrorLogger INSTANCE = new DefaultListenerErrorLogger();

        private DefaultListenerErrorLogger() {
        }

        @Override
        public void logError(String method, Command command, D context, Exception errorInListener) {
            final Logger logger = Logger.getLogger(getClass().getName());
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, String.format("Error in %s(%s, %s)", method, command, context), errorInListener);
            }
        }
    }
}
