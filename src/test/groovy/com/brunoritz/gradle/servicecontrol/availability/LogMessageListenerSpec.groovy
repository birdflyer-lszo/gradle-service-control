package com.brunoritz.gradle.servicecontrol.availability

import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.atomic.AtomicBoolean

class LogMessageListenerSpec
	extends Specification
{
	@Unroll
	def 'It shall signal the service running, once the expected log message fragment is encountered'(
		String logMessage, String expectedPartial, boolean match)
	{
		given:
			def running = new AtomicBoolean()
			def listener = new LogMessageListener(expectedPartial, running)

		when:
			listener.handle(logMessage)

		then:
			running.get() == match

		where:
			logMessage              | expectedPartial | match
			'prefix SUCCESS suffix' | 'SUCCESS'       | true
			'prefix FAILURE suffix' | 'SUCCESS'       | false
			'SUCCESS'               | 'SUCCESS'       | true
	}
}
