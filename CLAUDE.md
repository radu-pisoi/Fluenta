# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Fluenta is a DITA Translation Manager that combines DITA and XLIFF standards to simplify translation and localization workflows. It uses a hybrid architecture:

- **Backend**: Java 21 with modular architecture (`module-info.java`)
- **Frontend**: Electron 36.3.2 + TypeScript 5.8.3
- **Build**: Apache Ant for Java, npm/tsc for TypeScript
- **Data**: JSON files for projects/memories metadata, SQLite for translation units

## Build & Test Commands

### Building

```bash
# Build Java code (creates jars/fluenta.jar)
ant

# Build TypeScript (compiles ts/ to js/)
npm run build

# Build and run GUI application
npm start

# Clean build artifacts
ant clean
```

### Testing

```bash
# Run all JUnit 4 tests
ant run-tests

# Compile tests only
ant compile-tests

# Run single test class
java -cp "jars/*:testclasses:out" org.junit.runner.JUnitCore com.maxprograms.utils.TextUtilsTest
```

Test files are in `test/` directory. Compiled test classes go to `testclasses/`. Test reports are generated in `out-test/`.

### Development Workflow

When making changes:
1. Modify Java sources in `src/` or TypeScript in `ts/`
2. Run `ant` to rebuild Java code
3. Run `npm run build` for TypeScript changes
4. Run `ant run-tests` to verify tests pass
5. Test GUI with `npm start`

## Architecture

### Java Backend (src/com/maxprograms/)

**Core API Layer**:
- `fluenta/API.java` - Public static API for XLIFF generation/import and TMX operations
- `fluenta/CLI.java` - Command-line interface for headless operations

**Controllers** (business logic):
- `fluenta/controllers/LocalController.java` - Core XLIFF/TM operations, catalog management
- `fluenta/controllers/ProjectsManager.java` - Project persistence and CRUD (uses `projects.json`)
- `fluenta/controllers/MemoriesManager.java` - Memory persistence and CRUD (uses `memories.json`)
- `fluenta/controllers/TagErrorsReport.java` - Translation tag validation

**Models**:
- `fluenta/models/Project.java` - Project with metadata, history, language status
- `fluenta/models/Memory.java` - Translation memory metadata
- `fluenta/models/ProjectEvent.java` - History tracking (XLIFF_CREATED, XLIFF_IMPORTED)

**Utilities**:
- `utils/Preferences.java` - App-wide configuration, file paths
- `utils/FileUtils.java` - File I/O, JSON operations
- `utils/SimpleLogger.java` - Logging
- `utils/TextUtils.java` - Text manipulation utilities

### TypeScript Frontend (ts/)

**Main Application**:
- `fluenta.ts` - Electron main process, IPC handlers, Java process management (~118KB)
- `main.ts` - Entry point, view switching (projects/memories)

**Views**:
- `projectsView.ts` - Projects list UI
- `memoriesView.ts` - Memories list UI

**Key Dialogs**:
- `addProjectDialog.ts` / `editProjectDialog.ts` - Project CRUD
- `generateXliffDialog.ts` / `importXliffDialog.ts` - XLIFF workflows
- `addMemoryDialog.ts` / `editMemoryDialog.ts` - Memory CRUD
- `ProjectMemoriesDialog.ts` - Assign memories to projects
- `settingsDialog.ts` - App settings, XML catalog configuration
- `filterConfig.ts` - XML filter configuration

**Utilities**:
- `languages.ts` - BCP-47 language handling
- `i18n.ts` - Internationalization
- `preferences.ts` - Settings management

### Data Flow

1. **GUI → Java IPC**: Electron frontend sends IPC messages to Java backend via stdin/stdout
2. **Java Processing**: Controllers use models and libraries (OpenXLIFF, Swordfish) to process DITA/XLIFF
3. **Persistence**: Projects/memories stored as JSON; translation units in SQLite databases
4. **CLI Mode**: Direct Java API calls without Electron, using JSON config files

### Module System

The project uses Java modules (JPMS). See `src/module-info.java`:
- Exports: `com.maxprograms.fluenta`, `controllers`, `models`
- Dependencies: `openxliff`, `swordfish`, `xmljava`, `mapdb`, `jsoup`, `json`, `sqlite-jdbc`

## Key Concepts

### Projects

Projects link a DITA map to target languages and translation memories. Stored in:
- **Metadata**: `%APPDATA%\Fluenta\projects\projects.json` (Windows) or `~/.config/Fluenta/projects/projects.json` (Unix)
- **Skeletons**: `%APPDATA%\Fluenta\projects/<project-id>/` per build

**Project Status**:
- `"0"` = NEW (no XLIFF generated)
- `"1"` = IN_PROGRESS (XLIFF generated, not imported)
- `"2"` = COMPLETED (all XLIFF imported)

**Language Status**: `"3"` = UNTRANSLATED, `"4"` = TRANSLATED

Projects can only be created via GUI or direct JSON manipulation (no CLI/API for project CRUD).

### Translation Memories

Memories store translation units for reuse. Each memory has:
- **Metadata**: `memories.json` (name, description, source language)
- **Translation units**: SQLite database `<memory-id>.db` managed by Swordfish library

Multiple memories can be assigned to a project. During XLIFF generation, all assigned memories are searched for matches.

### XLIFF Workflow

1. **Generate XLIFF**: Convert DITA map → XLIFF files (one per target language)
   - Uses `LocalController.generateXLIFF()`
   - Leverages assigned TMs for pre-translation
   - Creates skeleton files for round-trip conversion
   - Supports XLIFF 1.2, 2.0, 2.1, 2.2
   - Paragraph or sentence-level segmentation

2. **Translate**: External CAT tools edit XLIFF files

3. **Import XLIFF**: Merge translated XLIFF back into DITA
   - Uses `LocalController.importXLIFF()`
   - Updates project TM with new translations
   - Validates tag integrity
   - Outputs translated DITA files

### XML Catalogs

Fluenta uses OASIS XML Catalogs for entity/URI resolution:
- **App catalog**: `catalog/catalog.xml` (bundled DITA 1.2/1.3, XLIFF, DocBook, etc.)
- **User catalog**: `%APPDATA%\Fluenta\catalog\catalog.xml` (delegates to app catalog + custom catalogs)

Add custom catalogs via GUI Settings → XML Options or edit user catalog directly with `<nextCatalog>` entries.

## Common Tasks

### Running CLI Operations

CLI requires a project/memory ID. Get IDs from JSON files or GUI.

**Generate XLIFF**:
```bash
java -cp "jars/*" com.maxprograms.fluenta.CLI -generateXLIFF config.json -verbose
```

Config file format:
```json
{
  "id": 1705405200000,
  "xliffFolder": "/path/to/output",
  "tgtLang": ["de-DE", "fr-FR"],
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

**Import XLIFF**:
```bash
java -cp "jars/*" com.maxprograms.fluenta.CLI -importXLIFF config.json -verbose
```

Config file format:
```json
{
  "id": 1705405200000,
  "xliffFile": "/path/to/translated.xlf",
  "outputFolder": "/path/to/output",
  "updateTM": true,
  "acceptUnapproved": false,
  "ignoreTagErrors": false
}
```

**TMX Import/Export**:
```bash
java -cp "jars/*" com.maxprograms.fluenta.CLI -importTmx <memoryId> -tmx file.tmx -verbose
java -cp "jars/*" com.maxprograms.fluenta.CLI -exportTmx <memoryId> -tmx backup.tmx
```

### Using Java API Programmatically

```java
import com.maxprograms.fluenta.API;

// Import TMX to memory
int count = API.importMemory(1705400000000L, "terms.tmx");

// Export memory to TMX
API.exportMemory(1705400000000L, "backup.tmx");

// Generate/import XLIFF (use config files)
API.generateXLIFF("generate-config.json", true);
API.importXLIFF("import-config.json", true);
```

Note: API methods for project/memory CRUD are not exposed. Use `ProjectsManager` / `MemoriesManager` directly or manipulate JSON files.

### Adding Tests

1. Create test class in `test/com/maxprograms/...` matching source structure
2. Use JUnit 4 annotations (`@Test`, `@Before`, `@After`)
3. Place test resources in `test-files/`
4. Run with `ant run-tests`

Example test structure:
```java
package com.maxprograms.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class MyUtilTest {
    @Test
    public void testSomething() {
        assertEquals(expected, actual);
    }
}
```

## File Locations

### Source Code
- Java: `src/com/maxprograms/`
- TypeScript: `ts/`
- Tests: `test/com/maxprograms/`

### Build Output
- Compiled Java: `out/` → `jars/fluenta.jar`
- Compiled TypeScript: `js/`
- Test classes: `testclasses/`
- Test reports: `out-test/`

### Resources
- HTML templates: `html/en/`, `html/es/`
- CSS themes: `css/`
- i18n: `i18n/fluenta_en.json`, `i18n/fluenta_es.json`
- Icons: `icons/`
- XML catalogs: `catalog/`
- SRX segmentation rules: `srx/`
- XML filters: `xmlfilter/`

### User Data (Runtime)
- Windows: `%APPDATA%\Fluenta\`
- Unix: `~/.config/Fluenta/`

Contains:
- `projects/projects.json` - Project metadata
- `projects/<id>/` - Per-project skeletons
- `memories/memories.json` - Memory metadata
- `memories/<id>.db` - SQLite TM databases
- `catalog/catalog.xml` - User XML catalog
- `preferences.json` - User preferences

## Dependencies

### Java Libraries (jars/)
- `openxliff.jar` - XLIFF conversion engine
- `swordfish.jar` - Translation memory engine
- `sqlite-jdbc-3.49.1.0.jar` - SQLite driver
- `mapdb.jar` - Embedded data structures
- `jsoup.jar` - HTML/XML parsing
- `json.jar` - JSON processing
- `bcp47j.jar` - Language tags
- `xmljava.jar` - XML utilities
- `junit-4.13.2.jar`, `hamcrest-2.2.jar` - Testing

### npm Dependencies
- **Production**: `typesbcp47`, `typesxml`
- **Dev**: `electron`, `typescript`

## Code Patterns

### JSON Persistence
Projects and memories use JSON files managed by their respective managers. Format:
```json
{
  "version": 1,
  "projects": [...] // or "memories": [...]
}
```

Each project/memory has:
- `id` - Timestamp in milliseconds (unique identifier)
- `creationDate`, `lastUpdate` - Format: `"yyyy-MM-dd HH:mm"`

### Thread Safety
- `ProjectsManager` and `MemoriesManager` use synchronized methods
- CLI operations use lock files to prevent concurrent access

### Error Handling
- Java exceptions propagate with descriptive messages
- GUI displays errors in dialogs
- CLI returns exit code 0 (success), 1 (lock error), 3 (general error)

### Internationalization
- Messages defined in `i18n/fluenta_en.json`, `i18n/fluenta_es.json`
- Java uses `Messages.java` hierarchy
- TypeScript uses `i18n.ts`

## Platform Considerations

- Uses `os.family` detection in `build.xml` for platform-specific builds
- File paths: Always use absolute paths for DITA maps in project configuration
- Lock files: Windows uses different path separators (CLI handles this)
- Line endings: Git configured with `.gitattributes`

## External Documentation

- TECHNICAL.md - Comprehensive technical documentation (1352 lines)
- JUNIT_SETUP.md - Test setup details
- README.md - Build instructions, licensing
- PDF manuals: `fluenta_en.pdf`, `fluenta_es.pdf`
