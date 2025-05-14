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

import com.intellij.AppTopics;
import com.intellij.ide.ApplicationInitializedListener;
import com.intellij.openapi.application.ApplicationManager;

/**
 * ApplicationInitializedListenerImpl is an implementation of the
 * ApplicationInitializedListener interface. It is responsible for registering a
 * {@link FileSaveListener} to listen for file document synchronization events in the
 * IntelliJ IDE's message bus system after the application components are initialized.
 *
 * In the overridden {@code componentsInitialized} method, it retrieves an instance of
 * FileSaveListener and subscribes it to the {@code AppTopics.FILE_DOCUMENT_SYNC} topic on
 * the application's message bus. This enables the listener to react to
 * document-save-related events for processing file-specific actions.
 *
 * @author Akmal Sarhan
 */
public class ApplicationInitializedListenerImpl implements ApplicationInitializedListener {

	@Override
	public void componentsInitialized() {
		// Register the file save listener
		FileSaveListener listener = ApplicationManager.getApplication().getService(FileSaveListener.class);

		ApplicationManager.getApplication().getMessageBus().connect().subscribe(AppTopics.FILE_DOCUMENT_SYNC, listener);
	}

}
