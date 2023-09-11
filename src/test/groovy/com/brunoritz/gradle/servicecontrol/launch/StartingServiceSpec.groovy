package com.brunoritz.gradle.servicecontrol.launch

import com.brunoritz.gradle.servicecontrol.common.PidFile
import spock.lang.Specification

import java.time.Duration

class StartingServiceSpec
	extends Specification
{
	def 'It shall fail, if the process dies during availability probing'()
	{
		given:
			def process = Mock(Process)
			def pidLocation = File.createTempFile('serivce', 'pid')
			def pidFile = PidFile.fromExisting(pidLocation).get()
			def availabilityCheck = Mock(ServiceAvailabilityCheck)
			def startingService = new StartingService(process, Duration.ofSeconds(3), pidFile)

		when:
			def result = startingService.awaitStartup(availabilityCheck)

		then:
			_ * availabilityCheck.isRunning() >> false
			1 * process.isAlive() >> false
			result.isLeft()
	}

	def 'It shall fail, if the service does not become available within the timeout'()
	{
		given:
			def process = Mock(Process)
			def pidLocation = File.createTempFile('serivce', 'pid')
			def pidFile = PidFile.fromExisting(pidLocation).get()
			def availabilityCheck = Mock(ServiceAvailabilityCheck)
			def startingService = new StartingService(process, Duration.ofSeconds(3), pidFile)

		when:
			def result = startingService.awaitStartup(availabilityCheck)

		then:
			(1.._) * availabilityCheck.isRunning() >> false
			_ * process.isAlive() >> true
			result.isLeft()
	}

	def 'It shall succeed, if the service becomes alive within the timeout'()
	{
		given:
			def process = Mock(Process)
			def pidLocation = File.createTempFile('serivce', 'pid')
			def pidFile = PidFile.fromExisting(pidLocation).get()
			def availabilityCheck = Mock(ServiceAvailabilityCheck)
			def startingService = new StartingService(process, Duration.ofSeconds(3),
				pidFile)

		when:
			def result = startingService.awaitStartup(availabilityCheck)

		then:
			(1.._) * availabilityCheck.isRunning() >> true
			_ * process.isAlive() >> true
			result.isRight()
	}
}
