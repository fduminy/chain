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

/**
 * A command is stateless task running in a given context.
 *
 * @param <C> The type of context.
 */
@FunctionalInterface
public interface Command<C> {
    void execute(C context) throws CommandException;

    @SuppressWarnings("UnusedParameters")
    default void revert(C context) {
    }
}
