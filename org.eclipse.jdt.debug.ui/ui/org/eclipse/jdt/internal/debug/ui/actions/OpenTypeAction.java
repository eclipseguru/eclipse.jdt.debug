package org.eclipse.jdt.internal.debug.ui.actions;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Iterator;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;

public abstract class OpenTypeAction extends ObjectActionDelegate {
	
	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		IStructuredSelection selection= getCurrentSelection();
		if (selection == null) {
			return;
		}
		Iterator enum= selection.iterator();
		try {
			while (enum.hasNext()) {
				Object element= enum.next();
				doAction(element);
			}
		} catch(DebugException e) {
			JDIDebugUIPlugin.log(e);
		}
	}
	
	protected abstract IDebugElement getDebugElement(IAdaptable element);
	
	protected abstract String getTypeNameToOpen(IDebugElement element) throws DebugException;
	
	protected void doAction(Object e) throws DebugException {
		IAdaptable element= (IAdaptable) e;
		IDebugElement dbgElement= getDebugElement(element);
		if (dbgElement != null) {
			String typeName= getTypeNameToOpen(dbgElement);
			try {
				IType t= findTypeInWorkspace(typeName);
				if (t != null) {
					IEditorPart part= EditorUtility.openInEditor(t);
					if (part != null)
						EditorUtility.revealInEditor(part, t);
				}
			} catch (CoreException x) {
				JDIDebugUIPlugin.log(x);
			}
		}
	}
	
	private IType findTypeInWorkspace(String typeName) throws JavaModelException {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IJavaProject[] projects= JavaCore.create(root).getJavaProjects();
		for (int i= 0; i < projects.length; i++) {
			IType type= findType(projects[i], typeName);
			if (type != null) {
				return type;
			}
		}
		return null;
	}
	
	/** 
	 * Finds a type by its qualified type name (dot separated).
	 * @param jproject The Java project to search in
	 * @param fullyQualifiedName The fully qualified name (type name with enclosing type names and package (all separated by dots))
	 * @return The type found, or <code>null<code> if no type found
	 * The method does not find inner types. Waiting for a Java Core solution
	 */	
	private IType findType(IJavaProject jproject, String fullyQualifiedName) throws JavaModelException {
		String pathStr= fullyQualifiedName.replace('.', '/') + ".java"; //$NON-NLS-1$
		IJavaElement jelement= jproject.findElement(new Path(pathStr));
		if (jelement == null) {
			// try to find it as inner type
			String qualifier= Signature.getQualifier(fullyQualifiedName);
			if (qualifier.length() > 0) {
				IType type= findType(jproject, qualifier); // recursive!
				if (type != null) {
					IType res= type.getType(Signature.getSimpleName(fullyQualifiedName));
					if (res.exists()) {
						return res;
					}
				}
			}
		} else if (jelement.getElementType() == IJavaElement.COMPILATION_UNIT) {
			String simpleName= Signature.getSimpleName(fullyQualifiedName);
			return ((ICompilationUnit) jelement).getType(simpleName);
		} else if (jelement.getElementType() == IJavaElement.CLASS_FILE) {
			return ((IClassFile) jelement).getType();
		}
		return null;
	}
}
