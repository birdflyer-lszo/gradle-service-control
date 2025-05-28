package com.brunoritz.gradle.servicecontrol.java;

import com.brunoritz.gradle.servicecontrol.ServiceDefinition;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.io.File;
import java.time.Duration;
import java.util.Map;

/**
 * The {@code javaServiceControl} extension takes the configuration of the service to be started. All settings of the
 * service have to be configured via this extension.
 */
public class JavaServiceDefinition
	implements ServiceDefinition
{
	private final String name;
	private final Property<CharSequence> mainClass;
	private final ListProperty<File> environmentFiles;
	private final MapProperty<CharSequence, CharSequence> environment;
	private final ListProperty<CharSequence> jvmArgs;
	private final ListProperty<CharSequence> args;
	private final MapProperty<CharSequence, CharSequence> systemProperties;
	private final Property<Integer> servicePort;
	private final Property<CharSequence> startupLogMessage;
	private final Property<Integer> debugPort;
	private final DirectoryProperty workingDirectory;
	private final RegularFileProperty argumentsFile;
	private final RegularFileProperty pidFile;
	private final RegularFileProperty standardOutputLog;
	private final RegularFileProperty errorOutputLog;
	private final Property<Duration> startTimeout;
	private final Property<Configuration> agent;
	private final Property<CharSequence> agentArgs;

	@Inject
	public JavaServiceDefinition(String name, Project project)
	{
		ObjectFactory factory = project.getObjects();
		DirectoryProperty buildDirectory = project.getLayout().getBuildDirectory();
		File logsDirectory = new File(project.getProjectDir(), "logs");

		this.name = name;

		mainClass = factory.property(CharSequence.class);
		environmentFiles = factory.listProperty(File.class);
		environment = factory.mapProperty(CharSequence.class, CharSequence.class);
		jvmArgs = factory.listProperty(CharSequence.class);
		args = factory.listProperty(CharSequence.class);
		systemProperties = factory.mapProperty(CharSequence.class, CharSequence.class);
		servicePort = factory.property(Integer.class);
		startupLogMessage = factory.property(CharSequence.class);
		debugPort = factory.property(Integer.class);
		workingDirectory = factory.directoryProperty();
		argumentsFile = factory.fileProperty();
		pidFile = factory.fileProperty();
		standardOutputLog = factory.fileProperty();
		errorOutputLog = factory.fileProperty();
		startTimeout = factory.property(Duration.class);
		agent = factory.property(Configuration.class);
		agentArgs = factory.property(CharSequence.class);

		workingDirectory.set(project.getProjectDir());
		argumentsFile.set(buildDirectory.file(String.format("jvmargs.%s.txt", name)));
		pidFile.set(project.file(String.format("service.%s.pid", name)));
		standardOutputLog.set(project.file(String.format("%s/stdout.%s.log", logsDirectory, name)));
		errorOutputLog.set(project.file(String.format("%s/stderr.%s.log", logsDirectory, name)));
		startTimeout.set(Duration.ofMinutes(10));
	}

	/**
	 * Returns the unique name of the service being defined. Each service is required to have a unique name so that the
	 * plugin can register appropriate control tasks for each service.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the arguments to pass to the JVM. These arguments can be used to further customize JVM specific
	 * settings.
	 * <p>
	 * This property should not be used for be used for configuring agents or debug settings. These should be
	 * configured using the dedicated methods instead.
	 * <p>
	 * Defaults to an empty list.
	 *
	 * @see #jvmArg(CharSequence)
	 * @see #getDebugPort()
	 * @see #getAgent()
	 * @see #getAgentArgs()
	 */
	public ListProperty<CharSequence> getJvmArgs()
	{
		return jvmArgs;
	}

	/**
	 * Adds a single JVM argument to be list of existing arguments.
	 *
	 * @see #getJvmArgs()
	 */
	public void jvmArg(CharSequence value)
	{
		getJvmArgs().add(value);
	}

	/**
	 * The fully qualified name of the service's entry point class.
	 * <p>
	 * <b>This is a mandatory property.</b>
	 */
	public Property<CharSequence> getMainClass()
	{
		return mainClass;
	}

	/**
	 * System properties to be set when the service is started. The system properties of the Gradle Daemon are not
	 * automatically added here.
	 * <p>
	 * Defaults to an empty map.
	 *
	 * @see #systemProperty(CharSequence, CharSequence)
	 */
	public MapProperty<CharSequence, CharSequence> getSystemProperties()
	{
		return systemProperties;
	}

	/**
	 * Adds a single system property to be set for the service to start. Neither key nor value may be {@code null}.
	 *
	 * @see #getSystemProperties()
	 */
	public void systemProperty(CharSequence key, CharSequence value)
	{
		systemProperties.put(key, value);
	}

	/**
	 * The port on which to provide a remote debugger. If not set, no remote debugger will be provided. If provided,
	 * the
	 * service will set to start immediately without waiting for a debugger to be attached.
	 * <p>
	 * Defaults to empty
	 */
	public Property<Integer> getDebugPort()
	{
		return debugPort;
	}

	/**
	 * The dependency configuration that contains the agent library to attach to the process. It is important that
	 * there be exactly one file in that configuration.
	 * <p>
	 * Defaults to no agent.
	 */
	public Property<Configuration> getAgent()
	{
		return agent;
	}

	/**
	 * The arguments to pass to the agent library. This property only has an effect, if an agent library is configured.
	 * <p>
	 * Defaults to no arguments.
	 */
	public Property<CharSequence> getAgentArgs()
	{
		return agentArgs;
	}

	/**
	 * The argument file used to pass all command line arguments to the JVM. An arguments file is needed since sometimes
	 * the classpath can be way too long for Windows to handle it.
	 * <p>
	 * Defaults to {@code buildDir/jvmargs.&lt;service-name&gt;.txt}.
	 */
	public RegularFileProperty getArgumentsFile()
	{
		return argumentsFile;
	}

	@Override
	public ListProperty<File> getEnvironmentFiles()
	{
		return environmentFiles;
	}

	@Override
	public void environmentFiles(File... files)
	{
		environmentFiles.addAll(files);
	}

	@Override
	public MapProperty<CharSequence, CharSequence> getEnvironment()
	{
		return environment;
	}

	@Override
	public void environment(Map<CharSequence, CharSequence> environment)
	{
		this.environment.putAll(environment);
	}

	@Override
	public ListProperty<CharSequence> getArgs()
	{
		return args;
	}

	@Override
	public Property<Integer> getServicePort()
	{
		return servicePort;
	}

	@Override
	public Property<CharSequence> getStartupLogMessage()
	{
		return startupLogMessage;
	}

	@Override
	public DirectoryProperty getWorkingDirectory()
	{
		return workingDirectory;
	}

	@Override
	public RegularFileProperty getPidFile()
	{
		return pidFile;
	}

	@Override
	public RegularFileProperty getStandardOutputLog()
	{
		return standardOutputLog;
	}

	@Override
	public RegularFileProperty getErrorOutputLog()
	{
		return errorOutputLog;
	}

	@Override
	public Property<Duration> getStartTimeout()
	{
		return startTimeout;
	}
}
