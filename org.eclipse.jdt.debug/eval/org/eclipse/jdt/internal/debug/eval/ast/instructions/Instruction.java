package org.eclipse.jdt.internal.debug.eval.ast.instructions;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import com.sun.jdi.InvocationException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.debug.core.IJavaClassType;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaInterfaceType;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.internal.debug.eval.ast.engine.IRuntimeContext;
import org.eclipse.jdt.internal.debug.eval.ast.engine.Interpreter;
 
/**
 * Common behavoir for instructions.
 */
public abstract class Instruction {

	private Interpreter fInterpreter;

	public abstract int getSize();
	
	public void setInterpreter(Interpreter interpreter) {
		fInterpreter= interpreter;
	}
	
	public void setLastValue(IJavaValue value) {
		fInterpreter.setLastValue(value);
	}
	
	public void stop() {
		fInterpreter.stop();
	}
	
	public static int getBinaryPromotionType(int left, int right) {
		return fTypeTable[left][right];
	}	
	public abstract void execute() throws CoreException;
	
	protected IRuntimeContext getContext() {
		return fInterpreter.getContext();
	}
	
	protected IJavaDebugTarget getVM() {
		return getContext().getVM();
	}
	
	/**
	 * Return the internal variable with the given name.
	 * 
	 * @see Interpreter#getInternalVariable(String)
	 */
	protected IVariable getInternalVariable(String name) {
		return fInterpreter.getInternalVariable(name);
	}
	
	/**
	 * Create and return a new internal variable with the given name
	 * and the given type.
	 * 
	 * @see Interpreter#createInternalVariable(String, String)
	 */
	protected IVariable createInternalVariable(String name, String referencTypeName) throws CoreException {
		return fInterpreter.createInternalVariable(name, getType(referencTypeName));
	}
	

	/**
	 * Answers the instance of Class that the given type represents.
	 */
	protected IJavaObject getClassObject(IJavaType type) throws CoreException {
		if (type instanceof IJavaClassType) {
			return ((IJavaClassType)type).getClassObject();
		}
		if (type instanceof IJavaInterfaceType) {
			return ((IJavaInterfaceType)type).getClassObject();
		}
		return null;
	}

	protected void jump(int offset) {
		fInterpreter.jump(offset);
	}
	
	protected void push(Object object) {
		fInterpreter.push(object);
	}
	
	protected Object pop() {
		return fInterpreter.pop();
	}
	
	protected IJavaValue popValue() throws CoreException {
		Object element = fInterpreter.pop();
		if (element instanceof IJavaVariable) {
			return (IJavaValue)((IJavaVariable)element).getValue();
		}
		return (IJavaValue)element;
	}	
	
	protected void pushNewValue(boolean value) {
		fInterpreter.push(newValue(value));
	}

	protected IJavaValue newValue(boolean value) {
		return getVM().newValue(value);
	}

	protected void pushNewValue(byte value) {
		fInterpreter.push(newValue(value));
	}

	protected IJavaValue newValue(byte value) {
		return getVM().newValue(value);
	}

	protected void pushNewValue(short value) {
		fInterpreter.push(newValue(value));
	}

	protected IJavaValue newValue(short value) {
		return getVM().newValue(value);
	}

	protected void pushNewValue(int value) {
		fInterpreter.push(newValue(value));
	}

	protected IJavaValue newValue(int value) {
		return getVM().newValue(value);
	}

	protected void pushNewValue(long value) {
		fInterpreter.push(newValue(value));
	}

	protected IJavaValue newValue(long value) {
		return getVM().newValue(value);
	}

	protected void pushNewValue(char value) {
		fInterpreter.push(newValue(value));
	}

	protected IJavaValue newValue(char value) {
		return getVM().newValue(value);
	}

	protected void pushNewValue(float value) {
		fInterpreter.push(newValue(value));
	}

	protected IJavaValue newValue(float value) {
		return getVM().newValue(value);
	}

	protected void pushNewValue(double value) {
		fInterpreter.push(newValue(value));
	}

	protected IJavaValue newValue(double value) {
		return getVM().newValue(value);
	}

	protected void pushNewValue(String value) {
		fInterpreter.push(newValue(value));
	}

	protected IJavaValue newValue(String value) {
		return getVM().newValue(value);
	}

	protected void pushNullValue() {
		fInterpreter.push(nullValue());
	}

	protected IJavaValue nullValue() {
		return getVM().nullValue();
	}

	public static int getUnaryPromotionType(int typeId) {
		return fTypeTable[typeId][T_int];
	}

	protected IJavaType getType(String qualifiedName) throws CoreException {
		// Force the class to be loaded, and record the class reference
		// for later use if there are multiple classes with the same name.
		IJavaObject classReference= classForName(qualifiedName);
		IJavaType[] types= getVM().getJavaTypes(qualifiedName);
		checkTypes(types);
		if (types.length == 1) {
			// Found only one class.
			return types[0];
		} else {
			// Found many classes, look for the right one for this scope.
			if (classReference == null) {
				throw new CoreException(null); // could not resolve type
			}
			for(int i= 0, length= types.length; i < length; i++) {
				IJavaType type= types[i];
				if (classReference.equals(getClassObject(type))) {
					return type;
				}
			}

			// At this point a very strange thing has happened,
			// the VM was able to return multiple types in the classesByName
			// call, but none of them were the class that was returned in
			// the classForName call.

			throw new CoreException(null);
		}
	}


	protected IJavaObject classForName(String qualifiedName) throws CoreException {
		IJavaType[] types= getVM().getJavaTypes(CLASS);
		checkTypes(types);
		if (types.length != 1) {
			throw new CoreException(null);
		}
		IJavaType receiver= types[0];
		IJavaValue[] args = new IJavaValue[] {newValue(qualifiedName)};
		try {
			return (IJavaObject)((IJavaClassType)receiver).sendMessage(FOR_NAME, FOR_NAME_SIGNATURE, args, getContext().getThread());
		} catch (CoreException e) {
			if (e.getStatus().getException() instanceof InvocationException) {
				// Don't throw ClassNotFoundException
				if (((InvocationException)e.getStatus().getException()).exception().referenceType().name().equals("java.lang.ClassNotFoundException")) {
					return null;
				}
			}
			throw e;
		}
	}


	protected void checkTypes(IJavaType[] types) throws CoreException {
		if (types == null || types.length == 0) {
			throw new CoreException(null); // unable to resolve type
		}
	}


	static public final int T_undefined =0;
	static public final int T_Object =1;
	static public final int T_char =2;
	static public final int T_byte =3;
	static public final int T_short =4;
	static public final int T_boolean =5;
	static public final int T_void =6;
	static public final int T_long =7;
	static public final int T_double =8;
	static public final int T_float =9;
	static public final int T_int =10;
	static public final int T_String =11;
	static public final int T_null =12;
	
	private static final int[][] fTypeTable= {
/* undefined */	{T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined},
/* object */	{T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_String, T_undefined},
/* char */		{T_undefined, T_undefined, T_int, T_int, T_int, T_undefined, T_undefined, T_long, T_double, T_float, T_int, T_String, T_undefined},
/* byte */		{T_undefined, T_undefined, T_int, T_int, T_int, T_undefined, T_undefined, T_long, T_double, T_float, T_int, T_String, T_undefined},
/* short */		{T_undefined, T_undefined, T_int, T_int, T_int, T_undefined, T_undefined, T_long, T_double, T_float, T_int, T_String, T_undefined},
/* boolean */	{T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_boolean, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_String, T_undefined},
/* void */		{T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined},
/* long */		{T_undefined, T_undefined, T_long, T_long, T_long, T_undefined, T_undefined, T_long, T_double, T_float, T_long, T_String, T_undefined},
/* double */	{T_undefined, T_undefined, T_double, T_double, T_double, T_undefined, T_undefined, T_double, T_double, T_double, T_double, T_String, T_undefined},
/* float */		{T_undefined, T_undefined, T_float, T_float, T_float, T_undefined, T_undefined, T_float, T_double, T_float, T_float, T_String, T_undefined},
/* int */		{T_undefined, T_undefined, T_int, T_int, T_int, T_undefined, T_undefined, T_long, T_double, T_float, T_int, T_String, T_undefined},
/* String */	{T_undefined, T_String, T_String, T_String, T_String, T_String, T_undefined, T_String, T_String, T_String, T_String, T_String, T_String},
/* null */		{T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_undefined, T_String, T_undefined},
	};

	public static final String CLASS= "java.lang.Class"; //$NON-NLS-1$
	
	public static final String FOR_NAME= "forName"; //$NON-NLS-1$


	public static final String FOR_NAME_SIGNATURE= "(Ljava/lang/String;)Ljava/lang/Class;"; //$NON-NLS-1$

}

