#!/bin/bash

# Javalang-analyzing-cli Release Script
# Script for building and preparing release files

set -e  # Stop execution on error

echo "🚀 Javalang-analyzing-cli Release Builder"
echo "========================================"

# Get version number
VERSION=$(grep 'version = ' build.gradle | sed "s/version = '//" | sed "s/'//")
echo "📋 Building version: $VERSION"

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean

# Refresh dependencies and build Fat JAR
echo "🔨 Building Fat JAR..."
./gradlew shadowJar --refresh-dependencies

# Check if build was successful
if [[ ! -f "build/libs/Javalang-analyzing-cli-${VERSION}-all.jar" ]]; then
    echo "❌ Build failed: JAR file not found"
    exit 1
fi

# Get JAR file size
JAR_SIZE=$(ls -lh "build/libs/Javalang-analyzing-cli-${VERSION}-all.jar" | awk '{print $5}')
echo "📦 JAR file size: $JAR_SIZE"

# Create release directory
RELEASE_DIR="release-${VERSION}"
echo "📁 Creating release directory: $RELEASE_DIR"
rm -rf "$RELEASE_DIR"
mkdir -p "$RELEASE_DIR"

# Copy files to release directory
echo "📋 Copying release files..."
cp "build/libs/Javalang-analyzing-cli-${VERSION}-all.jar" "$RELEASE_DIR/"
cp README.md "$RELEASE_DIR/"
cp CHANGELOG.md "$RELEASE_DIR/"
cp LICENSE "$RELEASE_DIR/"

# Create example configuration file
echo "📝 Creating example configuration..."
cat > "$RELEASE_DIR/analyzer.yml" << EOF
# Javalang-analyzing-cli Example Configuration File
# Copy and modify this file to fit your project

project: /path/to/your/java/project
threads: 8
lang: java
output:
  format: json
  file: analysis-results.json

# Task-specific configuration
ParseTestCaseToLlmContext:
  outputDirectory: /custom/output/path
  includeDisabledTests: false
EOF

# Create quick start scripts
echo "📝 Creating quick start scripts..."

# Linux/macOS script
cat > "$RELEASE_DIR/run.sh" << EOF
#!/bin/bash
# Javalang-analyzing-cli Quick Start Script

JAR_FILE="Javalang-analyzing-cli-${VERSION}-all.jar"

if [[ ! -f "\$JAR_FILE" ]]; then
    echo "Error: \$JAR_FILE not found in current directory"
    exit 1
fi

echo "🚀 Javalang-analyzing-cli v${VERSION}"
echo "Usage examples:"
echo ""
echo "1. Show help:"
echo "   java -jar \$JAR_FILE --help"
echo ""
echo "2. Analyze a project:"
echo "   java -jar \$JAR_FILE ParseTestCaseToLlmContext --project /path/to/project"
echo ""
echo "3. Use configuration file:"
echo "   java -jar \$JAR_FILE ParseTestCaseToLlmContext --config analyzer.yml"
echo ""

# If arguments are provided, execute directly
if [[ \$# -gt 0 ]]; then
    java -jar "\$JAR_FILE" "\$@"
fi
EOF

# Windows script
cat > "$RELEASE_DIR/run.bat" << EOF
@echo off
REM Javalang-analyzing-cli Quick Start Script

set JAR_FILE=Javalang-analyzing-cli-${VERSION}-all.jar

if not exist "%JAR_FILE%" (
    echo Error: %JAR_FILE% not found in current directory
    exit /b 1
)

echo 🚀 Javalang-analyzing-cli v${VERSION}
echo Usage examples:
echo.
echo 1. Show help:
echo    java -jar %JAR_FILE% --help
echo.
echo 2. Analyze a project:
echo    java -jar %JAR_FILE% ParseTestCaseToLlmContext --project C:\path\to\project
echo.
echo 3. Use configuration file:
echo    java -jar %JAR_FILE% ParseTestCaseToLlmContext --config analyzer.yml
echo.

REM If arguments are provided, execute directly
if "%~1" neq "" (
    java -jar "%JAR_FILE%" %*
)
EOF

# Set execute permissions
chmod +x "$RELEASE_DIR/run.sh"

# Create release notes
echo "📝 Creating release notes..."
cat > "$RELEASE_DIR/RELEASE_NOTES.md" << EOF
# Javalang-analyzing-cli v${VERSION}

## 📦 Release Files

- **Javalang-analyzing-cli-${VERSION}-all.jar** (${JAR_SIZE}) - Main executable Fat JAR
- **README.md** - Complete usage documentation
- **CHANGELOG.md** - Version change log
- **LICENSE** - Open source license
- **analyzer.yml** - Example configuration file
- **run.sh** / **run.bat** - Quick start scripts

## 🚀 Quick Start

### System Requirements
- Java 21 or higher

### Basic Usage
\`\`\`bash
# Check version
java -jar Javalang-analyzing-cli-${VERSION}-all.jar --version

# Analyze project
java -jar Javalang-analyzing-cli-${VERSION}-all.jar ParseTestCaseToLlmContext \\
    --project /path/to/your/java/project

# Use configuration file
java -jar Javalang-analyzing-cli-${VERSION}-all.jar ParseTestCaseToLlmContext \\
    --config analyzer.yml
\`\`\`

### Using Scripts
\`\`\`bash
# Linux/macOS
./run.sh ParseTestCaseToLlmContext --project /path/to/project

# Windows
run.bat ParseTestCaseToLlmContext --project C:\path\to\project
\`\`\`

## 📋 New Features

See CHANGELOG.md for complete feature list and updates.

## 🐛 Issue Reporting

For issues, please report them in GitHub Issues.
EOF

# Run basic tests
echo "🧪 Running basic tests..."
java -jar "$RELEASE_DIR/Javalang-analyzing-cli-${VERSION}-all.jar" --version > /dev/null
if [[ $? -eq 0 ]]; then
    echo "✅ Basic functionality test passed"
else
    echo "❌ Basic functionality test failed"
    exit 1
fi

# Generate checksums
echo "🔒 Generating checksums..."
cd "$RELEASE_DIR"
sha256sum "Javalang-analyzing-cli-${VERSION}-all.jar" > "Javalang-analyzing-cli-${VERSION}-all.jar.sha256"
cd ..

# Create compressed archives
echo "📦 Creating release archive..."
tar -czf "${RELEASE_DIR}.tar.gz" "$RELEASE_DIR"
zip -r "${RELEASE_DIR}.zip" "$RELEASE_DIR" > /dev/null

echo ""
echo "🎉 Release build completed successfully!"
echo "========================================"
echo "📁 Release directory: $RELEASE_DIR"
echo "📦 Archives created:"
echo "   - ${RELEASE_DIR}.tar.gz"
echo "   - ${RELEASE_DIR}.zip"
echo "📋 JAR file: Javalang-analyzing-cli-${VERSION}-all.jar (${JAR_SIZE})"
echo ""
echo "🚀 Ready for GitHub release!"
echo "   1. Create a new release on GitHub"
echo "   2. Upload the JAR file and archives"
echo "   3. Copy content from RELEASE_NOTES.md"
echo "" 