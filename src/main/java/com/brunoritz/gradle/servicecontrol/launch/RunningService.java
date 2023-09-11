package com.brunoritz.gradle.servicecontrol.launch;

import com.brunoritz.gradle.servicecontrol.common.PidFile;
import io.vavr.control.Try;

/**
 * Represents a service that has completed startup and can be used. This  class simply provides a method for
 * recording the started service's process ID.
 */
class RunningService
{
	private final Process serviceProcess;
	private final PidFile pidFile;

	RunningService(Process serviceProcess, PidFile pidFile)
	{
		this.serviceProcess = serviceProcess;
		this.pidFile = pidFile;
	}

	/**
	 * Records the running service's PID and returns the file containing the PID.
	 *
	 * @return The file containing the PID or the error that prevented recording the PID
	 */
	Try<Long> recordProcessId()
	{
		return pidFile.recordPid(serviceProcess);
	}
}
