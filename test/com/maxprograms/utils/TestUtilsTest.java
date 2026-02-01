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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

		Project project = TestUtils.getOrCreateProjectForDitaMap(ditaMap, new LocalController());

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
		Project project1 = TestUtils.getOrCreateProjectForDitaMap(ditaMap, controller);
		Project project2 = TestUtils.getOrCreateProjectForDitaMap(ditaMap, controller);

		assertNotNull(project1);
		assertNotNull(project2);
		assertEquals(project1.getId(), project2.getId());
		assertEquals(project1.getMap(), project2.getMap());
	}

}
