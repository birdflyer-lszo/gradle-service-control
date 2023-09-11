package com.brunoritz.gradle.servicecontrol.availability

import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PortAvailabilityCheckSpec
	extends Specification
{
	def 'It shall indicate the service be unavailable, if no connection can be established'()
	{
		given:
			def availabilityCheck = new PortAvailabilityCheck(1234)

		when:
			def result = availabilityCheck.isRunning()

		then:
			!result
	}

	def 'It shall succeed, if the TCP port becomes alive within the timeout'()
	{
		given:
			def latch = new CountDownLatch(1)
			def serverSocket = new ServerSocket(0)
			def availabilityCheck = new PortAvailabilityCheck(serverSocket.getLocalPort())

			def simulatedServer = new Thread(() -> {
				serverSocket.accept()
				latch.countDown()
			})

			simulatedServer.start()

		when:
			def result = availabilityCheck.isRunning()

		then:
			result
			latch.await(1, TimeUnit.SECONDS)
			latch.count == 0

		cleanup:
			simulatedServer.interrupt()
	}
}
