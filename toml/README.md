# Cava-Toml: TOML parser for Java/Kotlin (by ConsenSys)

Cava-Toml is a complete TOML parser with the following attributes:

* Supports the latest TOML specification version (0.5.0).
* Provides detailed error reporting, including error position.
* Performs error recovery, allowing parsing to continue after an error.

It uses the [ANTLR](https://github.com/antlr/antlr4/) parser-generator and
runtime library.

## Usage

Parsing is straightforward:

```
Path source = Paths.get("/path/to/file.toml");
TomlParseResult result = Toml.parse(source);
result.errors().forEach(error -> System.err.println(error.toString()));

String value = result.getString("a. dotted . key");
```

## Getting Cava-Toml

Cava-Toml is part of the [ConsenSys Cava](https://github.com/ConsenSys/cava) project, and is available whenever cava is included in a project.

It can also be included independently, using the cava-toml jar, which is published to [ConsenSys bintray repository](https://bintray.com/consensys/consensys/cava) and linked to JCenter.

With Maven:
```
<dependency>
  <groupId>net.consensys.cava</groupId>
  <artifactId>cava-toml</artifactId>
  <version>0.6.0</version>
</dependency>
```

With Gradle: `compile 'net.consensys.cava:cava-toml:0.6.0'`

## Links

- [GitHub project](https://github.com/consensys/cava)
- [Online Java documentation](https://consensys.github.io/cava/docs/java/latest/net/consensys/cava/toml/package-summary.html)
- [Online Kotlin documentation](https://consensys.github.io/cava/docs/kotlin/latest/cava/net.consensys.cava.toml/index.html)
- [Issue tracker: Report a defect or feature request](https://github.com/google/cava/issues/new)
- [StackOverflow: Ask "how-to" and "why-didn't-it-work" questions](https://stackoverflow.com/questions/ask?tags=cava+toml+java)
- [cava-discuss: For open-ended questions and discussion](http://groups.google.com/group/cava-discuss)
