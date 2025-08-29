package com.brunoritz.gradle.servicecontrol.launch;

import com.brunoritz.gradle.servicecontrol.common.PidFile;

/**
 * A service that failed to expose the configured TCP port within the allowed amount of time. This class provides a
 * method to clean up any state being kept about the service.
 */
class FailedService
{
	private final Process serviceProcess;
	private final PidFile pidFile;

	FailedService(Process serviceProcess, PidFile pidFile)
	{
		this.serviceProcess = serviceProcess;
		this.pidFile = pidFile;
	}

	/**
	 * Destroys the process, if it is running and removes its PID file so that future startups do not fail. The deletion
	 * of the PID file is best-effort.
	 */
	void cleanupService()
	{
		serviceProcess.destroy();
		pidFile.destroy();
	}
}
