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

public class ProfilingOneMapLevelParaTest {

  private String fluentaHomeFolderString = "test-files/profiling-one-map-para/fluenta-home";
  private String ditaFolderString = "test-files/profiling-one-map-para/dita";
  private String xliffFolderString = "test-files/profiling-one-map-para/xliff";
  private String xliffTranslatedFolderString = "test-files/profiling-one-map-para/xliff-translated";
  private String importFolderString = "test-files/profiling-one-map-para/dita-import";

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
