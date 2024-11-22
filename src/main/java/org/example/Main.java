package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.example.JsonTransformationMachine.pushCommands;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode sampleData = createSampleData(mapper);
        System.out.println("Initial Data:");

        testTransformation(sampleData,
                "$.users[*].email",
                "$.contactInfo[*].email",
                Map.of("john@example.com",mapper.valueToTree("mail")),
                false
        );
        printJson(sampleData);

        testTransformation(sampleData,
                "$.users[*].name",
                "$.contactInfo[*].name",
                null,
                false
        );

        testTransformation(sampleData,
                "$.employees[*].email",
                "$.contactInfo[*+$.users].email",
                null,
                false
        );

        testTransformation(sampleData,
                "$.employees[*].name",
                "$.contactInfo[*+$.users].name",
                null,
                false
        );
        testTransformation(sampleData,
                "$.company.name",
                "$.contactInfo[*].companyName",
                null,
                false
        );



        testTransformation(sampleData,
                "$.users[*]",
                "$.people[*]",
                null,
                false
        );

        testTransformation(sampleData,
                "$.employees[*]",
                "$.people[*+$.users]",
                null,
                false
        );

        printJson(sampleData);
    }

    private static ObjectNode createSampleData(ObjectMapper mapper) {
        ObjectNode root = mapper.createObjectNode();

        // Add company
        ObjectNode company = root.putObject("company");
        company.put("name", "Acme Corp");

        // Add users
        ArrayNode users = root.putArray("users");
        addPerson(users, "John Doe", "john@example.com", "developer");
        addPerson(users, "Jane Smith", "jane@example.com", "designer");

        // Add employees
        ArrayNode employees = root.putArray("employees");
        addPerson(employees, "Bob Wilson", "bob@example.com", "manager");
        addPerson(employees, "Alice Brown", "alice@example.com", "engineer");

        return root;
    }

    private static void addPerson(ArrayNode array, String name, String email, String role) {
        ObjectNode person = array.addObject();
        person.put("name", name);
        person.put("email", email);
        person.put("role", role);
    }

    private static void addUser(ArrayNode users, String name, String email,
                                String role, ObjectNode address) {
        ObjectNode user = users.addObject();
        user.put("name", name);
        user.put("email", email);
        user.put("role", role);
        user.set("address", address);
    }

    private static ObjectNode createAddress(ObjectMapper mapper, String city, String country) {
        ObjectNode address = mapper.createObjectNode();
        address.put("city", city);
        address.put("country", country);
        return address;
    }

    private static void testTransformation(JsonNode source, String sourcePath, String targetPath, Map<String, JsonNode> mapping, boolean debug) {
        try {
            if (debug) {
                System.out.printf("Moving from '%s' to '%s'%n", sourcePath, targetPath);
            }

            // Generate transformation instructions
            List<Instruction> instructions = JsonPathTransformer.generateMoveInstructions(
                    sourcePath, targetPath,
                    mapping
            );

            // Create new machine with copy of source data
            JsonTransformationMachine machine = new JsonTransformationMachine(
                    source, debug
            );

            // Execute transformation
            JsonNode result = machine.execute(instructions);

            if (debug) {
                System.out.println("Result:");
                printJson(result);

                // Print execution stats
                System.out.println("Execution State:");
                System.out.println(machine.getExecutionState());
            }

        } catch (Exception e) {
            System.err.println("Transformation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private static void printJson(JsonNode node) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}