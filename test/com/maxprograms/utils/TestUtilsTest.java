/*******************************************************************************
 * Copyright (c) 2015-2025 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/

package com.maxprograms.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.maxprograms.fluenta.controllers.LocalController;
import com.maxprograms.fluenta.models.Project;

public class TestUtilsTest {

	@Before
	public void setUp() {
		Preferences.resetInstance();
	}

	@After
	public void tearDown() {
		Preferences.resetInstance();
	}

	@Test
	public void testInitPreferences_returnsPreferencesWithGivenFolders() throws IOException {
		Path base = Files.createTempDirectory("fluenta-test-init-");
		File projectsFolder = base.resolve("projects").toFile();
		File memoriesFolder = base.resolve("memories").toFile();

		Preferences prefs = TestUtils.initPreferences(projectsFolder, memoriesFolder);

		assertNotNull(prefs);
		assertEquals(projectsFolder.getAbsolutePath(), prefs.getProjectsFolder().getAbsolutePath());
		assertEquals(memoriesFolder.getAbsolutePath(), prefs.getMemoriesFolder().getAbsolutePath());
	}

	@Test
	public void testInitPreferences_createsDirectoriesIfNotExist() throws IOException {
		Path base = Files.createTempDirectory("fluenta-test-create-");
		File projectsFolder = base.resolve("projects").toFile();
		File memoriesFolder = base.resolve("memories").toFile();

		assertTrue("projects folder should not exist before init", !projectsFolder.exists());
		assertTrue("memories folder should not exist before init", !memoriesFolder.exists());
		TestUtils.initPreferences(projectsFolder, memoriesFolder);

		assertTrue("projects folder should exist", projectsFolder.exists());
		assertTrue("projects folder should be directory", projectsFolder.isDirectory());
		assertTrue("memories folder should exist", memoriesFolder.exists());
		assertTrue("memories folder should be directory", memoriesFolder.isDirectory());
	}

	@Test
	public void testInitPreferences_subsequentGetInstanceReturnsSamePaths() throws IOException {
		Path base = Files.createTempDirectory("fluenta-test-same-");
		File projectsFolder = base.resolve("projects").toFile();
		File memoriesFolder = base.resolve("memories").toFile();
		Preferences prefs1 = TestUtils.initPreferences(projectsFolder, memoriesFolder);
		Preferences prefs2 = Preferences.getInstance();

		assertEquals(prefs1.getPreferencesFolder().getAbsolutePath(),
				prefs2.getPreferencesFolder().getAbsolutePath());
		assertEquals(prefs1.getProjectsFolder().getAbsolutePath(), prefs2.getProjectsFolder().getAbsolutePath());
		assertEquals(prefs1.getMemoriesFolder().getAbsolutePath(), prefs2.getMemoriesFolder().getAbsolutePath());
	}

	@Test
	public void testInitPreferences_resetWithDifferentDirs() throws IOException {
		Path base1 = Files.createTempDirectory("fluenta-test-a-");
		File projects1 = base1.resolve("projects").toFile();
		File memories1 = base1.resolve("memories").toFile();
		TestUtils.initPreferences(projects1, memories1);
		assertEquals(projects1.getAbsolutePath(), Preferences.getInstance().getProjectsFolder().getAbsolutePath());
		assertEquals(memories1.getAbsolutePath(), Preferences.getInstance().getMemoriesFolder().getAbsolutePath());

		Path base2 = Files.createTempDirectory("fluenta-test-b-");
		File projects2 = base2.resolve("projects").toFile();
		File memories2 = base2.resolve("memories").toFile();
		TestUtils.initPreferences(projects2, memories2);

		assertEquals(projects2.getAbsolutePath(), Preferences.getInstance().getProjectsFolder().getAbsolutePath());
		assertEquals(memories2.getAbsolutePath(), Preferences.getInstance().getMemoriesFolder().getAbsolutePath());
	}

	@Test
	public void testGetOrCreateProjectForDitaMap_createsProjectWhenNotExist() throws Exception {
		Path base = Files.createTempDirectory("fluenta-test-dita-");
		File projectsFolder = base.resolve("projects").toFile();
		File memoriesFolder = base.resolve("memories").toFile();
		TestUtils.initPreferences(projectsFolder, memoriesFolder);

		File ditaMap = new File("test-files/dita-sample-project/dita/sample.ditamap");
		if (!ditaMap.exists()) {
			ditaMap = new File("dita-sample-project/dita/sample.ditamap");
		}
		assertTrue("Sample DITA map must exist for test", ditaMap.exists());

		Project project = TestUtils.getOrCreateProjectForDitaMap(1, ditaMap, new LocalController(), Arrays.asList("de-DE"));

		assertNotNull(project);
		assertTrue("Project id should be positive", project.getId() > 0);
		assertEquals(ditaMap.getAbsolutePath(), project.getMap());
		assertNotNull(project.getTitle());
		assertTrue(project.getTitle().contains("sample") || "sample".equals(project.getTitle()));
	}

	@Test
	public void testGetOrCreateProjectForDitaMap_returnsSameProjectWhenCalledAgain() throws Exception {
		Path base = Files.createTempDirectory("fluenta-test-dita-same-");
		File projectsFolder = base.resolve("projects").toFile();
		File memoriesFolder = base.resolve("memories").toFile();
		TestUtils.initPreferences(projectsFolder, memoriesFolder);

		File ditaMap = new File("test-files/dita-sample-project/dita/sample.ditamap");
		if (!ditaMap.exists()) {
			ditaMap = new File("dita-sample-project/dita/sample.ditamap");
		}
		assertTrue("Sample DITA map must exist for test", ditaMap.exists());

    LocalController controller = new LocalController();
		Project project1 = TestUtils.getOrCreateProjectForDitaMap(1, ditaMap, controller, Arrays.asList("de-DE"));
		Project project2 = TestUtils.getOrCreateProjectForDitaMap(2, ditaMap, controller, Arrays.asList("de-DE"));

		assertNotNull(project1);
		assertNotNull(project2);
		assertEquals(project1.getId(), project2.getId());
		assertEquals(project1.getMap(), project2.getMap());
	}

	@Test
	public void testDeleteRecursively_removesDirectoryTree() throws Exception {
		Path base = Files.createTempDirectory("fluenta-test-delete-");
		Path nestedDir = base.resolve("nested");
		Path nestedFile = nestedDir.resolve("file.txt");
		Files.createDirectories(nestedDir);
		Files.write(nestedFile, "test".getBytes(StandardCharsets.UTF_8));

		assertTrue("nested file should exist before delete", Files.exists(nestedFile));
		TestUtils.deleteRecursively(base);

		assertFalse("base folder should be deleted", Files.exists(base));
	}

	@Test
	public void testGenerateTranslatedXliff_createsTargetElements() throws Exception {
		File inputXliff = new File("test-files/dita-sample-project/xliff/sample_de-DE.ditamap.xlf");
		if (!inputXliff.exists()) {
			inputXliff = new File("dita-sample-project/xliff/sample_de-DE.ditamap.xlf");
		}
		assertTrue("Input XLIFF must exist for test", inputXliff.exists());

		Path outputPath = Files.createTempFile("fluenta-test-translated-", ".xlf");
		try {
			TestUtils.generateTranslatedXliff(inputXliff.toPath(), outputPath);

			assertTrue("Output XLIFF should be created", Files.exists(outputPath));

			javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
			org.w3c.dom.Document doc = builder.parse(outputPath.toFile());

			org.w3c.dom.NodeList fileNodes = doc.getElementsByTagName("file");
			assertTrue("XLIFF should have at least one file element", fileNodes.getLength() > 0);

			for (int i = 0; i < fileNodes.getLength(); i++) {
				org.w3c.dom.Element file = (org.w3c.dom.Element) fileNodes.item(i);
				String targetLanguage = file.getAttribute("target-language");

				org.w3c.dom.NodeList bodyNodes = file.getElementsByTagName("body");
				assertTrue("File should have a body element", bodyNodes.getLength() > 0);

				org.w3c.dom.Element body = (org.w3c.dom.Element) bodyNodes.item(0);
				org.w3c.dom.NodeList transUnits = body.getElementsByTagName("trans-unit");

				for (int j = 0; j < transUnits.getLength(); j++) {
					org.w3c.dom.Element transUnit = (org.w3c.dom.Element) transUnits.item(j);
					org.w3c.dom.NodeList targetNodes = transUnit.getElementsByTagName("target");
					assertTrue("All trans-units should have a target element", targetNodes.getLength() > 0);

					org.w3c.dom.Element target = (org.w3c.dom.Element) targetNodes.item(0);
					org.w3c.dom.NodeList sourceNodes = transUnit.getElementsByTagName("source");
					assertTrue("Trans-unit should have a source element", sourceNodes.getLength() > 0);

					String targetText = target.getTextContent();
					if (!targetLanguage.isEmpty()) {
						assertTrue("Target text should start with language code prefix",
								targetText.startsWith(targetLanguage + ":") || targetText.contains(targetLanguage + ":"));
					}
				}
			}
		} finally {
			Files.deleteIfExists(outputPath);
		}
	}

}
