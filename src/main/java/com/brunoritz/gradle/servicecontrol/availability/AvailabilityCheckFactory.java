package com.brunoritz.gradle.servicecontrol.availability;

import com.brunoritz.gradle.servicecontrol.launch.ServiceAvailabilityCheck;
import io.vavr.control.Option;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/**
 * Creates service availability checkers based on the service configuration. Two types of checkers are supported:
 * <ul>
 *     <li>TCP listener port availability</li>
 *     <li>Log entry availability</li>
 * </ul>
 * <p>
 * If both, the port number and the expected success log message, are set, a port checker will be created.
 */
public final class AvailabilityCheckFactory
{
	private AvailabilityCheckFactory()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Creates a service availability checker.
	 *
	 * @param servicePort
	 * 	The property holding the service's TCP port, may have no value
	 * @param startupLogMessage
	 * 	The expected log message that indicates a successful start may have no value
	 * @param standardOutputLog
	 * 	The log file in which to look for the success message, may be empty but required when {@code startupLogMessage}
	 * 	is defined
	 *
	 * @return The checker or {@code none}, if configuration settings are missing
	 */
	public static Option<ServiceAvailabilityCheck> checkFromDefinition(
		Property<Integer> servicePort,
		Property<CharSequence> startupLogMessage,
		RegularFileProperty standardOutputLog)
	{
		ServiceAvailabilityCheck result = null;

		if (servicePort.isPresent()) {
			int port = servicePort.get();

			result = new PortAvailabilityCheck(port);
		} else if (startupLogMessage.isPresent()) {
			result = new LogMessageAvailabilityCheck(
				standardOutputLog.get().getAsFile(),
				startupLogMessage.get().toString()
			);
		}

		return Option.of(result);
	}
}
