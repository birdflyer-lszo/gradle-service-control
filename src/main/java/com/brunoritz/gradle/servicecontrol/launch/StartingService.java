package com.brunoritz.gradle.servicecontrol.launch;

import com.brunoritz.gradle.servicecontrol.common.PidFile;
import io.vavr.control.Either;

import java.time.Duration;

/**
 * Represents a service whose service process has been created. A service in that state might either (eventually)
 * start listening on the configured port or fail, if the internal startup fails.
 */
class StartingService
{
	private final Process serviceProcess;
	private final Duration startupTimeout;
	private final PidFile pidFile;

	/**
	 * Creates a new starting service representation.
	 *
	 * @param serviceProcess
	 * 	The process representing the starting service
	 * @param startupTimeout
	 * 	The amount of time allowed for the TCP socket to become available
	 * @param pidFile
	 * 	The file into which to write the PID once the service has completed startup
	 */
	StartingService(
		Process serviceProcess,
		Duration startupTimeout,
		PidFile pidFile)
	{
		this.serviceProcess = serviceProcess;
		this.startupTimeout = startupTimeout;
		this.pidFile = pidFile;
	}

	/**
	 * Periodically checks whether the process is alive and the service has become available. If the service process
	 * terminates at any time during startup, the waiting loop is aborted.
	 * <p>
	 * The service's availability is polled every second.
	 * <p>
	 * The instances returned can be used to either complete startup or perform cleanup in case of an error.
	 *
	 * @param availabilityCheck
	 * 	The strategy used to determine whether a service is up and running
	 *
	 * @return Either the started service or the failed service
	 */
	public Either<FailedService, RunningService> awaitStartup(ServiceAvailabilityCheck availabilityCheck)
	{
		if (awaitAvailability(availabilityCheck)) {
			return Either.right(new RunningService(serviceProcess, pidFile));
		} else {
			return Either.left(new FailedService(serviceProcess, pidFile));
		}
	}

	private boolean awaitAvailability(ServiceAvailabilityCheck availabilityCheck)
	{
		long latestWait = System.currentTimeMillis() + startupTimeout.toMillis();
		boolean started = false;

		try {
			while (serviceProcess.isAlive() && (System.currentTimeMillis() < latestWait) && !started) {
				started = availabilityCheck.isRunning();

				if (!started) {
					Thread.sleep(500);
				}
			}
		} catch (InterruptedException ignored) {
		}

		return started;
	}
}
