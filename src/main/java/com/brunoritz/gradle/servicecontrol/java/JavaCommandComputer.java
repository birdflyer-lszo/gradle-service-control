package com.brunoritz.gradle.servicecontrol.java;

import com.brunoritz.gradle.servicecontrol.launch.CommandComputer;
import io.vavr.collection.List;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.internal.jvm.Jvm;

import java.io.File;

/**
 * Computes the command needed to start a Java service. This implementation uses a Gradle internal class for
 * determining the JDK location. Unfortunately there seems to be no better/public way to find that location.
 * <p>
 * The computed command will contain the following elements:
 * <ol>
 *     <li>Java executable</li>
 *     <li>Arguments file (Windows cannot handle long command lines with a full classpath)</li>
 *     <li>Main class name</li>
 *     <li>Program arguments</li>
 * </ol>
 */
public class JavaCommandComputer
	implements CommandComputer
{
	private final RegularFileProperty argumentsFile;
	private final Property<CharSequence> mainClass;
	private final ListProperty<CharSequence> arguments;

	public JavaCommandComputer(
		RegularFileProperty argumentsFile,
		Property<CharSequence> mainClass,
		ListProperty<CharSequence> arguments)
	{
		this.argumentsFile = argumentsFile;
		this.mainClass = mainClass;
		this.arguments = arguments;
	}

	@Override
	public List<String> compute()
	{
		File javaExecutable = Jvm.current().getJavaExecutable();
		List<String> command = List.<String>empty()
			.append(javaExecutable.toString())
			.append(String.format("@%s", argumentsFile.getAsFile().get()))
			.append(mainClass.get().toString());

		return arguments.get().stream()
			.map(CharSequence::toString)
			.reduce(command, List::append, List::appendAll);
	}
}
