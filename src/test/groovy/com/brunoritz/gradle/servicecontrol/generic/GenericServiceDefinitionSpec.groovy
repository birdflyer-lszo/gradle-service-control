package com.brunoritz.gradle.servicecontrol.generic

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import java.time.Duration

class GenericServiceDefinitionSpec
	extends Specification
{
	def 'It shall configure the default working directory to be the project directry'()
	{
		given:
			def project = newProject()
			def service = newServiceDefinition(project)

		expect:
			service.workingDirectory.get().asFile == project.projectDir
	}

	def 'It shall configure the default PID file name'()
	{
		given:
			def project = newProject()
			def service = newServiceDefinition(project)

		expect:
			service.pidFile.get().asFile == project.file("service.${service.name}.pid")
	}

	def 'It shall configure the default output and error log file names'()
	{
		given:
			def project = newProject()
			def service = newServiceDefinition(project)

		expect:
			def logsDir = new File(project.projectDir, 'logs')

			service.standardOutputLog.get().asFile == new File(logsDir, "stdout.${service.name}.log")
			service.errorOutputLog.get().asFile == new File(logsDir, "stderr.${service.name}.log")
	}

	def 'It shall configure a default startup timeout of 10 minutes'()
	{
		given:
			def project = newProject()
			def service = newServiceDefinition(project)

		expect:
			service.startTimeout.get() == Duration.ofMinutes(10)
	}

	def 'It shall be possible to extend the existing list of environment files'()
	{
		given:
			def project = newProject()
			def service = newServiceDefinition(project)

			service.getEnvironmentFiles().set([new File('foo'), new File('bar')])

		when:
			service.environmentFiles(new File('baz'))

		then:
			service.environmentFiles.get() == [
				new File('foo'),
				new File('bar'),
				new File('baz')
			]
	}

	def 'It shall be possible to extend existing environent variables'()
	{
		given:
			def project = newProject()
			def service = newServiceDefinition(project)

			service.environment.set([
				FOO: 'bar'
			])

		when:
			service.environment([
				BAR: 'bar'
			])

		then:
			service.environment.get() == [
				FOO: 'bar',
				BAR: 'bar'
			]
	}

	private static Project newProject()
	{
		def project = ProjectBuilder.builder().build()

		project.plugins.apply('com.brunoritz.gradle.generic-service-control')

		return project
	}

	private static GenericServiceDefinition newServiceDefinition(Project project)
	{
		def controlExtension = project.extensions.getByName('genericServiceControl')
			as NamedDomainObjectContainer<GenericServiceDefinition>

		return controlExtension.create('testingService')
	}
}
