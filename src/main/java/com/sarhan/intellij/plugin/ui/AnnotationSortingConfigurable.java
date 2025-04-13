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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.sarhan.intellij.plugin.AnnotationSortingProjectService;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

/**
 * This class provides a configurable interface for managing and customizing the sorting
 * order of annotations within a project. It implements the `Configurable` interface,
 * allowing integration with IntelliJ IDEA's settings page.
 * <p>
 * The configurable allows users to: - Add new annotation patterns or packages to the
 * sorting list. - Remove existing patterns or packages. - Reset the list to a default
 * predefined order. - Configure the handling of unmatched annotations by specifying their
 * position in the list. - Drag and drop annotations within the list to change their
 * priority.
 * <p>
 * Features include: - an Interactive list of annotations with drag-and-drop
 * functionality. - Resetting to a default annotation order, which covers standard Java
 * annotations, commonly used Spring annotations, and annotations in other popular
 * frameworks. - Display customization for unmatched annotations, highlighted to indicate
 * their status.
 * <p>
 * The class also handles persistence by saving user-modified configuration and
 * determining if any changes have been made compared to the last saved state.
 *
 * @author Akmal Sarhan
 */
public class AnnotationSortingConfigurable implements Configurable {

	// Define default patterns as a static list for reusability
	private static final List<String> DEFAULT_ANNOTATION_ORDER = List.of(

			"Override", "Deprecated", "SuppressWarnings", "FunctionalInterface", "SafeVarargs",

			"org.springframework.stereotype.*", "org.springframework.web.bind.annotation.*",
			"org.springframework.beans.factory.annotation.*",

			"org.springframework.context.annotation.*", "org.springframework.*",
			"org.springframework.boot.autoconfigure.*", "org.springframework.boot.*",

			"javax.validation.*", "jakarta.validation.*",

			"javax.persistence.*", "jakarta.persistence.*",

			"com.fasterxml.jackson.*",

			"lombok.*", "lombok.extern.*",

			"org.junit.*", "org.springframework.test.*",

			"org.springframework.*",

			"java.lang.*", "javax.*", "jakarta.*");

	private JPanel mainPanel;

	private JBList<String> annotationList;

	private DefaultListModel<String> listModel;

	private JButton addButton;

	private JButton removeButton;

	private JButton resetToDefaultButton;

	private JButton exportButton;

	@Override
	@Nls
	public String getDisplayName() {
		return "Annotation Sorting";
	}

	@Override
	@Nullable
	public JComponent createComponent() {
		this.listModel = new DefaultListModel<>();
		this.annotationList = new JBList<>(this.listModel);
		this.annotationList.setDropMode(DropMode.INSERT);
		if (!GraphicsEnvironment.isHeadless()) {
			this.annotationList.setDragEnabled(true);
		}
		this.annotationList.setTransferHandler(new ListItemTransferHandler());

		// Custom renderer for UNMATCHED_MARKER
		this.annotationList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected,
						cellHasFocus);

				if (AnnotationSortingSettings.UNMATCHED_MARKER.equals(String.valueOf(value))) {
					setFont(getFont().deriveFont(Font.BOLD));
					setForeground(isSelected ? UIManager.getColor("List.selectionForeground") : JBColor.BLUE);
				}
				return rendererComponent;
			}
		});

		this.addButton = new JButton("Add");
		this.removeButton = new JButton("Remove");
		this.resetToDefaultButton = new JButton("Reset to Default");
		this.exportButton = new JButton("Export to Project root");

		this.addButton.addActionListener((ActionEvent e) -> {
			String input = JOptionPane.showInputDialog(this.mainPanel,
					"Enter annotation name or package pattern (e.g., 'jakarta.persistence.*'):");
			if ((input != null) && !input.trim().isEmpty()) {
				this.listModel.addElement(input.trim());
			}
		});

		this.removeButton.addActionListener((ActionEvent e) -> {
			int selectedIndex = this.annotationList.getSelectedIndex();
			if (selectedIndex >= 0) {
				String selectedItem = this.listModel.getElementAt(selectedIndex);
				// Prevent removing the unmatched marker
				if (!AnnotationSortingSettings.UNMATCHED_MARKER.equals(selectedItem)) {
					this.listModel.remove(selectedIndex);
				}
			}
		});

		this.resetToDefaultButton.addActionListener((ActionEvent e) -> {
			int option = Messages.showYesNoDialog("Are you sure you want to reset to default annotation order?",
					"Reset to Default", Messages.getQuestionIcon());

			if (option == Messages.YES) {
				loadDefaultSettings();
			}
		});

		this.exportButton.addActionListener((ActionEvent e) -> {
			Project project = CommonDataKeys.PROJECT
				.getData(DataManager.getInstance().getDataContext(this.exportButton));

			if (project != null) {
				// First, apply the current settings to ensure they're saved
				apply();
				// Then export them to the project
				AnnotationSortingProjectService.getInstance(project).exportSettingsToProject();
			}
			else {
				Messages.showErrorDialog("No open project found.", "Export Error");
			}
		});

		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		controlPanel.add(this.addButton);
		controlPanel.add(this.removeButton);
		controlPanel.add(this.resetToDefaultButton);
		controlPanel.add(this.exportButton);

		this.mainPanel = new JPanel(new BorderLayout());
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(new JLabel(
				"<html>Enter annotation names or package patterns (e.g., 'jakarta.persistence.*')<br>Earlier entries have higher priority<br>Drag the %s item to position where unmatched annotations should go</html>"
					.formatted(AnnotationSortingSettings.UNMATCHED_MARKER)),
				BorderLayout.NORTH);

		this.mainPanel.add(topPanel, BorderLayout.NORTH);
		this.mainPanel.add(new JScrollPane(this.annotationList), BorderLayout.CENTER);
		this.mainPanel.add(controlPanel, BorderLayout.SOUTH);

		reset();

		return this.mainPanel;
	}

	@Override
	public boolean isModified() {
		AnnotationSortingSettings settings = AnnotationSortingSettings.getInstance();
		List<String> currentList = getCurrentListItems();

		// Remove the unmatched marker from our working list
		List<String> currentListWithoutMarker = new ArrayList<>(currentList);
		int unmatchedIndex = currentList.indexOf(AnnotationSortingSettings.UNMATCHED_MARKER);
		if (unmatchedIndex >= 0) {
			currentListWithoutMarker.remove(AnnotationSortingSettings.UNMATCHED_MARKER);
		}

		AnnotationSortingSettings.State state = settings.getState();
		if (state == null) {
			return false;
		}
		List<String> savedList = new ArrayList<>(state.annotationOrder);
		int savedUnmatchedPos = state.unmatchedPosition;

		// Check if the annotation list changed (ignoring marker position)
		if (!savedList.equals(currentListWithoutMarker)) {
			return true;
		}

		// Check if the unmatched position changed
		return savedUnmatchedPos != unmatchedIndex;
	}

	private void loadDefaultSettings() {
		this.listModel.clear();

		// Add default items
		for (String item : DEFAULT_ANNOTATION_ORDER) {
			this.listModel.addElement(item);
		}

		// Add unmatched marker at the end
		this.listModel.addElement(AnnotationSortingSettings.UNMATCHED_MARKER);
	}

	private List<String> getCurrentListItems() {
		List<String> items = new ArrayList<>();
		for (int i = 0; i < this.listModel.getSize(); i++) {
			items.add(this.listModel.getElementAt(i));
		}
		return items;
	}

	@Override
	public void apply() {
		AnnotationSortingSettings settings = AnnotationSortingSettings.getInstance();
		List<String> currentList = getCurrentListItems();

		// Find and remove unmatched marker to get clean annotation list
		int unmatchedIndex = currentList.indexOf(AnnotationSortingSettings.UNMATCHED_MARKER);
		List<String> annotationsOnly = new ArrayList<>(currentList);
		if (unmatchedIndex >= 0) {
			annotationsOnly.remove(AnnotationSortingSettings.UNMATCHED_MARKER);
		}

		// Save both the clean list and the position
		AnnotationSortingSettings.State state = settings.getState();
		if (state != null) {
			state.annotationOrder = annotationsOnly;
			state.unmatchedPosition = unmatchedIndex;
		}
	}

	@Override
	public void reset() {
		this.listModel.clear();

		AnnotationSortingSettings settings = AnnotationSortingSettings.getInstance();
		AnnotationSortingSettings.State state = settings.getState();
		if (state == null) {
			return;
		}
		List<String> stored = state.annotationOrder;
		int unmatchedPos = state.unmatchedPosition;

		if (stored.isEmpty()) {
			// Use default settings
			stored = new ArrayList<>(DEFAULT_ANNOTATION_ORDER);
			unmatchedPos = stored.size(); // Default to end
		}

		populateListModel(stored, unmatchedPos);
	}

	private void populateListModel(List<String> annotations, int unmatchedPos) {
		// Add items and place unmatched marker at the specified position
		if ((unmatchedPos == -1) || (unmatchedPos >= annotations.size())) {
			// Add all items first, then unmatched marker
			for (String item : annotations) {
				this.listModel.addElement(item);
			}
			this.listModel.addElement(AnnotationSortingSettings.UNMATCHED_MARKER);
		}
		else if (unmatchedPos == 0) {
			// Unmatched marker first, then all items
			this.listModel.addElement(AnnotationSortingSettings.UNMATCHED_MARKER);
			for (String item : annotations) {
				this.listModel.addElement(item);
			}
		}
		else {
			// Unmatched marker in the middle
			for (int i = 0; i < unmatchedPos; i++) {
				this.listModel.addElement(annotations.get(i));
			}
			this.listModel.addElement(AnnotationSortingSettings.UNMATCHED_MARKER);
			for (int i = unmatchedPos; i < annotations.size(); i++) {
				this.listModel.addElement(annotations.get(i));
			}
		}
	}

	@Override
	public void disposeUIResources() {
		this.mainPanel = null;
		this.annotationList = null;
		this.listModel = null;
		this.addButton = null;
		this.removeButton = null;
		this.resetToDefaultButton = null;
		this.exportButton = null;
	}

}
