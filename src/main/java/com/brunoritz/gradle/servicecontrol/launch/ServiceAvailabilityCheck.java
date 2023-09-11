package com.brunoritz.gradle.servicecontrol.launch;

/**
 * Checks whether a service is up and running. An availability checker indicates whether the service is running at
 * the time of invocation.
 */
public interface ServiceAvailabilityCheck
	extends AutoCloseable
{
	/**
	 * Indicates if the service is running at the time of invocation. Multiple calls may be needed until a service is
	 * in a running state.
	 */
	boolean isRunning();
}
