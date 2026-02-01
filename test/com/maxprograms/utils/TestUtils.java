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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Utility methods for use in JUnit test cases. Provides temporary file/directory
 * creation, resource loading, and cleanup helpers.
 */
public final class TestUtils {

	private TestUtils() {
	}

	/**
	 * Initializes Preferences for tests with the given fluenta home, projects, and
	 * memories folders. Creates the directories if they do not exist. Resets the
	 * Preferences singleton so subsequent {@link Preferences#getInstance()} returns
	 * a Preferences instance using these paths.
	 *
	 * @param fluentaHome   fluenta home folder (work directory)
	 * @param projectsFolder projects folder
	 * @param memoriesFolder memories folder
	 * @return the initialized Preferences instance
	 * @throws IOException if directories cannot be created or Preferences cannot be initialized
	 */
	public static Preferences initPreferences(File projectsFolder, File memoriesFolder)
			throws IOException {
		File fluentaHome = new File(".");

		if (!projectsFolder.exists()) {
			Files.createDirectories(projectsFolder.toPath());
		}
		if (!memoriesFolder.exists()) {
			Files.createDirectories(memoriesFolder.toPath());
		}
		Preferences.initForTest(fluentaHome, projectsFolder, memoriesFolder);
		Preferences prefs = Preferences.getInstance();
		return prefs;
	}

}
