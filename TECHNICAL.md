# Fluenta DITA Translation Manager - Technical Documentation

## Table of Contents

1. [Project Overview](#project-overview)
2. [Features & Capabilities](#features--capabilities)
3. [Architecture Overview](#architecture-overview)
4. [Java API Documentation](#java-api-documentation)
5. [CLI Interface Documentation](#cli-interface-documentation)
6. [Data Models](#data-models)
7. [Project Management](#project-management)
8. [Translation Memory Management](#translation-memory-management)
9. [XML Catalog Setup](#xml-catalog-setup)
10. [Usage Examples](#usage-examples)
11. [Component Details](#component-details)
12. [Build & Development](#build--development)
13. [Dependencies](#dependencies)
14. [File Structure Reference](#file-structure-reference)

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

**Note**: The current API focuses on XLIFF operations and translation memory management. Project CRUD operations (create, update, delete) are not exposed via the API class but can be performed through direct JSON manipulation or the GUI.

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

## Project Management

### Overview

Projects in Fluenta are primarily managed through the GUI. There are **no dedicated CLI commands or public API methods** for creating, updating, or deleting projects. However, projects can be managed programmatically through:

1. **GUI Interface** (recommended)
2. **Direct JSON file manipulation**
3. **Java `ProjectsManager` class** (for custom Java applications)

### Project File Location

Projects are stored in JSON format at:
- **Windows**: `%APPDATA%\Fluenta\projects\projects.json`
- **Linux/macOS**: `~/.config/Fluenta/projects/projects.json`

### Creating Projects via GUI

The recommended method for creating projects:

1. Launch Fluenta application
2. Click "Add Project" button
3. Fill in project details (title, description, DITA map path, languages)
4. Assign translation memories (optional)
5. Click "Save"

The GUI automatically:
- Generates a unique project ID (timestamp)
- Creates an associated translation memory with the same ID
- Initializes language status tracking
- Saves to `projects.json`

### Managing Projects via JSON Manipulation

For automation or scripting, you can directly edit the `projects.json` file.

#### Creating a Project Manually

Add a new project object to the `projects` array:

```json
{
  "version": 1,
  "projects": [
    {
      "id": 1705405200000,
      "title": "My Translation Project",
      "description": "Technical documentation translation",
      "map": "/absolute/path/to/document.ditamap",
      "creationDate": "2025-01-16 10:00",
      "lastUpdate": "2025-01-16 10:00",
      "status": "0",
      "srcLanguage": "en-US",
      "tgtLanguages": ["de-DE", "fr-FR"],
      "memories": [1705405200000],
      "history": [],
      "languageStatus": {
        "de-DE": "3",
        "fr-FR": "3"
      }
    }
  ]
}
```

**Critical Requirements**:
- `id`: Unique timestamp in milliseconds (e.g., `new Date().getTime()`)
- `map`: **Must be an absolute path** to the DITA map file
- `status`: "0" (NEW), "1" (IN_PROGRESS), or "2" (COMPLETED)
- `languageStatus`: "3" (UNTRANSLATED) or "4" (TRANSLATED)
- `memories`: Array of memory IDs - typically includes the project's own memory
- Date format: `"yyyy-MM-dd HH:mm"`

**Also create a corresponding memory** in `memories.json`:

```json
{
  "version": 1,
  "memories": [
    {
      "id": 1705405200000,
      "name": "My Translation Project",
      "description": "Technical documentation translation",
      "creationDate": "2025-01-16 10:00",
      "lastUpdate": "2025-01-16 10:00",
      "srcLanguage": "en-US"
    }
  ]
}
```

#### Listing All Projects

**Linux/macOS**:
```bash
cat ~/.config/Fluenta/projects/projects.json | jq '.projects[] | {id, title, status, srcLanguage, tgtLanguages}'
```

**Windows PowerShell**:
```powershell
$json = Get-Content "$env:APPDATA\Fluenta\projects\projects.json" | ConvertFrom-Json
$json.projects | Select-Object id, title, status, srcLanguage, tgtLanguages
```

#### Getting Project ID

To find a project ID for CLI operations:

**Linux/macOS**:
```bash
cat ~/.config/Fluenta/projects/projects.json | jq '.projects[] | select(.title=="My Project") | .id'
```

**Windows PowerShell**:
```powershell
$json = Get-Content "$env:APPDATA\Fluenta\projects\projects.json" | ConvertFrom-Json
$json.projects | Where-Object {$_.title -eq "My Project"} | Select-Object -ExpandProperty id
```

#### Updating a Project

1. Locate the project by `id` in the `projects` array
2. Modify the desired fields
3. Update the `lastUpdate` field to current timestamp
4. Save the file

#### Deleting a Project

1. Remove the project object from the `projects` array
2. Optionally remove the associated memory from `memories.json`
3. Optionally clean up skeleton files in `%APPDATA%\Fluenta\projects\<project-id>\`

### Managing Projects via Java (Programmatic)

For custom Java applications, use the `ProjectsManager` class:

```java
import com.maxprograms.fluenta.controllers.ProjectsManager;
import com.maxprograms.fluenta.models.Project;
import com.maxprograms.fluenta.models.ProjectEvent;
import com.maxprograms.utils.Preferences;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.Map;
import java.util.Hashtable;

public class ProjectManagementExample {
    public static void main(String[] args) {
        try {
            // Initialize ProjectsManager
            Preferences prefs = Preferences.getInstance();
            File projectsFolder = prefs.getProjectsFolder();
            ProjectsManager manager = new ProjectsManager(projectsFolder);

            // Create a new project
            long projectId = System.currentTimeMillis();

            List<String> targetLangs = new Vector<>();
            targetLangs.add("de-DE");
            targetLangs.add("fr-FR");

            List<Long> memories = new Vector<>();
            memories.add(1705400000000L); // Existing memory ID

            List<ProjectEvent> history = new Vector<>();

            Map<String, String> langStatus = new Hashtable<>();
            langStatus.put("de-DE", "3"); // Untranslated
            langStatus.put("fr-FR", "3"); // Untranslated

            Project newProject = new Project(
                projectId,
                "API Test Project",
                "Created via Java API",
                "/path/to/documentation.ditamap",
                new Date(),                  // creationDate
                new Date(),                  // lastUpdate
                "en-US",                     // srcLanguage
                targetLangs,                 // tgtLanguages
                memories,                    // memories
                history,                     // history
                langStatus                   // languageStatus
            );

            // Add project to manager
            manager.add(newProject);
            System.out.println("Project created with ID: " + projectId);

            // Retrieve project by ID
            Project retrieved = manager.getProject(projectId);
            System.out.println("Project title: " + retrieved.getTitle());
            System.out.println("Source language: " + retrieved.getSrcLanguage());
            System.out.println("Target languages: " + retrieved.getLanguages());

            // Update project (modify fields as needed)
            retrieved.setLastUpdate(new Date());
            manager.update(retrieved);
            System.out.println("Project updated");

            // Remove project
            manager.remove(projectId);
            System.out.println("Project deleted");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

**Key Classes**:
- **`ProjectsManager`** (`src/com/maxprograms/fluenta/controllers/ProjectsManager.java`)
  - `add(Project project)` - Add new project and save to JSON
  - `getProject(long id)` - Retrieve project by ID
  - `update(Project project)` - Update existing project
  - `remove(long id)` - Delete project by ID
  - `getProjects()` - Get all projects (package-private)

- **`LocalController`** (`src/com/maxprograms/fluenta/controllers/LocalController.java`)
  - `getProject(long id)` - Retrieve project via LocalController
  - `updateProject(Project project)` - Update project with timestamp

### Working with Projects in CLI Workflows

While there are no CLI commands to create projects, you can reference existing projects in CLI operations by their ID:

**Step 1**: Find your project ID (see "Listing All Projects" above)

**Step 2**: Use the ID in XLIFF generation/import operations

```bash
# Generate XLIFF for project ID 1705405200000
java -cp "jars/*" com.maxprograms.fluenta.CLI -generateXLIFF config.json

# Where config.json contains:
# {
#   "id": 1705405200000,
#   "xliffFolder": "/path/to/output",
#   ...
# }
```

---

## Translation Memory Management

### Overview

Translation memories can be managed through:
1. **GUI Interface** (recommended for creation)
2. **CLI Commands** (for TMX import/export)
3. **Java API** (for TMX import/export)
4. **Direct JSON manipulation** (for create/update/delete)
5. **Java `MemoriesManager` class** (for custom Java applications)

### Memory File Location

Memories metadata is stored at:
- **Windows**: `%APPDATA%\Fluenta\memories\memories.json`
- **Linux/macOS**: `~/.config/Fluenta/memories/memories.json`

Translation units are stored in SQLite databases at:
- **Windows**: `%APPDATA%\Fluenta\memories\<memory-id>.db`
- **Linux/macOS**: `~/.config/Fluenta/memories/<memory-id>.db`

### Creating Memories via GUI

1. Launch Fluenta application
2. Switch to "Memories" view
3. Click "Add Memory" button
4. Fill in memory details (name, description, source language)
5. Click "Save"

### Managing Memories via JSON

#### Creating a Memory Manually

Add a new memory object to `memories.json`:

```json
{
  "version": 1,
  "memories": [
    {
      "id": 1705400000000,
      "name": "Technical Terms",
      "description": "General technical terminology",
      "creationDate": "2025-01-16 09:00",
      "lastUpdate": "2025-01-16 09:00",
      "srcLanguage": "en-US"
    }
  ]
}
```

**Requirements**:
- `id`: Unique timestamp in milliseconds
- `srcLanguage`: BCP-47 language code (e.g., "en-US", "de-DE")
- Date format: `"yyyy-MM-dd HH:mm"`

**Note**: Creating the JSON entry doesn't populate the memory with translations. Use TMX import to add translation units.

#### Listing All Memories

**Linux/macOS**:
```bash
cat ~/.config/Fluenta/memories/memories.json | jq '.memories[] | {id, name, srcLanguage}'
```

**Windows PowerShell**:
```powershell
$json = Get-Content "$env:APPDATA\Fluenta\memories\memories.json" | ConvertFrom-Json
$json.memories | Select-Object id, name, srcLanguage
```

#### Getting Memory ID

**Linux/macOS**:
```bash
cat ~/.config/Fluenta/memories/memories.json | jq '.memories[] | select(.name=="Technical Terms") | .id'
```

**Windows PowerShell**:
```powershell
$json = Get-Content "$env:APPDATA\Fluenta\memories\memories.json" | ConvertFrom-Json
$json.memories | Where-Object {$_.name -eq "Technical Terms"} | Select-Object -ExpandProperty id
```

### Populating Memories via CLI (TMX Import)

Once a memory exists, populate it with translation units:

```bash
# Import TMX file into memory
java -cp "jars/*" com.maxprograms.fluenta.CLI -importTmx 1705400000000 -tmx terminology.tmx -verbose
```

**Output**: Returns the number of translation units imported

### Exporting Memories via CLI (TMX Export)

```bash
# Export memory to TMX file
java -cp "jars/*" com.maxprograms.fluenta.CLI -exportTmx 1705400000000 -tmx backup.tmx
```

### Managing Memories via Java (Programmatic)

```java
import com.maxprograms.fluenta.controllers.MemoriesManager;
import com.maxprograms.fluenta.models.Memory;
import com.maxprograms.fluenta.API;
import com.maxprograms.languages.Language;
import com.maxprograms.languages.LanguageUtils;
import com.maxprograms.utils.Preferences;
import java.io.File;
import java.util.Date;

public class MemoryManagementExample {
    public static void main(String[] args) {
        try {
            // Initialize MemoriesManager
            Preferences prefs = Preferences.getInstance();
            File memoriesFolder = prefs.getMemoriesFolder();
            MemoriesManager manager = new MemoriesManager(memoriesFolder);

            // Create a new memory
            long memoryId = System.currentTimeMillis();
            Language srcLang = LanguageUtils.getLanguage("en-US");

            Memory newMemory = new Memory(
                memoryId,
                "Product Terms",
                "Product-specific terminology",
                new Date(),     // creationDate
                new Date(),     // lastUpdate
                srcLang
            );

            // Add memory to manager
            manager.add(newMemory);
            System.out.println("Memory created with ID: " + memoryId);

            // Populate memory with TMX import
            int imported = API.importMemory(memoryId, "product-terms.tmx");
            System.out.println("Imported " + imported + " translation units");

            // Retrieve memory by ID
            Memory retrieved = manager.getMemory(memoryId);
            System.out.println("Memory name: " + retrieved.getName());
            System.out.println("Source language: " + retrieved.getSrcLanguage().getCode());

            // Export memory to TMX
            API.exportMemory(memoryId, "backup.tmx");
            System.out.println("Memory exported to backup.tmx");

            // Update memory metadata
            retrieved.setName("Updated Product Terms");
            retrieved.setLastUpdate(new Date());
            manager.update(retrieved);

            // Remove memory
            manager.remove(memoryId);
            System.out.println("Memory deleted");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

**Key Classes**:
- **`MemoriesManager`** (`src/com/maxprograms/fluenta/controllers/MemoriesManager.java`)
  - `add(Memory memory)` - Add new memory (metadata only)
  - `getMemory(long id)` - Retrieve memory by ID
  - `update(Memory memory)` - Update memory metadata
  - `remove(long id)` - Delete memory and database
  - `getMemories()` - Get all memories (package-private)

- **`API`** (`src/com/maxprograms/fluenta/API.java`)
  - `importMemory(long id, String tmxFile)` - Import TMX into memory
  - `exportMemory(long id, String tmxFile)` - Export memory to TMX

### Assigning Memories to Projects

Memories are referenced in projects via the `memories` array field. When creating or updating a project, include the memory IDs:

```json
{
  "id": 1705405200000,
  "title": "My Project",
  "memories": [1705400000000, 1705400001000],
  ...
}
```

Multiple memories can be assigned to a single project. During XLIFF generation, Fluenta will search all assigned memories for translation matches.

---

## XML Catalog Setup

### Overview

Fluenta uses **OASIS XML Catalogs** for entity and URI resolution when parsing DITA maps and XML content (e.g. DTDs, XSDs, entities). The catalog ensures that public IDs and system IDs in your DITA and other XML files resolve to local schema files instead of remote URLs, enabling offline processing and consistent validation.

### Catalog Locations

| Location | Purpose |
|----------|---------|
| **Application catalog** | `catalog/` in the Fluenta installation directory. Contains the built-in master catalog (`catalog.xml`) and subcatalogs for DITA, XLIFF, DocBook, TMX, SRX, and other formats. |
| **User catalog** | User-specific catalog used at runtime. Path: `%APPDATA%\Fluenta\catalog\catalog.xml` (Windows) or `~/.config/Fluenta/catalog/catalog.xml` (Linux/macOS). |

### Initial Setup (First Run)

1. **GUI (Electron)**: On first launch, Fluenta copies the entire `catalog/` folder from the application directory to the user data folder. The user catalog is then a full copy of the application catalog.
2. **Java/CLI only**: If the user catalog file does not exist, `LocalController` creates it automatically. The created file is a minimal OASIS catalog containing a single `<nextCatalog>` entry that points to the application’s `catalog.xml` (resolved from `user.dir`). No copy of the full catalog folder is made; the Java process must be run with a working directory that contains the Fluenta `catalog/` folder.

### Adding Custom Catalogs (GUI)

To add project-specific or custom XML catalogs (e.g. organizational DITA extensions or proprietary schemas):

1. Open **Settings** (gear icon or menu).
2. Switch to the **XML Options** tab.
3. In the **XML Catalog** section, click **Add Catalog Entry**.
4. In the file dialog, select a `catalog.xml` file (or another OASIS catalog file). Use an absolute path so the entry remains valid.
5. The selected catalog is added as a `<nextCatalog>` in the user `catalog.xml`. Resolution will consult this catalog when the main catalog does not resolve an entity or URI.
6. Click **Save Settings** to persist.

To remove an entry: select the checkbox next to the catalog path in the list, then click **Remove Catalog Entry**.

### How the User Catalog Is Used

- The path to the **user** catalog is obtained via `Preferences.getCatalogFile()` and passed to the backend (e.g. OpenXLIFF/Swordfish) as the `catalog` parameter for XLIFF generation and import.
- The resolver loads the user `catalog.xml` first. Any `<nextCatalog>` entries (including the one pointing to the app catalog, and any you add) are followed in order to resolve public IDs, system IDs, and URIs.

### Catalog File Format

The user catalog is OASIS XML Catalog 1.1 format. Example with one delegated catalog:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<catalog xmlns="urn:oasis:names:tc:entity:xmlns:xml:catalog">
  <nextCatalog catalog="C:/path/to/fluenta/catalog/catalog.xml" />
  <nextCatalog catalog="D:/my-project/schemas/catalog.xml" />
</catalog>
```

- **`<nextCatalog catalog="..."/>`**: Delegates to another catalog file. The `catalog` attribute must be an absolute path (or a path that your environment resolves correctly).

### Manual Editing (Advanced)

You can edit the user catalog file directly to add or remove `<nextCatalog>` entries or other OASIS catalog elements. File to edit:

- **Windows**: `%APPDATA%\Fluenta\catalog\catalog.xml`
- **Linux/macOS**: `~/.config/Fluenta/catalog/catalog.xml`

After editing, restart Fluenta or ensure no cached catalog state is used. The GUI **XML Options** tab only manages `<nextCatalog>` entries; other entry types (e.g. `<public>`, `<system>`, `<uri>`) must be added by editing the file or the application catalog in the installation directory.

### Troubleshooting

- **"Catalog not found" or resolution errors**: Ensure the user catalog file exists and that any `<nextCatalog>` paths are absolute and point to existing files. When running from CLI, run with the Fluenta project directory as working directory so the default app catalog path is valid.
- **Missing DITA or schema resolution**: Confirm the application catalog (or your custom catalog) contains the right `<nextCatalog>` or `<system>`/`<public>`/`<uri>` entries for the DITA version and schemas you use. The bundled `catalog/catalog.xml` already includes DITA 1.2/1.3, XLIFF, DocBook, and other formats.

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

- **XML Catalogs**: OASIS catalog format for entity resolution (see [XML Catalog Setup](#xml-catalog-setup))
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
