package org.eclipse.jdt.internal.debug.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.HashMap;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.event.*;
import com.sun.jdi.request.EventRequest;

/**
 * Dispatches events generated by a debuggable VM.
 */

class EventDispatcher implements Runnable {
	/**
	 * The debug target associated with this dispatcher.
	 */
	protected JDIDebugTarget fTarget;
	/**
	 * Whether this dispatcher should continue reading events.
	 */
	protected boolean fKeepReading;
	
	protected EventSet fEventSet;
	
	protected EventIterator fIterator;
	
	protected HashMap fEventHandlers;
	
	/**
	 * Creates a new event dispatcher listening for events
	 * originating from the underlying VM.
	 */
	EventDispatcher(JDIDebugTarget process) {
		fEventHandlers = new HashMap(10);
		fTarget= process;
	}

	/**
	 * Dispatch an event set received from the VirtualMachine.
	 */
	protected void dispatch(EventSet eventSet) {
		if (!fKeepReading) {
			return;
		}
		fIterator= eventSet.eventIterator();
		boolean vote = false; 
		boolean resume = true;
		while (fIterator.hasNext()) {
			if (!fKeepReading) {
				return;
			}
			Event event= fIterator.nextEvent();
			if (event == null) {
				continue;
			}
			// The event types are checked in order
			// of their expected frequency, from the most specific type to the more general.
			
			// dispatch to handler if there is one
			IJDIEventListener listener = (IJDIEventListener)fEventHandlers.get(event.request());
			if (listener != null) {
				vote = true;
				resume = listener.handleEvent(event, fTarget) && resume;
				continue;
			}
			
			if (event instanceof StepEvent) {
				dispatchStepEvent((StepEvent)event);
			} else
				if (event instanceof ThreadStartEvent) {
					fTarget.handleThreadStart((ThreadStartEvent) event);
				} else
					if (event instanceof ThreadDeathEvent) {
						fTarget.handleThreadDeath((ThreadDeathEvent) event);
					} else
						if (event instanceof ClassPrepareEvent) {
							vote = true;
							fTarget.handleClassLoad((ClassPrepareEvent) event);
						} else
							if (event instanceof VMDeathEvent) {
								fTarget.handleVMDeath((VMDeathEvent) event);
								fKeepReading= false; // stop listening for events
							} else
								if (event instanceof VMDisconnectEvent) {
									fTarget.handleVMDisconnect((VMDisconnectEvent) event);
									fKeepReading= false; // stop listening for events
								} else if (event instanceof VMStartEvent) {
									fTarget.handleVMStart((VMStartEvent)event);
								} else {
									// Unknown Event Type
								}
		}
		if (vote && resume) {
			eventSet.resume();
		}
	}
	
	protected void dispatchStepEvent(StepEvent event) {
		ThreadReference threadRef= event.thread();
		JDIThread thread= findThread(threadRef);
		if (thread == null) {
			fTarget.resume(threadRef);
			return;
		} else {
			thread.handleStep(event);
		}
	}

	/**
	 * Convenience method for finding the model thread for 
	 * an underlying thread reference.
	 */
	protected JDIThread findThread(ThreadReference threadReference) {
		return fTarget.findThread(threadReference);
	}

	/**
	 * Continuously reads events that are coming from the event queue.
	 */
	public void run() {
		EventQueue q= fTarget.fVirtualMachine.eventQueue();
		fKeepReading= true;
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) {
					dispatch(fEventSet);	
				}
			};
			
		while (fKeepReading) {
			try {
				try {
					// Get the next event set.
					fEventSet= q.remove();
					if (fEventSet == null)
						break;
				} catch (VMDisconnectedException e) {
					break;
				}
								
				if(fKeepReading) {
					try {
						workspace.run(runnable, null);
					} catch (CoreException e) {
						JDIDebugPlugin.logError(e);
						break;
					}
				}
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	/**
	 * Shutdown the event dispatcher...stops
	 * reading and dispatching events from the event queue.	
	 */
	protected void shutdown() {
		fKeepReading= false;
	}
	
	protected boolean hasPendingEvents() {
		return fIterator.hasNext();
	}
	
	public void addJDIEventListener(IJDIEventListener listener, EventRequest request) {
		fEventHandlers.put(request, listener);
	}
	
	public void removeJDIEventListener(IJDIEventListener listener, EventRequest request) {
		fEventHandlers.remove(request);
	}
	
}

