# JSON Transformation Machine

System for transforming JSON structures using a stack-based approach. The system allows for complex JSON transformations using JSONPath-like syntax and a set of atomic operations.

## Overview

The system consists of several key components:
1. **JsonTransformationMachine**: The core engine that executes transformation instructions
2. **JsonPathTransformer**: Converts JSONPath expressions into machine instructions
3. **Stack-based Architecture**: Uses multiple stacks for managing state and operations

### Key Features

- JSONPath-style navigation and transformation
- Stack-based operation model
- Support for array operations with offset patterns
- Value mapping capabilities
- Nested structure handling
- Loop and conditional execution

## Components

### JsonTransformationMachine

The machine maintains several stacks and a current state:

- **Element Stack**: Holds JSON nodes for transformation
- **Value Stack**: Holds primitive values and counters
- **Command Stack**: Holds sequences of instructions for loops
- **Path Stack**: Tracks navigation path
- **State Stack**: Maintains parent nodes during navigation

### Command Types

#### Navigation Commands
```java
MOVE_TO_ROOT          // Move to root node
MOVE_OUT             // Move up one level
MOVE_INTO_OBJECT     // Move into object field
MOVE_INTO_ARRAY      // Move into array field
MOVE_INTO_INDEX      // Move to specific array index
MOVE_INTO_FIELD      // Move into field value
```

#### Stack Operations
```java
PUSH_ELEMENT         // Push current node to element stack
POP_ELEMENT         // Pop and set at current location
DUPLICATE_ELEMENT    // Duplicate top element
SWAP_ELEMENTS       // Swap top two elements
ROTATE_ELEMENTS     // Rotate top three elements
MAP_ELEMENT         // Apply mapping to top element
STORE_ELEMENT       // Store element at current location
```

#### Value Stack Operations
```java
STORE_VALUE         // Push value to value stack
STORE_SIZE         // Push array/object size to stack
POP_VALUE          // Remove top value
INCREMENT          // Increment top number
DECREMENT          // Decrement top number
COMPARE            // Compare top two values
ADD               // Add top two values
SUBTRACT          // Subtract top two values
NEGATE            // Negate top value
DUPLICATE_VALUE    // Duplicate top value
SWAP_VALUES       // Swap top two values
ROTATE_VALUES     // Rotate top three values
```

#### Control Flow
```java
PUSH_COMMAND        // Store command sequence
POP_COMMAND         // Execute stored commands
LOOP_UNTIL         // Loop command sequence
JUMP_IF_TRUE       // Conditional execution
JUMP_IF_FALSE      // Conditional execution
RESET              // Clear stacks and return to root
```

#### Array Operations
```java
MERGE_ARRAYS        // Combine two arrays
CONCAT_ARRAYS       // Join arrays preserving order
FILTER_ARRAY        // Filter array elements
MAP_ARRAY          // Transform array elements
```

#### Structure Modifications
```java
CREATE_OBJECT       // Create new object
CREATE_ARRAY        // Create new array
DELETE             // Remove current node
MERGE_OBJECTS       // Combine two objects
```

## Usage Examples

### Simple Field Movement
```java
// Move company name to a new location
testTransformation(data,
    "$.company.name",
    "$.info.companyName",
    null
);
```

### Array Transformation with Offset
```java
// Add employees after users in contact list
testTransformation(data,
    "$.employees[*].email",
    "$.contactInfo[*+$.users].email",
    null
);
```

### Value Mapping
```java
// Map values during transformation
Map<String, JsonNode> mapping = Map.of(
    "old_value", mapper.valueToTree("new_value")
);
testTransformation(data,
    "$.users[*].status",
    "$.statuses[*]",
    mapping
);
```

## Implementation Details

### Path Navigation
The system uses a path-based navigation system similar to JSONPath:
- `$` represents the root
- `.field` navigates to object fields
- `[n]` navigates to array indices
- `[*]` represents all array elements
- `[*+$.array]` represents array append with offset
