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

import com.intellij.codeInsight.actions.ReformatCodeAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.AnActionResult;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.sarhan.intellij.plugin.actions.BaseSortAction;
import org.jetbrains.annotations.NotNull;

/**
 * The PostReformatActionListener class implements the {@code AnActionListener} interface
 * and listens for formatting actions performed in the IntelliJ IDEA editor. It processes
 * {@code ReformatCodeAction} events after the reformatting is complete, applying custom
 * actions such as sorting annotations on classes within the reformatted file.
 * <p>
 * Functionality within this class relies on IntelliJ's PSI (Program Structure Interface)
 * to analyze and modify Java files programmatically. The class specifically targets Java
 * files to retrieve and process their structure (e.g., classes, fields, methods, etc.).
 * <p>
 * Responsibilities: - Listens for actions of type {@code ReformatCodeAction} from
 * IntelliJ IDEA. - Retrieves the PSI structure of the reformatted file. - Invokes custom
 * actions (e.g., sorting annotations) on each class within the file.
 * <p>
 * Methods: - {@code getPsiClassesFromFile(PsiFile psiFile)}: A utility method to extract
 * all {@code PsiClass} elements from the provided {@code PsiFile}, assuming it is a valid
 * {@code Psi
 * JavaFile}. -
 * {@code afterActionPerformed(@NotNull AnAction action, @NotNull DataContext dataContext, @NotNull AnActionEvent event)}:
 * Triggers custom processing of the file structure after a {@code ReformatCodeAction} has
 * been completed.
 * <p>
 * It's designed to be integrated with IntelliJ IDEA's editor and therefore depends on
 * IntelliJ's extension points and APIs such as {@code DataContext} and
 * {@code AnActionEvent}.
 *
 * @author Akmal Sarhan
 */
public class PostReformatActionListener implements AnActionListener {

	@Override
	public void afterActionPerformed(@NotNull AnAction action, @NotNull AnActionEvent event,
			@NotNull AnActionResult result) {
		if (action instanceof ReformatCodeAction) {

			// Get the editor and file
			PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
			VirtualFile selectedDir = event.getData(CommonDataKeys.VIRTUAL_FILE);
			if ((psiFile != null) || (selectedDir != null)) {
				WriteCommandAction.runWriteCommandAction(event.getData(CommonDataKeys.PROJECT), () -> BaseSortAction
					.processFileArray(event.getData(CommonDataKeys.PROJECT), selectedDir, psiFile));
			}

		}
	}

}
