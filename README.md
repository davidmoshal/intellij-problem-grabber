# Problem Grabber

<!-- Plugin description -->
Problem Grabber captures problem details from IntelliJ IDEA in a format optimized for sharing with Large Language Models (LLMs). It allows you to easily copy problem details, including line numbers, file paths, error messages, and quick fix suggestions, to help get better assistance from LLMs.
<!-- Plugin description end -->

## Compatibility

- **Minimum Support**: IntelliJ IDEA 2024.2+ (build 242)
- **Maximum Support**: Unlimited (future-proof for IntelliJ 2025.1, 2025.2, and beyond)
- **Platform**: Works with IntelliJ IDEA Community and Ultimate editions
- **Java**: Requires Java 21+

## Features

- **Capture File Problems**: Extract all problems from the current file
- **Capture Project Problems**: Extract all problems from the entire project
- **Error vs All Problems**: Choose between errors only or all problem types
- **LLM-Optimized Format**: Problems formatted in clean markdown for AI assistance
- **Clipboard Integration**: Instantly copy formatted problems for sharing with LLMs
- **Context Menu Integration**: Available in editor and project view right-click menus
- **Tools Menu Access**: Also available via Tools → Problem Grabber menu

## Usage

### Context Menu (Right-click)
1. Right-click on a file in the Project view or Editor
2. Select one of the Problem Grabber options:
   - **Capture File Errors**: Extract only error-level problems from current file
   - **Capture File Problems**: Extract all problems from current file
   - **Capture Project Errors**: Extract only error-level problems from entire project
   - **Capture Project Problems**: Extract all problems from entire project

### Tools Menu
1. Go to **Tools** → **Problem Grabber**
2. Choose the same options as above

### After Capture
- Problems are automatically copied to clipboard in markdown format
- Paste directly into your favorite LLM chat interface (ChatGPT, Claude, etc.)
- The format includes file paths, line numbers, problem descriptions, and suggested fixes

## Installation

### From Built Plugin
1. Build the plugin: `./gradlew buildPlugin`
2. Install the ZIP from `build/distributions/` in IntelliJ:
   - **File** → **Settings** → **Plugins** → **⚙️** → **Install Plugin from Disk**
   - Select the generated ZIP file

### For Development/Testing
```shell
# Run IntelliJ with your plugin for testing
./gradlew runIde
```

## Building

This plugin uses Gradle with the IntelliJ Platform Gradle Plugin 2.x.

### Prerequisites
- Java 21+
- IntelliJ IDEA 2024.2+ (for development)

### Build Commands

```shell
# Build the plugin (without tests)
./gradlew assemble
```

```shell
# Build with tests (if tests are working)
./gradlew build
```

```shell
# Run IntelliJ with your plugin for testing
./gradlew runIde
```

```shell
# Run IntelliJ in debug mode for troubleshooting
./gradlew debugIde
```

```shell
# Build the deployable plugin ZIP
./gradlew buildPlugin
```

```shell
# Build with detailed logs
./gradlew buildPlugin --info
```

### Build Status
✅ **Current Status**: Plugin builds successfully and is compatible with IntelliJ 2024.2+

The plugin has been updated to use:
- IntelliJ Platform Gradle Plugin 2.9.0 (latest)
- Java 21 compatibility
- IntelliJ IDEA 2024.2.4 platform
- Future-proof compatibility (no until-build restriction)
