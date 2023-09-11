package com.brunoritz.gradle.servicecontrol.launch;

import com.brunoritz.gradle.servicecontrol.ServiceDefinition;
import com.brunoritz.gradle.servicecontrol.availability.AvailabilityCheckFactory;
import com.brunoritz.gradle.servicecontrol.common.PidFile;
import io.vavr.control.Try;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.time.Duration;

/**
 * Starts a service and waits for it to become available. The task terminates successfully, if the
 * service has started and the configured availability check succeeds. If the service fails to start within the
 * configured amount of time, the task fails.
 * <p>
 * This task does not attempt the start a service, if its PID file already exists. In that case the service is
 * considered running and the task fails.
 *
 * @see ServiceDefinition
 */
public abstract class StartServiceTask
	extends DefaultTask
{
	private final CommandComputer command;

	@Inject
	public StartServiceTask(CommandComputer command)
	{
		this.command = command;
	}

	@Input
	@Optional
	public abstract Property<Integer> getServicePort();

	@Input
	@Optional
	public abstract Property<CharSequence> getStartupLogMessage();

	@Input
	public abstract Property<Duration> getStartTimeout();

	@Internal
	public abstract DirectoryProperty getWorkingDirectory();

	@Internal
	public abstract RegularFileProperty getStandardOutputLog();

	@Internal
	public abstract RegularFileProperty getErrorOutputLog();

	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract ListProperty<File> getEnvironmentFiles();

	@Input
	public abstract MapProperty<CharSequence, CharSequence> getEnvironment();

	@Internal
	public abstract RegularFileProperty getPidFile();

	/**
	 * Begins the service startup. This method initially attempts to create an empty PID file. It continues only if
	 * the PID file does not yet exist and can be created.
	 * <p>
	 * Once the service process has been created, the waiting for the TCP port begins.
	 */
	@TaskAction
	public void startService()
	{
		PidFile pidFile = PidFile.createEmpty(getPidFile().getAsFile().get())
			.getOrElseThrow(error -> new IllegalStateException("PID file could not be created or is in use", error));
		ServiceToStart serviceToStart =
			new ServiceToStart(
				command.compute(),
				pidFile,
				ProcessLauncher::new,
				getStartTimeout(),
				getWorkingDirectory(),
				getStandardOutputLog(),
				getErrorOutputLog(),
				getEnvironmentFiles(),
				getEnvironment()
			);

		serviceToStart.start()
			.flatMap(startingService -> Try
				.withResources(this::requestedAvailabilityCheck)
				.of(startingService::awaitStartup)
			)
			.onFailure(error -> {
				pidFile.destroy();

				throw new IllegalStateException("Failed to launch service process", error);
			})
			.get()
			.peek(this::recordPid)
			.peekLeft(failedService -> {
				failedService.cleanupService();

				throw new IllegalStateException("Service failed to start properly");
			});
	}

	private ServiceAvailabilityCheck requestedAvailabilityCheck()
	{
		return AvailabilityCheckFactory.checkFromDefinition(
				getServicePort(),
				getStartupLogMessage(),
				getStandardOutputLog()
			)
			.getOrElseThrow(
				() -> new IllegalStateException("Either a port or a success log message is required")
			);
	}

	private void recordPid(RunningService runningService)
	{
		runningService
			.recordProcessId()
			.onFailure(error -> {
				throw new IllegalStateException("Failed to record PID file", error);
			});
	}
}
