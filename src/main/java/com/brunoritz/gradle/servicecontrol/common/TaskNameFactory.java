package com.brunoritz.gradle.servicecontrol.common;

/**
 * The factory for computing the names of a service's controlling tasks. Using this factory helps unifying the service
 * names without duplicating all the logic.
 */
public final class TaskNameFactory
{
	private TaskNameFactory()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Computes the name for the start task in the form of {@code start<capitalized-service-name>}.
	 *
	 * @param serviceName
	 * 	The name of the service as passed into the service definition
	 */
	public static String startTaskName(String serviceName)
	{
		return taskName("start", serviceName);
	}

	/**
	 * Computes the name for the stop task in the form of {@code stop<capitalized-service-name>}.
	 *
	 * @param serviceName
	 * 	The name of the service as passed into the service definition
	 */
	public static String stopTaskName(String serviceName)
	{
		return taskName("stop", serviceName);
	}

	/**
	 * Computes the name for the restart task in the form of {@code restart<capitalized-service-name>}.
	 *
	 * @param serviceName
	 * 	The name of the service as passed into the service definition
	 */
	public static String restartTaskName(String serviceName)
	{
		return taskName("restart", serviceName);
	}

	/**
	 * Computes the name for an arbitrary action task {@code <actionName><capitalized-service-name>}.
	 *
	 * @param actionName
	 * 	The name of the action for which to compute a task name
	 * @param serviceName
	 * 	The name of the service as passed into the service definition
	 */
	public static String taskName(String actionName, String serviceName)
	{
		String capitalizedName = capitalizedName(serviceName);

		return String.format("%s%s", actionName, capitalizedName);
	}

	private static String capitalizedName(String originalName)
	{
		return String.format(
			"%s%s",
			Character.toUpperCase(originalName.charAt(0)),
			originalName.subSequence(1, originalName.length())
		);
	}
}
