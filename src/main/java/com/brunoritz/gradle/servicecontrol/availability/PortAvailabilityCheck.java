package com.brunoritz.gradle.servicecontrol.availability;

import com.brunoritz.gradle.servicecontrol.launch.ServiceAvailabilityCheck;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Checks whether a service is available by looking for a listening socket on a given port. A service is considered
 * running, if the socket accepts incoming connections. This checker does not perform any actual communication with
 * the service.
 */
public class PortAvailabilityCheck
	implements ServiceAvailabilityCheck
{
	private final int port;

	public PortAvailabilityCheck(int port)
	{
		this.port = port;
	}

	@Override
	public boolean isRunning()
	{
		Socket probe = new Socket();
		boolean alive;

		try {
			InetSocketAddress serviceEndpoint = new InetSocketAddress("127.0.0.1", port);

			probe.connect(serviceEndpoint, 500);
			alive = true;
		} catch (IOException e) {
			alive = false;
		} finally {
			closeQuietly(probe);
		}

		return alive;
	}

	private static void closeQuietly(Socket probe)
	{
		try {
			probe.close();
		} catch (IOException ignored) {
		}
	}

	@Override
	public void close()
	{
		// No operation - No permanent resources allocated
	}
}
