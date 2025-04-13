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

import java.util.List;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The AnnotationSortingStartupActivity class is a startup activity that runs when a
 * project is opened in the IDE. It is responsible for initializing and loading
 * project-specific annotation sorting settings.
 * <p>
 * This class implements the {@link ProjectActivity} interface and executes its
 * functionality using the {@link AnnotationSortingProjectService}. Specifically, it
 * ensures that the annotation sorting configuration for the opened project is loaded from
 * a settings file. The configuration includes rules such as the order of annotations and
 * the behavior for unmatched annotations, defined at the project level.
 * <p>
 * Key responsibilities of this class: - Trigger the loading of annotation sorting
 * settings specific to the project. - Ensure that project configurations are correctly
 * initialized and synced with the IDE session upon project opening.
 * <p>
 * The actual process for loading settings is delegated to the method
 * {@link AnnotationSortingProjectService#loadSettingsFromProject()}, which reads the
 * configuration from a file specific to the project and updates the IDE's settings
 * accordingly.
 * <p>
 * This activity is invoked each time a project is opened to ensure consistency in
 * annotation sorting behavior tied to the project-specific configuration. If the
 * configuration is unavailable or invalid, the IDE will gracefully fall back to default
 * settings.
 *
 * @author Akmal Sarhan
 */
public class AnnotationSortingStartupActivity implements ProjectActivity {

	@Override
	@Nullable
	public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
		// Load project-specific settings when a project is opened
		AnnotationSortingProjectService.getInstance(project).loadSettingsFromProject();
		// Create a dedicated disposable for cleanup when project is closed
		Disposable disposable = Disposer.newDisposable("AnnotationSortingListeners");
		Disposer.register(project, disposable); // Attach to project lifecycle
		project.getMessageBus().connect().subscribe(AnActionListener.TOPIC, new PostReformatActionListener());
		VirtualFileManager.getInstance().addAsyncFileListener(new AsyncFileListener() {
			@Override
			public @Nullable ChangeApplier prepareChange(@NotNull List<? extends VFileEvent> events) {

				List<VFileEvent> relevantEvents = events.stream().filter((VFileEvent event) -> {
					VirtualFile file = event.getFile();
					return (file != null) && file.getName().equals(AnnotationSortingProjectService.SETTINGS_FILENAME);
				})
					.map(VFileEvent.class::cast) // Explicit cast
					.toList();

				if (relevantEvents.isEmpty()) {
					return null;
				}

				return new ChangeApplier() {
					@Override
					public void afterVfsChange() {
						for (VFileEvent event : relevantEvents) {
							VirtualFile file = event.getFile();
							if (file != null) {
								AnnotationSortingProjectService.getInstance(project).loadSettingsFromProject();

							}
						}
					}
				};
			}
		}, disposable);
		return Unit.INSTANCE;
	}

}
