package com.brunoritz.gradle.servicecontrol.launch

import com.brunoritz.gradle.servicecontrol.common.PidFile
import spock.lang.Specification

class FailedServiceSpec
	extends Specification
{
	def 'It shall terminate the process and delete the PID file when cleaning up'()
	{
		given:
			def process = Mock(Process)
			def pidFile = File.createTempFile('service', 'pid')
			def failedService = new FailedService(process, PidFile.fromExisting(pidFile).get())

		when:
			failedService.cleanupService()

		then:
			1 * process.destroy()
			!pidFile.exists()
	}
}
