package org.example;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class JsonPathTransformer {
    /**
     * Generates instructions to move value(s) from source path to target path
     */
    public static List<Instruction> generateMoveInstructions(String sourcePath, String targetPath, Map<String, JsonNode> mapping) {
        List<PathComponent> sourceComponents = JsonPathParser.parse(sourcePath);
        List<PathComponent> targetComponents = JsonPathParser.parse(targetPath);

        // Check if target has array pattern but source doesn't
        boolean isReplication = !containsPattern(sourceComponents) && containsPattern(targetComponents);

        if (isReplication) {
            return generateReplicationInstructions(sourceComponents, targetComponents, mapping);
        } else if (containsPattern(sourceComponents)) {
            return generatePatternMoveInstructions(sourceComponents, targetComponents, mapping);
        } else {
            return generateSimpleMoveInstructions(sourceComponents, targetComponents, mapping);
        }
    }

    private static List<Instruction> generateReplicationInstructions(
            List<PathComponent> sourceComponents,
            List<PathComponent> targetComponents,
            Map<String, JsonNode> mapping
    ) {
        List<Instruction> instructions = new ArrayList<>();
        instructions.add(Instruction.of(Command.RESET));

        // First get the source value
        instructions.add(Instruction.of(Command.MOVE_TO_ROOT));

        // Navigate to source and store value
        for (PathComponent comp : sourceComponents) {
            if (comp.getType() == PathComponent.Type.FIELD) {
                instructions.add(Instruction.of(Command.MOVE_INTO_FIELD, comp.getField()));
            }
        }
        instructions.add(Instruction.of(Command.PUSH_ELEMENT));
        if (mapping != null) {
            instructions.add(Instruction.of(Command.MAP_ELEMENT, mapping));
        }
        instructions.add(Instruction.of(Command.MOVE_TO_ROOT));

        // Find target array component
        PathComponent arrayComponent = findArrayComponent(targetComponents);
        if (arrayComponent == null) {
            throw new IllegalArgumentException("No array pattern found in target path");
        }

        // Navigate to target array
        for (PathComponent comp : targetComponents) {
            if (comp == arrayComponent) {
                break;
            }
            if (comp.getType() == PathComponent.Type.FIELD) {
                instructions.add(Instruction.of(Command.MOVE_INTO_OBJECT, comp.getField()));
            }
        }

        // Create array if needed
        instructions.add(Instruction.of(Command.MOVE_INTO_ARRAY, arrayComponent.getField()));

        // Store array size or create initial size
        instructions.add(Instruction.of(Command.STORE_SIZE));
        instructions.addAll(Arrays.asList(
                // Ensure we have at least one element
                Instruction.of(Command.STORE_VALUE, 1),
                Instruction.of(Command.COMPARE, ">"),
                Instruction.of(Command.JUMP_IF_FALSE, Instruction.of(Command.CREATE_ARRAY))
        ));
        instructions.add(Instruction.of(Command.STORE_SIZE));
        instructions.add(Instruction.of(Command.STORE_VALUE, 0)); // Counter

        // Create loop body
        List<Instruction> loopBody = new ArrayList<>();

        // Start from array for each iteration
        loopBody.add(Instruction.of(Command.MOVE_INTO_INDEX, new ValueStackReference(0)));

        // Create or navigate to object structure
        boolean foundArray = false;
        for (PathComponent comp : targetComponents) {
            if (comp == arrayComponent) {
                foundArray = true;
                continue;
            }
            if (foundArray && comp.getType() == PathComponent.Type.FIELD) {
                loopBody.add(Instruction.of(Command.MOVE_INTO_OBJECT, comp.getField()));
            }
        }

        // Duplicate source value and set it
        loopBody.add(Instruction.of(Command.DUPLICATE_ELEMENT));
        loopBody.add(Instruction.of(Command.POP_ELEMENT));
        loopBody.add(Instruction.of(Command.MOVE_OUT));
        loopBody.add(Instruction.of(Command.MOVE_OUT));
        // Increment counter
        loopBody.add(Instruction.of(Command.INCREMENT));

        // Add loop body to instructions
        instructions.add(Instruction.of(Command.PUSH_COMMAND, loopBody));

        // Add loop condition
        instructions.add(Instruction.of(Command.LOOP_UNTIL, (Predicate<JsonTransformationMachine>) m -> {
            if (m.getValueStackSize() < 2) return true;
            Object size = m.peekValueStack(1);
            Object counter = m.peekValueStack(0);
            return ((Number) counter).intValue() >= ((Number) size).intValue();
        }));

        return instructions;
    }


    private static boolean containsPattern(List<PathComponent> components) {
        return components.stream().anyMatch(c ->
                c.getType() == PathComponent.Type.ARRAY_ALL ||
                        c.getType() == PathComponent.Type.ARRAY_RANGE ||
                        c.getType() == PathComponent.Type.ARRAY_VALUES ||
                        c.getType() == PathComponent.Type.WILDCARD ||
                        c.getType() == PathComponent.Type.RECURSIVE_DESCENT
        );
    }

    private static List<Instruction> generateSimpleMoveInstructions(
            List<PathComponent> sourceComponents,
            List<PathComponent> targetComponents,
            Map<String, JsonNode> mapping
    ) {
        List<Instruction> instructions = new ArrayList<>();
        instructions.add(Instruction.of(Command.RESET));
        // Move to source and get value
        instructions.add(Instruction.of(Command.MOVE_TO_ROOT));
        instructions.addAll(generatePathNavigation(sourceComponents));
        instructions.add(Instruction.of(Command.PUSH_ELEMENT));
        if (mapping != null) {
            instructions.add(Instruction.of(Command.MAP_ELEMENT, mapping));
        }
        instructions.add(Instruction.of(Command.MOVE_OUT));

        // Move to target and set value
        instructions.add(Instruction.of(Command.MOVE_TO_ROOT));
        instructions.addAll(generatePathNavigation(targetComponents));
        instructions.add(Instruction.of(Command.POP_ELEMENT));
        instructions.add(Instruction.of(Command.MOVE_OUT));

        return instructions;
    }

    private static List<Instruction> generatePatternMoveInstructions(
            List<PathComponent> sourceComponents,
            List<PathComponent> targetComponents,
            Map<String, JsonNode> mapping
    ) {
        List<Instruction> instructions = new ArrayList<>();
        instructions.add(Instruction.of(Command.RESET));
        // First get source array size
        instructions.add(Instruction.of(Command.MOVE_TO_ROOT));

        PathComponent sourceArray = findArrayComponent(sourceComponents);
        PathComponent targetArray = findArrayComponent(targetComponents);

        if (sourceArray == null || targetArray == null) {
            throw new IllegalArgumentException("Source or target array pattern not found");
        }

        // If we have an offset, get it first
        if (targetArray.getType() == PathComponent.Type.ARRAY_ALL_OFFSET) {
            // Parse and navigate to offset array
            List<PathComponent> offsetComponents = JsonPathParser.parse(targetArray.getOffsetPath());

            for (PathComponent comp : offsetComponents) {
                if (comp.getType() == PathComponent.Type.FIELD) {
                    instructions.add(Instruction.of(Command.MOVE_INTO_OBJECT, comp.getField()));
                } else if (comp.getType() == PathComponent.Type.ARRAY_ALL) {
                    instructions.add(Instruction.of(Command.MOVE_INTO_ARRAY, comp.getField()));
                }
            }

            // Store offset array size
            instructions.add(Instruction.of(Command.STORE_SIZE));    // Stack: [offsetSize]
        } else {
            // No offset, start from 0
            instructions.add(Instruction.of(Command.STORE_VALUE, 0)); // Stack: [0]
        }

        // Navigate to source array and get its size
        instructions.add(Instruction.of(Command.MOVE_TO_ROOT));
        for (PathComponent comp : sourceComponents) {
            if (comp == sourceArray) {
                instructions.add(Instruction.of(Command.MOVE_INTO_ARRAY, comp.getField()));
                break;
            }
            if (comp.getType() == PathComponent.Type.FIELD) {
                instructions.add(Instruction.of(Command.MOVE_INTO_OBJECT, comp.getField()));
            }
        }

        // Store source array size
        instructions.add(Instruction.of(Command.STORE_SIZE));    // Stack: [offsetSize, sourceSize]

        // Initialize counter
        instructions.add(Instruction.of(Command.STORE_VALUE, 0)); // Stack: [offsetSize, sourceSize, counter]

        // Create loop body
        List<Instruction> loopBody = new ArrayList<>();
        loopBody.add(Instruction.of(Command.MOVE_TO_ROOT));

        // Source navigation and value capture
        for (PathComponent comp : sourceComponents) {
            if (comp == sourceArray) {
                loopBody.add(Instruction.of(Command.MOVE_INTO_ARRAY, comp.getField()));
                loopBody.add(Instruction.of(Command.MOVE_INTO_INDEX, new ValueStackReference(0))); // Uses counter
            } else if (comp.getType() == PathComponent.Type.FIELD &&
                    sourceComponents.indexOf(comp) > sourceComponents.indexOf(sourceArray)) {
                loopBody.add(Instruction.of(Command.MOVE_INTO_FIELD, comp.getField()));
            }
        }

        loopBody.add(Instruction.of(Command.PUSH_ELEMENT));
        if (mapping != null) {
            loopBody.add(Instruction.of(Command.MAP_ELEMENT, mapping));
        }

        // Target navigation with offset
        loopBody.add(Instruction.of(Command.MOVE_TO_ROOT));
        for (PathComponent comp : targetComponents) {
            if (comp.getType() == PathComponent.Type.ARRAY_ALL_OFFSET ||
                    comp.getType() == PathComponent.Type.ARRAY_ALL) {
                loopBody.add(Instruction.of(Command.MOVE_INTO_ARRAY, comp.getField()));
                // Calculate target index (counter + offset)
                loopBody.add(Instruction.of(Command.STORE_VALUE, new ValueStackReference(0))); // counter
                loopBody.add(Instruction.of(Command.STORE_VALUE, new ValueStackReference(3))); // offset
                loopBody.add(Instruction.of(Command.ADD));
                loopBody.add(Instruction.of(Command.MOVE_INTO_INDEX, new ValueStackReference(0)));
                loopBody.add(Instruction.of(Command.POP_VALUE));
            } else if (comp.getType() == PathComponent.Type.FIELD &&
                    targetComponents.indexOf(comp) > targetComponents.indexOf(targetArray)) {
                loopBody.add(Instruction.of(Command.MOVE_INTO_FIELD, comp.getField()));
            }
        }

        loopBody.add(Instruction.of(Command.POP_ELEMENT));

        // Move out and increment
        for (int i = 0; i < 2; i++) {
            loopBody.add(Instruction.of(Command.MOVE_OUT));
        }
        loopBody.add(Instruction.of(Command.INCREMENT));

        instructions.add(Instruction.of(Command.PUSH_COMMAND, loopBody));

        // Add loop condition
        instructions.add(Instruction.of(Command.LOOP_UNTIL, (Predicate<JsonTransformationMachine>) m -> {
            if (m.getValueStackSize() < 3) return true;
            Object sourceSize = m.peekValueStack(1);
            Object counter = m.peekValueStack(0);
            return ((Number) counter).intValue() >= ((Number) sourceSize).intValue();
        }));

        return instructions;
    }

    private static PathComponent findArrayComponent(List<PathComponent> components) {
        for (PathComponent comp : components) {
            if (comp.getType() == PathComponent.Type.ARRAY_ALL ||
                    comp.getType() == PathComponent.Type.ARRAY_RANGE ||
                    comp.getType() == PathComponent.Type.ARRAY_VALUES||
                    comp.getType() == PathComponent.Type.ARRAY_ALL_OFFSET) {
                return comp;
            }
        }
        return null;
    }

    private static List<Instruction> generatePathNavigation(List<PathComponent> components) {
        List<Instruction> instructions = new ArrayList<>();

        for (PathComponent component : components) {
            switch (component.getType()) {
                case FIELD:
                    instructions.add(Instruction.of(Command.MOVE_INTO_OBJECT, component.getField()));
                    break;

                case ARRAY_INDEX:
                    instructions.add(Instruction.of(Command.MOVE_INTO_ARRAY, component.getField()));
                    instructions.add(Instruction.of(Command.MOVE_INTO_INDEX, component.getIndex()));
                    break;
            }
        }

        return instructions;
    }

    private static List<Instruction> generatePatternNavigation(List<PathComponent> components) {
        List<Instruction> instructions = new ArrayList<>();

        for (PathComponent component : components) {
            switch (component.getType()) {
                case FIELD:
                    instructions.add(Instruction.of(Command.MOVE_INTO_OBJECT, component.getField()));
                    break;

                case ARRAY_INDEX:
                    instructions.add(Instruction.of(Command.MOVE_INTO_ARRAY, component.getField()));
                    instructions.add(Instruction.of(Command.MOVE_INTO_INDEX, component.getIndex()));
                    break;

                case ARRAY_ALL:
                    instructions.add(Instruction.of(Command.MOVE_INTO_ARRAY, component.getField()));
                    break;

                case ARRAY_RANGE:
                    instructions.add(Instruction.of(Command.MOVE_INTO_ARRAY, component.getField()));
                    instructions.add(Instruction.of(Command.STORE_VALUE, component.getStartIndex()));
                    instructions.add(Instruction.of(Command.STORE_VALUE, component.getEndIndex()));
                    break;

                case ARRAY_VALUES:
                    instructions.add(Instruction.of(Command.MOVE_INTO_ARRAY, component.getField()));
                    component.getIndices().forEach(index ->
                            instructions.add(Instruction.of(Command.STORE_VALUE, index)));
                    break;
            }
        }

        return instructions;
    }

    private static List<Instruction> generateTargetNavigation(List<PathComponent> components) {
        List<Instruction> instructions = new ArrayList<>();
        PathComponent lastComponent = components.get(components.size() - 1);

        // Navigate to parent
        for (int i = 0; i < components.size() - 1; i++) {
            PathComponent component = components.get(i);
            switch (component.getType()) {
                case FIELD:
                    instructions.add(Instruction.of(Command.MOVE_INTO_OBJECT, component.getField()));
                    break;
                case ARRAY_INDEX:
                    instructions.add(Instruction.of(Command.MOVE_INTO_ARRAY, component.getField()));
                    instructions.add(Instruction.of(Command.MOVE_INTO_INDEX, component.getIndex()));
                    break;
                case ARRAY_ALL:
                    instructions.add(Instruction.of(Command.MOVE_INTO_ARRAY, component.getField()));
                    instructions.add(Instruction.of(Command.STORE_SIZE));
                    instructions.add(Instruction.of(Command.MOVE_INTO_INDEX, new ValueStackReference(0)));
                    break;
            }
        }

        // Handle last component
        switch (lastComponent.getType()) {
            case FIELD:
                instructions.add(Instruction.of(Command.MOVE_INTO_FIELD, lastComponent.getField()));
                break;
            case ARRAY_INDEX:
                instructions.add(Instruction.of(Command.MOVE_INTO_ARRAY, lastComponent.getField()));
                instructions.add(Instruction.of(Command.MOVE_INTO_INDEX, lastComponent.getIndex()));
                break;
            case ARRAY_ALL:
                instructions.add(Instruction.of(Command.MOVE_INTO_ARRAY, lastComponent.getField()));
                instructions.add(Instruction.of(Command.MOVE_INTO_INDEX, new ValueStackReference(0)));
                break;
        }

        return instructions;
    }
}
