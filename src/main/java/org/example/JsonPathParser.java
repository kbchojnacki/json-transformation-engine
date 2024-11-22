package org.example;

import java.util.ArrayList;
import java.util.List;

public class JsonPathParser {

    public static List<PathComponent> parse(String path) {
        if (path == null || path.isEmpty()) {
            throw new JsonPathParseException("Path cannot be null or empty");
        }

        List<PathComponent> components = new ArrayList<>();

        // Handle root
        if (path.startsWith("$.")) {
            path = path.substring(2);
        } else if (path.startsWith("$")) {
            path = path.substring(1);
        }

        // Handle recursive descent
        if (path.startsWith("..")) {
            components.add(PathComponent.recursiveDescent());
            path = path.substring(2);
        }

        StringBuilder currentPart = new StringBuilder();
        boolean inBrackets = false;

        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);

            if (c == '[') {
                inBrackets = true;
                currentPart.append(c);
            } else if (c == ']') {
                inBrackets = false;
                currentPart.append(c);
            } else if (c == '.' && !inBrackets) {
                // Process current part if not empty
                if (currentPart.length() > 0) {
                    processPathPart(currentPart.toString(), components);
                    currentPart.setLength(0);  // Clear builder
                }
            } else {
                currentPart.append(c);
            }
        }

        // Process last part if exists
        if (currentPart.length() > 0) {
            processPathPart(currentPart.toString(), components);
        }

        return components;
    }

    private static void processPathPart(String part, List<PathComponent> components) {
        if (part.isEmpty()) {
            return;
        }

        if (part.equals("*")) {
            components.add(PathComponent.wildcard());
            return;
        }

        if (part.contains("[")) {
            parseArrayNotation(part, components);
        } else {
            components.add(PathComponent.field(part));
        }
    }

    private static void parseArrayNotation(String part, List<PathComponent> components) {
        int bracketStart = part.indexOf('[');
        int bracketEnd = part.lastIndexOf(']');

        if (bracketStart == -1 || bracketEnd == -1 || bracketEnd <= bracketStart) {
            throw new JsonPathParseException("Invalid array notation: " + part);
        }

        String field = part.substring(0, bracketStart);
        String arrayPattern = part.substring(bracketStart + 1, bracketEnd);

        try {
            if (arrayPattern.equals("*")) {
                components.add(PathComponent.arrayAll(field));
            } else if (arrayPattern.startsWith("*+$")) {
                String offsetPath = arrayPattern.substring(2); // Remove *+
                components.add(PathComponent.arrayAllOffset(field, offsetPath));
            } else if (arrayPattern.contains(":")) {
                String[] range = arrayPattern.split(":");
                if (range.length != 2) {
                    throw new JsonPathParseException("Invalid range format: " + arrayPattern);
                }
                int start = Integer.parseInt(range[0].trim());
                int end = Integer.parseInt(range[1].trim());
                components.add(PathComponent.arrayRange(field, start, end));
            } else if (arrayPattern.contains(",")) {
                String[] indices = arrayPattern.split(",");
                List<Integer> values = new ArrayList<>();
                for (String idx : indices) {
                    values.add(Integer.parseInt(idx.trim()));
                }
                components.add(PathComponent.arrayValues(field, values));
            } else {
                int index = Integer.parseInt(arrayPattern.trim());
                components.add(PathComponent.arrayIndex(field, index));
            }
        } catch (NumberFormatException e) {
            throw new JsonPathParseException("Invalid number in array pattern: " + arrayPattern);
        }
    }


    public static class JsonPathParseException extends RuntimeException {
        public JsonPathParseException(String message) {
            super(message);
        }
    }

    // Helper method to validate and debug paths
    public static void debugPath(String path) {
        try {
            List<PathComponent> components = parse(path);
            System.out.println("Path: " + path);
            System.out.println("Components:");
            for (PathComponent comp : components) {
                System.out.printf("- Type: %s, Field: %s, Append: %s%n",
                        comp.getType(),
                        comp.getField()
                );
            }
        } catch (JsonPathParseException e) {
            System.err.println("Parse error: " + e.getMessage());
        }
    }

}



