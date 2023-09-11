package com.brunoritz.gradle.servicecontrol.java

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.internal.jvm.Jvm
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class JavaCommandComputerSpec
	extends Specification
{
	def 'It shall use the Java executable file indicated by Gradle'()
	{
		given:
			def service = newJavaService()
			def computer = new JavaCommandComputer(service.argumentsFile, service.mainClass, service.args)

			service.mainClass.set('ch.foo.Bar')

		when:
			def result = computer.compute()

		then:
			result.size() >= 1
			result[0] == Jvm.current().getJavaExecutable().toString()
	}

	def 'It shall pass arguments via an arguments file'()
	{
		given:
			def service = newJavaService()
			def computer = new JavaCommandComputer(service.argumentsFile, service.mainClass, service.args)

			service.mainClass.set('ch.foo.Bar')
			service.argumentsFile.set(new File('arguments.txt'))

		when:
			def result = computer.compute()

		then:
			result.size() >= 2
			result[1] == "@${service.argumentsFile.get()}"
	}

	def 'It shall launch the configured main class'()
	{
		given:
			def service = newJavaService()
			def computer = new JavaCommandComputer(service.argumentsFile, service.mainClass, service.args)

			service.mainClass.set('ch.foo.Bar')

		when:
			def result = computer.compute()

		then:
			result.size() >= 3
			result[2] == 'ch.foo.Bar'
	}

	def 'It shall pass all configured arguments to the main class'()
	{
		given:
			def service = newJavaService()
			def computer = new JavaCommandComputer(service.argumentsFile, service.mainClass, service.args)

			service.mainClass.set('ch.foo.Bar')
			service.getArgs().set(['--first', '--second'])

		when:
			def result = computer.compute()

		then:
			result.size() == 5
			result[2] == 'ch.foo.Bar'
			result[3] == '--first'
			result[4] == '--second'
	}

	private static JavaServiceDefinition newJavaService()
	{
		def project = newProject()
		def javaServices = project.extensions.getByName('javaServiceControl')
			as NamedDomainObjectContainer<JavaServiceDefinition>

		return javaServices.create('javaService')
	}

	private static Project newProject()
	{
		def project = ProjectBuilder.builder().build()

		project.plugins.apply('java')
		project.plugins.apply('com.brunoritz.gradle.java-service-control')

		return project
	}
}
