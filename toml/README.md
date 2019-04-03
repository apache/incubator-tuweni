# Tuweni-Toml: TOML parser for Java/Kotlin

Tuweni-Toml is a complete TOML parser with the following attributes:

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
