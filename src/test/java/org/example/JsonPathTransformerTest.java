package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonPathTransformerTest {
    private ObjectMapper mapper;
    private ObjectNode testData;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        testData = createTestData();
    }

    private ObjectNode createTestData() {
        ObjectNode root = mapper.createObjectNode();

        // Add company data
        ObjectNode company = root.putObject("company");
        company.put("name", "Test Corp");

        // Add users
        ArrayNode users = root.putArray("users");
        addPerson(users, "John", "john@test.com", "dev");
        addPerson(users, "Jane", "jane@test.com", "qa");

        // Add employees
        ArrayNode employees = root.putArray("employees");
        addPerson(employees, "Bob", "bob@test.com", "manager");
        addPerson(employees, "Alice", "alice@test.com", "designer");

        return root;
    }

    private void addPerson(ArrayNode array, String name, String email, String role) {
        ObjectNode person = array.addObject();
        person.put("name", name);
        person.put("email", email);
        person.put("role", role);
    }

    @Test
    void testSimpleFieldMove() {
        // Test moving a simple field
        List<Instruction> instructions = JsonPathTransformer.generateMoveInstructions(
                "$.company.name",
                "$.companyInfo.title",
                null
        );

        JsonNode result = executeInstructions(instructions);

        assertTrue(result.has("companyInfo"));
        assertEquals("Test Corp", result.get("companyInfo").get("title").asText());
    }

    @Test
    void testArrayToArray() {
        // Test moving all elements from one array to another
        List<Instruction> instructions = JsonPathTransformer.generateMoveInstructions(
                "$.users[*].email",
                "$.contactInfo[*].email",
                null
        );

        JsonNode result = executeInstructions(instructions);

        assertTrue(result.has("contactInfo"));
        ArrayNode contactInfo = (ArrayNode) result.get("contactInfo");
        assertEquals(2, contactInfo.size());
        assertEquals("john@test.com", contactInfo.get(0).get("email").asText());
        assertEquals("jane@test.com", contactInfo.get(1).get("email").asText());
    }

    @Test
    void testArrayWithOffset() {
        // First move users
        List<Instruction> instructions1 = JsonPathTransformer.generateMoveInstructions(
                "$.users[*].email",
                "$.contactInfo[*].email",
                null
        );
        JsonNode intermediate = executeInstructions(instructions1);

        // Then move employees with offset
        List<Instruction> instructions2 = JsonPathTransformer.generateMoveInstructions(
                "$.employees[*].email",
                "$.contactInfo[*+$.users].email",
                null
        );

        JsonTransformationMachine machine = new JsonTransformationMachine(intermediate);
        JsonNode result = machine.execute(instructions2);

        assertTrue(result.has("contactInfo"));
        ArrayNode contactInfo = (ArrayNode) result.get("contactInfo");
        assertEquals(4, contactInfo.size());
        assertEquals("john@test.com", contactInfo.get(0).get("email").asText());
        assertEquals("jane@test.com", contactInfo.get(1).get("email").asText());
        assertEquals("bob@test.com", contactInfo.get(2).get("email").asText());
        assertEquals("alice@test.com", contactInfo.get(3).get("email").asText());
    }

    @Test
    void testValueMapping() {
        Map<String, JsonNode> mapping = Map.of(
                "john@test.com", mapper.valueToTree("mapped-john"),
                "jane@test.com", mapper.valueToTree("mapped-jane")
        );

        List<Instruction> instructions = JsonPathTransformer.generateMoveInstructions(
                "$.users[*].email",
                "$.contactInfo[*].email",
                mapping
        );

        JsonNode result = executeInstructions(instructions);

        assertTrue(result.has("contactInfo"));
        ArrayNode contactInfo = (ArrayNode) result.get("contactInfo");
        assertEquals("mapped-john", contactInfo.get(0).get("email").asText());
        assertEquals("mapped-jane", contactInfo.get(1).get("email").asText());
    }

    @Test
    void testSingleValueToMultipleTargets() {
        List<Instruction> instructions = JsonPathTransformer.generateMoveInstructions(
                "$.company.name",
                "$.contactInfo[*].companyName",
                null
        );

        // First create some contacts
        testData.putArray("contactInfo")
                .add(mapper.createObjectNode())
                .add(mapper.createObjectNode());

        JsonNode result = executeInstructions(instructions);

        ArrayNode contactInfo = (ArrayNode) result.get("contactInfo");
        assertEquals(2, contactInfo.size());
        assertEquals("Test Corp", contactInfo.get(0).get("companyName").asText());
        assertEquals("Test Corp", contactInfo.get(1).get("companyName").asText());
    }

    @Test
    void testEntireObjectMove() {
        List<Instruction> instructions = JsonPathTransformer.generateMoveInstructions(
                "$.users[*]",
                "$.people[*]",
                null
        );

        JsonNode result = executeInstructions(instructions);

        assertTrue(result.has("people"));
        ArrayNode people = (ArrayNode) result.get("people");
        assertEquals(2, people.size());
        assertEquals("John", people.get(0).get("name").asText());
        assertEquals("dev", people.get(0).get("role").asText());
    }

    @Test
    void testInvalidPath() {
        assertThrows(JsonPathParser.JsonPathParseException.class, () -> {
            JsonPathTransformer.generateMoveInstructions(
                    "$.invalid[",
                    "$.target",
                    null
            );
        });
    }

    private JsonNode executeInstructions(List<Instruction> instructions) {
        JsonTransformationMachine machine = new JsonTransformationMachine(testData.deepCopy());
        return machine.execute(instructions);
    }
}