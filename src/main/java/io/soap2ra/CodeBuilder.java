package io.soap2ra;

public class CodeBuilder {

    private final StringBuilder stringBuilder = new StringBuilder();
    private String indent = "";

    public CodeBuilder ln(String code) {
        stringBuilder.append(indent).append(code);
        return this.ln();
    }

    public CodeBuilder ln(String code, Object... arguments) {
        stringBuilder.append(indent).append(String.format(code, arguments));
        return this.ln();
    }

    public CodeBuilder ln() {
        stringBuilder.append("\n");
        return this;
    }

    public CodeBuilder indent() {
        indent += "    ";
        return this;
    }

    public CodeBuilder unindent() {
        indent = indent.substring(0, indent.length() - 4);
        return this;
    }

    public String toString() {
        return stringBuilder.toString();
    }

    public static String methodNameFrom(String input) {
        return mask(input.substring(0, 1).toLowerCase() + input.substring(1, input.length()));
    }

    public static String classNameFrom(String input) {
        return mask(input.substring(0, 1).toUpperCase() + input.substring(1, input.length()));
    }

    private static String mask(String input) {
        return input.replaceAll("[^A-Za-z1-9]", "_");
    }
}