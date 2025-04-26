# Problem Grabber

<!-- Plugin description -->
Problem Grabber captures problem details from IntelliJ IDEA in a format optimized for sharing with Large Language Models (LLMs). It allows you to easily copy problem details, including line numbers, file paths, error messages, and quick fix suggestions, to help get better assistance from LLMs.
<!-- Plugin description end -->

## Features

- Capture problems in a specific file
- Capture all problems in a project
- Format problems in LLM-friendly markdown format
- Copy formatted problems to clipboard for easy sharing with LLMs

### Features (from more comprehensive first iteration, which we simplified)
TODO: consider adding these if missing:
- Capture single problem details
- Capture all problems in a file
- Capture all problems in a project
- Filter problems by severity or type
- Export to clipboard or file
- Format problems in LLM-friendly format

## Usage

1. Right-click on a file in the Project view or Editor
2. Select "Capture Problems for LLM"
3. Choose whether to capture problems in the current file or the entire project
4. Problems will be copied to the clipboard in markdown format
5. Paste into your favorite LLM chat interface

## Building

This plugin is built using Gradle. 

```shell
# Clean and build the project
./gradlew clean build             
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
# Build the deployable plugin zip with detailed logs
./gradlew buildPlugin --info      
```
