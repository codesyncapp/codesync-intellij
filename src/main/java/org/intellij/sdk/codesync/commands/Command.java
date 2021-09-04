package org.intellij.sdk.codesync.commands;

/*
Interface that can be used as a base type for all commands.
ref: Command Design Pattern
ref: https://refactoring.guru/design-patterns/command
 */
public interface Command {
    public void execute();
}
