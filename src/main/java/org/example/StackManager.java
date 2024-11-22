package org.example;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class StackManager {
    private final TransformationStack<JsonNode> elementStack;
    private final TransformationStack<Object> valueStack;
    private final TransformationStack<List<Instruction>> commandStack;
    private final TransformationStack<String> pathStack;
    private final TransformationStack<JsonNode> stateStack;

    public StackManager() {
        this.elementStack = new TransformationStack<>("Element");
        this.valueStack = new TransformationStack<>("Value");
        this.commandStack = new TransformationStack<>("Command");
        this.pathStack = new TransformationStack<>("Path");
        this.stateStack = new TransformationStack<>("State");
    }

    // Stack access methods
    public TransformationStack<JsonNode> elements() {
        return elementStack;
    }

    public TransformationStack<Object> values() {
        return valueStack;
    }

    public TransformationStack<List<Instruction>> commands() {
        return commandStack;
    }

    public TransformationStack<String> paths() {
        return pathStack;
    }

    public TransformationStack<JsonNode> states() {
        return stateStack;
    }

    public void clearAll() {
        elementStack.clear();
        valueStack.clear();
        commandStack.clear();
        pathStack.clear();
        stateStack.clear();
    }

    public String getDebugInfo() {
        return String.format(
                "Stacks state:\n" +
                        "Elements: %d\n" +
                        "Elements content: %s\n" +
                        "Values: %d\n" +
                        "Values content: %s\n" +
                        "Commands: %d\n" +
                        "Paths: %d\n" +
                        "States: %d",
                elementStack.size(),
                elementStack,
                valueStack.size(),
                valueStack,
                commandStack.size(),
                pathStack.size(),
                stateStack.size()
        );
    }
}