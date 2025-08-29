package com.brunoritz.gradle.servicecontrol;

import com.brunoritz.gradle.servicecontrol.common.StopServiceTask;
import com.brunoritz.gradle.servicecontrol.generic.GenericCommandComputer;
import com.brunoritz.gradle.servicecontrol.generic.GenericServiceDefinition;
import com.brunoritz.gradle.servicecontrol.launch.StartServiceTask;
import org.gradle.api.DefaultTask;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

import static com.brunoritz.gradle.servicecontrol.common.TaskNameFactory.restartTaskName;
import static com.brunoritz.gradle.servicecontrol.common.TaskNameFactory.startTaskName;
import static com.brunoritz.gradle.servicecontrol.common.TaskNameFactory.stopTaskName;

/**
 * The {@code generic-service-control} plugin allows starting, stopping and restarting of arbitrary services. The
 * services are kept alive for as long as the Gradle Daemon is running or stopped with the corresponding task.
 * <p>
 * This plugin provides a {@code genericServiceControl} extension through which the aspects of the services to be
 * controlled can be configured. Each service is associated the following tasks:
 * <ul>
 *     <li>{@code start&lt;serviceName&gt;}</li>
 *     <li>{@code stop&lt;serviceName&gt;}</li>
 *     <li>{@code restart&lt;serviceName&gt;}</li>
 * </ul>
 * <p>
 * Details on the behavior can be found in the documentation of the tasks and the service configuration container. The
 * {@code restart&lt;serviceName&gt;} task does nothing more than causing (in order) the invocation of
 * {@code stop&lt;serviceName&gt;} and {@code start&lt;serviceName&gt;}.
 *
 * @see GenericServiceDefinition
 * @see StartServiceTask
 * @see StopServiceTask
 */
public class GenericServiceControlPlugin
	implements Plugin<Project>
{
	private static final String SERVICE_CONTROL_GROUP = "Service Control";

	@Override
	public void apply(Project project)
	{
		ExtensionContainer extensions = project.getExtensions();
		NamedDomainObjectContainer<GenericServiceDefinition> genericServices =
			project.container(GenericServiceDefinition.class);

		extensions.add("genericServiceControl", genericServices);

		genericServices.whenObjectAdded(newService -> integrateNewService(project, newService));
		genericServices.whenObjectRemoved(removedService -> {
			throw new UnsupportedOperationException("Removing previously defined services is not supported");
		});
	}

	private void integrateNewService(Project project, GenericServiceDefinition newService)
	{
		TaskContainer tasks = project.getTasks();
		TaskProvider<StartServiceTask> startTask = tasks.register(
			startTaskName(newService.getName()),
			StartServiceTask.class,
			new GenericCommandComputer(newService.getExecutable(), newService.getArgs())
		);
		TaskProvider<StopServiceTask> stopTask = tasks.register(
			stopTaskName(newService.getName()),
			StopServiceTask.class,
			newService.getPidFile()
		);
		TaskProvider<DefaultTask> restartTask = tasks.register(
			restartTaskName(newService.getName()),
			DefaultTask.class
		);

		startTask.configure(task -> {
			task.setGroup(SERVICE_CONTROL_GROUP);
			task.mustRunAfter(stopTask);

			task.getServicePort().set(newService.getServicePort());
			task.getStartupLogMessage().set(newService.getStartupLogMessage());
			task.getStartTimeout().set(newService.getStartTimeout());
			task.getWorkingDirectory().set(newService.getWorkingDirectory());
			task.getStandardOutputLog().set(newService.getStandardOutputLog());
			task.getErrorOutputLog().set(newService.getErrorOutputLog());
			task.getEnvironmentFiles().set(newService.getEnvironmentFiles());
			task.getEnvironment().set(newService.getEnvironment());
			task.getPidFile().set(newService.getPidFile());

			task.getOutputs().upToDateWhen(t -> false);
		});

		stopTask.configure(task -> task.setGroup(SERVICE_CONTROL_GROUP));

		restartTask.configure(task -> {
			task.setGroup(SERVICE_CONTROL_GROUP);
			task.dependsOn(stopTask);
			task.dependsOn(startTask);
		});
	}
}
