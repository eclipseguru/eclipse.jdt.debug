package org.eclipse.jdi.internal;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import com.sun.jdi.Value;
import com.sun.jdi.VoidType;

/**
 * this class implements the corresponding interfaces
 * declared by the JDI specification. See the com.sun.jdi package
 * for more information.
 *
 */
public class VoidTypeImpl extends TypeImpl implements VoidType {
	/**
	 * Creates new instance.
	 */
	public VoidTypeImpl(VirtualMachineImpl vmImpl) {
		super("VoidType", vmImpl, "void" , "V");
	}

	/**
	 * @return Returns text representation of this type.
	 */
	public String name() {
		return fName;
	}
	
	/**
	 * @return JNI-style signature for this type.
	 */
	public String signature() {
		return fSignature;
	}

	/**
	 * @return Returns modifier bits.
	 */
	public int modifiers() {
		throw new InternalError("A VoidType does not have modifiers.");
	}
	
	/**
	 * @return Create a null value instance of the type.
	 */
	public Value createNullValue() {
		return new VoidValueImpl(virtualMachineImpl());
	}
}
