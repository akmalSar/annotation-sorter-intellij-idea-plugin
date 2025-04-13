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

import java.util.Arrays;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor;
import com.sarhan.intellij.plugin.actions.BaseSortAction;
import org.jetbrains.annotations.NotNull;

/**
 * The AnnotationSortPreFormatProcessor class is responsible for sorting annotations in
 * Java files as part of a post-formatting process. This class implements the
 * PostFormatProcessor interface and manipulates the annotations in classes, methods, and
 * fields within the PsiJavaFile to ensure consistent formatting.
 * <p>
 * This processor is applied during the code formatting process and utilizes the utilities
 * provided to access and modify the structure of Java files.
 *
 * @author Akmal Sarhan
 */

public class AnnotationSortPreFormatProcessor implements PostFormatProcessor {

	public static PsiClass[] getPsiClassesFromFile(PsiFile psiFile) {
		// Check if the PsiFile is a Java file
		return BaseSortAction.getPsiClassesFromFile(psiFile);
	}

	@Override
	@NotNull
	public PsiElement processElement(@NotNull PsiElement psiElement, @NotNull CodeStyleSettings codeStyleSettings) {
		return psiElement;
	}

	@Override
	@NotNull
	public TextRange processText(@NotNull PsiFile psiFile, @NotNull TextRange textRange,
			@NotNull CodeStyleSettings codeStyleSettings) {
		if (!(psiFile instanceof PsiJavaFile)) {
			return textRange; // If not a Java file, skip processing
		}
		Arrays.stream(getPsiClassesFromFile(psiFile))
			.forEach((PsiClass pc) -> BaseSortAction.sortClass(psiFile.getProject(), pc));
		return textRange;
	}

}
