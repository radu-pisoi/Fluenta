package com.maxprograms.fluenta.controllers;

import java.io.File;
import java.nio.file.Files;
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
 * Tests XLIFF generation and import for a single DITA map with multiple publications
 * (DITA profiling / conditional text).
 *
 * <h2>DITA profiling in this test</h2>
 * <ul>
 *   <li><b>One map, multiple outputs:</b> The same DITA map references three topics
 *       with {@code product} attributes. Two DITAVAL files define two "publications":
 *       <b>pub1</b> (product=pub1 only) and <b>pub2</b> (product=pub2 only).</li>
 *   <li><b>Topic coverage:</b>
 *       <ul>
 *         <li>topic1.dita: product="pub1" → included only in publication 1</li>
 *         <li>topic2.dita: product="pub1 pub2" → included in both publications</li>
 *         <li>topic3.dita: product="pub2" (on map topicref) → included only in publication 2</li>
 *       </ul>
 *   </li>
 *   <li><b>DITAVAL files:</b>
 *       <ul>
 *         <li>publication1.ditaval: include product=pub1, exclude product=pub2</li>
 *         <li>publication2.ditaval: include product=pub2, exclude product=pub1</li>
 *       </ul>
 *   </li>
 * </ul>
 * The test verifies that generating XLIFF per publication, translating, and importing
 * both XLIFFs into one folder preserves the map structure and all three topics with
 * correct profiling attributes.
 */
public class ProfilingOneMapTest {

  private String fluentaHomeFolderString = "test-files/profiling-one-map/fluenta-home";
  private String ditaFolderString = "test-files/profiling-one-map/dita";
  private String xliffFolderString = "test-files/profiling-one-map/xliff";
  private String xliffTranslatedFolderString = "test-files/profiling-one-map/xliff-translated";
  private String importFolderString = "test-files/profiling-one-map/dita-import";

  private File fluentaHomeFolder;
  private File xliffFolder;
  private File importFolder;
  private File xliffTranslatedFolder;

  private LocalController controller;
  private Project project;
  private File ditaMap;

  private long projectId = 1234567890;

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

    ditaMap = new File(ditaFolderString, "test_profile.ditamap");
    TestUtils.initPreferences(projectsFolder, memoriesFolder);

    controller = new LocalController();
    project = TestUtils.getOrCreateProjectForDitaMap(projectId, ditaMap, controller, Arrays.asList("de-DE"));
  }

  /**
   * Test plan: DITA profiling — generate XLIFF for two publications, translate, import both, verify merged output.
   * <ol>
   *   <li><b>Setup:</b> Clean fluenta-home, xliff, dita-import, xliff-translated; create project from DITA map with target de-DE.</li>
   *   <li><b>Generate XLIFF (pub1):</b> Call generateXliff with publication1.ditaval → output under xliff/pub1.</li>
   *   <li><b>Generate XLIFF (pub2):</b> Call generateXliff with publication2.ditaval → output under xliff/pub2.</li>
   *   <li><b>Translate pub1:</b> Use TestUtils to create translated XLIFF from pub1 source XLIFF.</li>
   *   <li><b>Translate pub2:</b> Use TestUtils to create translated XLIFF from pub2 source XLIFF.</li>
   *   <li><b>Import pub1:</b> Import translated pub1 XLIFF into dita-import (creates/overwrites map and topics).</li>
   *   <li><b>Import pub2:</b> Import translated pub2 XLIFF into same dita-import (merge second publication).</li>
   *   <li><b>Assert map:</b> test_profile.ditamap exists and contains topicrefs with product="pub1", "pub1, pub2", "pub2".</li>
   *   <li><b>Assert topics:</b> topic1.dita, topic2.dita, topic3.dita all exist in dita-import.</li>
   * </ol>
   */
  @Test
  public void testImportXLIFF() throws Exception {
    File prod1Ditaval = new File(ditaFolderString, "publication1.ditaval");
    // Generate XLIFF for publication 1 (German, product pub1)
    File prod1Xliff = new File(xliffFolder, "pub1");
    prod1Xliff.mkdirs();    
    controller.generateXliff(
      project,
      prod1Xliff.getAbsolutePath(),
      Arrays.asList(new Language("de-DE", "Deutsch")),
        true,
        true,
        false,
        prod1Ditaval.getAbsolutePath(),
        "1.2",
        true,
        false,
        false,
        false,
        true,
        new SimpleLogger(false));

    File prod2Ditaval = new File(ditaFolderString, "publication2.ditaval");
    // Generate XLIFF for publication 2 (German, product pub2)
    File prod2Xliff = new File(xliffFolder, "pub2");
    prod2Xliff.mkdirs();

    controller.generateXliff(
        project,
        prod2Xliff.getAbsolutePath(),
        Arrays.asList(new Language("de-DE", "Deutsch")),
        true,
        true,
        false,
        prod2Ditaval.getAbsolutePath(),
        "1.2",
        true,
        false,
        false,
        false,
        true,
        new SimpleLogger(false));

    // Translate in German for product 1
    Path translatedXliffProd1 = new File(prod1Xliff, "translated_test_profile_de-DE.ditamap.xlf").toPath();
    TestUtils.generateTranslatedXliff(
        new File(prod1Xliff, "test_profile_de-DE.ditamap.xlf").toPath(),
        translatedXliffProd1);

    // Translate in German for product 2
    Path translatedXliffProd2 = new File(prod2Xliff, "translated_test_profile_de-DE.ditamap.xlf").toPath();
    TestUtils.generateTranslatedXliff(
        new File(prod2Xliff, "test_profile_de-DE.ditamap.xlf").toPath(),
        translatedXliffProd2);

    SimpleLogger logger = new SimpleLogger(true);
    importFolder.mkdirs();

    logger.log("Importing XLIFF file...");

    // Import translated XLIFF to generate documentation for product 1
    controller.importXliff(
        project,
        translatedXliffProd1.toString(),
        importFolder.getAbsolutePath(),
        true,
        true,
        false, logger);

    // Import translated XLIFF to generate documentation for product 2
    controller.importXliff(
        project,
        translatedXliffProd2.toString(),
        importFolder.getAbsolutePath(),
        true,
        true,
        false, logger);

    // Assert import folder contains DITA map, DITA topics and image
    File importedMap = new File(importFolder, "test_profile.ditamap");
    Assert.assertTrue("DITA map should be present in import folder", importedMap.exists());
    String ditaMapContent = Files.readString(importedMap.toPath());
    Assert.assertTrue("DITA map content should contain product 1", ditaMapContent.contains("href=\"topic1.dita\" product=\"pub1\""));
    Assert.assertTrue("DITA map content should contain product 2", ditaMapContent.contains("href=\"topic2.dita\" product=\"pub1, pub2\""));
    Assert.assertTrue("DITA map content should contain product 3", ditaMapContent.contains("href=\"topic3.dita\" product=\"pub2\""));

    File importedTopic1 = new File(importFolder, "topic1.dita");
    File importedTopic2 = new File(importFolder, "topic2.dita");
    File importedTopic3 = new File(importFolder, "topic3.dita");
    Assert.assertTrue("DITA topic1.dita should be present in import folder", importedTopic1.exists());
    Assert.assertTrue("DITA topic2.dita should be present in import folder", importedTopic2.exists());
    Assert.assertTrue("DITA topic3.dita should be present in import folder", importedTopic3.exists());

  }

}
