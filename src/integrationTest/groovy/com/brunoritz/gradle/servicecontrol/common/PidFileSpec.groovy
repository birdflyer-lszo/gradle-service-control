package com.brunoritz.gradle.servicecontrol.common

import spock.lang.Specification
import spock.lang.Unroll

class PidFileSpec
	extends Specification
{
	def 'It shall only create an empty PID file, if none exists'()
	{
		given:
			def tempDirectory = File.createTempDir()
			def pidLocation = new File(tempDirectory, 'service.pid')

		when:
			def result = PidFile.createEmpty(pidLocation)

		then:
			result.isSuccess()
			pidLocation.exists()
	}

	def 'It shall fail creating an empty PID file, if one already exists'()
	{
		given:
			def pidLocation = File.createTempFile('service', 'pid')

		when:
			def result = PidFile.createEmpty(pidLocation)

		then:
			result.isFailure()
	}

	def 'It shall return a PID file instance, if the backing file exists'()
	{
		given:
			def pidLocation = File.createTempFile('service', 'pid')

		when:
			def result = PidFile.fromExisting(pidLocation)

		then:
			result.isDefined()
	}

	def 'It shall not return a PID file instance, if the backing file does not exist'()
	{
		given:
			def tempDirectory = File.createTempDir()
			def pidLocation = new File(tempDirectory, 'service.pid')

		when:
			def result = PidFile.fromExisting(pidLocation)

		then:
			result.isEmpty()
	}

	def 'It shall read the process ID from the file, if it contains a numeric ID'()
	{
		given:
			def pidLocation = File.createTempFile('service', 'pid')
			def pidFile = PidFile.fromExisting(pidLocation).get()

			pidLocation.text = '1234'

		when:
			def result = pidFile.readNumericPid()

		then:
			result.isSuccess()
			result.get().isDefined()
			result.get().get() == 1234
	}

	def 'It shall return no PID, if the file is empty'()
	{
		given:
			def pidLocation = File.createTempFile('service', 'pid')
			def pidFile = PidFile.fromExisting(pidLocation).get()

			pidLocation.text = ''

		when:
			def result = pidFile.readNumericPid()

		then:
			result.isSuccess()
			result.get().isEmpty()
	}

	@Unroll
	def 'It shall fail reading the process ID, if the file contains invalid data'(String content)
	{
		given:
			def pidLocation = File.createTempFile('service', 'pid')
			def pidFile = PidFile.fromExisting(pidLocation).get()

			pidLocation.text = content

		when:
			def result = pidFile.readNumericPid()

		then:
			result.isFailure()

		where:
			content     | _
			'not-a-pid' | _
			'1234x'     | _
	}

	def 'It shall record the PID of a given process'()
	{
		given:
			def pidLocation = File.createTempFile('service', 'pid')
			def process = Mock(Process)
			def pidFile = PidFile.fromExisting(pidLocation).get()

		when:
			pidFile.recordPid(process)

		then:
			1 * process.pid() >> 1234

		then:
			pidLocation.text == '1234'
	}
}
