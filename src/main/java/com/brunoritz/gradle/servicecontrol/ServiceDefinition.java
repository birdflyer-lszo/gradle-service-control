package com.brunoritz.gradle.servicecontrol;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

import java.io.File;
import java.time.Duration;
import java.util.Map;

/**
 * Technology independent configuration settings for services.
 */
public interface ServiceDefinition
{
	/**
	 * An optional list of files from which to read environment variables to pass to the application. The environment
	 * files are read in the given order, therefore those added later in the list taking precedence, if a variable is
	 * defined more than one time. Setting environment files using
	 * {@code javaServiceControl.environmentFiles.set([...]} will clear any previously set files.
	 * <p>
	 * Environment variables set via {@code javaServiceControl.environment([...]} will take precedence
	 * <p>
	 * Defaults to an empty list.
	 *
	 * @see #environmentFiles
	 * @see #getEnvironment
	 */
	ListProperty<File> getEnvironmentFiles();

	/**
	 * Adds additional environment variables from the given files to the list of existing environment files. Files are
	 * read in the given order.
	 *
	 * @param files
	 * 	The environment files to add to the existing ones
	 *
	 * @see #getEnvironmentFiles()
	 */
	void environmentFiles(File... files);

	/**
	 * The environment variables to set when starting the service. These variables will be merged with existing
	 * environment variables prior to starting a service. Setting environment variables using
	 * {@code javaServiceControl.environment.set([...]} will clear any previously set variables.
	 * <p>
	 * Defaults to an empty map.
	 */
	MapProperty<CharSequence, CharSequence> getEnvironment();

	/**
	 * Adds additional environment variables to set when starting the service. These variables will be
	 * merged with existing environment variables prior to starting a service. Environment variables that exist in
	 * both {@code environment} and the current configuration will be overwritten with those contained in
	 * {@code environment}.
	 * <p>
	 * Defaults to an empty map.
	 */
	void environment(Map<CharSequence, CharSequence> environment);

	/**
	 * Arguments to pass to the service's main class. If not empty, all arguments will be passed in the given order.
	 * <p>
	 * Defaults to an empty list.
	 */
	ListProperty<CharSequence> getArgs();

	/**
	 * The port number on which the service is expected to setup a listening TCP socket. A service is considered
	 * started once the socket is listening on this port. If no port is defined, a log marker message for the startup
	 * has to be specified.
	 *
	 * @see #getStartupLogMessage()
	 */
	Property<Integer> getServicePort();

	/**
	 * The message to expect in the log output when the serivce has started. If no log message is defined, a service
	 * port has to be specified.
	 * <p>
	 * The message defined here is partially matched against a service's standard output log. The expected message
	 * must not span multiple lines.
	 *
	 * @see #getServicePort()
	 */
	Property<CharSequence> getStartupLogMessage();

	/**
	 * The working directory of the service.
	 * <p>
	 * Defaults to the project's directory ({@code projectDir}).
	 */
	DirectoryProperty getWorkingDirectory();

	/**
	 * The file in which the service's process ID will be recorded.
	 * <p>
	 * Defaults to {@code buildDir/service.&lt;service-name&gt;.pid}.
	 */
	RegularFileProperty getPidFile();

	/**
	 * The file in which to record the service's standard output stream. The standard output stream will not be
	 * printed onto the build's console.
	 * <p>
	 * Defaults to {@code logs/stdout.&lt;service-name&gt;.log}.
	 */
	RegularFileProperty getStandardOutputLog();

	/**
	 * The file in which to record the service's standard error output stream. The standard output stream will not be
	 * printed onto the build's console.
	 * <p>
	 * Defaults to {@code logs/stderr.&lt;service-name&gt;.log}.
	 */
	RegularFileProperty getErrorOutputLog();

	/**
	 * The time the service is given to start successfully. The service is considered successfully started when it has
	 * set up a listening socket on the given port.
	 * <p>
	 * Defaults to 10 minutes.
	 *
	 * @see #getServicePort()
	 */
	Property<Duration> getStartTimeout();
}
