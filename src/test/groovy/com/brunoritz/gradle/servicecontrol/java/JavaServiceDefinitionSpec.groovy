package com.brunoritz.gradle.servicecontrol.java

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import java.time.Duration

class JavaServiceDefinitionSpec
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

	def 'It shall configure the default arguments file name'()
	{
		given:
			def project = newProject()
			def service = newServiceDefinition(project)

		expect:
			service.argumentsFile.get() == project.layout.buildDirectory.file("jvmargs.${service.name}.txt").get()
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

	def 'It shall be possible to define JVM arguments'()
	{
		given:
			def project = newProject()
			def service = newServiceDefinition(project)

		when:
			service.jvmArgs.set([
				'--first-argument', '--second-argument'
			])

		then:
			service.jvmArgs.get() == [
				'--first-argument',
				'--second-argument'
			]
	}

	def 'It shall be possible to extend JVM arguments'()
	{
		given:
			def project = newProject()
			def service = newServiceDefinition(project)

			service.jvmArgs.set([
				'--first-argument'
			])

		when:
			service.jvmArg('--second-argument')

		then:
			service.jvmArgs.get() == [
				'--first-argument',
				'--second-argument'
			]
	}

	def 'It shall be possible to define system properties'()
	{
		given:
			def project = newProject()
			def service = newServiceDefinition(project)

		when:
			service.systemProperties.set([
				foo: 'bar',
				bar: 'bar'
			])

		then:
			service.systemProperties.get() == [
				foo: 'bar',
				bar: 'bar'
			]
	}

	def 'It shall be possible to extend system properties'()
	{
		given:
			def project = newProject()
			def service = newServiceDefinition(project)

			service.systemProperties.set([
				foo: 'foo'
			])

		when:
			service.systemProperty('bar', 'bar')

		then:
			service.systemProperties.get() == [
				foo: 'foo',
				bar: 'bar'
			]
	}

	private static Project newProject()
	{
		def project = ProjectBuilder.builder().build()

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
