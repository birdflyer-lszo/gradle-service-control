package com.brunoritz.gradle.servicecontrol.launch

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import java.time.Duration

import static com.brunoritz.gradle.servicecontrol.ServiceFactory.createJavaService

class StartServiceTaskSpec
	extends Specification
{
	def 'It shall fail, if the PID file exists at the time of invocation'()
	{
		given:
			def project = newProject()
			def service = createJavaService(project) {
				mainClass.set('com.brunoritz.gradle.servicecontrol.launch.SimulatedService')
				servicePort.set(7171)
				startTimeout.set(Duration.ofSeconds(5))
			}

			service.pidFile.get().asFile.text = 'already-existing'

		when:
			startTaskIsExecuted(project)

		then:
			def error = thrown(IllegalStateException)

			error.message == 'PID file could not be created or is in use'
	}

	def 'It shall succeed, if the service becomes available after starting'()
	{
		given:
			def project = newProject()
			def service = createJavaService(project) {
				mainClass.set('com.brunoritz.gradle.servicecontrol.launch.SimulatedService')
				servicePort.set(7171)
				startTimeout.set(Duration.ofSeconds(5))
			}

			service.argumentsFile.get().asFile.text = "-cp ${System.getProperty('java.class.path')}"

		when:
			startTaskIsExecuted(project)

		then:
			def pid = service.pidFile.get().asFile.text
			def process = ProcessHandle.of(Long.parseLong(pid))

			noExceptionThrown()
			process.isPresent()
			process.get().isAlive()

		cleanup:
			process.ifPresent(running -> running.destroy())
	}

	def 'It shall fail, if the service does not become available within a defined timeout'()
	{
		given:
			def project = newProject()
			def service = createJavaService(project) {
				mainClass.set('com.brunoritz.gradle.servicecontrol.launch.SimulatedService')
				servicePort.set(7272)
				startTimeout.set(Duration.ofSeconds(5))
			}

			service.argumentsFile.get().asFile.text = "-cp ${System.getProperty('java.class.path')}"

		when:
			startTaskIsExecuted(project)

		then:
			thrown(IllegalStateException)
			!service.pidFile.get().asFile.exists()
	}

	def 'It shall delete the PID file, if the process fails to start'()
	{
		given:
			def project = newProject()
			def service = createJavaService(project) {
				mainClass.set('com.brunoritz.gradle.servicecontrol.launch.InexistentService')
				servicePort.set(7272)
				startTimeout.set(Duration.ofSeconds(5))
				workingDirectory.set(new File('inexistent'))
			}

			service.argumentsFile.get().asFile.text = "-cp ${System.getProperty('java.class.path')}"

		when:
			startTaskIsExecuted(project)

		then:
			thrown(IllegalStateException)
			!service.pidFile.get().asFile.exists()
	}

	private static Project newProject()
	{
		def project = ProjectBuilder.builder().build()

		project.plugins.apply('java')
		project.plugins.apply('com.brunoritz.gradle.java-service-control')

		project.layout.buildDirectory.get().asFile.mkdirs()

		return project
	}

	private static void startTaskIsExecuted(Project project)
	{
		def task = project.tasks.getByPath('startJavaService') as StartServiceTask

		task.startService()
	}
}
