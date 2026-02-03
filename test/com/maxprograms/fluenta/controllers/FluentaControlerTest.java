package com.maxprograms.fluenta.controllers;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.maxprograms.fluenta.models.Project;
import com.maxprograms.languages.Language;
import com.maxprograms.utils.SimpleLogger;
import com.maxprograms.utils.TestUtils;

/**
 * Tests the basic Fluenta workflow for a single DITA map with shared topics and one image.
 * <p>
 * Scenario: A DITA map ({@code sample.ditamap}) references the same topics (e.g. topic1.dita,
 * topic2.dita) and one image ({@code images/sample.png}). The workflow is: create project from
 * the map, generate XLIFF for a target language, translate the XLIFF, then import the translated
 * XLIFF back to DITA. The image is copied to the output folder during import so that the
 * translated documentation is complete.
 * </p>
 * <p>
 * This test verifies the end-to-end flow: DITA → XLIFF export → translation → XLIFF import → DITA
 * output, including that the image is present in the import output.
 * </p>
 */
public class FluentaControlerTest {

  private String fluentaHomeFolderString = "test-files/dita-sample-project/fluenta-home";
  private String ditaFolderString = "test-files/dita-sample-project/dita";
  private String xliffFolderString = "test-files/dita-sample-project/xliff";
  private String xliffTranslatedFolderString = "test-files/dita-sample-project/xliff-translated";
  private String importFolderString = "test-files/dita-sample-project/dita-import";

  private File fluentaHomeFolder;
  private File xliffFolder;
  private File importFolder;
  private File xliffTranslatedFolder;
  
  private LocalController controller;
  private Project project;
  private File ditaMap;

  private long projectId = 1;

	@Before
	public void setUp() throws Exception {
    fluentaHomeFolder = new File(fluentaHomeFolderString);
		File projectsFolder = new File(fluentaHomeFolder, "projects");
    File memoriesFolder = new File(fluentaHomeFolder, "memories");
    xliffFolder = new File(xliffFolderString);
    importFolder = new File(importFolderString);
    xliffTranslatedFolder = new File(xliffTranslatedFolderString);

    // Delete fluenta home folder and xliff folder
    if (fluentaHomeFolder != null && fluentaHomeFolder.exists()) {
      TestUtils.deleteRecursively(fluentaHomeFolder.toPath());
    }
    if (xliffFolder != null && xliffFolder.exists()) {
      TestUtils.deleteRecursively(xliffFolder.toPath());
    }
    if (importFolder != null && importFolder.exists()) {
      TestUtils.deleteRecursively(importFolder.toPath());
    }
    importFolder.mkdirs();
    if (xliffTranslatedFolder != null && xliffTranslatedFolder.exists()) {
      TestUtils.deleteRecursively(xliffTranslatedFolder.toPath());
    }
    xliffTranslatedFolder.mkdirs();
    
    ditaMap = new File(ditaFolderString, "sample.ditamap");
    TestUtils.initPreferences(projectsFolder, memoriesFolder);

    controller = new LocalController();
    project = TestUtils.getOrCreateProjectForDitaMap(projectId, ditaMap, controller, Arrays.asList("de-DE"));
	}

  /**
   * Basic workflow: DITA map with same topics and one image; image is copied to the output.
   * <p>
   * Test plan:
   * </p>
   * <ol>
   *   <li><b>Setup:</b> Create project from DITA map (sample.ditamap) with target language de-DE.</li>
   *   <li><b>Export:</b> Generate XLIFF from project via generateXliff() into xliff folder.</li>
   *   <li><b>Translate:</b> Simulate translation via TestUtils.generateTranslatedXliff() from source
   *       XLIFF to translated XLIFF (translated_sample_de-DE_ditamap.xlf).</li>
   *   <li><b>Import:</b> Call importXliff() to convert translated XLIFF back to DITA in import folder;
   *       non-translatable resources (e.g. the image) are copied to the output.</li>
   *   <li><b>Verify:</b> Assert import folder contains sample.ditamap, topic1.dita, topic2.dita,
   *       and images/sample.png (image copied to output).</li>
   * </ol>
   *
   * @throws Exception if setup, XLIFF generation, translation, or import fails
   */
  @Test
  public void testImportXLIFF() throws Exception {
    // Generate first XLIFF to translate in German
    controller.generateXliff(
      project, 
      xliffFolder.getAbsolutePath(), 
      Arrays.asList(new Language("de-DE", "Deutsch")), 
      false, 
      false, 
      false, 
      null, 
      "1.2", 
      true, 
      false, 
      false, 
      false,
      true,
      new SimpleLogger(false));

    
    // Translate in German
    Path translatedXliff = new File(xliffTranslatedFolder, "translated_sample_de-DE_ditamap.xlf").toPath();
    TestUtils.generateTranslatedXliff(
      new File(xliffFolder, "sample_de-DE.ditamap.xlf").toPath(), 
      translatedXliff);

    
     SimpleLogger logger = new SimpleLogger(true);
    importFolder.mkdirs();

    logger.log("Importing XLIFF file...");
    
    // Import translated XLIFF to generate documentation
    controller.importXliff(
      project, 
      translatedXliff.toString(), 
      importFolder.getAbsolutePath(), 
      true,
      true, 
      false, logger);

    // Assert import folder contains DITA map, DITA topics and image
    File importedMap = new File(importFolder, "sample.ditamap");
    Assert.assertTrue("DITA map should be present in import folder", importedMap.exists());

    File importedTopic1 = new File(importFolder, "topic1.dita");
    File importedTopic2 = new File(importFolder, "topic2.dita");
    Assert.assertTrue("DITA topic1.dita should be present in import folder", importedTopic1.exists());
    Assert.assertTrue("DITA topic2.dita should be present in import folder", importedTopic2.exists());

    File importedImage = new File(importFolder, "images/sample.png");
    Assert.assertTrue("Image should be present in import folder", importedImage.exists());
  }

}
