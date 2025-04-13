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

package com.sarhan.intellij.plugin.actions;

import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * A custom action designed for sorting Java annotations in a project or directory within
 * the IntelliJ platform. This class allows users to either process a directory of Java
 * files or an individual Java file and sorts the annotations on fields, methods, and
 * classes in a structured order.
 *
 * @author Akmal Sarhan
 * @deprecated not needed anymore as we attatch to reformat cycle
 */
@Deprecated(forRemoval = true, since = "1.0.0")
public class SortAnnotationsDirectoryAction extends BaseSortAction {

	@Override
	@NotNull
	public ActionUpdateThread getActionUpdateThread() {
		// Use POOLED_THREAD for PSI operations
		return ActionUpdateThread.BGT;
	}

	/**
	 * Handles the action triggered by the user in the IntelliJ platform. This method
	 * processes either a single Java file or all Java files in a selected directory. It
	 * performs sorting operations on annotations within the processed Java files.
	 * @param event the action event triggered, which contains context data such as the
	 * selected project, file, or directory in the IDE.
	 */
	@Override
	public void actionPerformed(@NotNull AnActionEvent event) {
		Project project = event.getProject();
		if ((project == null) || project.isDisposed()) {
			return;
		}

		VirtualFile directory = event.getData(CommonDataKeys.VIRTUAL_FILE);
		PsiFile psiFile = event.getData(LangDataKeys.PSI_FILE);

		processFileArray(project, directory, psiFile);
	}

	/**
	 * Updates the visibility and enabled state of the action based on the current
	 * context. This method evaluates the selected project and selected items in the IDE
	 * to determine if the action should be available to the user.
	 * @param actionEvent the event triggered by the user action, containing context such
	 * as the selected project and the selected items in the IDE.
	 */
	@Override
	public void update(@NotNull AnActionEvent actionEvent) {
		Object[] objects = actionEvent.getData(LangDataKeys.SELECTED_ITEMS);
		actionEvent.getPresentation().setEnabledAndVisible(false);
		if ((objects != null) && (objects.length == 1)) {
			if (objects[0] instanceof PsiDirectoryNode) {
				actionEvent.getPresentation().setEnabledAndVisible(true);
			}
		}
	}

}
