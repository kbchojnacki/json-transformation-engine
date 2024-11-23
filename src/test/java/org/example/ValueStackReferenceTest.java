package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ValueStackReferenceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testResolveField() throws Exception {
        String jsonString = "{ \"age\": 30 }";
        JsonNode inputJson = mapper.readTree(jsonString);

        JsonTransformationMachine machine = new JsonTransformationMachine(inputJson);

        ValueStackReference fieldRef = ValueStackReference.field("age");
        Object value = fieldRef.resolve(machine);

        assertTrue(value instanceof Number);
        assertEquals(30, ((Number) value).intValue());
    }

    @Test
    void testResolveStack() {
        JsonTransformationMachine machine = new JsonTransformationMachine(mapper.createObjectNode());
        machine.getStackManager().values().push(42);

        ValueStackReference stackRef = ValueStackReference.stack(0);
        Object value = stackRef.resolve(machine);

        assertEquals(42, value);
    }

    @Test
    void testResolveCurrent() throws Exception {
        String jsonString = "{ \"name\": \"Alice\" }";
        JsonNode inputJson = mapper.readTree(jsonString);

        JsonTransformationMachine machine = new JsonTransformationMachine(inputJson);

        ValueStackReference currentRef = ValueStackReference.current();
        Object value = currentRef.resolve(machine);

        assertTrue(value instanceof JsonNode);
        assertEquals(inputJson, value);
    }

}