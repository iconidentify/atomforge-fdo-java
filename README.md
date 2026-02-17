# AtomForge FDO Java Library

A pure Java 21 library for compiling and decompiling Field Data Objects (FDO). This library provides complete FDO91 specification support with a fluent DSL for programmatic construction.

## Features

- **Cross-Platform**: Pure Java implementation, runs on any platform with Java 21+
- **Type-Safe DSL**: Fluent builder API with compile-time validation and IDE autocomplete
- **Object Model**: Query and extract data from decoded FDO streams
- **P3 Framing**: Frame-aware compilation for protocol integration
- **Round-Trip Verified**: Decode and re-encode produces identical binary
- **Code Generation**: Convert FDO text source to type-safe Java DSL
- **Zero Dependencies**: No runtime dependencies beyond the Java standard library

## Installation

### Maven

```xml
<dependency>
    <groupId>com.atomforge</groupId>
    <artifactId>atomforge-fdo</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

For GitHub Packages, add the repository:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/iconidentify/atomforge-fdo-java</url>
    </repository>
</repositories>
```

### Gradle

```groovy
implementation 'com.atomforge:atomforge-fdo:1.0.0-SNAPSHOT'
```

## Quick Start

### Compiling FDO Text to Binary

```java
import com.atomforge.fdo.FdoCompiler;

FdoCompiler compiler = FdoCompiler.create();

String source = """
    uni_start_stream <00x>
      man_start_object <independent, "My Window">
        mat_object_id <32-105>
        mat_orientation <vcf>
      man_end_object
    uni_end_stream
    """;

byte[] binary = compiler.compile(source);
```

### Decompiling Binary to Text

```java
import com.atomforge.fdo.FdoDecompiler;

FdoDecompiler decompiler = FdoDecompiler.create();
String source = decompiler.decompile(binaryData);
```

### Decoding to Object Model

```java
import com.atomforge.fdo.model.FdoStream;

FdoStream stream = FdoStream.decode(binaryData);

// Query atoms by name
String value = stream.findFirst("de_data").orElseThrow().getString();

// Get all matching atoms
List<FdoAtom> allDeData = stream.findAll("de_data");

// Round-trip: decode and re-encode
byte[] recompiled = stream.toBytes();
```

---

## FdoStream Object Model

The `FdoStream` API provides type-safe programmatic access to decoded FDO data. Use this when you need to extract values from binary FDO packets.

### Decoding and Querying

```java
FdoStream stream = FdoStream.decode(binaryData);

// Find first atom by name
Optional<FdoAtom> atom = stream.findFirst("mat_object_id");

// Find all atoms by name
List<FdoAtom> atoms = stream.findAll("de_data");

// Find by protocol number
List<FdoAtom> manAtoms = stream.findByProtocol(1);  // MAN protocol

// Filter with predicate
List<FdoAtom> strings = stream.filter(FdoAtom::isString);

// Java Stream API
stream.stream()
    .filter(a -> a.name().startsWith("de_"))
    .map(FdoAtom::getString)
    .forEach(System.out::println);
```

### Type-Safe Value Access

```java
FdoAtom atom = stream.findFirst("de_data").orElseThrow();

// Direct access (throws if wrong type)
String strValue = atom.getString();
long numValue = atom.getNumber();
FdoGid gidValue = atom.getGid();
boolean boolValue = atom.getBoolean();

// Safe optional access
Optional<String> maybeString = atom.getStringOpt();
Optional<Long> maybeNumber = atom.getNumberOpt();

// Type checking
if (atom.isString()) { ... }
if (atom.isNumber()) { ... }
if (atom.isGid()) { ... }

// Nested streams
FdoStream nested = atom.getNestedStream();
```

### Real-World Example: Extracting Login Credentials

```java
// Dd frame contains username and password in de_data atoms
byte[] ddPayload = ...; // FDO portion of Dd frame
FdoStream stream = FdoStream.decode(ddPayload);

List<FdoAtom> deDataAtoms = stream.findAll("de_data");
String username = deDataAtoms.get(0).getString();
String password = deDataAtoms.get(1).getString();
```

---

## FdoScript Type-Safe DSL

The `FdoScript` DSL provides compile-time type safety, IDE autocomplete, and a fluent builder API for programmatically constructing FDO streams. The DSL compiles to byte-identical binary as the original Ada32.dll compiler.

### Basic Usage

```java
import com.atomforge.fdo.dsl.FdoScript;
import com.atomforge.fdo.dsl.values.*;

byte[] binary = FdoScript.stream()
    .startObject(ObjectType.IND_GROUP, "Login Window")
        .orientation(Orientation.VCF)
        .position(Position.CENTER_CENTER)
        .preciseWidth(400)
        .preciseHeight(300)
        .title("Welcome")
    .endObject()
    .compile();
```

### Type-Safe Enums

The DSL uses strongly-typed enums to prevent errors at compile time:

```java
// IDE autocomplete shows all valid options
.orientation(Orientation.VCF)
.position(Position.CENTER_CENTER)
.fontId(FontId.ARIAL)
.triggerStyle(TriggerStyle.FRAMED)
.frameStyle(FrameStyle.DOUBLE_LINE)
.titlePosition(TitlePosition.ABOVE_CENTER)

// Compile error - type mismatch
.orientation("vcf")  // Won't compile!
```

### Available Enums

| Enum | Values |
|------|--------|
| `ObjectType` | `IND_GROUP`, `TRIGGER`, `VIEW`, `ORNAMENT`, `EDIT_VIEW`, etc. |
| `Orientation` | `VCF`, `HCC`, `VLC`, `HLF`, etc. |
| `Position` | `CENTER_CENTER`, `TOP_LEFT`, `BOTTOM_RIGHT`, etc. |
| `FontId` | `ARIAL`, `TIMES`, `COURIER`, `SYSTEM`, etc. |
| `FontStyle` | `PLAIN`, `BOLD`, `ITALIC`, `BOLD_ITALIC` |
| `TriggerStyle` | `FRAMED`, `UNFRAMED`, `RECTANGLE`, `PICTURE`, etc. |
| `FrameStyle` | `NONE`, `SINGLE`, `DOUBLE_LINE`, `SHADOW`, etc. |
| `TitlePosition` | `ABOVE_CENTER`, `BELOW_CENTER`, `LEFT`, etc. |
| `Criterion` | `SELECT`, `CLOSE`, `OPEN`, `GAIN_FOCUS`, etc. |

### Building Complex UI Elements

```java
// Button with action handler
byte[] binary = FdoScript.stream()
    .startObject(ObjectType.TRIGGER, "OK Button")
        .position(Position.BOTTOM_CENTER)
        .triggerStyle(TriggerStyle.FRAMED)
        .fontSis(FontId.ARIAL, 12, FontStyle.BOLD)
        .colorFace(0, 109, 170)
        .colorText(255, 255, 255)
        .onSelect(action -> action
            .data("Button clicked")
        )
    .endObject()
    .compile();
```

### Using Protocol-Specific Atom Enums

For fine-grained control, use the protocol-specific atom enums directly:

```java
import com.atomforge.fdo.dsl.atoms.*;

byte[] binary = FdoScript.stream()
    .uni(UniAtom.START_STREAM)
    .man(ManAtom.START_OBJECT, ObjectType.VIEW, "Text View")
    .mat(MatAtom.SIZE, 10, 40)
    .mat(MatAtom.ORIENTATION, Orientation.VCF)
    .de(DeAtom.DATA, "Hello, World!")
    .man(ManAtom.END_OBJECT)
    .uni(UniAtom.END_STREAM)
    .compile();
```

### Typed Convenience Methods

The DSL provides typed methods for all 1,887 atoms across 54 protocols:

```java
FdoScript.stream()
    // UNI protocol
    .uniStartStream()
    .uniEndStream()
    .uniInvokeLocal(FdoGid.of(32, 105))

    // MAN protocol
    .manStartObject(ObjectType.IND_GROUP, "Window")
    .manEndObject()
    .manClose(FdoGid.of(32, 105))

    // MAT protocol
    .matOrientation(Orientation.VCF)
    .matPosition(Position.CENTER_CENTER)
    .matObjectId(FdoGid.of(32, 105))
    .matPreciseWidth(400)
    .matFontSis(FontId.ARIAL, 12, FontStyle.BOLD)
    .matTriggerStyle(TriggerStyle.FRAMED)

    // ACT protocol
    .actSetCriterion(Criterion.SELECT)
    .actReplaceAction(nested -> nested
        .uniStartStream()
        .manCloseUpdate()
        .uniEndStream()
    )

    // DE protocol
    .deData("Hello")
    .deValidate()

    .compile();
```

### Round-Trip Compatibility

DSL-compiled binaries maintain perfect round-trip fidelity:

```java
// Build with DSL
byte[] dslBinary = FdoScript.stream()
    .startObject(ObjectType.VIEW, "Test")
    .endObject()
    .compile();

// Decompile to text
String text = FdoScript.decompile(dslBinary);

// Compile text back to binary
byte[] recompiled = FdoScript.compile(text);

// They match
assertArrayEquals(dslBinary, recompiled);
```

---

## Code Generation

Convert existing FDO text source to type-safe Java DSL code.

### Basic Code Generation

```java
import com.atomforge.fdo.codegen.DslCodeGenerator;
import com.atomforge.fdo.codegen.CodeGenConfig;

String fdoSource = """
    uni_start_stream <00x>
    man_start_object <ind_group, "Window">
    mat_orientation <vcf>
    mat_object_id <32-105>
    man_end_object
    uni_end_stream
    """;

DslCodeGenerator generator = new DslCodeGenerator();

// Generate just the method body
String methodBody = generator.generateMethodBody(fdoSource);
// Returns:
// FdoScript.stream()
//     .uniStartStream()
//     .manStartObject(ObjectType.IND_GROUP, "Window")
//     .matOrientation(Orientation.VCF)
//     .matObjectId(FdoGid.of(32, 105))
//     .manEndObject()
//     .uniEndStream()
//     .compile()

// Generate a complete class
String javaClass = generator.generate(fdoSource,
    CodeGenConfig.fullClass("com.example", "MyWindow"));
```

### Code Generation Features

The code generator produces type-safe code with:

- **Typed Methods**: Uses `matOrientation(Orientation.VCF)` instead of `.atom("mat_orientation", "vcf")`
- **Typed Enums**: `ObjectType.IND_GROUP`, `Criterion.SELECT`, `FontId.ARIAL`, etc.
- **GID Objects**: `FdoGid.of(32, 105)` instead of string literals
- **Proper Imports**: Generates all necessary import statements

### Verified Binary Equivalence

Generated DSL code produces byte-identical output to the original text source:

```java
String fdoSource = loadFdoFile("window.fdo");
byte[] textCompiled = FdoCompiler.create().compile(fdoSource);

String generatedDsl = generator.generateMethodBody(fdoSource);
byte[] dslCompiled = executeDsl(generatedDsl);  // Via JShell or direct execution

assertArrayEquals(textCompiled, dslCompiled);  // Byte-identical
```

---

## P3 Framing Support

For server implementations that need to wrap FDO data in P3 frames, the library provides a streaming callback API that respects frame boundaries.

### P3 Packet Structure

P3 packets consist of:

1. **Header** (8 bytes): sync, CRC, length, sequence numbers, type flag
2. **Token** (2 bytes): Indicates packet nature (e.g., `AT`, `Ki`, `f1`)
3. **Data**: FDO atom stream payload

P3 payload sizes were limited to 119 bytes maximum.

### Frame-Aware Compilation

```java
FdoCompiler compiler = FdoCompiler.create();

final int P3_MAX_PAYLOAD = 119;

compiler.compileToFrames(fdoSource, P3_MAX_PAYLOAD, (frameData, frameIndex, isLastFrame) -> {
    // Wrap the FDO payload in your P3 frame structure
    P3Frame frame = P3Frame.create()
        .setPayload(frameData)
        .setSequence(frameIndex)
        .setFinalFlag(isLastFrame);

    connection.send(frame);
});
```

### FrameConsumer Interface

```java
@FunctionalInterface
public interface FrameConsumer {
    /**
     * Called when a complete frame of atom data is ready.
     *
     * @param frameData   The encoded atom bytes (never exceeds maxFrameSize)
     * @param frameIndex  Zero-based sequence number
     * @param isLastFrame True if this is the final frame
     */
    void onFrame(byte[] frameData, int frameIndex, boolean isLastFrame);
}
```

### Frame-Aware Guarantees

The `FrameAwareEncoder` provides:

- **No Split Atoms**: Individual atoms are never split across frames
- **Automatic Continuation**: Large atoms use UNI continuation protocol
- **Protocol State**: Context maintained across frame boundaries
- **Minimum Frame Size**: Frame size must be at least 4 bytes

---

## Error Handling

The library uses `FdoException` for all error conditions with Ada32-compatible error codes:

```java
try {
    byte[] binary = compiler.compile(source);
} catch (FdoException e) {
    System.err.println("Error: " + e.getMessage());
    System.err.println("Code: " + e.getCode());

    if (e.hasLocation()) {
        System.err.println("Line: " + e.getLine());
        System.err.println("Column: " + e.getColumn());
    }
}
```

### Error Codes

| Code | Name | Description |
|------|------|-------------|
| `0x0006` | `MISSING_QUOTE` | Unterminated string literal |
| `0x0007` | `MISSING_OPEN_BRACKET` | Expected `<` for argument list |
| `0x0008` | `MISSING_CLOSE_BRACKET` | Expected `>` to close arguments |
| `0x0009` | `MISSING_COMMA` | Expected comma between arguments |
| `0x000A` | `UNRECOGNIZED_ATOM` | Unknown atom name |
| `0x000B` | `BAD_ARGUMENT_FORMAT` | Invalid argument syntax |
| `0x000E` | `BAD_NUMBER_FORMAT` | Invalid numeric literal |
| `0x0010` | `BAD_GID_FORMAT` | Invalid Global ID format |
| `0x0020` | `INVALID_BINARY_FORMAT` | Malformed binary data |
| `0x0021` | `UNEXPECTED_EOF` | Premature end of input |

---

## API Reference

### FdoCompiler

| Method | Description |
|--------|-------------|
| `create()` | Create compiler with default atom table |
| `compile(String)` | Compile source text to binary |
| `compile(File)` | Compile from file |
| `compile(InputStream)` | Compile from input stream |
| `compileToFrames(String, int, FrameConsumer)` | Streaming compile with frame callback |

### FdoDecompiler

| Method | Description |
|--------|-------------|
| `create()` | Create decompiler with default atom table |
| `decompile(byte[])` | Decompile binary to source text |
| `decompile(File)` | Decompile from file |
| `decompile(InputStream)` | Decompile from input stream |

### FdoStream

| Method | Description |
|--------|-------------|
| `decode(byte[])` | Decode binary FDO data to object model |
| `toBytes()` | Re-encode to binary (round-trip) |
| `findFirst(String)` | Find first atom by name |
| `findAll(String)` | Find all atoms by name |
| `findByProtocol(int)` | Find atoms by protocol number |
| `filter(Predicate)` | Filter atoms with predicate |
| `stream()` | Get Java Stream for functional operations |

### FdoAtom

| Method | Description |
|--------|-------------|
| `getString()` | Get string value (throws if wrong type) |
| `getStringOpt()` | Get string value as Optional |
| `getNumber()` | Get numeric value (throws if wrong type) |
| `getNumberOpt()` | Get numeric value as Optional |
| `getGid()` | Get GID value (throws if wrong type) |
| `getBoolean()` | Get boolean value |
| `getNestedStream()` | Get nested FdoStream |

### FdoScript

| Method | Description |
|--------|-------------|
| `stream()` | Start building an FDO stream |
| `compile(String)` | Compile FDO text to binary |
| `decompile(byte[])` | Decompile binary to text |

### DslCodeGenerator

| Method | Description |
|--------|-------------|
| `generateMethodBody(String)` | Generate DSL method body from FDO source |
| `generate(String, CodeGenConfig)` | Generate complete class with configuration |

---

## Building from Source

### Prerequisites

- Java 21 or later
- Maven 3.8+

### Build Commands

```bash
# Compile
mvn compile

# Run tests (80,000+ tests including golden tests)
mvn test

# Package as JAR
mvn package

# Install to local repository
mvn install
```

---

## License

This project is proprietary software. All rights reserved.
