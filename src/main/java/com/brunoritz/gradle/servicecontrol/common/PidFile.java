package com.brunoritz.gradle.servicecontrol.common;

import io.vavr.control.Option;
import io.vavr.control.Try;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

/**
 * A utility for reading and writing PID files.
 */
public class PidFile
{
	private static final Logger logger = Logging.getLogger(PidFile.class);

	private final File pidFile;

	/**
	 * Creates an empty PID file. This method only succeeds, if the specified PID file does not exist at the time of
	 * invocation.
	 *
	 * @param pidFile
	 * 	The path of the PID file to create
	 *
	 * @return The PID or the error that occurred during creation
	 */
	public static Try<PidFile> createEmpty(File pidFile)
	{
		return Try.of(() -> {
			if (!pidFile.createNewFile()) {
				throw new IllegalStateException("Service is already running (PID file exists)");
			}

			return new PidFile(pidFile);
		});
	}

	/**
	 * Attaches to an existing PID file. This method returns a PID file instance, if the specified PID file exists at
	 * the time of invocation.
	 *
	 * @param pidFile
	 * 	The path of the existing PID file
	 *
	 * @return The PID instance or empty, if the backing file does not exist
	 */
	public static Option<PidFile> fromExisting(File pidFile)
	{
		if (pidFile.exists()) {
			return Option.of(new PidFile(pidFile));
		} else {
			return Option.none();
		}
	}

	private PidFile(File pidFile)
	{
		this.pidFile = pidFile;
	}

	/**
	 * Records the PID of the given process.
	 *
	 * @param process
	 * 	The process whose ID to record
	 *
	 * @return The recorded ID or the error that occurred
	 */
	public Try<Long> recordPid(Process process)
	{
		return Try
			.withResources(() -> new BufferedWriter(new FileWriter(pidFile, StandardCharsets.UTF_8)))
			.of(writer -> {
				long processId = process.pid();

				writer.append(Long.toString(processId));

				return processId;
			});
	}

	/**
	 * Reads the process ID contained in the underlying PID file. If the PID file contains a numeric value, that value
	 * is returned. Otherwise {@code none} will be returned.
	 *
	 * @return The process ID or the error that occurred
	 */
	public Try<Option<Long>> readNumericPid()
	{
		return Try
			.withResources(() -> new BufferedReader(new FileReader(pidFile, StandardCharsets.UTF_8)))
			.of(BufferedReader::readLine)
			.map(PidFile::toOptionalNumeric);
	}

	private static Option<Long> toOptionalNumeric(String content)
	{
		return Option.of(content)
			.filter(line -> !line.isBlank())
			.map(Long::parseLong);
	}

	/**
	 * Deletes an existing PID file. This is a best-effort method and depending on open file descriptors, it may not
	 * be able to delete the PID file.
	 */
	public void destroy()
	{
		if (!pidFile.delete()) {
			logger.warn("Failed to delete PID file '{}'. Manual deletion required.", pidFile);
		}
	}
}
