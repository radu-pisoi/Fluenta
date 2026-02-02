package com.maxprograms.fluenta.controllers;

import java.io.File;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.maxprograms.fluenta.models.Project;
import com.maxprograms.languages.Language;
import com.maxprograms.utils.SimpleLogger;
import com.maxprograms.utils.TestUtils;

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
    // if (xliffTranslatedFolder != null && xliffTranslatedFolder.exists()) {
    //   TestUtils.deleteRecursively(xliffTranslatedFolder.toPath());
    // }
    
    ditaMap = new File(ditaFolderString, "sample.ditamap");
    TestUtils.initPreferences(projectsFolder, memoriesFolder);

    controller = new LocalController();
    project = TestUtils.getOrCreateProjectForDitaMap(projectId, ditaMap, controller, Arrays.asList("de-DE"));
	}

  @Test
  public void testImportXLIFF() throws Exception {
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

    
     SimpleLogger logger = new SimpleLogger(true);
    importFolder.mkdirs();

    logger.log("Importing XLIFF file...");
    
    controller.importXliff(
      project, 
      new File(xliffTranslatedFolder, "translated_sample_de-DE_ditamap.xlf").getAbsolutePath(), 
      importFolder.getAbsolutePath(), 
      false, 
      true, 
      false, logger);
  }

@After
public void tearDown() throws Exception {
  

}


}
