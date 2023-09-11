package com.brunoritz.gradle.servicecontrol.availability

import spock.lang.Specification

import java.nio.file.Files
import java.util.function.Supplier

class LogMessageAvailabilityCheckSpec
	extends Specification
{
	def 'It shall indicate a running service, if the expected message is appended to the log'()
	{
		given:
			def logFile = Files.createTempFile('stdout', 'log').toFile()
			def availabilityCheck = new LogMessageAvailabilityCheck(logFile, 'service started')
			def resultBeforeLog = availabilityCheck.isRunning()

		when:
			logFile << 'the service started successfully\n'

		then:
			!resultBeforeLog

			conditionFulfilled(() -> availabilityCheck.isRunning())
	}

	def 'It shall not indicate a running service, if the expected message is not appended to the log'()
	{
		given:
			def logFile = Files.createTempFile('stdout', 'log').toFile()
			def availabilityCheck = new LogMessageAvailabilityCheck(logFile, 'service started')

		when:
			logFile << 'a log message, but not the one we are looking for\n'

		then:
			!conditionFulfilled(() -> availabilityCheck.isRunning())
	}

	def 'It shall indicate a running service, if the expected message already exists in the log'()
	{
		given:
			def logFile = Files.createTempFile('stdout', 'log').toFile()
			def availabilityCheck = new LogMessageAvailabilityCheck(logFile, 'service started')

		when:
			logFile << 'the service started successfully\n'

		then:
			conditionFulfilled(() -> availabilityCheck.isRunning())
	}

	private boolean conditionFulfilled(Supplier<Boolean> condition)
	{
		def result = false
		def latestWait = System.currentTimeMillis() + 5_000

		while (!result && System.currentTimeMillis() < latestWait) {
			result = condition.get()

			if (!result) {
				Thread.sleep(500)
			}
		}

		return result
	}
}
