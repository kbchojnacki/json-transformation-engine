package org.example;

import java.util.Arrays;

public class Instruction {
    private final Command command;
    private final Object[] params;
    private final boolean storeLocation;

    private Instruction(Command command, boolean storeLocation, Object... params) {
        this.command = command;
        this.storeLocation = storeLocation;
        this.params = params;
    }

    public static Instruction of(Command command, Object... params) {
        return new Instruction(command, false, params);
    }

    public static Instruction withStore(Command command, Object... params) {
        return new Instruction(command, true, params);
    }

    public Command getCommand() {
        return command;
    }

    public Object[] getParams() {
        return params;
    }

    public boolean shouldStoreLocation() {
        return storeLocation;
    }

    @Override
    public String toString() {
        return String.format("Instruction{command=%s, storeLocation=%s, params=%s}",
                command, storeLocation, Arrays.toString(params));
    }
}