package org.example;

import com.fasterxml.jackson.databind.JsonNode;

public class ValueStackReference {
    private final String field;         // Field name for object access
    private final Integer stackOffset;  // Offset from top of stack
    private final ValueType type;       // Type of reference

    private enum ValueType {
        FIELD,      // Reference to a field in current object
        STACK,      // Reference to stack value by offset
        CURRENT     // Reference to current value
    }

    // Constructor for field reference
    public ValueStackReference(String field) {
        this.field = field;
        this.stackOffset = null;
        this.type = ValueType.FIELD;
    }

    // Constructor for stack offset reference
    public ValueStackReference(int stackOffset) {
        this.field = null;
        this.stackOffset = stackOffset;
        this.type = ValueType.STACK;
    }

    // Constructor for current value reference
    public ValueStackReference() {
        this.field = null;
        this.stackOffset = null;
        this.type = ValueType.CURRENT;
    }

    public Object resolve(JsonTransformationMachine machine) {
        switch (type) {
            case FIELD:
                return resolveField(machine);
            case STACK:
                return resolveStack(machine);
            case CURRENT:
                return resolveCurrent(machine);
            default:
                throw new IllegalStateException("Unknown reference type: " + type);
        }
    }

    private Object resolveField(JsonTransformationMachine machine) {
        if (field == null) {
            throw new IllegalStateException("Field reference with null field name");
        }

        JsonNode currentState = machine.getCurrentState();
        if (currentState.has(field)) {
            JsonNode fieldValue = currentState.get(field);
            if (fieldValue.isNumber()) {
                return fieldValue.asDouble();
            } else if (fieldValue.isBoolean()) {
                return fieldValue.asBoolean();
            } else if (fieldValue.isTextual()) {
                return fieldValue.asText();
            } else {
                return fieldValue;
            }
        }
        return null;
    }

    private Object resolveStack(JsonTransformationMachine machine) {
        if (stackOffset == null) {
            throw new IllegalStateException("Stack reference with null offset");
        }
        return machine.peekValueStack(stackOffset);
    }

    private Object resolveCurrent(JsonTransformationMachine machine) {
        return machine.getCurrentState();
    }

    @Override
    public String toString() {
        switch (type) {
            case FIELD:
                return "FieldRef(" + field + ")";
            case STACK:
                return "StackRef(" + stackOffset + ")";
            case CURRENT:
                return "CurrentRef()";
            default:
                return "UnknownRef";
        }
    }

    // Static factory methods for cleaner creation
    public static ValueStackReference field(String field) {
        return new ValueStackReference(field);
    }

    public static ValueStackReference stack(int offset) {
        return new ValueStackReference(offset);
    }

    public static ValueStackReference current() {
        return new ValueStackReference();
    }
}