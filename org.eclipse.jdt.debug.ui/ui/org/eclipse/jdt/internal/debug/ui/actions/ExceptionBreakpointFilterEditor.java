package org.eclipse.jdt.internal.debug.ui.actions;

/*
 * (c) Copyright IBM Corp. 2002.
 * All Rights Reserved.
 */ 
 
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.debug.core.IJavaExceptionBreakpoint;
import org.eclipse.jdt.internal.debug.ui.ExceptionHandler;
import org.eclipse.jdt.internal.debug.ui.Filter;
import org.eclipse.jdt.internal.debug.ui.FilterLabelProvider;
import org.eclipse.jdt.internal.debug.ui.FilterViewerSorter;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;

public class ExceptionBreakpointFilterEditor extends FieldEditor {

	private IJavaExceptionBreakpoint fBreakpoint;
	private Button fAddFilterButton;
	private Button fAddPackageButton;
	private Button fAddTypeButton;
	private Button fRemoveFilterButton;
	private Text fEditorText;
	private String fInvalidEditorText= null;
	private TableEditor fTableEditor;
	private TableItem fNewTableItem;
	private Filter fNewFilter;
	private Label fTableLabel;
	private TableViewer fFilterViewer;
	private Table fFilterTable;
	private Composite fOuter;
	private Label fIncludeExcludeLabel;
	private Button fInclusiveRadioButton;
	private Button fExclusiveRadioButton;
	
	private FilterContentProvider fFilterContentProvider;
	
	protected static final String DEFAULT_PACKAGE= "(default package)"; //$NON-NLS-1$
	
	/**
	 * Content provider for the table.  Content consists of instances of Filter.
	 */	
	protected class FilterContentProvider implements IStructuredContentProvider {
		
		private TableViewer fViewer;
		private List fFilters;
		
		public FilterContentProvider(TableViewer viewer) {
			fViewer = viewer;
			populateFilters();
		}
		
		protected void populateFilters() {
			String[] filters= null;
			try {
				filters = fBreakpoint.getFilters();
			} catch (CoreException ce) {
				JDIDebugUIPlugin.log(ce);
				filters= new String[]{};
			}
			fFilters= new ArrayList(1);
			for (int i = 0; i < filters.length; i++) {
				String name = filters[i];
				if (name.length() == 0) {
					name= DEFAULT_PACKAGE;
				}
				addFilter(name);
			}
		}
		
		public Filter addFilter(String name) {
			Filter filter = new Filter(name, false);
			if (!fFilters.contains(filter)) {
				fFilters.add(filter);
				fViewer.add(filter);
			}
			return filter;
		}
		
		
		public void removeFilters(Object[] filters) {
			for (int i = 0; i < filters.length; i++) {
				Filter filter = (Filter)filters[i];
				fFilters.remove(filter);
			}
			fViewer.remove(filters);
		}
		
		/**
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object inputElement) {
			return fFilters.toArray();
		}
		
		/**
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		
		/**
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
		}		
	}
	
	public ExceptionBreakpointFilterEditor(Composite parent, IJavaExceptionBreakpoint breakpoint) {
		fBreakpoint= breakpoint;
		init(JavaBreakpointPreferenceStore.EXCEPTION_FILTER, ActionMessages.getString("ExceptionBreakpointFilterEditor.Re&strict_to_Selected_Location(s)__1")); //$NON-NLS-1$
		createControl(parent);
	}
	
	/**
	 * @see FieldEditor#adjustForNumColumns(int)
	 */
	protected void adjustForNumColumns(int numColumns) {
		((GridData) fOuter.getLayoutData()).horizontalSpan = numColumns;
	}

	/**
	 * @see FieldEditor#doFillIntoGrid(Composite, int)
	 */
	protected void doFillIntoGrid(Composite parent, int numColumns) {	
		// top level container
		fOuter = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		
		fOuter.setLayout(layout);
		GridData gd = new GridData();
		gd.verticalAlignment = GridData.FILL;
		gd.horizontalAlignment = GridData.FILL;
		fOuter.setLayoutData(gd);
		
		gd = new GridData();
		gd.horizontalSpan = numColumns;
		getLabelControl(fOuter).setLayoutData(gd);
		
		// filter table
		fFilterTable= new Table(fOuter, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		
		TableLayout tableLayout= new TableLayout();
		ColumnLayoutData[] columnLayoutData= new ColumnLayoutData[1];
		columnLayoutData[0]= new ColumnWeightData(100);		
		tableLayout.addColumnData(columnLayoutData[0]);
		fFilterTable.setLayout(tableLayout);
		new TableColumn(fFilterTable, SWT.NONE);

		fFilterViewer = new TableViewer(fFilterTable);
		fTableEditor = new TableEditor(fFilterTable);
		fFilterViewer.setLabelProvider(new FilterLabelProvider());
		fFilterViewer.setSorter(new FilterViewerSorter());
		fFilterContentProvider = new FilterContentProvider(fFilterViewer);
		fFilterViewer.setContentProvider(fFilterContentProvider);
		// input just needs to be non-null
		fFilterViewer.setInput(this);
		gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		gd.widthHint = 100;
		fFilterViewer.getTable().setLayoutData(gd);
		fFilterViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection.isEmpty()) {
					fRemoveFilterButton.setEnabled(false);
				} else {
					fRemoveFilterButton.setEnabled(true);					
				}
			}
		});		
		
		createFilterButtons(fOuter);
		createIncludeExcludeRadioButtons(fOuter);
	}

	private void createFilterButtons(Composite container) {
		// button container
		Composite buttonContainer = new Composite(container, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		GridLayout buttonLayout = new GridLayout();
		buttonLayout.numColumns = 1;
		buttonLayout.marginHeight = 0;
		buttonLayout.marginWidth = 0;
		buttonContainer.setLayout(buttonLayout);
		
		// Add filter button
		fAddFilterButton = new Button(buttonContainer, SWT.PUSH);
		fAddFilterButton.setText(ActionMessages.getString("ExceptionBreakpointFilterEditor.&Add_2")); //$NON-NLS-1$
		fAddFilterButton.setToolTipText(ActionMessages.getString("ExceptionBreakpointFilterEditor.Key_in_the_name_of_a_new_scope_expression_3")); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		fAddFilterButton.setLayoutData(gd);
		fAddFilterButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent se) {
				editFilter();
			}
			public void widgetDefaultSelected(SelectionEvent se) {
			}
		});
		
		// Add type button
		fAddTypeButton = new Button(buttonContainer, SWT.PUSH);
		fAddTypeButton.setText(ActionMessages.getString("ExceptionBreakpointFilterEditor.Add_&Class_4")); //$NON-NLS-1$
		fAddTypeButton.setToolTipText(ActionMessages.getString("ExceptionBreakpointFilterEditor.Choose_a_Java_Class_to_Add_It_to_the_Breakpoint_Scope_5")); //$NON-NLS-1$
		gd = getButtonGridData(fAddTypeButton);
		fAddTypeButton.setLayoutData(gd);
		fAddTypeButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent se) {
				addType();
			}
			public void widgetDefaultSelected(SelectionEvent se) {
			}
		});
		
		// Add package button
		fAddPackageButton = new Button(buttonContainer, SWT.PUSH);
		fAddPackageButton.setText(ActionMessages.getString("ExceptionBreakpointFilterEditor.Add_&Package_6")); //$NON-NLS-1$
		fAddPackageButton.setToolTipText(ActionMessages.getString("ExceptionBreakpointFilterEditor.Choose_a_Package_to_Add_to_the_Breakpoint_Scope_7")); //$NON-NLS-1$
		gd = getButtonGridData(fAddPackageButton);
		fAddPackageButton.setLayoutData(gd);
		fAddPackageButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent se) {
				addPackage();
			}
			public void widgetDefaultSelected(SelectionEvent se) {
			}
		});
		
		// Remove button
		fRemoveFilterButton = new Button(buttonContainer, SWT.PUSH);
		fRemoveFilterButton.setText(ActionMessages.getString("ExceptionBreakpointFilterEditor.&Remove_8")); //$NON-NLS-1$
		fRemoveFilterButton.setToolTipText(ActionMessages.getString("ExceptionBreakpointFilterEditor.Remove_all_selected_scoping_elements_9")); //$NON-NLS-1$
		gd = getButtonGridData(fRemoveFilterButton);
		fRemoveFilterButton.setLayoutData(gd);
		fRemoveFilterButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent se) {
				removeFilters();
			}
			public void widgetDefaultSelected(SelectionEvent se) {
			}
		});
		fRemoveFilterButton.setEnabled(false);
		
	}
	
	private void createIncludeExcludeRadioButtons(Composite container) {
		Composite comp = new Composite(container, SWT.NONE);
		GridLayout compLayout = new GridLayout();
		compLayout.marginHeight = 0;
		compLayout.marginWidth = 0;
		comp.setLayout(compLayout);
		
		fIncludeExcludeLabel = new Label(comp, SWT.NONE);
		fIncludeExcludeLabel.setText(ActionMessages.getString("ExceptionBreakpointFilterEditor.Selected_location_semantics_1"));  //$NON-NLS-1$
		
		Composite radioComp = new Composite(comp, SWT.NONE);
		GridLayout radioLayout = new GridLayout();
		radioLayout.marginHeight = 0;
		radioLayout.marginWidth = 0;
		radioComp.setLayout(radioLayout);
		GridData gd = new GridData();
		gd.horizontalIndent = HORIZONTAL_GAP;
		radioComp.setLayoutData(gd);

		fInclusiveRadioButton = new Button(radioComp, SWT.RADIO);
		fInclusiveRadioButton.setText(ActionMessages.getString("ExceptionBreakpointFilterEditor.I&nclusive_(only_stop_in_specified_locations)_2")); //$NON-NLS-1$
		fExclusiveRadioButton = new Button(radioComp, SWT.RADIO);
		fExclusiveRadioButton.setText(ActionMessages.getString("ExceptionBreakpointFilterEditor.E&xclusive_(only_stop_outside_of_specified_locations)_3"));  //$NON-NLS-1$
			
		try {
			if (!fBreakpoint.isInclusiveFiltered()) {
				fExclusiveRadioButton.setSelection(true);
			} else {
				fInclusiveRadioButton.setSelection(true);			
			}			
		} catch (CoreException ce) {
			fInclusiveRadioButton.setSelection(true);
		}
	}
	
	private GridData getButtonGridData(Button button) {
		GridData gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		GC gc = new GC(button);
		gc.setFont(button.getFont());
		FontMetrics fontMetrics= gc.getFontMetrics();
		gc.dispose();
		int widthHint= Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.BUTTON_WIDTH);
		gd.widthHint= Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		
		gd.heightHint= Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.BUTTON_HEIGHT);
		return gd;
	}
	
	/**
	 * Create a new filter in the table (with the default 'new filter' value),
	 * then open up an in-place editor on it.
	 */
	private void editFilter() {
		// if a previous edit is still in progress, finish it
		if (fEditorText != null) {
			validateChangeAndCleanup();
		}
		
		fNewFilter = fFilterContentProvider.addFilter("");		 //$NON-NLS-1$
		fNewTableItem = fFilterTable.getItem(0);
		
		// create & configure Text widget for editor
		// Fix for bug 1766.  Border behavior on Windows & Linux for text
		// fields is different.  On Linux, you always get a border, on Windows,
		// you don't.  Specifying a border on Linux results in the characters
		// getting pushed down so that only there very tops are visible.  Thus,
		// we have to specify different style constants for the different platforms.
		int textStyles = SWT.SINGLE | SWT.LEFT;
		if (SWT.getPlatform().equals("win32")) {  //$NON-NLS-1$
			textStyles |= SWT.BORDER;
		}
		fEditorText = new Text(fFilterTable, textStyles);
		GridData gd = new GridData(GridData.FILL_BOTH);
		fEditorText.setLayoutData(gd);
		
		// set the editor
		fTableEditor.horizontalAlignment = SWT.LEFT;
		fTableEditor.grabHorizontal = true;
		fTableEditor.setEditor(fEditorText, fNewTableItem, 0);
		
		// get the editor ready to use
		fEditorText.setText(fNewFilter.getName());
		fEditorText.selectAll();
		setEditorListeners(fEditorText);
		fEditorText.setFocus();
	}
	
	private void setEditorListeners(Text text) {
		// CR means commit the changes, ESC means abort and don't commit
		text.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {				
				if (event.character == SWT.CR) {
					if (fInvalidEditorText != null) {
						fEditorText.setText(fInvalidEditorText);
						fInvalidEditorText= null;
					} else {
						validateChangeAndCleanup();
					}
				} else if (event.character == SWT.ESC) {
					removeNewFilter();
					cleanupEditor();
				}
			}
		});
		// Consider loss of focus on the editor to mean the same as CR
		text.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent event) {
				if (fInvalidEditorText != null) {
					fEditorText.setText(fInvalidEditorText);
					fInvalidEditorText= null;
				} else {
					validateChangeAndCleanup();
				}
			}
		});
		// Consume traversal events from the text widget so that CR doesn't 
		// traverse away to dialog's default button.  Without this, hitting
		// CR in the text field closes the entire dialog.
		text.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event event) {
				event.doit = false;
			}
		});
	}
	
	private void validateChangeAndCleanup() {
		String trimmedValue = fEditorText.getText().trim();
		// if the new value is blank, remove the filter
		if (trimmedValue.length() < 1) {
			removeNewFilter();
		}
		// if it's invalid, beep and leave sitting in the editor
		else if (!validateEditorInput(trimmedValue)) {
			fInvalidEditorText= trimmedValue;
			fEditorText.setText(ActionMessages.getString("ExceptionBreakpointFilterEditor.Invalid_expression._Return_to_continue;_escape_to_exit_11")); //$NON-NLS-1$
			fEditorText.getDisplay().beep();			
			return;
		// otherwise, commit the new value if not a duplicate
		} else {		
			Object[] filters= fFilterContentProvider.getElements(null);
			for (int i = 0; i < filters.length; i++) {
				Filter filter = (Filter)filters[i];
				if (filter.getName().equals(trimmedValue)) {
					removeNewFilter();
					cleanupEditor();
					return;
				}	
			}
			fNewTableItem.setText(trimmedValue);
			fNewFilter.setName(trimmedValue);
			fFilterViewer.refresh();
		}
		cleanupEditor();
	}
	
	/**
	 * A valid filter is simply one that is a valid Java identifier.
	 * and, as defined in the JDI spec, the regular expressions used for
	 * scoping must be limited to exact matches or patterns that
	 * begin with '*' or end with '*'. Beyond this, a string cannot be validated
	 * as corresponding to an existing type or package (and this is probably not
	 * even desirable).  
	 */
	private boolean validateEditorInput(String trimmedValue) {
		char firstChar= trimmedValue.charAt(0);
		if (!Character.isJavaIdentifierStart(firstChar)) {
			if (!(firstChar == '*')) {
				return false;
			}
		}
		int length= trimmedValue.length();
		for (int i= 1; i < length; i++) {
			char c= trimmedValue.charAt(i);
			if (!Character.isJavaIdentifierPart(c)) {
				if (c == '.' && i != (length - 1)) {
					continue;
				}
				if (c == '*' && i == (length - 1)) {
					continue;
				}
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Cleanup all widgetry & resources used by the in-place editing
	 */
	private void cleanupEditor() {
		if (fEditorText != null) {
			fNewFilter = null;
			fNewTableItem = null;
			fTableEditor.setEditor(null, null, 0);			
			fEditorText.getDisplay().asyncExec(new Runnable() {
				public void run() {
					fEditorText.dispose();
					fEditorText = null;
				}
			});		
		}
	}
	
	private void removeFilters() {
		IStructuredSelection selection = (IStructuredSelection)fFilterViewer.getSelection();		
		fFilterContentProvider.removeFilters(selection.toArray());
	}
	
	private void removeNewFilter() {
		fFilterContentProvider.removeFilters(new Object[] {fNewFilter});
	}
	
	private void addPackage() {
		Shell shell= fAddPackageButton.getDisplay().getActiveShell();
		ElementListSelectionDialog dialog = null;
		try {
			dialog = JDIDebugUIPlugin.createAllPackagesDialog(shell, null, true);
		} catch (JavaModelException jme) {
			String title= ActionMessages.getString("ExceptionBreakpointFilterEditor.Add_Package_to_Scope_14"); //$NON-NLS-1$
			String message= ActionMessages.getString("ExceptionBreakpointFilterEditor.Could_not_open_package_selection_dialog_for_scope_definition_13");  //$NON-NLS-1$
			ExceptionHandler.handle(jme, title, message);
			return;			
		}
	
		dialog.setTitle(ActionMessages.getString("ExceptionBreakpointFilterEditor.Add_Package_to_Scope_14"));  //$NON-NLS-1$
		dialog.setMessage(ActionMessages.getString("ExceptionBreakpointFilterEditor.&Select_a_package_to_add_to_the_scope_of_the_breakpoint_15")); //$NON-NLS-1$
		dialog.setMultipleSelection(true);
		if (dialog.open() == IDialogConstants.CANCEL_ID) {
			return;
		}
		Object[] packages= dialog.getResult();
		if (packages != null) {
			for (int i = 0; i < packages.length; i++) {
				IJavaElement pkg = (IJavaElement)packages[i];
				String filter = pkg.getElementName();
				if (filter.length() < 1) {
					filter = DEFAULT_PACKAGE;  
				} else {
					filter += ".*"; //$NON-NLS-1$
				}
				fFilterContentProvider.addFilter(filter);
			}
		}		
	}
				
	private void addType() {
		Shell shell= fAddTypeButton.getDisplay().getActiveShell();
		SelectionDialog dialog= null;
		try {
			dialog= JavaUI.createTypeDialog(shell, new ProgressMonitorDialog(shell),
				SearchEngine.createWorkspaceScope(), IJavaElementSearchConstants.CONSIDER_CLASSES, false);
		} catch (JavaModelException jme) {
			String title= ActionMessages.getString("ExceptionBreakpointFilterEditor.Add_Class_to_Scope_17"); //$NON-NLS-1$
			String message= ActionMessages.getString("ExceptionBreakpointFilterEditor.Could_not_open_class_selection_dialog_for_scope_definition_18"); //$NON-NLS-1$
			ExceptionHandler.handle(jme, title, message);
			return;
		}
	
		dialog.setTitle(ActionMessages.getString("ExceptionBreakpointFilterEditor.Add_Class_to_Scope_17")); //$NON-NLS-1$
		dialog.setMessage(ActionMessages.getString("ExceptionBreakpointFilterEditor.&Select_a_class_to_add_to_the_scope_of_the_breakpoint_20")); //$NON-NLS-1$
		if (dialog.open() == IDialogConstants.CANCEL_ID) {
			return;
		}
		
		Object[] types= dialog.getResult();
		if (types != null) {
			for (int i = 0; i < types.length; i++) {
				IType type = (IType)types[i];
				fFilterContentProvider.addFilter(type.getFullyQualifiedName());
			}
		}		
	}
	
	/**
	 * Creates composite control and sets the default layout data.
	 *
	 * @param parent  the parent of the new composite
	 * @param numColumns  the number of columns for the new composite
	 * @param labelText  the text label of the new composite
	 * @return the newly-created composite
	 */
	private Composite createLabelledComposite(Composite parent, int numColumns, String labelText) {
		Composite comp = new Composite(parent, SWT.NONE);
		
		//GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		comp.setLayout(layout);

		//GridData
		GridData gd= new GridData();
		gd.verticalAlignment = GridData.FILL;
		gd.horizontalAlignment = GridData.FILL;
		comp.setLayoutData(gd);
		
		//Label
		Label label = new Label(comp, SWT.NONE);
		label.setText(labelText);
		gd = new GridData();
		gd.horizontalSpan = numColumns;
		label.setLayoutData(gd);

		return comp;
	}
	
	/**
	 * @see FieldEditor#doLoad()
	 */
	protected void doLoad() {
	}

	/**
	 * @see FieldEditor#doLoadDefault()
	 */
	protected void doLoadDefault() {
	}

	/**
	 * @see FieldEditor#doStore()
	 */
	protected void doStore() {
		Object[] filters= fFilterContentProvider.getElements(null);
		String[] stringFilters= new String[filters.length];
		for (int i = 0; i < filters.length; i++) {
			Filter filter = (Filter)filters[i];
			String name= filter.getName();
			if (name.equals(DEFAULT_PACKAGE)) {
				name= ""; //$NON-NLS-1$
			}
			stringFilters[i]= name;
		}
		try {
			fBreakpoint.setFilters(stringFilters, fInclusiveRadioButton.getSelection());
		} catch (CoreException ce) {
			JDIDebugUIPlugin.log(ce);
		}
		
	}

	/**
	 * @see FieldEditor#getNumberOfControls()
	 */
	public int getNumberOfControls() {
		return 2;
	}
}
