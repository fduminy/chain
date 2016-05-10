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
 * @param <Context> The type of Context.
 */
public class SimpleChain<Context, CMD extends Command<Context>> implements Chain<Context> {
    private final List<? extends CMD> commands;
    private final CommandListener listener;
    private final ListenerErrorLogger listenerErrorLogger;

    public SimpleChain(ListenerErrorLogger listenerErrorLogger, CMD... commands) {
        this(listenerErrorLogger, null, commands);
    }

    public SimpleChain(ListenerErrorLogger listenerErrorLogger, CommandListener listener, CMD... commands) {
        this.commands = Arrays.asList(commands);
        this.listener = listener;
        this.listenerErrorLogger = (listenerErrorLogger == null) ? DefaultListenerErrorLogger.INSTANCE : listenerErrorLogger;
    }

    @Override
    public void execute(Context context) throws Exception {
        executeCommands(commands, context);
    }

    private void executeCommands(List<? extends CMD> commands, Context context) throws Exception {
        if (commands == null) {
            return;
        }

        for (CMD command : commands) {
            if (listener != null) {
                try {
                    listener.commandStarted(command, context);
                } catch (Exception errorInListener) {
                    logError("commandStarted", command, context, errorInListener);
                }
            }

            Exception errorInCommand = null;
            try {
                command.execute(context);
            } catch (Exception e) {
                logError("execute", command, context, e);
                errorInCommand = e;
                throw e;
            } finally {
                if (listener != null) {
                    try {
                        listener.commandFinished(command, context, errorInCommand);
                    } catch (Exception errorInListener) {
                        logError("commandFinished", command, context, errorInListener);
                    }
                }
                if (errorInCommand != null) {
                    command.revert(context);
                }
            }
        }
    }

    private void logError(String method, Command command, Context context, Exception errorInListener) {
        listenerErrorLogger.logError(method, command, context, errorInListener);
    }

    public interface ListenerErrorLogger<Context> {
        void logError(String method, Command command, Context context, Exception errorInListener);
    }

    static class DefaultListenerErrorLogger<Context> implements ListenerErrorLogger<Context> {
        static final DefaultListenerErrorLogger INSTANCE = new DefaultListenerErrorLogger();

        private DefaultListenerErrorLogger() {
        }

        @Override
        public void logError(String method, Command command, Context context, Exception errorInListener) {
            final Logger logger = Logger.getLogger(getClass().getName());
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, String.format("Error in %s(%s, %s)", method, command, context), errorInListener);
            }
        }
    }
}
