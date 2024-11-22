package org.example;

import java.util.List;

public class PathComponent {

    public enum Type {
        FIELD,              // Simple field: .name
        ARRAY_INDEX,        // Specific index: [0]
        ARRAY_ALL,          // All elements: [*]
        ARRAY_ALL_OFFSET,   // [*+$.users] - append with offset from users array
        ARRAY_RANGE,        // Range of elements: [1:3]
        ARRAY_VALUES,       // Specific values: [1,3,5]
        WILDCARD,          // Any field: .*
        RECURSIVE_DESCENT   // All nested levels: ..
    }

    private final Type type;
    private final String field;
    private final Integer index;
    private final Integer startIndex;
    private final Integer endIndex;
    private final List<Integer> indices;
    private final String offsetPath;

    private PathComponent(Type type, String field, Integer index,
                          Integer startIndex, Integer endIndex,
                          List<Integer> indices, String offsetPath) {
        this.type = type;
        this.field = field;
        this.index = index;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.indices = indices;
        this.offsetPath = offsetPath;
    }

    // Static factory methods
    public static PathComponent field(String field) {
        return new PathComponent(Type.FIELD, field, null, null, null, null, null);
    }

    public static PathComponent arrayIndex(String field, int index) {
        return new PathComponent(Type.ARRAY_INDEX, field, index, null, null, null, null);
    }

    public static PathComponent arrayAll(String field) {
        return new PathComponent(Type.ARRAY_ALL, field, null, null, null, null, null);
    }

    public static PathComponent arrayRange(String field, int start, int end) {
        return new PathComponent(Type.ARRAY_RANGE, field, null, start, end, null, null);
    }

    public static PathComponent arrayValues(String field, List<Integer> indices) {
        return new PathComponent(Type.ARRAY_VALUES, field, null, null, null, indices, null);
    }

    public static PathComponent wildcard() {
        return new PathComponent(Type.WILDCARD, null, null, null, null, null, null);
    }

    public static PathComponent recursiveDescent() {
        return new PathComponent(Type.RECURSIVE_DESCENT, null, null, null, null, null, null);
    }

    // Add factory method for offset array pattern
    public static PathComponent arrayAllOffset(String field, String offsetPath) {
        return new PathComponent(Type.ARRAY_ALL_OFFSET, field, null, null, null, null, offsetPath);
    }

    public String getOffsetPath() {
        return offsetPath;
    }

    @Override
    public String toString() {
        switch (type) {
            case FIELD:
                return "." + field;
            case ARRAY_INDEX:
                return field + "[" + index + "]";
            case ARRAY_ALL:
                return field + "[*]";
            case ARRAY_ALL_OFFSET:
                return field + "[*+"+getOffsetPath()+"]";
            case ARRAY_RANGE:
                return field + "[" + startIndex + ":" + endIndex + "]";
            case ARRAY_VALUES:
                return field + "[" + String.join(",", indices.stream()
                        .map(String::valueOf).toList()) + "]";
            case WILDCARD:
                return ".*";
            case RECURSIVE_DESCENT:
                return "..";
            default:
                return "unknown";
        }
    }

    public Type getType() {
        return type;
    }

    public String getField() {
        return field;
    }

    public List<Integer> getIndices() {
        return indices;
    }

    public Integer getIndex() {
        return index;
    }

    public Integer getEndIndex() {
        return endIndex;
    }

    public Integer getStartIndex() {
        return startIndex;
    }
}
