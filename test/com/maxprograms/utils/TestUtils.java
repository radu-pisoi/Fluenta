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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.json.JSONException;

import com.maxprograms.fluenta.controllers.LocalController;
import com.maxprograms.fluenta.controllers.MemoriesManager;
import com.maxprograms.fluenta.controllers.ProjectsManager;
import com.maxprograms.fluenta.models.Memory;
import com.maxprograms.fluenta.models.Project;
import com.maxprograms.fluenta.models.ProjectEvent;
import com.maxprograms.languages.LanguageUtils;

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
	public static Project getOrCreateProjectForDitaMap(long id, File ditaMapFile, LocalController controller, List<String> targetLanguages)
			throws Exception {
		if (ditaMapFile == null || !ditaMapFile.exists()) {
			throw new IOException("DITA Map file does not exist: " + (ditaMapFile == null ? "null" : ditaMapFile.getAbsolutePath()));
		}
		String mapPath = ditaMapFile.getAbsolutePath();
		ProjectsManager projectsManager = getProjectsManager(controller);
		
		List<Project> projects = invokeGetProjects(projectsManager);
		String normalizedMapPath = normalizePath(mapPath);
		for (Project p : projects) {
			if (normalizePath(p.getMap()).equals(normalizedMapPath)) {
				return controller.getProject(p.getId());
			}
		}
    Date now = new Date();
		Project newProject = createNewProjectForMap(id, ditaMapFile, mapPath, targetLanguages, now);
    projectsManager.add(newProject);

    MemoriesManager memoriesManager = getMemoriesManager(controller);
    memoriesManager.add(
      new Memory(id, "Test Memory", 
      "Test Memory Description", 
      now, now, 
      LanguageUtils.getLanguage("en-US")));
    
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

    private static MemoriesManager getMemoriesManager(LocalController controller)
        throws ReflectiveOperationException, IOException, JSONException, ParseException {
      java.lang.reflect.Field field = LocalController.class.getDeclaredField("memoriesManager");
      field.setAccessible(true);
      MemoriesManager manager = (MemoriesManager) field.get(controller);
      if (manager == null) {
        Preferences prefs = Preferences.getInstance();
        manager = new MemoriesManager(prefs.getMemoriesFolder());
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

	private static Project createNewProjectForMap(
    long id, File ditaMapFile, String mapPath, List<String> targetLanguages, 
        Date date) {
		String title = ditaMapFile.getName();
		if (title.endsWith(".ditamap")) {
			title = title.substring(0, title.length() - ".ditamap".length());
		}
		String description = "Test project for " + mapPath;
		List<Long> memories = new Vector<>();
		List<ProjectEvent> history = new Vector<>();
		Hashtable<String, String> languageStatus = new Hashtable<>();
		return new Project(id, title, description, mapPath, date, date, DEFAULT_SRC_LANGUAGE,
				targetLanguages, memories, history, languageStatus);
	}

	/**
	 * Recursively deletes a file or directory and all its contents.
	 *
	 * @param path the file or directory to delete
	 * @throws IOException if a delete operation fails
	 */
	public static void deleteRecursively(Path path) throws IOException {
		if (path == null || !Files.exists(path)) {
			return;
		}
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.deleteIfExists(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc != null) {
					throw exc;
				}
				Files.deleteIfExists(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Generates a translated XLIFF file by creating target elements for all translation units
	 * that are missing targets. The target content is created by prefixing the source content
	 * with the target language code.
	 *
	 * @param inputXliff  path to the XLIFF file to translate
	 * @param outputXliff path where the translated XLIFF will be written
	 * @throws Exception if reading, parsing, or writing fails
	 */
	public static void generateTranslatedXliff(Path inputXliff, Path outputXliff) throws Exception {
		javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
		org.w3c.dom.Document doc = builder.parse(inputXliff.toFile());

		org.w3c.dom.NodeList fileNodes = doc.getElementsByTagName("file");
		for (int i = 0; i < fileNodes.getLength(); i++) {
			org.w3c.dom.Element file = (org.w3c.dom.Element) fileNodes.item(i);
			String targetLanguage = file.getAttribute("target-language");

			org.w3c.dom.NodeList bodyNodes = file.getElementsByTagName("body");
			if (bodyNodes.getLength() > 0) {
				org.w3c.dom.Element body = (org.w3c.dom.Element) bodyNodes.item(0);
				org.w3c.dom.NodeList transUnits = body.getElementsByTagName("trans-unit");

				for (int j = 0; j < transUnits.getLength(); j++) {
					org.w3c.dom.Element transUnit = (org.w3c.dom.Element) transUnits.item(j);
					org.w3c.dom.NodeList targetNodes = transUnit.getElementsByTagName("target");

					if (targetNodes.getLength() == 0) {
						org.w3c.dom.NodeList sourceNodes = transUnit.getElementsByTagName("source");
						if (sourceNodes.getLength() > 0) {
							org.w3c.dom.Element source = (org.w3c.dom.Element) sourceNodes.item(0);
							org.w3c.dom.Element target = doc.createElement("target");

							org.w3c.dom.NodeList sourceChildren = source.getChildNodes();
							boolean prefixAdded = false;

							for (int k = 0; k < sourceChildren.getLength(); k++) {
								org.w3c.dom.Node node = sourceChildren.item(k);
								org.w3c.dom.Node clonedNode = node.cloneNode(true);

								if (!prefixAdded && node.getNodeType() == org.w3c.dom.Node.TEXT_NODE && !targetLanguage.isEmpty()) {
									String text = node.getTextContent();
									clonedNode.setTextContent(targetLanguage + ":" + text);
									prefixAdded = true;
								}

								target.appendChild(clonedNode);
							}

							transUnit.appendChild(doc.createTextNode("\n   "));
							transUnit.appendChild(target);
							transUnit.appendChild(doc.createTextNode("\n"));
						}
					}
				}
			}
		}

		javax.xml.transform.TransformerFactory transformerFactory = javax.xml.transform.TransformerFactory.newInstance();
		javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "no");
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "no");

		javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(doc);
		javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(outputXliff.toFile());
		transformer.transform(source, result);
	}

	/**
	 * Adds a new topic reference to a DITA Map as the last topic. The method creates
	 * a {@code <topicref>} element with the relative path from the map directory to the
	 * topic file and appends it to the map element.
	 *
	 * @param ditaMapPath absolute path to the DITA map file (.ditamap)
	 * @param topicPath   absolute path to the topic file (.dita) to be added
	 * @throws IOException  if the files cannot be read or written
	 * @throws Exception    if XML parsing or transformation fails
	 */
	public static void addTopicRefToDitaMap(Path ditaMapPath, Path topicPath) throws Exception {
		if (!Files.exists(ditaMapPath) || !Files.isRegularFile(ditaMapPath)) {
			throw new IOException("DITA Map file does not exist: " + ditaMapPath);
		}
		if (!Files.exists(topicPath) || !Files.isRegularFile(topicPath)) {
			throw new IOException("Topic file does not exist: " + topicPath);
		}

		javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		factory.setFeature("http://xml.org/sax/features/validation", false);
		javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
		org.w3c.dom.Document doc = builder.parse(ditaMapPath.toFile());

		String relativePath = ditaMapPath.getParent().relativize(topicPath).toString();
		relativePath = relativePath.replace('\\', '/');

		org.w3c.dom.Element mapElement = doc.getDocumentElement();
		if (mapElement == null || !"map".equals(mapElement.getNodeName())) {
			throw new Exception("Invalid DITA Map: <map> element not found");
		}

		org.w3c.dom.Element topicref = doc.createElement("topicref");
		topicref.setAttribute("href", relativePath);

		mapElement.appendChild(doc.createTextNode("\n  "));
		mapElement.appendChild(topicref);
		mapElement.appendChild(doc.createTextNode("\n"));

		javax.xml.transform.TransformerFactory transformerFactory = javax.xml.transform.TransformerFactory.newInstance();
		javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "no");
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "no");

		org.w3c.dom.DocumentType doctype = doc.getDoctype();
		if (doctype != null) {
			transformer.setOutputProperty(javax.xml.transform.OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
			transformer.setOutputProperty(javax.xml.transform.OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
		}

		javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(doc);
		javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(ditaMapPath.toFile());
		transformer.transform(source, result);
	}

}
