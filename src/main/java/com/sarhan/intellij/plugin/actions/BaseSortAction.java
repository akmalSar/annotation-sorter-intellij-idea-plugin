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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.sarhan.intellij.plugin.ui.AnnotationSortingSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An abstract base class that provides common functionality for actions related to
 * sorting annotations on fields within a given class or across classes in a project. It
 * extends {@code AnAction} to integrate with IntelliJ IDEA's action system.
 *
 * @author Akmal Sarhan
 */
public abstract class BaseSortAction extends DumbAwareAction {

	public static PsiClass[] getPsiClassesFromFile(PsiFile psiFile) {
		// Check if the PsiFile is a Java file
		if (psiFile instanceof PsiJavaFile javaFile) {
			return javaFile.getClasses();
		}
		return new PsiClass[0];
	}

	private static void perform(Project project, PsiElement psiField, PsiModifierList modifierList,
			PsiAnnotation[] annotations, List<String> modifiers, List<String> sortedAnnotations) {
		if (!psiField.isValid() || !modifierList.isValid()) {
			return;
		}

		Arrays.stream(annotations).forEach(PsiElement::delete);
		Arrays.stream(PsiModifier.MODIFIERS)
			.forEach((String modifier) -> modifierList.setModifierProperty(modifier, false));

		PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

		sortedAnnotations
			.forEach((String annotationText) -> addAnnotationSafely(factory, modifierList, annotationText));

		modifiers.forEach((String modifier) -> modifierList.setModifierProperty(modifier, true));
	}

	/**
	 * Adds an annotation to the specified {@code PsiModifierList} safely, handling any
	 * exceptions that may occur. If the annotation cannot be created or added directly
	 * due to an exception, a fallback mechanism is used.
	 * @param factory the {@code PsiElementFactory} used to create the annotation
	 * @param modifierList the {@code PsiModifierList} where the annotation will be added
	 * @param annotationText the text representation of the annotation to be added
	 */
	private static void addAnnotationSafely(PsiElementFactory factory, PsiModifierList modifierList,
			String annotationText) {
		try {
			modifierList.add(factory.createAnnotationFromText(annotationText, null));
		}
		catch (Exception exception) {
			modifierList.addAnnotation(annotationText.split("[( ]")[0].substring(1));
		}
	}

	/**
	 * Sorts an array of {@code PsiAnnotation} elements based on their qualified names
	 * using a custom comparison logic.
	 * <p>
	 * The method filters out invalid {@code PsiAnnotation} elements, sorts the valid
	 * annotations lexicographically by their qualified names, and converts them to their
	 * text representations.
	 * @param annotations the array of {@code PsiAnnotation} elements to be sorted
	 * @return a list of string representations of the sorted annotations
	 */
	private static List<String> sort(PsiAnnotation[] annotations) {
		// Get settings
		AnnotationSortingSettings settings = AnnotationSortingSettings.getInstance();
		AnnotationSortingSettings.State state = settings.getState();

		List<String> annotationOrder;
		int unmatchedPosition;
		if (state.projectAnnotationOrder != null) {
			annotationOrder = state.projectAnnotationOrder;
			unmatchedPosition = state.projectUnmatchedPosition;
		}
		else {
			unmatchedPosition = state.unmatchedPosition;
			annotationOrder = state.annotationOrder;
		}

		return Arrays.stream(annotations).filter(PsiElement::isValid).sorted((PsiAnnotation a1, PsiAnnotation a2) -> {
			String name1 = Optional.ofNullable(a1.getQualifiedName()).orElse("");
			String name2 = Optional.ofNullable(a2.getQualifiedName()).orElse("");
			return compareWithPreferences(name1, name2, annotationOrder, unmatchedPosition);
		}).map(PsiAnnotation::getText).toList();
	}

	private static int compareWithPreferences(String name1, String name2, List<String> preferences,
			int unmatchedPosition) {
		// Determine if annotations match any patterns
		boolean name1Matches = matchesAnyPattern(name1, preferences);
		boolean name2Matches = matchesAnyPattern(name2, preferences);

		// Get position indexes for matched annotations
		int posIndex1 = getPositionIndex(name1, preferences);
		int posIndex2 = getPositionIndex(name2, preferences);

		// Adjust position indexes based on unmatched position
		if (!name1Matches) {
			posIndex1 = unmatchedPosition;
		}

		if (!name2Matches) {
			posIndex2 = unmatchedPosition;
		}

		// Compare by position first
		if (posIndex1 != posIndex2) {
			return Integer.compare(posIndex1, posIndex2);
		}

		// If both are unmatched or have the same position, use original logic
		return compareAnnotations(name1, name2);
	}

	private static int compareAnnotations(String name1, String name2) {
		String[] parts1 = name1.split("\\.");
		String[] parts2 = name2.split("\\.");

		String pkg1 = String.join(".", Arrays.copyOf(parts1, Math.max(0, parts1.length - 1)));
		String pkg2 = String.join(".", Arrays.copyOf(parts2, Math.max(0, parts2.length - 1)));
		String className1 = (parts1.length > 0) ? parts1[parts1.length - 1] : "";
		String className2 = (parts2.length > 0) ? parts2[parts2.length - 1] : "";

		// If packages are different, compare packages
		if (!pkg1.equals(pkg2)) {
			return name1.compareTo(name2);
		}

		// If packages are the same, compare by class name length, then lexicographically
		int lengthCompare = Integer.compare(className1.length(), className2.length());
		return (lengthCompare != 0) ? lengthCompare : className1.compareTo(className2);
	}

	// Helper to check if an annotation matches any pattern
	private static boolean matchesAnyPattern(String qualifiedName, List<String> patterns) {
		// Extract simple name (part after the last dot)
		String simpleName = qualifiedName;
		int lastDot = qualifiedName.lastIndexOf('.');
		if (lastDot >= 0) {
			simpleName = qualifiedName.substring(lastDot + 1);
		}

		// Check for exact matches (both qualified and simple)
		if (patterns.contains(qualifiedName) || patterns.contains(simpleName)) {
			return true;
		}

		// Check for package pattern match
		for (String pattern : patterns) {
			if (pattern.endsWith(".*")) {
				String packagePrefix = pattern.substring(0, pattern.length() - 2);
				if (qualifiedName.startsWith(packagePrefix + ".")) {
					return true;
				}
			}
		}

		return false;
	}

	// Get position index for an annotation in the preference list
	private static int getPositionIndex(String qualifiedName, List<String> preferences) {
		// Extract simple name
		String simpleName = qualifiedName;
		int lastDot = qualifiedName.lastIndexOf('.');
		if (lastDot >= 0) {
			simpleName = qualifiedName.substring(lastDot + 1);
		}

		// Check for exact match (both qualified and simple)
		int exactQualifiedIndex = preferences.indexOf(qualifiedName);
		int exactSimpleIndex = preferences.indexOf(simpleName);

		if (exactQualifiedIndex >= 0) {
			return exactQualifiedIndex;
		}

		if (exactSimpleIndex >= 0) {
			return exactSimpleIndex;
		}

		// Check for package pattern match
		for (int i = 0; i < preferences.size(); i++) {
			String pattern = preferences.get(i);
			if (pattern.endsWith(".*")) {
				String packagePrefix = pattern.substring(0, pattern.length() - 2);
				if (qualifiedName.startsWith(packagePrefix + ".")) {
					return i;
				}
			}
		}

		// Not found
		return preferences.size();
	}

	/**
	 * Sorts annotations for all fields of the specified {@code PsiClass}. For each field
	 * that has multiple annotations, the annotations are processed and sorted.
	 * @param project the current project in which the action is being performed
	 * @param psiClass the {@code PsiClass} whose field annotations need to be sorted
	 */
	private static void sortAnnotationsForFields(Project project, PsiClass psiClass) {
		Arrays.stream(psiClass.getFields())
			.filter((PsiField field) -> field.getAnnotations().length > 1)
			.forEach((PsiField field) -> processFieldAnnotations(project, field));
	}

	private static void sortAnnotationsForConstructors(Project project, PsiClass psiClass) {
		Arrays.stream(psiClass.getConstructors())
			.filter((PsiMethod field) -> field.getAnnotations().length > 1)
			.forEach((PsiMethod field) -> processMethodAnnotations(project, field));
	}

	public static void sortClass(Project project, PsiClass psiClass) {

		PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
		DumbService.getInstance(project).runWhenSmart(() -> {
			if (!psiClass.isValid()) {
				return;
			}
			Document document = psiDocumentManager.getDocument(psiClass.getContainingFile());
			if ((document != null) && psiDocumentManager.isCommitted(document)) {
				try {
					WriteCommandAction.runWriteCommandAction(project, () -> {
						sortAnnotationsForFields(project, psiClass);
						sortAnnotationsForMethods(project, psiClass);
						sortAnnotationsForConstructors(project, psiClass);
						sortAnnotationsForInnerClasses(project, psiClass);
						PsiModifierList modifierList = psiClass.getModifierList();
						if ((modifierList == null) || !modifierList.isValid()) {
							return;
						}

						// Get all annotations
						PsiAnnotation[] annotations = modifierList.getAnnotations();
						if (annotations.length < 2) {
							return;
						}

						// Get all modifier strings (public, private, static, etc.)
						List<String> modifiers = new ArrayList<>();
						for (String modifier : PsiModifier.MODIFIERS) {
							if (modifierList.hasExplicitModifier(modifier)) {
								modifiers.add(modifier);
							}
						}

						perform(project, psiClass, modifierList, annotations, modifiers, sort(annotations));
					});
				}
				catch (PsiInvalidElementAccessException accessException) {
					Messages.showErrorDialog(project, accessException.getMessage(), "Error");
				}
			}
		});
	}

	private static void sortAnnotationsForInnerClasses(Project project, PsiClass psiClass) {
		Arrays.stream(psiClass.getInnerClasses())
			.filter((PsiClass aClass) -> aClass.getAnnotations().length > 1)
			.forEach((PsiClass aClass) -> sortClass(project, aClass));
	}

	private static void processFieldAnnotations(Project project, PsiField field) {
		Optional.ofNullable(field.getModifierList())
			.filter(PsiModifierList::isValid)
			.ifPresent((PsiModifierList modifierList) -> {
				PsiAnnotation[] annotations = modifierList.getAnnotations();
				if (annotations.length > 1) {
					List<String> modifiers = Arrays.stream(PsiModifier.MODIFIERS)
						.filter(modifierList::hasExplicitModifier)
						.toList();
					perform(project, field, modifierList, annotations, modifiers, sort(annotations));
				}
			});
	}

	private static void sortAnnotationsForMethods(Project project, PsiClass psiClass) {
		Arrays.stream(psiClass.getMethods())
			.filter((PsiMethod field) -> field.getAnnotations().length > 1)
			.forEach((PsiMethod field) -> processMethodAnnotations(project, field));
	}

	private static void processMethodAnnotations(Project project, PsiMethod psiMethod) {
		Optional.of(psiMethod.getModifierList())
			.filter(PsiModifierList::isValid)
			.ifPresent((PsiModifierList modifierList) -> {
				PsiAnnotation[] annotations = modifierList.getAnnotations();
				if (annotations.length > 1) {
					List<String> modifiers = Arrays.stream(PsiModifier.MODIFIERS)
						.filter(modifierList::hasExplicitModifier)
						.toList();
					perform(project, psiMethod, modifierList, annotations, modifiers, sort(annotations));
				}
			});
	}

	public static void processFileArray(Project project, @Nullable VirtualFile directory, @Nullable PsiFile psiFile) {

		PsiManager psiManager = PsiManager.getInstance(project);
		List<PsiJavaFile> javaFiles = new ArrayList<>();
		if ((directory != null) && directory.isDirectory()) {
			collectJavaFiles(directory, psiManager, javaFiles);
		}
		else {
			if (psiFile instanceof PsiJavaFile javaFile) {
				javaFiles.add(javaFile);
			}
		}

		ProgressManager.getInstance().run(new Task.Backgroundable(project, "Processing java files", true) {
			@Override
			public void run(@NotNull ProgressIndicator indicator) {

				indicator.setIndeterminate(false);
				indicator.setText("Processing Java files...");

				int totalFiles = javaFiles.size();
				int processedFiles = 0;

				for (PsiJavaFile psiFile : javaFiles) {
					if (indicator.isCanceled()) {
						break;
					}

					String fileName = psiFile.getName();
					indicator.setText("Processing: " + fileName);
					indicator.setText2("File " + (processedFiles + 1) + " of " + totalFiles);
					indicator.setFraction((double) processedFiles / totalFiles);
					WriteCommandAction.runWriteCommandAction(project, () -> processJavaFile(project, psiFile));
					processedFiles++;
				}

			}
		});
	}

	private static void collectJavaFiles(VirtualFile directory, PsiManager psiManager, List<PsiJavaFile> javaFiles) {
		for (VirtualFile file : directory.getChildren()) {
			if (file.isDirectory()) {
				collectJavaFiles(file, psiManager, javaFiles);
			}
			else if (file.getName().endsWith(".java")) {
				PsiFile psiFile = psiManager.findFile(file);
				if (psiFile instanceof PsiJavaFile javaFile) {
					javaFiles.add(javaFile);
				}
			}
		}
	}

	private static void processJavaFile(Project project, PsiJavaFile psiFile) {

		PsiClass[] classes = psiFile.getClasses();
		for (PsiClass psiClass : classes) {
			if (psiClass.isValid()) {
				sortClass(project, psiClass);
			}
		}

	}

}
