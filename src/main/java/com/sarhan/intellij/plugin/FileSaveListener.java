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

import com.intellij.codeInsight.actions.onSave.FormatOnSaveOptions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.serviceContainer.AlreadyDisposedException;
import com.sarhan.intellij.plugin.actions.BaseSortAction;
import org.jetbrains.annotations.NotNull;

/**
 * The FileSaveListener class listens to document-save events and performs specific
 * actions on the files being saved based on project-related configurations.
 *
 * This implementation checks whether the "Reformat code" option is enabled during the
 * save operation and applies specified processing to valid documents associated with open
 * projects.
 *
 * Implements the FileDocumentManagerListener interface to handle notifications of
 * document saving events.
 *
 * Methods: - beforeDocumentSaving: Invoked before a document is saved. Processes the
 * document by determining its associated project and applying file-specific commands if
 * the "Reformat code" option is enabled. - getProjectForDocument: Determines the
 * associated project for a given document's corresponding virtual file, if available and
 * valid.
 *
 * @author Akmal Sarhan
 */
public class FileSaveListener implements FileDocumentManagerListener {

	@Override
	public void beforeDocumentSaving(@NotNull Document document) {
		ApplicationManager.getApplication().invokeLater(() -> {
			// Only proceed if "Reformat code" is enabled in Actions on Save
			Project projectForDocument = getProjectForDocument(document);
			if (projectForDocument == null) {
				return;
			}
			FormatOnSaveOptions formatOptions = FormatOnSaveOptions.getInstance(projectForDocument);
			if (!formatOptions.isRunOnSaveEnabled()) {
				return;
			}

			// Get the file from the document
			VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
			if ((virtualFile == null) || !virtualFile.isValid()) {
				return;
			}

			// Process the file for each open project
			for (Project project : ProjectManager.getInstance().getOpenProjects()) {
				try {
					if (project.isDisposed()) {
						continue;
					}

					PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
					PsiFile psiFile = psiDocumentManager.getCachedPsiFile(document);

					if ((psiFile != null) && psiFile.isValid()) {
						WriteCommandAction.runWriteCommandAction(project,
								() -> BaseSortAction.processFileArray(project, virtualFile, psiFile));
					}
				}
				catch (AlreadyDisposedException ignored) {
					// Skip if project is already disposed
				}
			}
		});
	}

	private Project getProjectForDocument(Document document) {
		VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
		if ((virtualFile == null) || !virtualFile.isValid()) {
			return null;
		}

		// Get the project for this specific file
		return ProjectLocator.getInstance().guessProjectForFile(virtualFile);
	}

}
