package com.brunoritz.gradle.servicecontrol.launch;

import com.brunoritz.gradle.servicecontrol.common.PidFile;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * The service to be started along with all required configuration. Once the process has been started, its process ID
 * will be recorded in the given PID file. The standard output and error streams of the created process will be
 * redirected to the configured log files.
 * <p>
 * Registers a shutdown hook that terminates the process upon termination of the Gradle Daemon.
 */
class ServiceToStart
{
	private final List<String> command;
	private final PidFile pidFile;
	private final Supplier<ProcessLauncher> launcherFactory;
	private final Property<Duration> startTimeout;
	private final DirectoryProperty workingDirectory;
	private final RegularFileProperty standardOutputLog;
	private final RegularFileProperty errorOutputLog;
	private final ListProperty<File> environmentFiles;
	private final MapProperty<CharSequence, CharSequence> environment;

	ServiceToStart(
		List<String> command,
		PidFile pidFile,
		Supplier<ProcessLauncher> launcherFactory,
		Property<Duration> startTimeout,
		DirectoryProperty workingDirectory,
		RegularFileProperty standardOutputLog,
		RegularFileProperty errorOutputLog,
		ListProperty<File> environmentFiles,
		MapProperty<CharSequence, CharSequence> environment)
	{
		this.command = command;
		this.pidFile = pidFile;
		this.launcherFactory = launcherFactory;
		this.startTimeout = startTimeout;
		this.workingDirectory = workingDirectory;
		this.standardOutputLog = standardOutputLog;
		this.errorOutputLog = errorOutputLog;
		this.environmentFiles = environmentFiles;
		this.environment = environment;
	}

	/**
	 * Attempts to start the service. Once the process has been started, a representation of the starting service is
	 * returned. Completing this method without error does not mean that the service has finished starting up, but
	 * only that the process was created successfully.
	 *
	 * @return The starting service or the error encountered during startup
	 */
	Try<StartingService> start()
	{
		return configureLauncher()
			.flatMap(ProcessLauncher::start)
			.onSuccess(process -> Runtime.getRuntime().addShutdownHook(new Thread(process::destroy)))
			.map(process -> new StartingService(process, startTimeout.get(), pidFile));
	}

	private Try<ProcessLauncher> configureLauncher()
	{
		return prepareLogging()
			.flatMap(setupDirs -> computeEnvironment())
			.map(envBuilder -> launcherFactory.get()
				.command(command)
				.workingDirectory(workingDirectory.get().getAsFile())
				.storeStdOutIn(standardOutputLog.get().getAsFile())
				.storeStdErrIn(errorOutputLog.get().getAsFile())
				.appendEnvironment(envBuilder.environment())
			);
	}

	private Try<?> prepareLogging()
	{
		return Try
			.of(
				() -> {
					boolean stdOutDirCreated = parentOf(standardOutputLog.get().getAsFile())
						.filter(logDir -> !logDir.exists())
						.map(File::mkdirs)
						.getOrElse(true);
					boolean errOutDirCreated = parentOf(errorOutputLog.get().getAsFile())
						.filter(logDir -> !logDir.exists())
						.map(File::mkdirs)
						.getOrElse(true);

					if (!(stdOutDirCreated && errOutDirCreated)) {
						throw new IOException("Unable to create log output directories");
					}

					return true;
				}
			)
			.flatMap(ignored -> Try
				.of(() -> {
					boolean outputLogDeleted = !standardOutputLog.get().getAsFile().exists()
						|| standardOutputLog.get().getAsFile().delete();
					boolean errorLogDeleted = !errorOutputLog.get().getAsFile().exists()
						|| errorOutputLog.get().getAsFile().delete();

					if (!(outputLogDeleted && errorLogDeleted)) {
						throw new IOException("Unable to delete existing log files");
					}

					return true;
				})
			);
	}

	private static Option<File> parentOf(File child)
	{
		return Option.of(child)
			.flatMap(childFile -> Option.of(childFile.getParentFile()));
	}

	private Try<EnvironmentBuilder> computeEnvironment()
	{
		HashMap<String, String> environmentVariables = HashMap.ofAll(environment.get())
			.bimap(CharSequence::toString, CharSequence::toString);

		return EnvironmentBuilder.empty()
			.append(List.ofAll(environmentFiles.get()))
			.map(existingVariables -> existingVariables.append(environmentVariables));
	}
}
