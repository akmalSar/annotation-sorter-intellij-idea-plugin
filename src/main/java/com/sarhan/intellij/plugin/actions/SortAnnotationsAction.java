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

import java.util.Arrays;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

/**
 * The SortAnnotationsAction provides functionality to sort annotations in PSI classes and
 * fields in IntelliJ IDEA. This action operates within the IDE's functionality and
 * framework, making use of relevant PSI elements and commands.
 * <p>
 * This class is marked as deprecated for removal in future versions.
 * <p>
 * The action is triggered only when a valid selection is made in the editor allowing
 * sorting of annotations either on the class or its fields.
 * <p>
 * Key features: - Identifies the PSI class at the caret position inside the editor. -
 * Checks for multiple annotations in the class or its fields. - Provides sorting of
 * annotations through a write command action.
 * <p>
 * Override methods: - `getActionUpdateThread`: Specifies the background thread for this
 * action's updates. - `actionPerformed`: Executes the sorting logic when the action is
 * triggered. - `update`: Manages availability and visibility of the action based on
 * editor context.
 * <p>
 * Internal methods: - `getValidPsiFile`: Retrieves a valid PsiFile from the editor
 * context if available. - `findValidClassAtCaret`: Finds and validates the PSI class at
 * the current caret position. - `findClassAtCaret`: Retrieves the PSI class at the
 * current caret position without validation.
 *
 * @author Akmal Sarhan
 * @deprecated not needed anymore as we attatch to reformat cycle
 */

@Deprecated(forRemoval = true, since = "1.0.0")
public class SortAnnotationsAction extends BaseSortAction {

	@Override
	@NotNull
	public ActionUpdateThread getActionUpdateThread() {
		// Use POOLED_THREAD for PSI operations
		return ActionUpdateThread.BGT;
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent event) {
		Project project = event.getProject();
		if ((project == null) || project.isDisposed()) {
			return;
		}

		PsiFile psiFile = getValidPsiFile(event);
		if ((psiFile == null) || !psiFile.isValid()) {
			return;
		}

		Editor editor = event.getData(CommonDataKeys.EDITOR);
		if ((editor == null) || editor.isDisposed()) {
			return;
		}
		PsiClass psiClass = findValidClassAtCaret(editor, psiFile);
		if ((psiClass == null) || !psiClass.isValid()) {
			return;
		}
		WriteCommandAction.runWriteCommandAction(project, () -> sortClass(project, psiClass));
	}

	private PsiFile getValidPsiFile(AnActionEvent event) {
		PsiFile result;
		try {
			PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
			if (psiFile == null) {
				psiFile = event.getData(LangDataKeys.PSI_FILE);
			}
			if (psiFile == null) {
				VirtualFile virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE);
				if ((virtualFile != null) && (event.getProject() != null)) {
					psiFile = PsiManager.getInstance(event.getProject()).findFile(virtualFile);
				}
			}
			result = ((psiFile != null) && psiFile.isValid()) ? psiFile : null;
		}
		catch (PsiInvalidElementAccessException accessException) {
			result = null;
		}
		return result;
	}

	private PsiClass findValidClassAtCaret(Editor editor, PsiFile psiFile) {
		PsiClass result = null;
		try {
			if (psiFile.isValid()) {
				int offset = editor.getCaretModel().getOffset();
				PsiElement element = psiFile.findElementAt(offset);
				if ((element != null) && element.isValid()) {
					PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
					result = ((psiClass != null) && psiClass.isValid()) ? psiClass : null;
				}
			}

		}
		catch (PsiInvalidElementAccessException accessException) {
		}
		return result;
	}

	private PsiClass findClassAtCaret(Editor editor, PsiFile psiFile) {
		int offset = editor.getCaretModel().getOffset();
		PsiElement element = psiFile.findElementAt(offset);
		return PsiTreeUtil.getParentOfType(element, PsiClass.class);
	}

	@Override
	public void update(@NotNull AnActionEvent event) {
		// Enable the action only when a class is selected
		final Editor editor = event.getData(CommonDataKeys.EDITOR);
		PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
		VirtualFile virtualFile = null;
		if (editor != null) {
			virtualFile = editor.getVirtualFile();
		}
		if ((virtualFile != null) && (psiFile == null) && (event.getProject() != null)) {
			psiFile = PsiManager.getInstance(event.getProject()).findFile(virtualFile);
		}
		boolean enabled = false;
		if ((editor != null) && (psiFile != null)) {
			PsiClass psiClass = findClassAtCaret(editor, psiFile);
			if (psiClass != null) {
				long fieldsWithMultipleAnnotations = Arrays.stream(psiClass.getFields())
					.filter((PsiField field) -> field.getAnnotations().length > 1)
					.count();
				PsiModifierList modifierList = psiClass.getModifierList();
				enabled = (modifierList != null) && (modifierList.getAnnotations().length > 1);
				enabled = enabled || (fieldsWithMultipleAnnotations > 0);
			}
		}

		event.getPresentation().setEnabledAndVisible(enabled);
	}

}
