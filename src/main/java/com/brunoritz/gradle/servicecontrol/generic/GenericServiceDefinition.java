package com.brunoritz.gradle.servicecontrol.generic;

import com.brunoritz.gradle.servicecontrol.ServiceDefinition;
import org.gradle.api.Project;
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
 * The {@code genericServiceControl} extension takes the configuration of the service to be started. All settings of the
 * service have to be configured via this extension.
 */
public class GenericServiceDefinition
	implements ServiceDefinition
{
	private final String name;
	private final Property<CharSequence> executable;
	private final ListProperty<CharSequence> args;
	private final ListProperty<File> environmentFiles;
	private final MapProperty<CharSequence, CharSequence> environment;
	private final Property<Integer> servicePort;
	private final Property<CharSequence> startupLogMessage;
	private final DirectoryProperty workingDirectory;
	private final RegularFileProperty pidFile;
	private final RegularFileProperty standardOutputLog;
	private final RegularFileProperty errorOutputLog;
	private final Property<Duration> startTimeout;

	@Inject
	public GenericServiceDefinition(String name, Project project)
	{
		ObjectFactory factory = project.getObjects();
		File logsDirectory = new File(project.getProjectDir(), "logs");

		this.name = name;

		executable = factory.property(CharSequence.class);
		args = factory.listProperty(CharSequence.class);
		environmentFiles = factory.listProperty(File.class);
		environment = factory.mapProperty(CharSequence.class, CharSequence.class);
		servicePort = factory.property(Integer.class);
		startupLogMessage = factory.property(CharSequence.class);
		workingDirectory = factory.directoryProperty();
		pidFile = factory.fileProperty();
		standardOutputLog = factory.fileProperty();
		errorOutputLog = factory.fileProperty();
		startTimeout = factory.property(Duration.class);

		workingDirectory.set(project.getProjectDir());
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
	 * The application to execute. This can either be the fully qualified path or the name of a binary contained in
	 * the directories in {@code PATH}.
	 * <p>
	 * <b>This is a mandatory property</b>
	 */
	public Property<CharSequence> getExecutable()
	{
		return executable;
	}

	@Override
	public ListProperty<CharSequence> getArgs()
	{
		return args;
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
