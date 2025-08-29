package com.brunoritz.gradle.servicecontrol.availability;

import com.brunoritz.gradle.servicecontrol.launch.ServiceAvailabilityCheck;
import org.apache.commons.io.input.Tailer;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Checks whether a service is available by looking for a specific log message in the service's standard output log
 * file. A service is considered running, if any message is found that contains the specified text.
 * <p>
 * Consumers have to ensure {@link #close()} be called once this checker is no longer needed.
 */
public class LogMessageAvailabilityCheck
	implements ServiceAvailabilityCheck
{
	private final AtomicBoolean serviceRunning;
	private final Thread tailerThread;
	private final Tailer tailer;

	public LogMessageAvailabilityCheck(File logFile, String expectedLogMessage)
	{
		serviceRunning = new AtomicBoolean();
		tailer = Tailer.builder()
			.setFile(logFile)
			.setTailerListener(new LogMessageListener(expectedLogMessage, serviceRunning))
			.get();
		tailerThread = new Thread(tailer);
		tailerThread.setDaemon(true);
	}

	@Override
	public boolean isRunning()
	{
		// Important: Only start the tailer thread here as otherwise an existing file may be tailed, leading to wrong
		// results, if it already contains the success message string
		if (!serviceRunning.get() && !tailerThread.isAlive()) {
			tailerThread.start();
		}

		return serviceRunning.get();
	}

	@Override
	public void close()
	{
		tailer.close();
	}
}
