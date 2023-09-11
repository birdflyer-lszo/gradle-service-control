package com.brunoritz.gradle.servicecontrol.launch

/**
 * A simple service mock that establishes a TCP socket. This is used to test starting Java services.
 */
class SimulatedService
{
	static void main(String... args)
	{
		def server = new ServerSocket(7171)

		while (!Thread.currentThread().isInterrupted()) {
			server.accept()
		}
	}
}
