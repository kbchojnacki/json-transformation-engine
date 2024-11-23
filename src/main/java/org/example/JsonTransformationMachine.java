package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.function.Predicate;

public class JsonTransformationMachine {
    private static final int MAX_LOOP_ITERATIONS = 10000;
    private static final int MAX_TOTAL_OPERATIONS = 100000;

    private final ObjectMapper mapper;
    private JsonNode currentState;
    private JsonNode rootState;
    private String currentPath;
    private final StackManager stacks;
    private final Map<Integer, Integer> loopCounters;
    private int loopLevel;
    private int totalOperations;
    private boolean debug;

    public JsonTransformationMachine(JsonNode initialState, boolean debug) {
        this.mapper = new ObjectMapper();
        this.currentState = initialState;
        this.rootState = initialState;
        this.currentPath = "$";
        this.stacks = new StackManager();
        this.loopCounters = new HashMap<>();
        this.loopLevel = 0;
        this.totalOperations = 0;
        this.debug = debug;
    }

    public JsonTransformationMachine(JsonNode initialState) {
        this(initialState, false);
    }

    public JsonNode execute(List<Instruction> instructions) {
        for (Instruction instruction : instructions) {
            executeInstruction(instruction);
            checkOperationLimit();
        }
        return rootState;
    }

    private void executeInstruction(Instruction instruction) {
        if (debug) {
            System.out.printf("Executing: %s at path: %s%n", instruction, currentPath);
        }

        try {
            switch (instruction.getCommand()) {
                // Navigation commands
                case MOVE_TO_ROOT:
                    moveToRoot();
                    break;
                case MOVE_OUT:
                    moveOut();
                    break;
                case MOVE_INTO_OBJECT:
                    moveIntoObject((String) resolveValue(instruction.getParams()[0]));
                    break;
                case MOVE_INTO_ARRAY:
                    moveIntoArray((String) resolveValue(instruction.getParams()[0]));
                    break;
                case MOVE_INTO_INDEX:
                    moveIntoIndex(((Number) resolveValue(instruction.getParams()[0])).intValue());
                    break;
                case MOVE_INTO_FIELD:
                    moveIntoField((String) resolveValue(instruction.getParams()[0]));
                    break;

                // Stack operations
                case PUSH_ELEMENT:
                    stacks.elements().push(currentState.deepCopy());
                    break;
                case POP_ELEMENT:
                    setCurrentElement(stacks.elements().pop());
                    break;
                case STORE_ELEMENT:
                    setCurrentElement((JsonNode)instruction.getParams()[0]);
                    break;
                case DUPLICATE_ELEMENT:
                    if (!stacks.elements().isEmpty()) {
                        JsonNode topElement = stacks.elements().peek();
                        if (topElement != null) {
                            stacks.elements().push(topElement.deepCopy());
                        } else {
                            stacks.elements().push(null);
                        }
                    }
                    break;
                case SWAP_ELEMENTS:
                    if (stacks.elements().size() >= 2) {
                        JsonNode top = stacks.elements().pop();
                        JsonNode second = stacks.elements().pop();
                        stacks.elements().push(top);
                        stacks.elements().push(second);
                    }
                    break;
                case ROTATE_ELEMENTS:
                    if (stacks.elements().size() >= 3) {
                        JsonNode top = stacks.elements().pop();
                        JsonNode middle = stacks.elements().pop();
                        JsonNode bottom = stacks.elements().pop();
                        stacks.elements().push(middle);
                        stacks.elements().push(top);
                        stacks.elements().push(bottom);
                    }
                    break;
                case MAP_ELEMENT:
                    @SuppressWarnings("unchecked")
                    Map<String, JsonNode> mapping = (Map<String, JsonNode>) (instruction.getParams()[0]);
                    if (!stacks.elements().isEmpty()) {
                        JsonNode val = stacks.elements().pop();
                        Object key = unwrap(val);
                        JsonNode mappedValue = mapping.getOrDefault(key, val).deepCopy();
                        stacks.elements().push(mappedValue);
                    }
                    break;
                // Value stack operations
                case STORE_VALUE:
                    stacks.values().push(resolveValue(instruction.getParams()[0]));
                    break;
                case POP_VALUE:
                    stacks.values().pop();
                    break;
                case INCREMENT:
                    if (!stacks.values().isEmpty() && stacks.values().peek() instanceof Number) {
                        Number num = (Number) stacks.values().pop();
                        stacks.values().push(num.intValue() + 1);
                    }
                    break;
                case DECREMENT:
                    if (!stacks.values().isEmpty() && stacks.values().peek() instanceof Number) {
                        Number num = (Number) stacks.values().pop();
                        stacks.values().push(num.intValue() - 1);
                    }
                    break;
                case COMPARE:
                    compareValues((String) instruction.getParams()[0]);
                    break;
                case ADD:
                    if (stacks.values().size() >= 2) {
                        Number val2 = (Number) stacks.values().pop();
                        Number val1 = (Number) stacks.values().pop();
                        stacks.values().push(val1.intValue() + val2.intValue());
                    }
                    break;
                case SUBTRACT:
                    if (stacks.values().size() >= 2) {
                        Number val2 = (Number) stacks.values().pop();
                        Number val1 = (Number) stacks.values().pop();
                        stacks.values().push(val1.intValue() - val2.intValue());
                    }
                    break;
                case NEGATE:
                    if (!stacks.values().isEmpty()) {
                        Number val = (Number) stacks.values().pop();
                        stacks.values().push(-val.intValue());
                    }
                    break;
                // Size operations
                case STORE_SIZE:
                    storeSize();
                    break;
                case DUPLICATE_VALUE:
                    if (!stacks.values().isEmpty()) {
                        stacks.values().push(stacks.values().peek());
                    }
                    break;
                case SWAP_VALUES:
                    if (stacks.values().size() >= 2) {
                        Object top = stacks.values().pop();
                        Object second = stacks.values().pop();
                        stacks.values().push(top);
                        stacks.values().push(second);
                    }
                    break;
                case ROTATE_VALUES:
                    if (stacks.values().size() >= 3) {
                        Object top = stacks.values().pop();
                        Object middle = stacks.values().pop();
                        Object bottom = stacks.values().pop();
                        stacks.values().push(middle);
                        stacks.values().push(top);
                        stacks.values().push(bottom);
                    }
                    break;
                case ELEMENT_TO_VALUE:
                    if(!stacks.elements().isEmpty()) {
                        JsonNode top = stacks.elements().pop();
                        stacks.values().push(unwrap(top));
                    }
                    break;
                case VALUE_TO_ELEMENT:
                    if(!stacks.values().isEmpty()) {
                        Object top = stacks.values().pop();
                        stacks.elements().push(mapper.valueToTree(top));
                    }
                    break;
                // Control flow
                case PUSH_COMMAND:
                    @SuppressWarnings("unchecked")
                    List<Instruction> commands = (List<Instruction>) instruction.getParams()[0];
                    stacks.commands().push(commands);
                    break;
                case POP_COMMAND:
                    executeCommandSequence();
                    break;
                case LOOP_UNTIL:
                    @SuppressWarnings("unchecked")
                    Predicate<JsonTransformationMachine> condition =
                            (Predicate<JsonTransformationMachine>) instruction.getParams()[0];
                    loopUntil(condition);
                    break;
                case JUMP_IF_TRUE:
                    if (!stacks.values().isEmpty()) {
                        Object val = stacks.values().pop();
                        boolean con = (val instanceof Boolean && (Boolean) val)
                                || (val instanceof Number && ((Number) val).doubleValue() != 0);
                        if (con) {
                            executeInstruction((Instruction) instruction.getParams()[0]);
                        }
                    }
                    break;
                case JUMP_IF_FALSE:
                    Object val = stacks.values().pop();
                    boolean con = (val == null)
                            || (val instanceof Boolean && !(Boolean) val)
                            || (val instanceof Number && ((Number) val).doubleValue() == 0);
                    if (con) {
                        executeInstruction((Instruction) instruction.getParams()[0]);
                    }
                    break;
                case RESET:
                    this.currentState = rootState;
                    this.currentPath = "$";
                    this.stacks.clearAll();
                    this.loopLevel = 0;
                    break;
                case MERGE_ARRAYS:
                    mergeArrays();
                    break;
                case CONCAT_ARRAYS:
                    concatArrays();
                    break;
                case FILTER_ARRAY:
                    @SuppressWarnings("unchecked")
                    Predicate<JsonNode> filterPredicate =
                            (Predicate<JsonNode>) instruction.getParams()[0];
                    filterArray(filterPredicate);
                    break;
                case MAP_ARRAY:
                    @SuppressWarnings("unchecked")
                    List<Instruction> mapInstructions =
                            (List<Instruction>) instruction.getParams()[0];
                    mapArray(mapInstructions);
                    break;

                // Structure modifications
                case CREATE_OBJECT:
                    setCurrentElement(mapper.createObjectNode());
                    break;
                case CREATE_ARRAY:
                    setCurrentElement(mapper.createArrayNode());
                    break;
                case DELETE:
                    deleteCurrentElement();
                    break;
                case MERGE_OBJECTS:
                    mergeObjects();
                    break;
            }

            if (debug) {
                System.out.println("Path: " + currentPath);
                System.out.println(stacks.getDebugInfo());
                System.out.println(rootState.toPrettyString());
            }

        } catch (Exception e) {
            throw new RuntimeException(String.format(
                    "Error executing instruction %s at path %s: %s",
                    instruction, currentPath, e.getMessage()
            ), e);
        }
    }

    // Navigation methods
    private void moveToRoot() {
        currentState = rootState;
        currentPath = "$";
        stacks.paths().clear();
        stacks.states().clear();
    }

    private void moveOut() {
        if (!stacks.paths().isEmpty() && !stacks.states().isEmpty()) {
            currentPath = stacks.paths().pop();
            currentState = stacks.states().pop();
        }
    }

    private void moveIntoObject(String field) {
        stacks.paths().push(currentPath);
        stacks.states().push(currentState);

        if (currentState instanceof ObjectNode) {
            ObjectNode objNode = (ObjectNode) currentState;
            if (!objNode.has(field)) {
                objNode.set(field, mapper.createObjectNode());
            }
            currentState = objNode.get(field);
            currentPath = currentPath + "." + field;
        }
    }

    private void moveIntoArray(String field) {
        stacks.paths().push(currentPath);
        stacks.states().push(currentState);

        if (currentState instanceof ObjectNode) {
            ObjectNode objNode = (ObjectNode) currentState;
            if (!objNode.has(field)) {
                objNode.set(field, mapper.createArrayNode());
            }
            JsonNode arrayNode = objNode.get(field);
            if (!arrayNode.isArray()) {
                ArrayNode newArray = mapper.createArrayNode();
                objNode.set(field, newArray);
                arrayNode = newArray;
            }
            currentState = arrayNode;
            currentPath = currentPath + "." + field;
        }
    }

    private void moveIntoIndex(int index) {
        stacks.paths().push(currentPath);
        stacks.states().push(currentState);

        if (currentState instanceof ArrayNode) {
            ArrayNode arrayNode = (ArrayNode) currentState;
            while (arrayNode.size() <= index) {
                arrayNode.addObject();
            }
            currentState = arrayNode.get(index);
            currentPath = currentPath + "[" + index + "]";
        } else {
            throw new IllegalStateException(
                    "Cannot move to index " + index + " in non-array node at path: " + currentPath
            );
        }
    }

    private void moveIntoField(String field) {
        if (currentState instanceof ObjectNode) {
            ObjectNode objNode = (ObjectNode) currentState;
            // Create field with null if it doesn't exist
            if (!objNode.has(field)) {
                objNode.putNull(field);
            }

            stacks.paths().push(currentPath);
            stacks.states().push(currentState);
            currentState = objNode.get(field);
            currentPath = currentPath + "." + field;
        }
    }

    // Array operations
    private void mergeArrays() {
        if (stacks.elements().size() < 2) {
            throw new IllegalStateException("Need two arrays to merge");
        }

        JsonNode array2 = stacks.elements().pop();
        JsonNode array1 = stacks.elements().pop();

        if (!array1.isArray() || !array2.isArray()) {
            throw new IllegalStateException("Both elements must be arrays");
        }

        ArrayNode result = mapper.createArrayNode();
        array1.forEach(result::add);
        array2.forEach(result::add);

        stacks.elements().push(result);
    }

    private void concatArrays() {
        if (stacks.elements().size() < 2) {
            throw new IllegalStateException("Need two arrays to concatenate");
        }

        JsonNode array2 = stacks.elements().pop();
        JsonNode array1 = stacks.elements().pop();

        if (!array1.isArray() || !array2.isArray()) {
            throw new IllegalStateException("Both elements must be arrays");
        }

        ArrayNode result = array1.deepCopy();
        array2.forEach(((ArrayNode) result)::add);

        stacks.elements().push(result);
    }

    private void filterArray(Predicate<JsonNode> predicate) {
        if (!currentState.isArray()) {
            throw new IllegalStateException("Current state must be an array for filtering");
        }

        ArrayNode arrayNode = (ArrayNode) currentState;
        ArrayNode filtered = mapper.createArrayNode();

        for (JsonNode element : arrayNode) {
            if (predicate.test(element)) {
                filtered.add(element.deepCopy());
            }
        }

        setCurrentElement(filtered);
    }

    private void mapArray(List<Instruction> mapInstructions) {
        if (!currentState.isArray()) {
            throw new IllegalStateException("Current state must be an array for mapping");
        }

        ArrayNode arrayNode = (ArrayNode) currentState;
        ArrayNode mapped = mapper.createArrayNode();

        for (JsonNode element : arrayNode) {
            JsonTransformationMachine subMachine = new JsonTransformationMachine(element);
            mapped.add(subMachine.execute(mapInstructions));
        }

        setCurrentElement(mapped);
    }

    private void executeCommandSequence() {
        if (!stacks.commands().isEmpty()) {
            List<Instruction> commands = stacks.commands().peek();
            for (Instruction cmd : commands) {
                executeInstruction(cmd);
            }
        }
    }

    private void loopUntil(Predicate<JsonTransformationMachine> condition) {
        loopLevel++;
        int currentLoopId = loopLevel;
        loopCounters.put(currentLoopId, 0);

        try {
            while (!condition.test(this)) {
                int iterations = loopCounters.get(currentLoopId);
                if (iterations >= MAX_LOOP_ITERATIONS) {
                    throw new RuntimeException(
                            String.format("Loop iteration limit exceeded at level %d", currentLoopId)
                    );
                }

                executeCommandSequence();
                loopCounters.put(currentLoopId, iterations + 1);
                checkOperationLimit();
            }
        } finally {
            loopCounters.remove(currentLoopId);
            loopLevel--;

            if (!stacks.commands().isEmpty()) {
                stacks.commands().pop();
            }
        }
    }

    private void compareValues(String operator) {
        if (stacks.values().size() < 2) {
            throw new IllegalStateException("Need two values to compare");
        }

        Object val2 = stacks.values().pop();
        Object val1 = stacks.values().pop();

        switch (operator) {
            case "==":
                stacks.values().push(Objects.equals(val1, val2));
                break;
            case "!=":
                stacks.values().push(!Objects.equals(val1, val2));
                break;
            case "<":
                if (val1 instanceof Number && val2 instanceof Number) {
                    stacks.values().push(
                            ((Number) val1).doubleValue() < ((Number) val2).doubleValue()
                    );
                }
                break;
            case ">":
                if (val1 instanceof Number && val2 instanceof Number) {
                    stacks.values().push(
                            ((Number) val1).doubleValue() > ((Number) val2).doubleValue()
                    );
                }
                break;
            case "<=":
                if (val1 instanceof Number && val2 instanceof Number) {
                    stacks.values().push(
                            ((Number) val1).doubleValue() <= ((Number) val2).doubleValue()
                    );
                }
                break;
            case ">=":
                if (val1 instanceof Number && val2 instanceof Number) {
                    stacks.values().push(
                            ((Number) val1).doubleValue() >= ((Number) val2).doubleValue()
                    );
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    private Object unwrap(JsonNode node) {
        if(node.isTextual()) {
            return node.asText();
        }else if(node.isNumber()) {
            return node.numberValue();
        }else if(node.isBoolean()) {
            return node.asBoolean();
        }else if(node.isNull()) {
            return null;
        }else {
            return node;
        }
    }

    // Structure modification methods
    private void mergeObjects() {
        if (stacks.elements().size() < 2) {
            throw new IllegalStateException("Need two objects to merge");
        }

        JsonNode obj2 = stacks.elements().pop();
        JsonNode obj1 = stacks.elements().pop();

        if (!obj1.isObject() || !obj2.isObject()) {
            throw new IllegalStateException("Both elements must be objects");
        }

        ObjectNode merged = obj1.deepCopy();
        obj2.fields().forEachRemaining(entry ->
                ((ObjectNode) merged).set(entry.getKey(), entry.getValue().deepCopy())
        );

        stacks.elements().push(merged);
    }

    private void deleteCurrentElement() {
        JsonNode parent = stacks.states().peek();
        if (parent instanceof ObjectNode) {
            String field = getCurrentField();
            ((ObjectNode) parent).remove(field);
        } else if (parent instanceof ArrayNode) {
            int index = getCurrentArrayIndex();
            ((ArrayNode) parent).remove(index);
        }
    }

    // Helper methods
    private void setCurrentElement(JsonNode element) {
        JsonNode immediateParent = stacks.states().peek();
        if (immediateParent instanceof ArrayNode) {
            // We're setting an element within an array
            ArrayNode array = (ArrayNode) immediateParent;
            int index = getCurrentArrayIndex(); // Use current path's index

            if (index >= 0) {
                array.set(index, element);
            } else {
                throw new IllegalStateException(
                        "Cannot determine array index from path: " + currentPath
                );
            }
        } else if (immediateParent instanceof ObjectNode) {
            // Regular object field setting
            String field = getCurrentField();
            ((ObjectNode) immediateParent).set(field, element);
        }

        currentState = element;
    }

    private int getCurrentArrayIndex() {
        int bracketStart = currentPath.lastIndexOf('[');
        int bracketEnd = currentPath.lastIndexOf(']');
        if (bracketStart >= 0 && bracketEnd > bracketStart) {
            String indexStr = currentPath.substring(bracketStart + 1, bracketEnd);
            try {
                return Integer.parseInt(indexStr);
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                        "Invalid array index in path: " + currentPath
                );
            }
        }
        return -1;
    }


    private void storeSize() {
        if (currentState.isArray()) {
            stacks.values().push(currentState.size());
        } else if (currentState.isObject()) {
            stacks.values().push(currentState.size());
        } else {
            throw new IllegalStateException(
                    "Cannot get size of non-container node at path: " + currentPath
            );
        }
    }

    private Object resolveValue(Object value) {
        if (value instanceof ValueStackReference) {
            return ((ValueStackReference) value).resolve(this);
        }
        return value;
    }

    private String getCurrentField() {
        String[] parts = currentPath.split("\\.|\\[\\d+\\]");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        } else {
            throw new IllegalStateException("Invalid current path: " + currentPath);
        }
    }


    private void checkOperationLimit() {
        totalOperations++;
        if (totalOperations > MAX_TOTAL_OPERATIONS) {
            throw new RuntimeException(String.format(
                    "Operation limit exceeded: %d operations. Possible infinite loop detected.",
                    MAX_TOTAL_OPERATIONS
            ));
        }
    }

    // Public access methods
    public JsonNode getCurrentState() {
        return currentState;
    }

    public int getValueStackSize() {
        return stacks.values().size();
    }

    public int getElementStackSize() {
        return stacks.elements().size();
    }

    public Object peekValueStack(int offset) {
        return stacks.values().peek(offset);
    }

    public JsonNode peekElementStack(int offset) {
        return stacks.elements().peek(offset);
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public String getExecutionState() {
        return String.format(
                "Execution State:%n" +
                        "Current Path: %s%n" +
                        "Total Operations: %d%n" +
                        "Loop Level: %d%n" +
                        "Loop Counters: %s%n" +
                        "Stack Info:%n%s",
                currentPath,
                totalOperations,
                loopLevel,
                loopCounters,
                stacks.getDebugInfo()
        );
    }

    // Static utility methods for common operations
    public static Instruction pushCommands(Instruction... commands) {
        return Instruction.of(Command.PUSH_COMMAND, Arrays.asList(commands));
    }

    public static Instruction pushCommands(List<Instruction> commands) {
        return Instruction.of(Command.PUSH_COMMAND, commands);
    }

    // Debug helper
    private void debugState(String operation) {
        if (debug) {
            System.out.printf(
                    "%s:%nPath: %s%nCurrent State: %s%nStacks:%n%s%n",
                    operation,
                    currentPath,
                    currentState,
                    stacks.getDebugInfo()
            );
        }
    }

    public StackManager getStackManager() {
        return stacks;
    }
}