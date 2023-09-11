package com.brunoritz.gradle.servicecontrol;

import com.brunoritz.gradle.servicecontrol.common.StopServiceTask;
import com.brunoritz.gradle.servicecontrol.java.CreateArgumentsFileTask;
import com.brunoritz.gradle.servicecontrol.java.JavaCommandComputer;
import com.brunoritz.gradle.servicecontrol.java.JavaServiceDefinition;
import com.brunoritz.gradle.servicecontrol.launch.StartServiceTask;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

import static com.brunoritz.gradle.servicecontrol.common.TaskNameFactory.restartTaskName;
import static com.brunoritz.gradle.servicecontrol.common.TaskNameFactory.startTaskName;
import static com.brunoritz.gradle.servicecontrol.common.TaskNameFactory.stopTaskName;
import static com.brunoritz.gradle.servicecontrol.common.TaskNameFactory.taskName;

/**
 * The {@code java-service-control} plugin allows starting, stopping and restarting of Java services. The services
 * are kept alive for as long as the Gradle Daemon is running or they are stopped with the corresponding task.
 * <p>
 * This plugin provides a {@code javaServiceControl} extension through which the aspects of the services to be
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
 * @see JavaServiceDefinition
 * @see StartServiceTask
 * @see StopServiceTask
 */
public class JavaServiceControlPlugin
	implements Plugin<Project>
{
	private static final String SERVICE_CONTROL_GROUP = "Service Control";

	@Override
	public void apply(Project project)
	{
		ExtensionContainer extensions = project.getExtensions();
		NamedDomainObjectContainer<JavaServiceDefinition> javaServices =
			project.container(JavaServiceDefinition.class);

		extensions.add("javaServiceControl", javaServices);

		javaServices.whenObjectAdded(newService -> integrateNewService(project, newService));
		javaServices.whenObjectRemoved(removedService -> {
			throw new UnsupportedOperationException("Removing previously defined services is not supported");
		});

		project.afterEvaluate(JavaServiceControlPlugin::ensureJavaPluginApplied);
	}

	private void integrateNewService(Project project, JavaServiceDefinition newService)
	{
		TaskContainer tasks = project.getTasks();
		TaskProvider<CreateArgumentsFileTask> createArgsTask = tasks.register(
			taskName("createArguments", newService.getName()),
			CreateArgumentsFileTask.class
		);
		TaskProvider<StartServiceTask> startTask = tasks.register(
			startTaskName(newService.getName()),
			StartServiceTask.class,
			new JavaCommandComputer(newService.getArgumentsFile(), newService.getMainClass(), newService.getArgs())
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

		createArgsTask.configure(task -> {
			task.getDebugPort().set(newService.getDebugPort());
			task.getSystemProperties().set(newService.getSystemProperties());
			task.getArgumentsFile().set(newService.getArgumentsFile());
			task.getAgent().set(newService.getAgent());
			task.getAgentArgs().set(newService.getAgentArgs());
			task.getJvmArgs().set(newService.getJvmArgs());

			task.getRuntimeClasspath().set(project
				.getExtensions()
				.getByType(JavaPluginExtension.class)
				.getSourceSets()
				.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
				.getRuntimeClasspath()
			);

			task.getOutputs().upToDateWhen(t -> false);
		});

		startTask.configure(task -> {
			task.setGroup(SERVICE_CONTROL_GROUP);
			task.dependsOn(createArgsTask);
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

		project.afterEvaluate(evaluatedProject ->
			JavaServiceControlPlugin.determineMainClassFromApplication(evaluatedProject, newService)
		);
	}

	private static void ensureJavaPluginApplied(Project evaluatedProject)
	{
		if (!evaluatedProject.getPlugins().hasPlugin("java")) {
			throw new GradleException("java-service-control plugin requires a Java project");
		}
	}

	private static void determineMainClassFromApplication(Project evaluatedProject, JavaServiceDefinition newService)
	{
		JavaApplication application = evaluatedProject.getExtensions().findByType(JavaApplication.class);
		Property<CharSequence> serviceMainClass = newService.getMainClass();

		if ((application != null) && (serviceMainClass.getOrElse("").length() == 0)) {
			String mainClass = application.getMainClass().getOrElse("");

			serviceMainClass.set(mainClass);
		}
	}
}
