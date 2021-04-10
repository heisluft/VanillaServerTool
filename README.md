# VanillaServerTool
A Simple Command-Line Tool for downloading Vanilla Minecraft Servers

Requires Java 11 to run.

## Usage
run:
```
java -jar <jarName> <directory> [version]
```
The version option can be explicitly written (e.g 1.11.2) or inferred using 'latest' or 'latest-snapshot'. It defaults to 'latest'

## Building
On Windows:
```bat
gradlew.bat shadowJar
```
On UNIX/UNIX-Like:
```sh
./gradlew shadowJar
```

The built jar will reside in `build/libs/`.
