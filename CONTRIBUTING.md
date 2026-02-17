# Contributing to AtomForge FDO Java

Thank you for your interest in contributing to the AtomForge FDO Java library. This document provides guidelines and information for contributors.

## Reporting Bugs

Please open a [GitHub Issue](https://github.com/iconidentify/atomforge-fdo-java/issues/new?template=bug_report.md) with:

- A clear description of the bug
- Steps to reproduce the issue
- Expected vs. actual behavior
- Java version and OS information
- Minimal code example or FDO source that triggers the bug

## Suggesting Features

Feature requests are welcome. Please open a [GitHub Issue](https://github.com/iconidentify/atomforge-fdo-java/issues/new?template=feature_request.md) describing:

- The use case or problem you're trying to solve
- Your proposed solution (if any)
- Any alternatives you've considered

## Development Setup

### Prerequisites

- Java 21 or later
- Maven 3.8+

### Building

```bash
# Compile the project
mvn compile

# Run the full test suite (80,000+ tests)
mvn test

# Package as JAR
mvn package

# Install to local Maven repository
mvn install
```

### Project Structure

```
src/main/java/com/atomforge/fdo/
  FdoCompiler.java          - Text-to-binary compiler
  FdoDecompiler.java        - Binary-to-text decompiler
  atom/                     - Atom definitions and registry
  binary/                   - Binary encoding/decoding
  codegen/                  - DSL code generator
  dsl/                      - Type-safe DSL builders
  model/                    - FdoStream object model
  text/                     - Lexer, parser, formatter
```

## Making Changes

1. Fork the repository
2. Create a feature branch from `main`
3. Make your changes
4. Ensure all tests pass: `mvn test`
5. Submit a pull request

### Code Style

- Follow existing code conventions in the project
- Use Java 21 features where appropriate (records, sealed interfaces, pattern matching, etc.)
- Keep methods focused and classes cohesive
- Write tests for new functionality

### Commit Messages

- Use a short, descriptive subject line
- Use the imperative mood ("Add feature" not "Added feature")
- Reference issue numbers where applicable

## Pull Request Process

1. Ensure your branch is up to date with `main`
2. All tests must pass
3. Provide a clear description of the changes and their motivation
4. Link any related issues
5. Be responsive to review feedback

## License

By contributing to this project, you agree that your contributions will be licensed under the [MIT License](LICENSE).

## File Headers

Per-file license headers are not required for this project. The top-level `LICENSE` file covers the entire repository.
