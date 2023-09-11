package com.brunoritz.gradle.servicecontrol.common;

import io.vavr.control.Try;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Stops a running service. This task reads the process ID of the task to be stopped from the PID file. Once the PID is
 * known, the process is asked to terminate. After termination the PID file is deleted.
 * <p>
 * If no PID file exists, the process is considered stopped and no further action is taken. This task does not fail, if
 * no PID file can be found.
 */
public class StopServiceTask
	extends DefaultTask
{
	private final RegularFileProperty pidLocation;

	/**
	 * Creates a new process stop task for a given PID file. The PID file is not required to exist at the time of
	 * instantiation.
	 *
	 * @param pidLocation
	 * 	The file holding the process' numeric PID.
	 */
	@Inject
	public StopServiceTask(RegularFileProperty pidLocation)
	{
		this.pidLocation = pidLocation;
	}

	@TaskAction
	public void stopService()
	{
		PidFile.fromExisting(pidLocation.get().getAsFile())
			.peek(existingPid -> {
				terminateServiceProcess(existingPid);
				existingPid.destroy();
			})
			.onEmpty(() -> getLogger().warn("Service not running (PID file not found)"));
	}

	private void terminateServiceProcess(PidFile pidFile)
	{
		pidFile.readNumericPid()
			.onSuccess(pid -> pid
				.onEmpty(() -> getLogger().warn("Emtpy PID file found. Process might be in undefined state."))
				.map(ProcessHandle::of)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.peek(this::terminateHierarchy)
			)
			.onFailure(error -> {
				throw new IllegalStateException("Failed to parse stored PID", error);
			});
	}

	private void terminateHierarchy(ProcessHandle toTerminate)
	{
		Try.of(
				() -> {
					/*
					 * On Windows certain processes fail to terminate, if only the one launched by this plugin is
					 * requested
					 * to terminate, in particular NodeJS. So we terminate all processes in the hierarchy.
					 */
					toTerminate.descendants().forEach(ProcessHandle::destroyForcibly);
					toTerminate.destroy();
					toTerminate.onExit().get(30, TimeUnit.SECONDS);

					return toTerminate;
				}
			)
			.onSuccess(process -> getLogger().info("Service stopped"))
			.onFailure(error -> {
				throw new IllegalStateException("Failed to terminate running process", error);
			});
	}
}
