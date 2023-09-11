package com.brunoritz.gradle.servicecontrol

final class LivenessProbe
{
	private LivenessProbe()
	{
		throw new UnsupportedOperationException()
	}

	static boolean serverListeningOnPort(int number)
	{
		Socket probe = new Socket()

		try {
			InetSocketAddress serviceEndpoint = new InetSocketAddress('127.0.0.1', number)

			probe.connect(serviceEndpoint)

			return true
		} catch (IOException ignored) {
			return false
		} finally {
			closeQuietly(probe)
		}
	}

	private static void closeQuietly(Socket probe)
	{
		try {
			probe.close()
		} catch (IOException ignored) {
		}
	}
}
