package org.example;


import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class TransformationStack<T> {
    private final Stack<T> stack = new Stack<>();
    private final String name;

    public TransformationStack(String name) {
        this.name = name;
    }

    public void push(T value) {
        stack.push(value);
    }

    public T pop() {
        if (stack.isEmpty()) {
            throw new IllegalStateException(name + " stack is empty");
        }
        return stack.pop();
    }

    public T peek() {
        if (stack.isEmpty()) {
            throw new IllegalStateException(name + " stack is empty");
        }
        return stack.peek();
    }

    public T peek(int offset) {
        if (offset >= stack.size()) {
            throw new IllegalStateException(
                    String.format("Cannot peek at offset %d, stack size is %d",
                            offset, stack.size())
            );
        }
        return stack.get(stack.size() - 1 - offset);
    }

    public int size() {
        return stack.size();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public void clear() {
        stack.clear();
    }

    public List<T> getTopN(int n) {
        int start = Math.max(0, stack.size() - n);
        return new ArrayList<>(stack.subList(start, stack.size()));
    }

    @Override
    public String toString() {
        return "[" + stack.stream().map(Object::toString).collect(Collectors.joining(", ")) + "] ";
    }
}
