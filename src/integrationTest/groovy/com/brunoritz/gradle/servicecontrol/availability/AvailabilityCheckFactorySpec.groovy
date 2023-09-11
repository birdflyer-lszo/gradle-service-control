package com.brunoritz.gradle.servicecontrol.availability

import com.brunoritz.gradle.servicecontrol.java.JavaServiceDefinition
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class AvailabilityCheckFactorySpec
	extends Specification
{
	def 'It shall return a TCP port checker, if the service has a defined port number'()
	{
		given:
			def definition = serviceDefinition()

			definition.servicePort.set(1234)

		when:
			def result = AvailabilityCheckFactory.checkFromDefinition(
				definition.servicePort,
				definition.startupLogMessage,
				definition.standardOutputLog
			)

		then:
			result.isDefined()
			result.get() instanceof PortAvailabilityCheck
	}

	def 'It shall return a log message checker, if the service has a defined success message'()
	{
		given:
			def definition = serviceDefinition()

			definition.startupLogMessage.set('blah blah')

		when:
			def result = AvailabilityCheckFactory.checkFromDefinition(
				definition.servicePort,
				definition.startupLogMessage,
				definition.standardOutputLog
			)

		then:
			result.isDefined()
			result.get() instanceof LogMessageAvailabilityCheck
	}

	def 'It shall return a TCP port checker, if the service has a defined port number and success message'()
	{
		given:
			def definition = serviceDefinition()

			definition.servicePort.set(1234)
			definition.startupLogMessage.set('blah blah')

		when:
			def result = AvailabilityCheckFactory.checkFromDefinition(
				definition.servicePort,
				definition.startupLogMessage,
				definition.standardOutputLog
			)

		then:
			result.isDefined()
			result.get() instanceof PortAvailabilityCheck
	}

	def 'It shall return no checker, if both TCP port number and success log message are missing'()
	{
		given:
			def definition = serviceDefinition()

		when:
			def result = AvailabilityCheckFactory.checkFromDefinition(
				definition.servicePort,
				definition.startupLogMessage,
				definition.standardOutputLog
			)

		then:
			result.isEmpty()
	}

	private static JavaServiceDefinition serviceDefinition()
	{
		def project = ProjectBuilder.builder().build()

		project.plugins.apply('com.brunoritz.gradle.java-service-control')

		return project.extensions.getByName('javaServiceControl').create('testingService')
	}
}
