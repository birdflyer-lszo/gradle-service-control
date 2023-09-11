package com.brunoritz.gradle.servicecontrol.java;

import io.vavr.collection.List;
import io.vavr.control.Try;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This task creates an arguments file for the Java service to be started. Argument files are necessary because the
 * classpath of a Java application can be come longer than Windows allows passing via command line.
 * <p>
 * The arguments that are written into the arguments file will be the following:
 * <ul>
 *     <li>If defined, a Java agent along with its argument</li>
 *     <li>If defined, the remote debugging settings</li>
 *     <li>If defined, the supplied JVM arguments</li>
 *     <li>The application's runtime classpath</li>
 *     <li>If defined, additional system properties</li>
 * </ul>
 * <p>
 * When a debugger port is set, the application with be configured for remote debugging. The application will start
 * immediately without waiting for a debugger to be attached.
 */
public abstract class CreateArgumentsFileTask
	extends DefaultTask
{
	@Input
	@Optional
	public abstract Property<Integer> getDebugPort();

	@Input
	public abstract MapProperty<CharSequence, CharSequence> getSystemProperties();

	@Classpath
	@Optional
	public abstract Property<FileCollection> getAgent();

	@Input
	@Optional
	public abstract Property<CharSequence> getAgentArgs();

	@Input
	public abstract ListProperty<CharSequence> getJvmArgs();

	@Classpath
	public abstract Property<FileCollection> getRuntimeClasspath();

	@OutputFile
	public abstract RegularFileProperty getArgumentsFile();

	@TaskAction
	public void createFile()
	{
		List<String> arguments = List.<String>empty()
			.append(computeAgentArgument())
			.append(computeDebuggerArgument())
			.appendAll(computeJvmArguments())
			.append(computeClasspathArgument())
			.appendAll(computeSystemProperties());

		writeArgumentsToFile(arguments)
			.getOrElseThrow(error -> new TaskExecutionException(this, error));
	}

	private String computeAgentArgument()
	{
		Set<File> agentFiles = getAgent().map(FileCollection::getFiles)
			.getOrElse(Set.of());

		if (agentFiles.size() == 1) {
			File agentFile = agentFiles.iterator().next();
			String args = getAgentArgs().map(value -> String.format("=%s", value))
				.getOrElse("");

			return String.format("-javaagent:%s%s", agentFile, args);
		} else if (agentFiles.size() == 0) {
			return "";
		} else {
			throw new IllegalArgumentException("Agent configuration may only contain one single file");
		}
	}

	private String computeDebuggerArgument()
	{
		return getDebugPort()
			.map(debugPort ->
				String.format("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:%d", debugPort))
			.getOrElse("");
	}

	private List<String> computeJvmArguments()
	{
		return List.ofAll(
			getJvmArgs().get().stream()
				.map(Object::toString)
		);
	}

	private String computeClasspathArgument()
	{
		Set<String> classpathFiles = getRuntimeClasspath()
			.get()
			.getFiles()
			.stream()
			.map(File::getAbsolutePath)
			.collect(Collectors.toSet());
		String joinedClasspath = String.join(File.pathSeparator, classpathFiles);

		return String.format("-cp %s", joinedClasspath);
	}

	private List<String> computeSystemProperties()
	{
		return List.ofAll(
			getSystemProperties().get().entrySet().stream()
				.map(entry -> String.format("-D%s=%s", entry.getKey(), entry.getValue()))
		);
	}

	private Try<Void> writeArgumentsToFile(List<String> arguments)
	{
		File outputFile = getArgumentsFile().get().getAsFile();

		return Try
			.withResources(() -> new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8)))
			.of(writer -> {
				String allArguments = arguments
					.intersperse(" ")
					.foldLeft(new StringBuilder(), StringBuilder::append)
					.toString();

				writer.append(allArguments);

				return null;
			});
	}
}
