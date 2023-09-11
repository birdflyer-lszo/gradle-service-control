package com.brunoritz.gradle.servicecontrol.generic

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class GenericCommandComputerSpec
	extends Specification
{
	def 'It shall use the executable file specified in the service definition'()
	{
		given:
			def service = newGenericService()
			def computer = new GenericCommandComputer(service.executable, service.args)

			service.executable.set('/bin/bash')

		when:
			def result = computer.compute()

		then:
			result.size() >= 1
			result[0] == '/bin/bash'
	}

	def 'It shall pass all configured arguments to the main class'()
	{
		given:
			def service = newGenericService()
			def computer = new GenericCommandComputer(service.executable, service.args)

			service.executable.set('/bin/bash')
			service.getArgs().set(['--first', '--second'])

		when:
			def result = computer.compute()

		then:
			result.size() == 3
			result[0] == '/bin/bash'
			result[1] == '--first'
			result[2] == '--second'
	}

	private static GenericServiceDefinition newGenericService()
	{
		def project = newProject()
		def genericServices = project.extensions.getByName('genericServiceControl')
			as NamedDomainObjectContainer<GenericServiceDefinition>

		return genericServices.create('genericService')
	}

	private static Project newProject()
	{
		def project = ProjectBuilder.builder().build()

		project.plugins.apply('com.brunoritz.gradle.generic-service-control')

		return project
	}
}
