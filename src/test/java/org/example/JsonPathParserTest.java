package org.example;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Nested
class JsonPathParserTest {

    @Test
    void testParseSimpleField() throws JsonPathParser.JsonPathParseException {
        String path = "$.name";
        List<PathComponent> components = JsonPathParser.parse(path);
        assertEquals(1, components.size());
        assertEquals(PathComponent.Type.FIELD, components.get(0).getType());
        assertEquals("name", components.get(0).getField());
    }

    @Test
    void testParseNestedFields() throws JsonPathParser.JsonPathParseException {
        String path = "$.store.book.title";
        List<PathComponent> components = JsonPathParser.parse(path);
        assertEquals(3, components.size());
        assertEquals("store", components.get(0).getField());
        assertEquals("book", components.get(1).getField());
        assertEquals("title", components.get(2).getField());
    }

    @Test
    void testParseArrayIndex() throws JsonPathParser.JsonPathParseException {
        String path = "$.store.book[0]";
        List<PathComponent> components = JsonPathParser.parse(path);
        assertEquals(2, components.size());
        assertEquals("store", components.get(0).getField());
        assertEquals(PathComponent.Type.ARRAY_INDEX, components.get(1).getType());
        assertEquals("book", components.get(1).getField());
        assertEquals(0, components.get(1).getIndex());
    }

    @Test
    void testParseArrayAll() throws JsonPathParser.JsonPathParseException {
        String path = "$.store.book[*]";
        List<PathComponent> components = JsonPathParser.parse(path);
        assertEquals(2, components.size());
        assertEquals(PathComponent.Type.ARRAY_ALL, components.get(1).getType());
        assertEquals("book", components.get(1).getField());
    }

    @Test
    void testGenerateMoveInstructions() throws JsonPathParser.JsonPathParseException {
        String sourcePath = "$.store.book[*].title";
        String targetPath = "$.titles[*]";

        List<Instruction> instructions = JsonPathTransformer.generateMoveInstructions(sourcePath, targetPath,null);
        assertNotNull(instructions);
        assertFalse(instructions.isEmpty());
    }

    @Test
    void testGenerateReplicationInstructions() throws JsonPathParser.JsonPathParseException {
        String sourcePath = "$.users[*]";
        String targetPath = "$.messages[*].user";

        List<Instruction> instructions = JsonPathTransformer.generateMoveInstructions(sourcePath, targetPath,null);
        assertNotNull(instructions);
        assertFalse(instructions.isEmpty());
    }

}