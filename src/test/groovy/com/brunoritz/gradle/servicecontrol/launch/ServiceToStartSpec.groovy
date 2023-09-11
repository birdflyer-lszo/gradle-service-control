package com.brunoritz.gradle.servicecontrol.launch

import com.brunoritz.gradle.servicecontrol.common.PidFile
import com.brunoritz.gradle.servicecontrol.java.JavaServiceDefinition
import io.vavr.collection.HashMap
import io.vavr.collection.List
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static io.vavr.control.Try.failure
import static io.vavr.control.Try.success

class ServiceToStartSpec
	extends Specification
{
	def 'It shall return the starting service, if the process could be launched'()
	{
		given:
			def project = newProject()
			def serviceDefinition = newServiceDefinition(project)
			def command = List.<String> empty()
				.append('/bin/java')
				.append('ch.foo.Bar')
			def pidFile = PidFile.createEmpty(project.file('service.pid')).get()
			def launcher = Mock(ProcessLauncher)
			def serviceProcess = Mock(Process)
			def service = new ServiceToStart(
				command,
				pidFile,
				() -> launcher,
				serviceDefinition.startTimeout,
				serviceDefinition.workingDirectory,
				serviceDefinition.standardOutputLog,
				serviceDefinition.errorOutputLog,
				serviceDefinition.environmentFiles,
				serviceDefinition.environment
			)

			serviceDefinition.servicePort.set(1234)

		when:
			def result = service.start()

		then:
			1 * launcher.command(_) >> launcher
			1 * launcher.workingDirectory(_) >> launcher
			1 * launcher.storeStdOutIn(_) >> launcher
			1 * launcher.storeStdErrIn(_) >> launcher
			1 * launcher.appendEnvironment(_) >> launcher
			1 * launcher.start() >> success(serviceProcess)

			result.isSuccess()
	}

	def 'It shall return the error, if the process failed to launch'()
	{
		given:
			def project = newProject()
			def serviceDefinition = newServiceDefinition(project)
			def command = List.<String> empty()
				.append('/bin/java')
				.append('ch.foo.Bar')
			def pidFile = PidFile.createEmpty(project.file('service.pid')).get()
			def launcher = Mock(ProcessLauncher)
			def service = new ServiceToStart(
				command,
				pidFile,
				() -> launcher,
				serviceDefinition.startTimeout,
				serviceDefinition.workingDirectory,
				serviceDefinition.standardOutputLog,
				serviceDefinition.errorOutputLog,
				serviceDefinition.environmentFiles,
				serviceDefinition.environment
			)

			serviceDefinition.servicePort.set(1234)

		when:
			def result = service.start()

		then:
			1 * launcher.command(_) >> launcher
			1 * launcher.workingDirectory(_) >> launcher
			1 * launcher.storeStdOutIn(_) >> launcher
			1 * launcher.storeStdErrIn(_) >> launcher
			1 * launcher.appendEnvironment(_) >> launcher
			1 * launcher.start() >> failure(new IOException('simulated failure'))

			result.isFailure()
	}

	def 'It shall apply the settings provided via the extension'()
	{
		given:
			def project = newProject()
			def serviceDefinition = newServiceDefinition(project)
			def command = List.<String> empty()
				.append('/bin/java')
				.append('ch.foo.Bar')
			def pidFile = PidFile.createEmpty(project.file('service.pid')).get()
			def launcher = Mock(ProcessLauncher)
			def environmentFile = File.createTempFile('environment', 'env')
			def serviceProcess = Mock(Process)
			def service = new ServiceToStart(
				command,
				pidFile,
				() -> launcher,
				serviceDefinition.startTimeout,
				serviceDefinition.workingDirectory,
				serviceDefinition.standardOutputLog,
				serviceDefinition.errorOutputLog,
				serviceDefinition.environmentFiles,
				serviceDefinition.environment
			)

			with(serviceDefinition) {
				environment.set([
					FOO: 'FOO_VALUE',
					BAR: 'BAR_VALUE'
				])
				environmentFiles(environmentFile)
				servicePort.set(1234)
				workingDirectory.set(project.file('working-dir'))
				standardOutputLog.set(project.file('stdout.log'))
				errorOutputLog.set(project.file('error.log'))
			}

			environmentFile.text = 'BAZ=BAZ_VALUE'

		when:
			service.start()

		then:
			1 * launcher.command(List.of('/bin/java', 'ch.foo.Bar')) >> launcher
			1 * launcher.workingDirectory(project.file('working-dir')) >> launcher
			1 * launcher.storeStdOutIn(project.file('stdout.log')) >> launcher
			1 * launcher.storeStdErrIn(project.file('error.log')) >> launcher
			1 * launcher.appendEnvironment(HashMap.ofAll([
				FOO: 'FOO_VALUE',
				BAR: 'BAR_VALUE',
				BAZ: 'BAZ_VALUE'
			])) >> launcher

		then:
			1 * launcher.start() >> success(serviceProcess)
	}

	def 'Environment variables set via the map shall take precedence over those set via files'()
	{
		given:
			def project = newProject()
			def serviceDefinition = newServiceDefinition(project)
			def command = List.<String> empty()
				.append('/bin/java')
				.append('ch.foo.Bar')
			def pidFile = PidFile.createEmpty(project.file('service.pid')).get()
			def launcher = Mock(ProcessLauncher)
			def environmentFile = File.createTempFile('environment', 'env')
			def serviceProcess = Mock(Process)
			def service = new ServiceToStart(
				command,
				pidFile,
				() -> launcher,
				serviceDefinition.startTimeout,
				serviceDefinition.workingDirectory,
				serviceDefinition.standardOutputLog,
				serviceDefinition.errorOutputLog,
				serviceDefinition.environmentFiles,
				serviceDefinition.environment
			)

			with(serviceDefinition) {
				environmentFiles(environmentFile)
				environment.set([
					FOO: 'FOO_VALUE',
					BAR: 'BAR_VALUE'
				])
				servicePort.set(1234)
			}

			environmentFile.text = '''
				FOO=UNEXPECTED
				'''

		when:
			service.start()

		then:
			1 * launcher.command(List.of('/bin/java', 'ch.foo.Bar')) >> launcher
			1 * launcher.workingDirectory(_) >> launcher
			1 * launcher.storeStdOutIn(_) >> launcher
			1 * launcher.storeStdErrIn(_) >> launcher
			1 * launcher.appendEnvironment(HashMap.ofAll([
				FOO: 'FOO_VALUE',
				BAR: 'BAR_VALUE',
			])) >> launcher

		then:
			1 * launcher.start() >> success(serviceProcess)
	}

	private static Project newProject()
	{
		def project = ProjectBuilder.builder().build()

		project.plugins.apply('java')
		project.plugins.apply('com.brunoritz.gradle.java-service-control')

		return project
	}

	private static JavaServiceDefinition newServiceDefinition(Project project)
	{
		def controlExtension = project.extensions.getByName('javaServiceControl')
			as NamedDomainObjectContainer<JavaServiceDefinition>

		return controlExtension.create('testingService')
	}
}
