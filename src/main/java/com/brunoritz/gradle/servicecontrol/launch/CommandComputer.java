package com.brunoritz.gradle.servicecontrol.launch;

import io.vavr.collection.List;

/**
 * Computes the command to execute for starting a service. The computed command includes both the executable and all
 * arguments to pass.
 */
public interface CommandComputer
{
	/**
	 * Returns the service start command. The first entry in the returned list is the executable making up the service.
	 * All subsequent entries are considered arguments.
	 */
	List<String> compute();
}
