package com.brunoritz.gradle.servicecontrol.launch;

import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Try;

import java.io.File;

/**
 * A small wrapper around {@code ProcessBuilder} to make process creation testable. {@code ProcessBuilder} is
 * {@code final} and therefore it cannot be mocked.
 * <p>
 * Since launching of processes is critical, the code has to undergo proper testing and relying on certain binaries
 * be present on all platforms would lead to unstable tests.
 */
class ProcessLauncher
{
	private final ProcessBuilder builder;

	ProcessLauncher()
	{
		builder = new ProcessBuilder();
	}

	ProcessLauncher command(List<String> command)
	{
		builder.command(command.asJava());

		return this;
	}

	ProcessLauncher workingDirectory(File target)
	{
		builder.directory(target);

		return this;
	}

	ProcessLauncher storeStdOutIn(File target)
	{
		builder.redirectOutput(target);

		return this;
	}

	ProcessLauncher storeStdErrIn(File target)
	{
		builder.redirectError(target);

		return this;
	}

	ProcessLauncher appendEnvironment(Map<String, String> envVars)
	{
		builder.environment().putAll(envVars.toJavaMap());

		return this;
	}

	Try<Process> start()
	{
		return Try.of(builder::start);
	}
}
