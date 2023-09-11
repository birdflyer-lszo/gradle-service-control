package com.brunoritz.gradle.servicecontrol.common

import spock.lang.Specification

class TaskNameFactorySpec
	extends Specification
{
	def 'It shall format the start task name in the format "start*capitalized-service-name*"'()
	{
		given:
			def serviceName = 'testService'

		when:
			def result = TaskNameFactory.startTaskName(serviceName)

		then:
			result == 'startTestService'
	}

	def 'It shall format the stop task name in the format "stop*capitalized-service-name*"'()
	{
		given:
			def serviceName = 'testService'

		when:
			def result = TaskNameFactory.stopTaskName(serviceName)

		then:
			result == 'stopTestService'
	}

	def 'It shall format the restart task name in the format "restart*capitalized-service-name*"'()
	{
		given:
			def serviceName = 'testService'

		when:
			def result = TaskNameFactory.restartTaskName(serviceName)

		then:
			result == 'restartTestService'
	}

	def 'It shall format a generic task name in the format "*action**capitalized-service-name*"'()
	{
		given:
			def actionName = 'messUp'
			def serviceName = 'testService'

		when:
			def result = TaskNameFactory.taskName(actionName, serviceName)

		then:
			result == 'messUpTestService'
	}
}
