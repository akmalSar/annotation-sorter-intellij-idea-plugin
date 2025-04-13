/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sarhan.intellij.plugin;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.sarhan.intellij.plugin.ui.AnnotationSortingSettings;

/**
 * This class provides project-level services for managing, loading, and exporting
 * annotation sorting settings. It integrates directly with the project's environment to
 * allow the storage of configuration in a project-specific settings file.
 * <p>
 * The class listens to modifications of the settings file within the project and adjusts
 * the IDE's settings accordingly. It facilitates synchronization between project-specific
 * settings and IDE-wide settings, ensuring consistency.
 * <p>
 * Key Responsibilities: - Load annotation sorting settings from a project-specific file.
 * - Export annotation sorting settings to a project-specific file. - Register and
 * unregister a file listener to monitor changes to the settings file. - Provide a
 * centralized service instance via {@link #getInstance(Project)}.
 * <p>
 * This service operates using the file named {@value #SETTINGS_FILENAME}, which resides
 * in the root directory of a project.
 * <p>
 * The service is disposable and ensures that any registered resources, such as the file
 * listener, are appropriately released during disposal.
 *
 * @author Akmal Sarhan
 */
@Service(Service.Level.PROJECT)
public final class AnnotationSortingProjectService implements Disposable {

	/**
	 * The name of the settings file used to store annotation sorting configurations. This
	 * file is located in the root directory of the project.
	 * <p>
	 * The file is used by the {@link AnnotationSortingProjectService} class to load and
	 * export settings related to annotation sorting. It should be a JSON-formatted file
	 * with appropriate configuration details.
	 */
	public static final String SETTINGS_FILENAME = ".annotation-sorting.json";

	private final Project project;

	public AnnotationSortingProjectService(Project project) {
		this.project = project;

	}

	// Static method to get service instance
	public static AnnotationSortingProjectService getInstance(Project project) {
		return project.getService(AnnotationSortingProjectService.class);
	}

	/**
	 * Loads the annotation sorting settings from a project-level settings file. This
	 * method attempts to locate and parse a JSON-based settings file within the root
	 * directory of the current project. If the file is found and valid, it updates the
	 * IDE-level annotation sorting settings with the values retrieved from the file,
	 * including the annotation order and unmatched position settings. If any exception
	 * occurs during this process, the method logs a warning and retains the existing IDE
	 * settings.
	 * @return true if the settings were successfully loaded from the project file; false
	 * if the file was not found, contained errors, or failed to load due to any
	 * exception.
	 */
	public boolean loadSettingsFromProject() {
		try {

			VirtualFile projectDir = ProjectUtil.guessProjectDir(this.project);
			if (projectDir == null) {
				return false;
			}
			VirtualFile settingsFile = projectDir.findChild(SETTINGS_FILENAME);

			if ((settingsFile != null) && settingsFile.exists()) {
				// Read file content
				String json = new String(settingsFile.contentsToByteArray(), StandardCharsets.UTF_8);

				// Parse JSON
				Gson gson = new Gson();
				SettingsFileContent settings = gson.fromJson(json, SettingsFileContent.class);

				// Update IDE settings with file settings
				AnnotationSortingSettings ideSettings = AnnotationSortingSettings.getInstance();
				AnnotationSortingSettings.State state = ideSettings.getState();
				if (state != null) {
					state.projectAnnotationOrder = settings.annotationOrder;
					state.projectUnmatchedPosition = settings.unmatchedPosition;
				}

				Logger.getInstance(AnnotationSortingProjectService.class)
					.info("Loaded annotation sorting settings from project file");
				return true;
			}
		}
		catch (Exception ex) {
			Logger.getInstance(AnnotationSortingProjectService.class)
				.warn("Failed to load project annotation sorting settings", ex);
		}
		return false;
	}

	/**
	 * Exports the current annotation sorting settings for the project to a JSON file
	 * located in the project's root directory. The method retrieves the current settings
	 * from the {@link AnnotationSortingSettings} instance, serializes them to JSON
	 * format, and writes the output to a file. If the operation is successful, the
	 * changes are reflected in the virtual file system, and a success message is
	 * displayed. In case of an error, an error message is shown to the user.
	 * <p>
	 * The export process involves the following steps: - Retrieve the current
	 * {@link AnnotationSortingSettings.State} instance. - Serialize the annotation order
	 * and unmatched position to a JSON file using {@link Gson}. - Write the serialized
	 * JSON object to a file in the project directory. - Refresh the virtual file system
	 * to notify it about the newly created/updated file. - Display appropriate messages
	 * to the user on success or failure.
	 * <p>
	 * Exceptions that occur during the export process are caught and displayed to the
	 * user.
	 */
	public void exportSettingsToProject() {
		try {
			// Get current settings
			AnnotationSortingSettings settings = AnnotationSortingSettings.getInstance();
			AnnotationSortingSettings.State state = settings.getState();
			if (state == null) {
				return;
			}

			// Create a settings object to serialize
			SettingsFileContent fileContent = new SettingsFileContent();
			fileContent.annotationOrder = state.annotationOrder;
			fileContent.unmatchedPosition = state.unmatchedPosition;

			// Convert to JSON
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String json = gson.toJson(fileContent);

			// Write to file in project root
			VirtualFile projectDir = ProjectUtil.guessProjectDir(this.project);
			if (projectDir == null) {
				return;
			}
			File settingsFile = new File(projectDir.getPath(), SETTINGS_FILENAME);

			try (FileWriter writer = new FileWriter(settingsFile)) {
				writer.write(json);
			}

			// Refresh VirtualFileSystem to detect the new file
			projectDir.refresh(false, false);

			Messages.showInfoMessage("Settings exported to " + settingsFile.getPath(), "Export Successful");
		}
		catch (Exception ex) {
			Messages.showErrorDialog("Failed to export settings: " + ex.getMessage(), "Export Error");
		}
	}

	@Override
	public void dispose() {

	}

	/**
	 * Represents the content and structure of a settings file used for managing
	 * annotation sorting configurations within a project. This class provides the
	 * necessary structure to store and manipulate the order of annotations and the
	 * position for unmatched annotations.
	 * <p>
	 * The class includes: - A list to define the specific order in which annotations
	 * should be arranged. - A position value indicating where unmatched annotations
	 * should appear.
	 * <p>
	 * It is used within the project service to manage and persist settings regarding
	 * annotation sorting behavior.
	 */
	public static class SettingsFileContent {

		/**
		 * Defines the order in which annotations should be sorted. The list contains
		 * annotation names as strings, specifying their desired sequence during sorting.
		 * If the list is empty, no specific order will be applied to annotations.
		 */
		public List<String> annotationOrder = new ArrayList<>();

		/**
		 * Represents the position at which unmatched annotations should appear during
		 * sorting. A value of -1 indicates that unmatched annotations will be excluded
		 * from the sorting output. Positive integer values represent the specified index
		 * where unmatched annotations will be placed.
		 */
		public int unmatchedPosition = -1;

	}

}
