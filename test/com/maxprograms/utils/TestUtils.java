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
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.json.JSONException;

import com.maxprograms.fluenta.controllers.LocalController;
import com.maxprograms.fluenta.controllers.ProjectsManager;
import com.maxprograms.fluenta.models.Project;
import com.maxprograms.fluenta.models.ProjectEvent;

/**
 * Utility methods for use in JUnit test cases. Provides temporary file/directory
 * creation, resource loading, and cleanup helpers.
 */
public final class TestUtils {

	private static final String DEFAULT_SRC_LANGUAGE = "en-US";

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

	/**
	 * Returns the project for the given DITA Map file, creating one if it does not
	 * exist. Uses {@link LocalController} to access and persist projects.
	 * Caller must have initialized Preferences (e.g. {@link #initPreferences}) before calling.
	 *
	 * @param ditaMapFile the DITA Map file (e.g. sample.ditamap)
	 * @return the existing or newly created project for this map
	 * @throws IOException        if preferences/controller/project access fails
	 * @throws JSONException      if project serialization fails
	 * @throws ParseException     if project date parsing fails
	 * @throws ReflectiveOperationException if reflection on LocalController fails
	 */
	public static Project getOrCreateProjectForDitaMap(File ditaMapFile)
			throws IOException, JSONException, ParseException, ReflectiveOperationException {
		if (ditaMapFile == null || !ditaMapFile.exists()) {
			throw new IOException("DITA Map file does not exist: " + (ditaMapFile == null ? "null" : ditaMapFile.getAbsolutePath()));
		}
		String mapPath = ditaMapFile.getAbsolutePath();
		LocalController controller = new LocalController();
		ProjectsManager projectsManager = getProjectsManager(controller);
		List<Project> projects = invokeGetProjects(projectsManager);
		String normalizedMapPath = normalizePath(mapPath);
		for (Project p : projects) {
			if (normalizePath(p.getMap()).equals(normalizedMapPath)) {
				return controller.getProject(p.getId());
			}
		}
		Project newProject = createNewProjectForMap(ditaMapFile, mapPath);
    projectsManager.add(newProject);
    
		return controller.getProject(newProject.getId());
	}

	private static ProjectsManager getProjectsManager(LocalController controller)
			throws ReflectiveOperationException, IOException, JSONException, ParseException {
		java.lang.reflect.Field field = LocalController.class.getDeclaredField("projectsManager");
		field.setAccessible(true);
		ProjectsManager manager = (ProjectsManager) field.get(controller);
		if (manager == null) {
			Preferences prefs = Preferences.getInstance();
			manager = new ProjectsManager(prefs.getProjectsFolder());
			field.set(controller, manager);
		}
		return manager;
	}

	@SuppressWarnings("unchecked")
	private static List<Project> invokeGetProjects(ProjectsManager projectsManager) throws ReflectiveOperationException {
		Method m = ProjectsManager.class.getDeclaredMethod("getProjects");
		m.setAccessible(true);
		return (List<Project>) m.invoke(projectsManager);
	}

	private static String normalizePath(String path) {
		if (path == null) {
			return "";
		}
		try {
			return new File(path).getCanonicalPath();
		} catch (IOException e) {
			return new File(path).getAbsolutePath();
		}
	}

	private static Project createNewProjectForMap(File ditaMapFile, String mapPath) {
		long id = System.currentTimeMillis();
		String title = ditaMapFile.getName();
		if (title.endsWith(".ditamap")) {
			title = title.substring(0, title.length() - ".ditamap".length());
		}
		String description = "Test project for " + mapPath;
		Date now = new Date();
		List<String> tgtLanguages = new Vector<>();
		List<Long> memories = new Vector<>();
		List<ProjectEvent> history = new Vector<>();
		Hashtable<String, String> languageStatus = new Hashtable<>();
		return new Project(id, title, description, mapPath, now, now, DEFAULT_SRC_LANGUAGE,
				tgtLanguages, memories, history, languageStatus);
	}

}
