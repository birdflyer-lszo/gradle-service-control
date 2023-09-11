package com.brunoritz.gradle.servicecontrol.launch

import com.brunoritz.gradle.servicecontrol.common.PidFile
import spock.lang.Specification

class RunningServiceSpec
	extends Specification
{
	def 'It shall record the PID of the service process to the file'()
	{
		given:
			def process = Mock(Process)
			def pidLocation = File.createTempFile('serivce', 'pid')
			def pidFile = PidFile.fromExisting(pidLocation).get()
			def runningService = new RunningService(process, pidFile)

		when:
			def result = runningService.recordProcessId()

		then:
			1 * process.pid() >> 1234

		then:
			result.isSuccess()
			pidLocation.text.trim() == '1234'
	}
}
