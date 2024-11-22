package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class JsonTransformationMachineTest {
    private ObjectMapper mapper;
    private JsonTransformationMachine machine;
    private ObjectNode testData;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        testData = createTestData();
        machine = new JsonTransformationMachine(testData,true);
    }

    private ObjectNode createTestData() {
        ObjectNode root = mapper.createObjectNode();
        root.put("scalar", "value");
        root.putObject("object").put("field", "value");
        ArrayNode array = root.putArray("array");
        array.add("item1");
        array.add("item2");
        return root;
    }

    @Nested
    class NavigationTests {
        @Test
        void testMoveToRoot() {
            List<Instruction> instructions = Arrays.asList(
                    Instruction.of(Command.MOVE_INTO_OBJECT, "object"),
                    Instruction.of(Command.MOVE_TO_ROOT)
            );

            machine.execute(instructions);
            assertEquals("$", machine.getCurrentPath());
        }

        @Test
        void testMoveIntoObject() {
            List<Instruction> instructions = Arrays.asList(
                    Instruction.of(Command.MOVE_INTO_OBJECT, "object"),
                    Instruction.of(Command.MOVE_INTO_FIELD, "field")
            );

            machine.execute(instructions);
            assertEquals("$.object.field", machine.getCurrentPath());
        }

        @Test
        void testMoveIntoArray() {
            List<Instruction> instructions = Arrays.asList(
                    Instruction.of(Command.MOVE_INTO_ARRAY, "array"),
                    Instruction.of(Command.MOVE_INTO_INDEX, 0)
            );

            machine.execute(instructions);
            assertEquals("$.array[0]", machine.getCurrentPath());
        }

        @Test
        void testMoveOut() {
            List<Instruction> instructions = Arrays.asList(
                    Instruction.of(Command.MOVE_INTO_OBJECT, "object"),
                    Instruction.of(Command.MOVE_INTO_FIELD, "field"),
                    Instruction.of(Command.MOVE_OUT)
            );

            machine.execute(instructions);
            assertEquals("$.object", machine.getCurrentPath());
        }
    }

    @Nested
    class StackOperationTests {
        @Test
        void testPushAndPopElement() {
            List<Instruction> instructions = Arrays.asList(
                    Instruction.of(Command.MOVE_INTO_FIELD, "scalar"),
                    Instruction.of(Command.PUSH_ELEMENT),
                    Instruction.of(Command.MOVE_TO_ROOT),
                    Instruction.of(Command.MOVE_INTO_OBJECT, "target"),
                    Instruction.of(Command.POP_ELEMENT)
            );

            JsonNode result = machine.execute(instructions);
            assertEquals("value", result.get("target").asText());
        }

        @Test
        void testDuplicateElement() {
            List<Instruction> instructions = Arrays.asList(
                    Instruction.of(Command.MOVE_INTO_FIELD, "scalar"),
                    Instruction.of(Command.PUSH_ELEMENT),
                    Instruction.of(Command.DUPLICATE_ELEMENT)
            );

            machine.execute(instructions);
            assertEquals(2, machine.getElementStackSize());
        }

        @Test
        void testSwapElements() {
            List<Instruction> instructions = Arrays.asList(
                    Instruction.of(Command.STORE_VALUE, "first"),
                    Instruction.of(Command.STORE_VALUE, "second"),
                    Instruction.of(Command.SWAP_VALUES)
            );

            machine.execute(instructions);
            assertEquals("first", machine.peekValueStack(0));
            assertEquals("second", machine.peekValueStack(1));
        }
    }

    @Nested
    class ValueStackOperationTests {
        @Test
        void testStoreAndPopValue() {
            List<Instruction> instructions = Arrays.asList(
                    Instruction.of(Command.STORE_VALUE, 42),
                    Instruction.of(Command.POP_VALUE)
            );

            machine.execute(instructions);
            assertEquals(0, machine.getValueStackSize());
        }

        @Test
        void testIncrement() {
            List<Instruction> instructions = Arrays.asList(
                    Instruction.of(Command.STORE_VALUE, 41),
                    Instruction.of(Command.INCREMENT)
            );

            machine.execute(instructions);
            assertEquals(42, machine.peekValueStack(0));
        }

        @Test
        void testDecrement() {
            List<Instruction> instructions = Arrays.asList(
                    Instruction.of(Command.STORE_VALUE, 43),
                    Instruction.of(Command.DECREMENT)
            );

            machine.execute(instructions);
            assertEquals(42, machine.peekValueStack(0));
        }

        @Test
        void testCompare() {
            List<Instruction> instructions = Arrays.asList(
                    Instruction.of(Command.STORE_VALUE, 1),
                    Instruction.of(Command.STORE_VALUE, 2),
                    Instruction.of(Command.COMPARE, "<")
            );

            machine.execute(instructions);
            assertTrue((Boolean) machine.peekValueStack(0));
        }
    }

    @Nested
    class ControlFlowTests {
        @Test
        void testLoopUntil() {
            List<Instruction> instructions = Arrays.asList(
                    Instruction.of(Command.STORE_VALUE, 0),
                    Instruction.of(Command.PUSH_COMMAND, Arrays.asList(
                            Instruction.of(Command.INCREMENT)
                    )),
                    Instruction.of(Command.LOOP_UNTIL, (Predicate<JsonTransformationMachine>) m ->
                            ((Number) m.peekValueStack(0)).intValue() >= 3)
            );

            machine.execute(instructions);
            assertEquals(3, machine.peekValueStack(0));
        }

        @Test
        void testJumpIfTrue() {
            List<Instruction> instructions = Arrays.asList(
                    Instruction.of(Command.STORE_VALUE, true),
                    Instruction.of(Command.JUMP_IF_TRUE,
                            Instruction.of(Command.STORE_VALUE, "executed"))
            );

            machine.execute(instructions);
            assertEquals("executed", machine.peekValueStack(0));
        }
    }

    @Nested
    class ArrayOperationTests {
        @Test
        void testMergeArrays() throws JsonProcessingException {
            List<Instruction> instructions = Arrays.asList(
                    // Create first array
                    Instruction.of(Command.MOVE_INTO_ARRAY, "array1"),
                    Instruction.of(Command.MOVE_INTO_INDEX, 0),
                    Instruction.of(Command.MOVE_INTO_FIELD,"value"),
                    Instruction.of(Command.STORE_ELEMENT, mapper.readTree("\"first\"")),
                    Instruction.of(Command.MOVE_OUT),
                    Instruction.of(Command.MOVE_OUT),
                    Instruction.of(Command.PUSH_ELEMENT),

                    // Create second array
                    Instruction.of(Command.MOVE_TO_ROOT),
                    Instruction.of(Command.MOVE_INTO_ARRAY, "array2"),
                    Instruction.of(Command.MOVE_INTO_INDEX, 0),
                    Instruction.of(Command.MOVE_INTO_FIELD,"value"),
                    Instruction.of(Command.STORE_ELEMENT, mapper.readTree("\"second\"")),
                    Instruction.of(Command.MOVE_OUT),
                    Instruction.of(Command.MOVE_OUT),
                    Instruction.of(Command.PUSH_ELEMENT),

                    // Merge arrays
                    Instruction.of(Command.MERGE_ARRAYS)
            );

            machine.execute(instructions);
            JsonNode result = machine.peekElementStack(0);
            assertEquals(2, result.size());
        }

        @Test
        void testFilterArray() {
            List<Instruction> instructions = Arrays.asList(
                    Instruction.of(Command.MOVE_INTO_ARRAY, "array"),
                    Instruction.of(Command.FILTER_ARRAY,
                            (Predicate<JsonNode>) node -> node.asText().equals("item1"))
            );

            machine.execute(instructions);
            ArrayNode result = (ArrayNode) machine.getCurrentState();
            assertEquals(1, result.size());
            assertEquals("item1", result.get(0).asText());
        }
    }
}