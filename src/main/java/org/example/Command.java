package org.example;


public enum Command {
    // Navigation commands
    MOVE_TO_ROOT,          // Move back to root node
    MOVE_OUT,             // Move out one level
    MOVE_INTO_OBJECT,     // Move into object field
    MOVE_INTO_ARRAY,      // Move into array field
    MOVE_INTO_INDEX,      // Move to specific array index
    MOVE_INTO_FIELD,      // Move into field value

    // Stack operations
    PUSH_ELEMENT,         // Push current element to element stack
    POP_ELEMENT,          // Pop element and set at current location
    DUPLICATE_ELEMENT,    // Duplicate top element on element stack
    SWAP_ELEMENTS,        // Swap top two elements on element stack
    ROTATE_ELEMENTS,      // Rotate top three elements on element stack
    MAP_ELEMENT,          // Map top element on element stack
    STORE_ELEMENT,        // Store element at current location

    // Value stack operations
    STORE_VALUE,          // Store a value on the value stack
    STORE_SIZE,          // store size of array on value stack
    POP_VALUE,           // Pop a value from the value stack
    INCREMENT,           // Increment top number on value stack
    DECREMENT,           // Decrement top number on value stack
    COMPARE,             // Compare top two values on stack
    ADD,                // Add top two values on stack
    SUBTRACT,                // Subtract top two values on stack
    NEGATE,          // Negate top value on stack
    DUPLICATE_VALUE,    // Duplicate top values on value stack
    SWAP_VALUES,        // Swap top two values on value stack
    ROTATE_VALUES,      // Rotate top three values on value stack
    ELEMENT_TO_VALUE,   // Move top element from element stack to value stack
    VALUE_TO_ELEMENT,   // Move top element from value stack to element stack

    // Control flow
    PUSH_COMMAND,         // Push commands onto command stack
    POP_COMMAND,          // Pop and execute command from stack
    LOOP_UNTIL,          // Execute commands until condition
    JUMP_IF_TRUE,        // Execute next command if top of stack is true
    JUMP_IF_FALSE,       // Execute next command if top of stack is false
    RESET,               // clear stacks and navigate to root

    // Array operations
    MERGE_ARRAYS,         // Merge two arrays
    CONCAT_ARRAYS,        // Concatenate arrays
    FILTER_ARRAY,         // Filter array based on condition
    MAP_ARRAY,           // Transform array elements

    // Structure modifications
    CREATE_OBJECT,        // Create new empty object
    CREATE_ARRAY,         // Create new empty array
    DELETE,              // Delete current node
    MERGE_OBJECTS        // Merge top two objects from stack
}