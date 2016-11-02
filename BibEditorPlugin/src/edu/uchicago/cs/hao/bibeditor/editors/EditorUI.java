/*******************************************************************************
 * Copyright (c) Oct 2016 Hao Jiang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Hao Jiang - initial API and implementation and/or initial documentation
 *******************************************************************************/

package edu.uchicago.cs.hao.bibeditor.editors;

import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.function.Consumer;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TableColumn;

import edu.uchicago.cs.hao.bibeditor.filemodel.BibEntry;
import edu.uchicago.cs.hao.bibeditor.filemodel.BibModel;
import edu.uchicago.cs.hao.bibeditor.filemodel.BibParser;
import edu.uchicago.cs.hao.bibeditor.filemodel.BibParser.Token;
import edu.uchicago.cs.hao.bibeditor.filemodel.BibParser.TokenType;
import edu.uchicago.cs.hao.bibeditor.filemodel.BibProp;

public class EditorUI implements PropertyChangeListener {

	private BibModel model;

	public BibEntry selected() {
		return (BibEntry) ((StructuredSelection) table.getSelection()).getFirstElement();
	}

	private boolean dirty;

	private PropertyChangeSupport support;

	// Listeners

	private ModifyListener textModifyListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent e) {
			if (selected() != null) {
				// Try parse the content, block the change if syntex incorrect
				String text = editText.getText();
				try {
					BibModel tempmodel = new BibParser().parse(text);
					if (1 != tempmodel.getEntries().size())
						throw new IllegalArgumentException();
					else {
						// Update table info
						BibEntry newEntry = tempmodel.getEntries().get(0);
						// Reset selection
						EditorUI.this.model.replaceEntry(selected(), newEntry);
						table.setSelection(new StructuredSelection(newEntry));
					}
					// Add color to the text
					updateTextContent(editText.getText());
					EditorUI.this.setDirty(true);
					editText.setBackground(null);
				} catch (Exception ex) {
					editText.setBackground(warnBackground);
				}

			}
		}
	};

	// Components
	private TableViewer table;

	private ColumnViewerComparator tableComparator;

	private StyledText editText;

	private MenuManager menuManager;

	// Resources
	private ResourcePool pool = new ResourcePool();

	private Color brown;

	private Color darkBlue;

	private Color magenta;

	private Color warnBackground;

	public MenuManager getMenuManager() {
		return menuManager;
	}

	public TableViewer getTable() {
		return table;
	}

	public EditorUI() {
		super();
		support = new PropertyChangeSupport(this);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		support.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		support.removePropertyChangeListener(listener);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getSource() instanceof BibModel) {

			if ("addEntry".equals(evt.getPropertyName())) {
				// Disable comparator
				table.setComparator(null);
				table.insert(evt.getNewValue(), model.getEntries().size() - 1);
				table.setSelection(new StructuredSelection(evt.getNewValue()));
				updateTextContent(evt.getNewValue().toString());
			}
			if ("removeEntry".equals(evt.getPropertyName())) {
				table.remove(evt.getOldValue());
				if (model.getEntries().size() > 0) {
					BibEntry first = model.getEntries().get(0);
					table.setSelection(new StructuredSelection(first));
					updateTextContent(first.toString());
				} else {
					updateTextContent("");
				}
			}
			if ("entries".equals(evt.getPropertyName())) {
				IndexedPropertyChangeEvent ipc = (IndexedPropertyChangeEvent) evt;
				table.replace(ipc.getNewValue(), ipc.getIndex());
			}
			setDirty(true);
		}
	}

	public void createUI(Composite parent) {
		brown = new Color(Display.getCurrent(), 147, 2, 22);
		darkBlue = new Color(Display.getCurrent(), 6, 45, 107);
		magenta = new Color(Display.getCurrent(), 66, 6, 14);
		warnBackground = new Color(Display.getCurrent(), 255, 188, 196);
		pool.add(brown, darkBlue, magenta, warnBackground);

		// plusIcon = new Image(Display.getCurrent(), "icons/toolbar_plus.gif");
		// minusIcon = new Image(Display.getCurrent(),
		// "icons/toolbar_minus.gif");

		SashForm form = new SashForm(parent, SWT.VERTICAL);
		form.setLayout(new FillLayout());

		createTable(form);

		createEditPanel(form);

		form.setWeights(new int[] { 70, 30 });

		menuManager = new MenuManager();
		Menu menu = menuManager.createContextMenu(table.getTable());
		// set the menu on the SWT widget
		table.getTable().setMenu(menu);
	}

	public void dispose() {
		pool.dispose();

		// plusIcon.dispose();
		// minusIcon.dispose();

		table = null;
		editText = null;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		boolean oldDirty = this.dirty;
		this.dirty = dirty;
		support.firePropertyChange("dirty", oldDirty, dirty);
	}

	public void save(File target) {
		try {
			PrintWriter output = new PrintWriter(new FileOutputStream(target));

			for (BibEntry entry : model.getEntries()) {
				output.println(entry.toString());
			}

			output.close();

			setDirty(false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void createTable(Composite form) {
		Composite tablePanel = new Composite(form, SWT.NONE);
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		tablePanel.setLayout(tableColumnLayout);

		table = new TableViewer(tablePanel, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		table.setContentProvider(new ArrayContentProvider());

		TableColumn keyColumn = new TableColumn(table.getTable(), SWT.NONE);
		keyColumn.setText("cite_key");
		TableViewerColumn keyColumnViewer = new TableViewerColumn(table, keyColumn);
		keyColumnViewer.setLabelProvider(new BibEntryColumnLabelProvider(0));
		keyColumn.addSelectionListener(new ColumnSelectionListener(0));

		TableColumn typeColumn = new TableColumn(table.getTable(), SWT.NONE);
		typeColumn.setText("Type");
		TableViewerColumn typeColumnViewer = new TableViewerColumn(table, typeColumn);
		typeColumnViewer.setLabelProvider(new BibEntryColumnLabelProvider(1));
		typeColumn.addSelectionListener(new ColumnSelectionListener(1));

		TableColumn titleColumn = new TableColumn(table.getTable(), SWT.NONE);
		titleColumn.setText("Title");
		TableViewerColumn titleColumnViewer = new TableViewerColumn(table, titleColumn);
		titleColumnViewer.setLabelProvider(new BibEntryColumnLabelProvider(2));
		titleColumn.addSelectionListener(new ColumnSelectionListener(2));

		TableColumn yearColumn = new TableColumn(table.getTable(), SWT.NONE);
		yearColumn.setText("Year");
		TableViewerColumn yearColumnViewer = new TableViewerColumn(table, yearColumn);
		yearColumnViewer.setLabelProvider(new BibEntryColumnLabelProvider(3));
		yearColumn.addSelectionListener(new ColumnSelectionListener(3));

		TableColumn authorColumn = new TableColumn(table.getTable(), SWT.NONE);
		authorColumn.setText("Author");
		TableViewerColumn authorColumnViewer = new TableViewerColumn(table, authorColumn);
		authorColumnViewer.setLabelProvider(new BibEntryColumnLabelProvider(4));

		tableColumnLayout.setColumnData(keyColumn, new ColumnWeightData(5, 150, true));
		tableColumnLayout.setColumnData(typeColumn, new ColumnWeightData(5, 150, true));
		tableColumnLayout.setColumnData(titleColumn, new ColumnWeightData(50, 400, true));
		tableColumnLayout.setColumnData(yearColumn, new ColumnPixelData(50));
		tableColumnLayout.setColumnData(authorColumn, new ColumnWeightData(40, 400, true));

		if (!model.getEntries().isEmpty())
			table.setInput(model.getEntries());
		table.getTable().setLinesVisible(true);
		table.getTable().setHeaderVisible(true);

		table.getTable().addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				Object selection = ((StructuredSelection) table.getSelection()).getFirstElement();
				if (null != selection) {
					updateTextContent(selection.toString());
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				widgetSelected(event);
			}
		});
		// Supporting Table Sorting, only when clicking
		tableComparator = new ColumnViewerComparator();
		table.setComparator(null);
	}

	private void createEditPanel(Composite parent) {
		Composite editPanel = new Composite(parent, SWT.NONE);
		editPanel.setLayout(new FillLayout());
		editText = new StyledText(editPanel, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);

		editText.addModifyListener(textModifyListener);
	}

	private void updateTextContent(String content) {
		editText.removeModifyListener(textModifyListener);

		try {
			if (!content.equals(editText.getText()))
				editText.setText(content);
			InputStream in = new ByteArrayInputStream(content.getBytes("UTF-8"));
			Iterable<Token> tokens = new BibParser().tokenize(in);
			tokens.forEach(new Consumer<Token>() {
				@Override
				public void accept(Token t) {
					switch (t.getType()) {
					case TokenType.KEY:
						editText.setStyleRange(new StyleRange(t.getFrom(), t.getLength(), brown, null, SWT.BOLD));
						break;
					case TokenType.TYPE:
						editText.setStyleRange(new StyleRange(t.getFrom(), t.getLength(), darkBlue, null, SWT.BOLD));
						break;
					case TokenType.PROP_KEY:
						editText.setStyleRange(new StyleRange(t.getFrom(), t.getLength(), magenta, null, SWT.BOLD));
						break;
					default:
						break;
					}
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			editText.addModifyListener(textModifyListener);
		}

	}

	public BibModel getModel() {
		return model;
	}

	public void setModel(BibModel model) {
		if (this.model != null)
			this.model.removePropertyChangeListener(this);
		this.model = model;
		if (this.model != null)
			this.model.addPropertyChangeListener(this);
	}

	private static class BibEntryColumnLabelProvider extends ColumnLabelProvider {

		private int index;

		public BibEntryColumnLabelProvider(int index) {
			super();
			this.index = index;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof BibEntry) {
				return extract(element, index);
			}
			return super.getText(element);
		}

		static String extract(Object element, int index) {
			BibEntry entry = (BibEntry) element;
			switch (index) {
			case 0:
				return entry.getId();
			case 1:
				return entry.getType();
			case 2:
				return entry.getProperty(BibProp.TITLE);
			case 3:
				return entry.getProperty(BibProp.YEAR);
			case 4:
				return entry.getProperty(BibProp.AUTHOR);
			default:
				return "";
			}
		}
	}

	protected class ColumnViewerComparator extends ViewerComparator {
		private int propertyIndex;
		private static final int DESCENDING = 1;
		private int direction = DESCENDING;

		public ColumnViewerComparator() {
			this.propertyIndex = 0;
			direction = DESCENDING;
		}

		public int getDirection() {
			return direction == 1 ? SWT.DOWN : SWT.UP;
		}

		public void setColumn(int column) {
			if (column == this.propertyIndex) {
				// Same column as last sort; toggle the direction
				direction = 1 - direction;
			} else {
				// New column; do an ascending sort
				this.propertyIndex = column;
				direction = DESCENDING;
			}
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			String data1 = BibEntryColumnLabelProvider.extract(e1, propertyIndex);
			String data2 = BibEntryColumnLabelProvider.extract(e2, propertyIndex);
			if (null == data1)
				data1 = "";
			if (null == data2)
				data2 = "";
			int rc = data1.compareTo(data2);
			// If descending order, flip the direction
			if (direction == DESCENDING) {
				rc = -rc;
			}
			return rc;
		}
	}

	private class ColumnSelectionListener implements SelectionListener {

		int index = -1;

		public ColumnSelectionListener(int index) {
			super();
			this.index = index;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			tableComparator.setColumn(this.index);
			table.setComparator(tableComparator);
			table.refresh();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {

		}
	}
}
