package com.brunoritz.gradle.servicecontrol.common

import com.brunoritz.gradle.servicecontrol.generic.GenericServiceDefinition
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.IgnoreIf
import spock.lang.Specification

class StopServiceTaskSpec
	extends Specification
{
	def 'It shall not fail, if the PID file does not exist'()
	{
		given:
			def project = newProject()

			createGenericService(project)

		when:
			stopTaskIsExecuted(project)

		then:
			noExceptionThrown()
	}

	def 'It shall proceed without errors, if the PID fie is empty'()
	{
		given:
			def project = newProject()
			def serviceDefinition = createGenericService(project)

			serviceDefinition.pidFile.get().asFile.text = ''

		when:
			stopTaskIsExecuted(project)

		then:
			noExceptionThrown()
	}

	def 'It shall fail, if the PID file contains a non-numeric ID'()
	{
		given:
			def project = newProject()
			def serviceDefinition = createGenericService(project)

			serviceDefinition.pidFile.get().asFile.text = 'xx1234'

		when:
			stopTaskIsExecuted(project)

		then:
			def error = thrown(IllegalStateException)

			error.message == 'Failed to parse stored PID'
	}

	@IgnoreIf({ System.getProperty('os.name').containsIgnoreCase('windows') })
	def 'It shall terminate the process identified in the PID file'()
	{
		given:
			def project = newProject()
			def process = runningBackgroundProcess()
			def serviceDefinition = createGenericService(project)

			serviceDefinition.pidFile.get().asFile.text = process.pid()

		expect:
			process.isAlive()

		when:
			stopTaskIsExecuted(project)

		then:
			!process.isAlive()
	}

	private static Project newProject()
	{
		def project = ProjectBuilder.builder().build()

		project.plugins.apply('com.brunoritz.gradle.generic-service-control')
		project.layout.buildDirectory.get().asFile.mkdirs()

		return project
	}

	private static GenericServiceDefinition createGenericService(Project project)
	{
		def configuration = project.extensions.getByName('genericServiceControl')
			as NamedDomainObjectContainer<GenericServiceDefinition>

		return configuration.create('genericService')
	}

	private static Process runningBackgroundProcess()
	{
		def processOutput = File.createTempFile('process', 'log')

		processOutput.deleteOnExit()

		return new ProcessBuilder(['ping', '127.0.0.1'])
			.redirectErrorStream(true)
			.redirectOutput(processOutput)
			.start()
	}

	private static void stopTaskIsExecuted(Project project)
	{
		def task = project.tasks.getByPath('stopGenericService') as StopServiceTask

		task.stopService()
	}
}
