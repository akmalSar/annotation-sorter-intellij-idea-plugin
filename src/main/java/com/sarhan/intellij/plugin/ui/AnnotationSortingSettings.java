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

package com.sarhan.intellij.plugin.ui;

import java.util.ArrayList;
import java.util.List;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

@State(name = "Sarhan_Annotation_Sorting_Settings", storages = { @Storage("annotation-sorting.xml") })
public class AnnotationSortingSettings implements PersistentStateComponent<AnnotationSortingSettings.State> {

	// Special marker for unmatched annotations position
	protected static final String UNMATCHED_MARKER = "[ALL OTHER ANNOTATIONS]";

	private State state = new State();

	public static AnnotationSortingSettings getInstance() {
		return ApplicationManager.getApplication().getService(AnnotationSortingSettings.class);
	}

	@Override
	public State getState() {
		return this.state;
	}

	@Override
	public void loadState(@NotNull State state) {
		this.state = state;
	}

	/**
	 * Represents the internal state for annotation sorting settings. This class is used
	 * to persist and manage the sorting order of annotations and track relevant runtime
	 * state for unmatched annotations.
	 */
	public static class State {

		/**
		 * Represents the order of annotations in a list. This list is used to maintain
		 * and track the sequence of annotations, which can be manipulated dynamically
		 * during processing or usage.
		 */
		public List<String> annotationOrder = new ArrayList<>();

		/**
		 * Represents the order of project-specific annotations in a list. This list is
		 * used to organize and maintain the sequence of project-level annotations,
		 * allowing for dynamic manipulation and retrieval during processing or runtime
		 * operations.
		 */
		public List<String> projectAnnotationOrder = new ArrayList<>();

		/**
		 * Represents the position in a sequence or list where a mismatch or anomaly was
		 * detected. The initial value is set to -1, indicating no mismatches have been
		 * identified. This variable is updated dynamically during runtime based on the
		 * processing context.
		 */
		public int unmatchedPosition = -1;

		/**
		 * Represents the position in a sequence or list where a mismatch or anomaly was
		 * detected specifically for project-level annotations. The initial value is set
		 * to -1, indicating no mismatches have been identified. This variable is updated
		 * dynamically during runtime based on the context of project-specific annotation
		 * processing.
		 */
		public int projectUnmatchedPosition = -1;

	}

}
