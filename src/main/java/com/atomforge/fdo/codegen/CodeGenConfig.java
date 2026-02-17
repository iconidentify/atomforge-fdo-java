package com.atomforge.fdo.codegen;

/**
 * Configuration options for DSL code generation.
 */
public final class CodeGenConfig {

    private final String packageName;
    private final String className;
    private final boolean includeImports;
    private final String indent;
    private final boolean useScopedOutput;

    private CodeGenConfig(Builder builder) {
        this.packageName = builder.packageName;
        this.className = builder.className;
        this.includeImports = builder.includeImports;
        this.indent = builder.indent;
        this.useScopedOutput = builder.useScopedOutput;
    }

    /**
     * @return The package name for the generated class, or null for no package
     */
    public String packageName() {
        return packageName;
    }

    /**
     * @return The class name for the generated class
     */
    public String className() {
        return className;
    }

    /**
     * @return Whether to include import statements in the output
     */
    public boolean includeImports() {
        return includeImports;
    }

    /**
     * @return The indentation string to use (default is 4 spaces)
     */
    public String indent() {
        return indent;
    }

    /**
     * @return Whether to use scoped lambda output (object(), stream()) instead of flat manStartObject/manEndObject
     */
    public boolean useScopedOutput() {
        return useScopedOutput;
    }

    /**
     * Create a new builder with default settings.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a default configuration for generating just a method body.
     */
    public static CodeGenConfig methodBodyOnly() {
        return builder().build();
    }

    /**
     * Create a configuration for generating a full class with scoped output.
     */
    public static CodeGenConfig fullClass(String packageName, String className) {
        return builder()
                .packageName(packageName)
                .className(className)
                .includeImports(true)
                .useScopedOutput(true)
                .build();
    }

    /**
     * Create a configuration for generating a full class with flat output (legacy).
     */
    public static CodeGenConfig fullClassFlat(String packageName, String className) {
        return builder()
                .packageName(packageName)
                .className(className)
                .includeImports(true)
                .useScopedOutput(false)
                .build();
    }

    public static final class Builder {
        private String packageName = null;
        private String className = "GeneratedFdo";
        private boolean includeImports = false;
        private String indent = "    ";
        private boolean useScopedOutput = true;

        private Builder() {}

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder includeImports(boolean includeImports) {
            this.includeImports = includeImports;
            return this;
        }

        public Builder indent(String indent) {
            this.indent = indent;
            return this;
        }

        public Builder useScopedOutput(boolean useScopedOutput) {
            this.useScopedOutput = useScopedOutput;
            return this;
        }

        public CodeGenConfig build() {
            return new CodeGenConfig(this);
        }
    }
}
