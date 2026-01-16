# Fluenta DITA Translation Manager - Technical Documentation

## Table of Contents

1. [Project Overview](#project-overview)
2. [Features & Capabilities](#features--capabilities)
3. [Architecture Overview](#architecture-overview)
4. [Java API Documentation](#java-api-documentation)
5. [CLI Interface Documentation](#cli-interface-documentation)
6. [Data Models](#data-models)
7. [Usage Examples](#usage-examples)
8. [Component Details](#component-details)
9. [Build & Development](#build--development)
10. [Dependencies](#dependencies)
11. [File Structure Reference](#file-structure-reference)

---

## Project Overview

**Fluenta** is an open-source DITA translation management tool that simplifies the translation and localization of DITA projects by combining two OASIS standards: **DITA** (Darwin Information Typing Architecture) and **XLIFF** (XML Localization Interchange File Format).

### Technology Stack

- **Backend**: Java 21 LTS
- **Frontend**: Electron 36.3.2 + TypeScript 5.8.3
- **Build Tools**: Apache Ant 1.10.14, npm, TypeScript Compiler
- **Databases**: SQLite (via sqlite-jdbc), MapDB
- **Module System**: Java modules (module-info.java)

### Version Information

- **Version**: 5.4.0
- **Build**: 20250529_1723
- **License**: EPL-1.0 (Eclipse Public License 1.0)
- **Author**: Rodolfo M. Raya (Maxprograms)
- **Homepage**: https://www.maxprograms.com

---

## Features & Capabilities

### Core Features

- **XLIFF Generation**: Convert DITA maps to XLIFF files for translator workflow
  - Supports XLIFF versions: 1.2, 2.0, 2.1, 2.2
  - Configurable segmentation (paragraph or sentence-level)
  - Optional DITAVAL filtering for conditional content
  - Modified files only mode
  - Skeleton embedding options

- **XLIFF Import**: Import translated XLIFF back into DITA projects
  - Accept unapproved translations option
  - Tag error detection and reporting
  - Automatic TM updates

- **Translation Memory (TM) Management**:
  - Create and manage multiple translation memories
  - Import/Export TMX (Translation Memory eXchange) files
  - ICE (In-Context Exact) matching support
  - Automatic TM leverage during XLIFF generation
  - Per-language source language configuration

- **Multi-Language Project Support**:
  - Multiple target languages per project
  - Language-specific status tracking
  - Build numbering and history tracking

- **Advanced Features**:
  - Repetition analysis
  - Word count generation
  - Tag error reporting
  - Project event history tracking
  - Ignore tracked changes (Office formats)
  - SVG content filtering

- **Dual Interface**:
  - **GUI Mode**: Full-featured Electron desktop application
  - **Headless CLI Mode**: Batch processing for automation and CI/CD integration

- **Internationalization**:
  - Multi-language UI (English, Spanish)
  - Theme support (Light, Dark, High Contrast, Neutral)

---

## Architecture Overview

Fluenta uses a **hybrid architecture** combining a Java backend for heavy processing with an Electron/TypeScript frontend for cross-platform desktop UI.

```
┌─────────────────────────────────────────────────────────────┐
│                    Electron Frontend                        │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │  Projects    │  │  Memories    │  │  30+ Dialog     │  │
│  │  View        │  │  View        │  │  Components     │  │
│  └──────────────┘  └──────────────┘  └─────────────────┘  │
│         │                 │                    │            │
│         └─────────────────┴────────────────────┘            │
│                          │                                  │
│                    IPC Messages                             │
│                          │                                  │
└──────────────────────────┼──────────────────────────────────┘
                           │
┌──────────────────────────┼──────────────────────────────────┐
│                    Java Backend                             │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  API Layer (API.java)                                  │ │
│  │  - importMemory()  - exportMemory()                    │ │
│  │  - generateXLIFF() - importXLIFF()                     │ │
│  └────────────────────────────────────────────────────────┘ │
│                          │                                  │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Controllers                                           │ │
│  │  - LocalController   - ProjectsManager                 │ │
│  │  - MemoriesManager   - TagErrorsReport                 │ │
│  └────────────────────────────────────────────────────────┘ │
│                          │                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │  Models      │  │  Libraries   │  │  Utilities      │  │
│  │  - Project   │  │  - OpenXLIFF │  │  - FileUtils    │  │
│  │  - Memory    │  │  - Swordfish │  │  - Preferences  │  │
│  │  - Event     │  │  - XML/JSON  │  │  - Messages     │  │
│  └──────────────┘  └──────────────┘  └─────────────────┘  │
│                          │                                  │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Data Persistence                                      │ │
│  │  - projects.json  - memories.json  - preferences.json  │ │
│  │  - SQLite (TM storage via swordfish.jar)              │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Design Patterns

1. **MVC-like Separation**:
   - Models: Project, Memory, ProjectEvent
   - Controllers: LocalController, ProjectsManager, MemoriesManager
   - Views: TypeScript UI components

2. **Singleton/Stateless API**:
   - API class: Static methods only, not instantiable
   - CLI class: Static operations for command-line interface

3. **File Locking for Concurrency**:
   - Lock file mechanism prevents concurrent CLI operations
   - Synchronized methods in managers for thread safety

4. **JSON-based Data Persistence**:
   - Projects: Stored in `projects.json`
   - Memories: Stored in `memories.json`
   - Translation units: SQLite database (via swordfish.jar)

5. **Externalized Configuration**:
   - XML catalogs for entity resolution
   - Filter configurations for XML processing
   - SRX files for segmentation rules

---

## Java API Documentation

### API Class

**File**: `src/com/maxprograms/fluenta/API.java`

The API class provides static methods for programmatic access to Fluenta's core functionality.

#### Import Translation Memory

```java
public static int importMemory(long id, String tmxFile)
    throws IOException, SQLException, SAXException,
           ParserConfigurationException, JSONException,
           ParseException, URISyntaxException
```

**Parameters**:
- `id` - Memory ID (long)
- `tmxFile` - Path to TMX file to import

**Returns**: Number of translation units imported (int)

**Throws**: IOException if memory not found or file errors

#### Export Translation Memory

```java
public static void exportMemory(long id, String tmxFile)
    throws IOException, SQLException, SAXException,
           ParserConfigurationException, JSONException,
           ParseException, URISyntaxException
```

**Parameters**:
- `id` - Memory ID (long)
- `tmxFile` - Output path for TMX file

**Throws**: IOException if memory not found

#### Generate XLIFF (CLI/Batch)

```java
protected static void generateXLIFF(String jsonFile, boolean verbose)
    throws IOException, SAXException, ParserConfigurationException,
           URISyntaxException, SQLException, JSONException, ParseException
```

**Parameters**:
- `jsonFile` - Path to JSON configuration file
- `verbose` - Enable verbose logging

**JSON Configuration Format**:

```json
{
  "id": 1234567890,
  "xliffFolder": "/path/to/xliff/output",
  "tgtLang": ["de-DE", "fr-FR", "es-ES"],
  "ditaval": "/path/to/filter.ditaval",
  "version": "2.1",
  "useICE": true,
  "useTM": true,
  "generateCount": true,
  "embedSkeleton": false,
  "paragraph": false,
  "modifiedFilesOnly": false,
  "ignoreTrackedChanges": false,
  "ignoreSVG": false
}
```

#### Import XLIFF (CLI/Batch)

```java
protected static void importXLIFF(String jsonFile, boolean verbose)
    throws IOException, NumberFormatException, SAXException,
           ParserConfigurationException, SQLException,
           URISyntaxException, JSONException, ParseException
```

**Parameters**:
- `jsonFile` - Path to JSON configuration file
- `verbose` - Enable verbose logging

**JSON Configuration Format**:

```json
{
  "id": 1234567890,
  "xliffFile": "/path/to/translated.xlf",
  "outputFolder": "/path/to/output",
  "updateTM": true,
  "acceptUnapproved": false,
  "ignoreTagErrors": false
}
```

---

## CLI Interface Documentation

### CLI Class

**File**: `src/com/maxprograms/fluenta/CLI.java`

The CLI provides command-line access for headless/batch operations.

### Available Commands

#### Version Information

```bash
java -cp "jars/*" com.maxprograms.fluenta.CLI -version
```

**Output** (JSON):
```json
{
  "version": "5.4.0",
  "build": "20250529_1723"
}
```

#### System Information

```bash
java -cp "jars/*" com.maxprograms.fluenta.CLI -about
```

**Output** (JSON):
```json
{
  "version": "5.4.0",
  "build": "20250529_1723",
  "java": "21.0.x",
  "vendor": "Eclipse Adoptium",
  "swordfish": "10.x.x-yyyyMMdd_HHmm",
  "openxliff": "3.x.x-yyyyMMdd_HHmm",
  "bcp47j": "1.x.x-yyyyMMdd_HHmm",
  "xmljava": "2.x.x-yyyyMMdd_HHmm"
}
```

#### Generate XLIFF

```bash
java -cp "jars/*" com.maxprograms.fluenta.CLI -generateXLIFF config.json [-verbose]
```

**Parameters**:
- `config.json` - JSON configuration file (see API documentation for format)
- `-verbose` - Optional: Enable detailed logging

#### Import XLIFF

```bash
java -cp "jars/*" com.maxprograms.fluenta.CLI -importXLIFF config.json [-verbose]
```

**Parameters**:
- `config.json` - JSON configuration file (see API documentation for format)
- `-verbose` - Optional: Enable detailed logging

#### Import TMX to Memory

```bash
java -cp "jars/*" com.maxprograms.fluenta.CLI -importTmx <memoryId> -tmx <tmxFile> [-verbose]
```

**Parameters**:
- `<memoryId>` - Numeric ID of the translation memory
- `<tmxFile>` - Path to TMX file to import
- `-verbose` - Optional: Enable detailed logging

**Output**: Displays number of translation units imported

#### Export Memory to TMX

```bash
java -cp "jars/*" com.maxprograms.fluenta.CLI -exportTmx <memoryId> -tmx <tmxFile> [-verbose]
```

**Parameters**:
- `<memoryId>` - Numeric ID of the translation memory
- `<tmxFile>` - Output path for TMX file
- `-verbose` - Optional: Enable detailed logging

### Exit Codes

- `0` - Success
- `3` - Error (check logs for details)
- `1` - Lock file error (another instance running)

---

## Data Models

### Project Model

**File**: `src/com/maxprograms/fluenta/models/Project.java`

#### Structure

```java
public class Project {
    private long id;                          // Unique identifier (timestamp)
    private String title;                     // Project title
    private String description;               // Project description
    private String map;                       // Path to DITA map file
    private Date creationDate;                // Creation timestamp
    private Date lastUpdate;                  // Last modification timestamp
    private String srcLanguage;               // Source language code (BCP-47)
    private List<String> tgtLanguages;        // Target language codes
    private List<Long> memories;              // Associated memory IDs
    private List<ProjectEvent> history;       // Event history
    private Map<String, String> languageStatus; // Per-language status
}
```

#### Project Status Values

- `"0"` - NEW (no XLIFF generated yet)
- `"1"` - IN_PROGRESS (XLIFF generated, not imported)
- `"2"` - COMPLETED (all XLIFF imported)

#### Language Status Values

- `"3"` - UNTRANSLATED
- `"4"` - TRANSLATED

#### JSON Format (projects.json)

```json
{
  "version": 1,
  "projects": [
    {
      "id": 1705405200000,
      "title": "Product Documentation v2.0",
      "description": "User guide and API reference",
      "map": "docs/product.ditamap",
      "creationDate": "2025-01-16 10:00",
      "lastUpdate": "2025-01-16 14:30",
      "status": "1",
      "srcLanguage": "en-US",
      "tgtLanguages": ["de-DE", "fr-FR", "es-ES"],
      "memories": [1705400000000, 1705400001000],
      "history": [
        {
          "type": "xliffCreated",
          "language": "de-DE",
          "date": "2025-01-16 11:00",
          "build": 0
        }
      ],
      "languageStatus": {
        "de-DE": "3",
        "fr-FR": "3",
        "es-ES": "3"
      }
    }
  ]
}
```

### Memory Model

**File**: `src/com/maxprograms/fluenta/models/Memory.java`

#### Structure

```java
public class Memory {
    private long id;                // Unique identifier (timestamp)
    private String name;            // Memory name
    private String description;     // Memory description
    private Date creationDate;      // Creation timestamp
    private Date lastUpdate;        // Last modification timestamp
    private Language srcLanguage;   // Source language
}
```

#### JSON Format (memories.json)

```json
{
  "version": 1,
  "memories": [
    {
      "id": 1705400000000,
      "name": "General Tech",
      "description": "General technical terminology",
      "creationDate": "2025-01-15 09:00",
      "lastUpdate": "2025-01-16 14:00",
      "srcLanguage": "en-US"
    }
  ]
}
```

### ProjectEvent Model

```java
public class ProjectEvent {
    public static final String XLIFF_CREATED = "xliffCreated";
    public static final String XLIFF_IMPORTED = "xliffImported";

    private String type;       // Event type
    private String language;   // Language code
    private Date date;         // Event timestamp
    private int build;         // Build number
}
```

---

## Usage Examples

### GUI Workflow: Creating a Project

1. Launch Fluenta: `npm start`
2. Click "Add Project" button
3. Fill in project details:
   - **Title**: Project name
   - **Description**: Project description
   - **DITA Map**: Browse to .ditamap file
   - **Source Language**: Select from dropdown (e.g., "en-US")
   - **Target Languages**: Add languages using "Add Language" button
4. Assign translation memories (optional)
5. Click "Save" - Project ID is generated from timestamp

### CLI: Batch XLIFF Generation

**Step 1**: Create configuration file `generate-xliff.json`:

```json
{
  "id": 1705405200000,
  "xliffFolder": "D:/xliff-output/product-docs",
  "tgtLang": ["de-DE", "fr-FR", "es-ES"],
  "ditaval": "D:/filters/exclude-draft.ditaval",
  "version": "2.1",
  "useICE": true,
  "useTM": true,
  "generateCount": true,
  "embedSkeleton": false,
  "paragraph": false,
  "modifiedFilesOnly": false,
  "ignoreTrackedChanges": false,
  "ignoreSVG": false
}
```

**Step 2**: Run command:

```bash
java -cp "jars/*" com.maxprograms.fluenta.CLI -generateXLIFF generate-xliff.json -verbose
```

### CLI: Batch XLIFF Import

**Step 1**: Create configuration file `import-xliff.json`:

```json
{
  "id": 1705405200000,
  "xliffFile": "D:/xliff-translated/product-docs-de-DE.xlf",
  "outputFolder": "D:/translated-output/de-DE",
  "updateTM": true,
  "acceptUnapproved": false,
  "ignoreTagErrors": false
}
```

**Step 2**: Run command:

```bash
java -cp "jars/*" com.maxprograms.fluenta.CLI -importXLIFF import-xliff.json -verbose
```

### CLI: TMX Import/Export

**Import TMX to Memory**:

```bash
# Find memory ID from memories.json or GUI
java -cp "jars/*" com.maxprograms.fluenta.CLI -importTmx 1705400000000 -tmx terminology.tmx -verbose
```

**Export Memory to TMX**:

```bash
java -cp "jars/*" com.maxprograms.fluenta.CLI -exportTmx 1705400000000 -tmx exported-memory.tmx
```

### Java API: Programmatic Usage

```java
import com.maxprograms.fluenta.API;

public class FluentaAutomation {
    public static void main(String[] args) {
        try {
            // Import TMX to memory
            long memoryId = 1705400000000L;
            int imported = API.importMemory(memoryId, "terminology.tmx");
            System.out.println("Imported " + imported + " translation units");

            // Generate XLIFF using config file
            API.generateXLIFF("generate-xliff.json", true);

            // Export memory
            API.exportMemory(memoryId, "backup.tmx");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

## Component Details

### Java Backend Components

#### Controllers

| Class | File | Responsibility |
|-------|------|----------------|
| LocalController | `src/com/maxprograms/fluenta/controllers/LocalController.java` | Core business logic for XLIFF generation/import, TM operations, catalog/filter management |
| ProjectsManager | `src/com/maxprograms/fluenta/controllers/ProjectsManager.java` | Project persistence, CRUD operations, JSON serialization |
| MemoriesManager | `src/com/maxprograms/fluenta/controllers/MemoriesManager.java` | Memory persistence, CRUD operations, JSON serialization |
| TagErrorsReport | `src/com/maxprograms/fluenta/controllers/TagErrorsReport.java` | Generate reports for translation tag errors |

#### Models

| Class | File | Purpose |
|-------|------|---------|
| Project | `src/com/maxprograms/fluenta/models/Project.java` | Represents DITA translation project with metadata |
| Memory | `src/com/maxprograms/fluenta/models/Memory.java` | Represents translation memory with metadata |
| ProjectEvent | `src/com/maxprograms/fluenta/models/ProjectEvent.java` | Tracks project history events |

#### Utilities

| Class | File | Purpose |
|-------|------|---------|
| FileUtils | `src/com/maxprograms/utils/FileUtils.java` | File operations, JSON I/O |
| Preferences | `src/com/maxprograms/utils/Preferences.java` | Application configuration management |
| SimpleLogger | `src/com/maxprograms/utils/SimpleLogger.java` | Logging functionality |
| Messages | `src/com/maxprograms/fluenta/Messages.java` | Internationalized message handling |

### TypeScript Frontend Components

#### Main Application

| File | Purpose |
|------|---------|
| `ts/fluenta.ts` | Main Electron application logic, IPC handlers, Java process management (~118KB) |
| `ts/main.ts` | Application entry point, view switching (projects/memories) |

#### Views

| File | Purpose |
|------|---------|
| `ts/projectsView.ts` | Projects list and management UI |
| `ts/memoriesView.ts` | Translation memories list and management UI |

#### Key Dialogs

| File | Purpose |
|------|---------|
| `ts/addProjectDialog.ts` | Create new translation project |
| `ts/editProjectDialog.ts` | Modify existing project |
| `ts/generateXliffDialog.ts` | Configure XLIFF generation options |
| `ts/importXliffDialog.ts` | Configure XLIFF import options |
| `ts/addMemoryDialog.ts` | Create new translation memory |
| `ts/editMemoryDialog.ts` | Modify translation memory |
| `ts/ProjectMemoriesDialog.ts` | Assign memories to projects |
| `ts/settingsDialog.ts` | Application settings |
| `ts/filterConfig.ts` | XML filter configuration |

#### Utilities

| File | Purpose |
|------|---------|
| `ts/languages.ts` | Language management and BCP-47 handling |
| `ts/i18n.ts` | Internationalization support |
| `ts/preferences.ts` | Application preferences handling |

---

## Build & Development

### Prerequisites

- **Java 21 LTS** - Download from [Adoptium](https://adoptium.net/)
- **Apache Ant 1.10.14** - Download from [Apache Ant](https://ant.apache.org)
- **Node.js 22.12.0 LTS** - Download from [nodejs.org](https://nodejs.org/)

### Building from Source

```bash
# Clone repository
git clone https://github.com/rmraya/Fluenta.git
cd Fluenta

# Set JAVA_HOME to JDK 21
export JAVA_HOME=/path/to/jdk-21  # Unix/Mac
set JAVA_HOME=C:\path\to\jdk-21   # Windows

# Build Java code
ant

# Install Node dependencies
npm install

# Run application
npm start
```

### Build Process Details

#### Java Build (Ant)

**Build file**: `build.xml`

**Key targets**:
- `init` - Creates output directory, copies resources
- `clean` - Removes build artifacts
- `compile` - Compiles Java code to JAR
- `link` - Creates Java runtime using jlink
- `dist` - Main distribution target (default)
- `copyWindows` - Windows-specific distribution
- `copyUnix` - Unix/Linux/macOS distribution

**Output**:
- Compiled classes: `out/`
- JAR file: `jars/fluenta.jar`
- Runtime binaries: `bin/`, `conf/`, `lib/`, `legal/`

#### TypeScript Build (npm)

**Configuration**: `tsconfig.json`

**Settings**:
- Target: ES6
- Module: CommonJS
- Module Resolution: Node
- Input: `ts/**/*.ts`
- Output: `js/`

**Commands**:
- `npm run build` - Compiles TypeScript to JavaScript
- `npm start` - Builds and launches Electron app

### Directory Structure

```
Fluenta/
├── src/                    # Java source code
│   └── com/maxprograms/
│       ├── fluenta/        # Main application code
│       └── utils/          # Utilities
├── ts/                     # TypeScript source code
├── js/                     # Compiled JavaScript (generated)
├── jars/                   # Java dependencies + fluenta.jar
├── html/                   # HTML UI templates (en/, es/)
├── css/                    # Stylesheets (themes)
├── i18n/                   # Internationalization files
├── icons/                  # Application icons
├── docs/                   # DITA documentation source
├── catalog/                # XML catalogs
├── xmlfilter/              # XML filter configurations
├── srx/                    # Segmentation rules
├── out/                    # Java compilation output
├── build.xml               # Ant build configuration
├── tsconfig.json           # TypeScript configuration
├── package.json            # npm configuration
└── README.md               # Basic project info
```

---

## Dependencies

### Java Dependencies (jars/)

| JAR File | Version | Purpose |
|----------|---------|---------|
| openxliff.jar | Latest | XLIFF conversion and validation |
| swordfish.jar | Latest | Translation memory engine (SQLite backend) |
| sqlite-jdbc-3.49.1.0.jar | 3.49.1.0 | SQLite database driver |
| mapdb.jar | Latest | Embedded database and data structures |
| jsoup.jar | Latest | HTML/XML parsing |
| json.jar | Latest | JSON processing |
| bcp47j.jar | Latest | BCP-47 language tag handling |
| xmljava.jar | Latest | XML utilities |

### npm Dependencies

**Production**:
```json
{
  "typesbcp47": "^1.5.5",  // BCP-47 TypeScript types
  "typesxml": "^1.7.0"     // XML TypeScript types
}
```

**Development**:
```json
{
  "electron": "^36.3.2",    // Desktop application framework
  "typescript": "^5.8.3"    // TypeScript compiler
}
```

### External Resources

- **XML Catalogs**: OASIS catalog format for entity resolution
- **DITA Filters**: Element-level filtering configurations
- **SRX Files**: Sentence segmentation rules
- **DITAVAL**: Conditional processing filters

---

## File Structure Reference

### Critical Configuration Files

| File | Purpose |
|------|---------|
| `projects.json` | Project persistence (in user preferences folder) |
| `memories.json` | Memory persistence (in user preferences folder) |
| `preferences.json` | Application preferences |
| `module-info.java` | Java module descriptor |
| `.classpath` | Eclipse classpath configuration |
| `.project` | Eclipse project configuration |

### Key Source Files by Function

#### XLIFF Operations
- `src/com/maxprograms/fluenta/API.java` - Public API
- `src/com/maxprograms/fluenta/controllers/LocalController.java` - Core logic
- `ts/generateXliffDialog.ts` - GUI for generation
- `ts/importXliffDialog.ts` - GUI for import

#### Translation Memory
- `src/com/maxprograms/fluenta/models/Memory.java` - Model
- `src/com/maxprograms/fluenta/controllers/MemoriesManager.java` - Persistence
- `ts/memoriesView.ts` - GUI management
- `ts/addMemoryDialog.ts` - Creation dialog

#### Project Management
- `src/com/maxprograms/fluenta/models/Project.java` - Model
- `src/com/maxprograms/fluenta/controllers/ProjectsManager.java` - Persistence
- `ts/projectsView.ts` - GUI management
- `ts/addProjectDialog.ts` - Creation dialog

#### Command-Line Interface
- `src/com/maxprograms/fluenta/CLI.java` - CLI entry point

---

## Additional Resources

- **User Manual**: `fluenta_en.pdf`, `fluenta_es.pdf`
- **DITA Documentation Source**: `docs/en/`, `docs/es/`
- **Official Website**: https://www.maxprograms.com/products/fluentadownload.html
- **Support**: Maxprograms Support at [Groups.io](https://groups.io/g/maxprograms/)
- **Source Code**: https://github.com/rmraya/Fluenta

---

*This technical documentation was generated from codebase analysis on 2025-01-16 for Fluenta v5.4.0*
