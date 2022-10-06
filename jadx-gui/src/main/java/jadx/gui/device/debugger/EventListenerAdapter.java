package jadx.gui.device.debugger;

import io.github.skylot.jdwp.JDWP.Event.Composite.BreakpointEvent;
import io.github.skylot.jdwp.JDWP.Event.Composite.ClassPrepareEvent;
import io.github.skylot.jdwp.JDWP.Event.Composite.ClassUnloadEvent;
import io.github.skylot.jdwp.JDWP.Event.Composite.ExceptionEvent;
import io.github.skylot.jdwp.JDWP.Event.Composite.FieldAccessEvent;
import io.github.skylot.jdwp.JDWP.Event.Composite.FieldModificationEvent;
import io.github.skylot.jdwp.JDWP.Event.Composite.MethodEntryEvent;
import io.github.skylot.jdwp.JDWP.Event.Composite.MethodExitEvent;
import io.github.skylot.jdwp.JDWP.Event.Composite.MethodExitWithReturnValueEvent;
import io.github.skylot.jdwp.JDWP.Event.Composite.MonitorContendedEnterEvent;
import io.github.skylot.jdwp.JDWP.Event.Composite.MonitorContendedEnteredEvent;
import io.github.skylot.jdwp.JDWP.Event.Composite.MonitorWaitEvent;
import io.github.skylot.jdwp.JDWP.Event.Composite.MonitorWaitedEvent;
import io.github.skylot.jdwp.JDWP.Event.Composite.SingleStepEvent;
import io.github.skylot.jdwp.JDWP.Event.Composite.ThreadDeathEvent;
import io.github.skylot.jdwp.JDWP.Event.Composite.ThreadStartEvent;
import io.github.skylot.jdwp.JDWP.Event.Composite.VMDeathEvent;
import io.github.skylot.jdwp.JDWP.Event.Composite.VMStartEvent;

abstract class EventListenerAdapter {
	void onVMStart(VMStartEvent event) {
	}

	void onVMDeath(VMDeathEvent event) {
	}

	void onSingleStep(SingleStepEvent event) {
	}

	void onBreakpoint(BreakpointEvent event) {
	}

	void onMethodEntry(MethodEntryEvent event) {
	}

	void onMethodExit(MethodExitEvent event) {
	}

	void onMethodExitWithReturnValue(MethodExitWithReturnValueEvent event) {
	}

	void onMonitorContendedEnter(MonitorContendedEnterEvent event) {
	}

	void onMonitorContendedEntered(MonitorContendedEnteredEvent event) {
	}

	void onMonitorWait(MonitorWaitEvent event) {
	}

	void onMonitorWaited(MonitorWaitedEvent event) {
	}

	void onException(ExceptionEvent event) {
	}

	void onThreadStart(ThreadStartEvent event) {
	}

	void onThreadDeath(ThreadDeathEvent event) {
	}

	void onClassPrepare(ClassPrepareEvent event) {
	}

	void onClassUnload(ClassUnloadEvent event) {
	}

	void onFieldAccess(FieldAccessEvent event) {
	}

	void onFieldModification(FieldModificationEvent event) {
	}
}
