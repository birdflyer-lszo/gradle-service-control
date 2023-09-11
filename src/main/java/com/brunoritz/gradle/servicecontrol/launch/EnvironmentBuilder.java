package com.brunoritz.gradle.servicecontrol.launch;

import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Try;

import static io.vavr.control.Try.success;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * A utility that assembles the environment variables for a process. Environment variables can be read from files as
 * well as be defined via a map.
 */
class EnvironmentBuilder
{
	private final Map<String, String> environmentVariables;

	/**
	 * Returns an environment builder with no variables defined yet. This method should be used as the starting point
	 * for building a process environment.
	 */
	static EnvironmentBuilder empty()
	{
		return new EnvironmentBuilder(HashMap.empty());
	}

	private EnvironmentBuilder(Map<String, String> environmentVariables)
	{
		this.environmentVariables = environmentVariables;
	}

	/**
	 * Appends environment variables from the given files to the environment being built. The variables form all files
	 * are merged into one single result. Variables defined in a file later in the list take precedence over those
	 * defined earlier.
	 * {@code file} take precedence over existing variables in this builder.
	 *
	 * @param files
	 * 	The files from which to read environment variables in. They are expected to be Java {@code .properties} files
	 *
	 * @return The updated builder or the error that prevented the file from being read
	 */
	Try<EnvironmentBuilder> append(List<File> files)
	{
		Try<EnvironmentBuilder> result = success(this);

		return files.foldLeft(result, (previousResult, inputFile) ->
			previousResult.flatMap(existingBuilder -> Try
				.withResources(() -> new FileInputStream(inputFile))
				.of(inputStream -> {
					Properties additionalVariables = new Properties();

					additionalVariables.load(inputStream);

					return additionalVariables;
				})
				.map(EnvironmentBuilder::toMap)
				.map(additionalVariables -> merge(existingBuilder.environmentVariables, additionalVariables))
				.map(EnvironmentBuilder::new))
		);
	}

	/**
	 * Appends environment variables from the given map to the environment being built. Variables defined in
	 * {@code input} take precedence over existing variables in this builder.
	 *
	 * @param input
	 * 	The properties to append
	 *
	 * @return An updated builder instance
	 */
	EnvironmentBuilder append(Map<String, String> input)
	{
		return new EnvironmentBuilder(merge(environmentVariables, input));
	}

	private static Map<String, String> toMap(Properties input)
	{
		Map<String, String> result = HashMap.empty();

		for (java.util.Map.Entry<Object, Object> entry : input.entrySet()) {
			result = result.put(entry.getKey().toString(), entry.getValue().toString());
		}

		return result;
	}

	private static Map<String, String> merge(Map<String, String> left, Map<String, String> right)
	{
		return left.merge(right, (leftValue, rightValue) -> rightValue);
	}

	/**
	 * Returns the final environment variables to be passed to the process.
	 */
	Map<String, String> environment()
	{
		return environmentVariables;
	}
}
