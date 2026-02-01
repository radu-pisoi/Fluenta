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
		File fluentaHome = base.resolve("home").toFile();
		File projectsFolder = base.resolve("projects").toFile();
		File memoriesFolder = base.resolve("memories").toFile();

		Preferences prefs = TestUtils.initPreferences(fluentaHome, projectsFolder, memoriesFolder);

		assertNotNull(prefs);
		assertEquals(projectsFolder.getAbsolutePath(), prefs.getProjectsFolder().getAbsolutePath());
		assertEquals(memoriesFolder.getAbsolutePath(), prefs.getMemoriesFolder().getAbsolutePath());
	}

	@Test
	public void testInitPreferences_createsDirectoriesIfNotExist() throws IOException {
		Path base = Files.createTempDirectory("fluenta-test-create-");
		File fluentaHome = base.resolve("home").toFile();
		File projectsFolder = base.resolve("projects").toFile();
		File memoriesFolder = base.resolve("memories").toFile();

		assertTrue("fluenta home should not exist before init", !fluentaHome.exists());
		TestUtils.initPreferences(fluentaHome, projectsFolder, memoriesFolder);

		assertTrue("fluenta home should exist", fluentaHome.exists());
		assertTrue("fluenta home should be directory", fluentaHome.isDirectory());
		assertTrue("projects folder should exist", projectsFolder.exists());
		assertTrue("projects folder should be directory", projectsFolder.isDirectory());
		assertTrue("memories folder should exist", memoriesFolder.exists());
		assertTrue("memories folder should be directory", memoriesFolder.isDirectory());
	}

	@Test
	public void testInitPreferencesWithTempDirs_returnsPreferencesWithTempDirs() throws IOException {
		Preferences prefs = TestUtils.initPreferencesWithTempDirs();

		assertNotNull(prefs);
		File projectsFolder = prefs.getProjectsFolder();
		File memoriesFolder = prefs.getMemoriesFolder();

		assertTrue("projects folder should exist", projectsFolder.exists());
		assertTrue("projects folder should be directory", projectsFolder.isDirectory());
		assertTrue("memories folder should exist", memoriesFolder.exists());
		assertTrue("memories folder should be directory", memoriesFolder.isDirectory());
		assertEquals("projects and memories should share same parent (fluenta home)",
				projectsFolder.getParent(), memoriesFolder.getParent());
	}

	@Test
	public void testInitPreferencesWithTempDirs_subsequentGetInstanceReturnsSamePaths() throws IOException {
		Preferences prefs1 = TestUtils.initPreferencesWithTempDirs();
		Preferences prefs2 = Preferences.getInstance();

		assertEquals(prefs1.getPreferencesFolder().getAbsolutePath(),
				prefs2.getPreferencesFolder().getAbsolutePath());
		assertEquals(prefs1.getProjectsFolder().getAbsolutePath(), prefs2.getProjectsFolder().getAbsolutePath());
		assertEquals(prefs1.getMemoriesFolder().getAbsolutePath(), prefs2.getMemoriesFolder().getAbsolutePath());
	}

	@Test
	public void testInitPreferences_resetWithDifferentDirs() throws IOException {
		Path base1 = Files.createTempDirectory("fluenta-test-a-");
		File home1 = base1.resolve("home").toFile();
		File projects1 = base1.resolve("projects").toFile();
		File memories1 = base1.resolve("memories").toFile();
		TestUtils.initPreferences(home1, projects1, memories1);
		assertEquals(projects1.getAbsolutePath(), Preferences.getInstance().getProjectsFolder().getAbsolutePath());
		assertEquals(memories1.getAbsolutePath(), Preferences.getInstance().getMemoriesFolder().getAbsolutePath());

		Path base2 = Files.createTempDirectory("fluenta-test-b-");
		File home2 = base2.resolve("home").toFile();
		File projects2 = base2.resolve("projects").toFile();
		File memories2 = base2.resolve("memories").toFile();
		TestUtils.initPreferences(home2, projects2, memories2);

		assertEquals(projects2.getAbsolutePath(), Preferences.getInstance().getProjectsFolder().getAbsolutePath());
		assertEquals(memories2.getAbsolutePath(), Preferences.getInstance().getMemoriesFolder().getAbsolutePath());
	}

}
