package org.example;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class PathComponentTest {

    @Test
    void testFieldComponent() {
        PathComponent component = PathComponent.field("name");
        assertEquals(PathComponent.Type.FIELD, component.getType());
        assertEquals("name", component.getField());
    }

    @Test
    void testArrayIndexComponent() {
        PathComponent component = PathComponent.arrayIndex("book", 2);
        assertEquals(PathComponent.Type.ARRAY_INDEX, component.getType());
        assertEquals("book", component.getField());
        assertEquals(2, component.getIndex());
    }

    @Test
    void testArrayValuesComponent() {
        PathComponent component = PathComponent.arrayValues("book", Arrays.asList(1, 3, 5));
        assertEquals(PathComponent.Type.ARRAY_VALUES, component.getType());
        assertEquals(Arrays.asList(1, 3, 5), component.getIndices());
    }

    @Test
    void testToString() {
        PathComponent component = PathComponent.field("name");
        assertEquals(".name", component.toString());
    }
}