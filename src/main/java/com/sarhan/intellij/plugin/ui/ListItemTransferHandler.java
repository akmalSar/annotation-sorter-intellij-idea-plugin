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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

import com.intellij.openapi.ui.Messages;

/**
 * A custom transfer handler designed to support drag-and-drop operations for a JList
 * component. This handler enables the movement of list items within the same list by
 * creating a transferable object containing the selected value and updating the list's
 * model during the drop operation.
 *
 * @author Akmal Sarhan
 */
public class ListItemTransferHandler extends TransferHandler {

	private int indexFrom = -1;

	@Override
	protected Transferable createTransferable(JComponent c) {
		JList<?> list = (JList<?>) c;
		this.indexFrom = list.getSelectedIndex();
		Object value = list.getSelectedValue();
		return new StringSelection(value.toString());
	}

	@Override
	public int getSourceActions(JComponent c) {
		return MOVE;
	}

	@Override
	public boolean canImport(TransferHandler.TransferSupport info) {
		return info.isDrop() && info.isDataFlavorSupported(DataFlavor.stringFlavor);
	}

	@Override
	public boolean importData(TransferHandler.TransferSupport info) {
		if (!canImport(info)) {
			return false;
		}

		JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
		int indexTo = dl.getIndex();
		boolean insert = dl.isInsert();

		JList<?> list = (JList<?>) info.getComponent();
		DefaultListModel model = (DefaultListModel) list.getModel();

		try {
			String value = (String) info.getTransferable().getTransferData(DataFlavor.stringFlavor);
			if ((this.indexFrom < 0) || (this.indexFrom >= model.getSize())) {
				return false;
			}
			model.remove(this.indexFrom);
			if (indexTo > this.indexFrom) {
				indexTo--; // adjust for remove shift
			}
			model.add(indexTo, value);
			list.setSelectedIndex(indexTo);
			return true;
		}
		catch (UnsupportedFlavorException | IOException exception) {
			Messages.showErrorDialog(list, exception.getMessage(), "Error");
		}

		return false;
	}

}
