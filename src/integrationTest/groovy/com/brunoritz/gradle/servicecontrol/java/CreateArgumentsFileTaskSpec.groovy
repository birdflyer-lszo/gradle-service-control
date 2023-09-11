package com.brunoritz.gradle.servicecontrol.java

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static com.brunoritz.gradle.servicecontrol.ServiceFactory.createJavaService

class CreateArgumentsFileTaskSpec
	extends Specification
{
	def 'It shall configure a Java Agent, if one is defined'()
	{
		given:
			def project = newProject()
			def agentConfiguration = project.configurations.create('simulatedAgent')
			def agentLibrary = project.files('agent.jar')

			createJavaService(project) {
				mainClass.set('ch.foo.Bar')
				servicePort.set(1234)
				agent.set(agentConfiguration)
				agentArgs.set('foo=bar')
			}

			project.dependencies {
				simulatedAgent agentLibrary
			}

		when:
			argFileTaskIsExecuted(project)

		then:
			def arguments = fileContent(project, 'jvmargs.javaService.txt')
			def agentLibPath = agentLibrary.files.toArray()[0].getAbsolutePath()

			arguments.startsWith("-javaagent:${agentLibPath}=foo=bar ")
	}

	def 'It shall fail, if the agent configuration contains more than one file'()
	{
		given:
			def project = newProject()
			def agentConfiguration = project.configurations.create('simulatedAgent')
			def agentLibrary = project.files('agent.jar', 'should-not-be-here.jar')

			createJavaService(project) {
				mainClass.set('ch.foo.Bar')
				servicePort.set(1234)
				agent.set(agentConfiguration)
			}

			project.dependencies {
				simulatedAgent agentLibrary
			}

		when:
			argFileTaskIsExecuted(project)

		then:
			def error = thrown(IllegalArgumentException)

			error.message == 'Agent configuration may only contain one single file'
	}

	def 'It shall configure a debugger, if the debugger port is set'()
	{
		given:
			def project = newProject()

			createJavaService(project) {
				mainClass.set('ch.foo.Bar')
				servicePort.set(1234)
				debugPort.set(5678)
			}

		when:
			argFileTaskIsExecuted(project)

		then:
			def arguments = fileContent(project, 'jvmargs.javaService.txt')

			arguments.startsWith('-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5678 ')
	}

	def 'It shall not configure a debugger, if the debug port is not set'()
	{
		given:
			def project = newProject()

			createJavaService(project) {
				mainClass.set('ch.foo.Bar')
				servicePort.set(1234)
			}

		when:
			argFileTaskIsExecuted(project)

		then:
			def arguments = fileContent(project, 'jvmargs.javaService.txt')

			!arguments.contains('-agentlib:jdwp')
	}

	def 'It shall pass any additional JVM argument, if defined'()
	{
		given:
			def project = newProject()

			createJavaService(project) {
				mainClass.set('ch.foo.Bar')
				servicePort.set(1234)
				jvmArgs.set([
					'-Xms64m',
					'-Xms256m'])
			}

		when:
			argFileTaskIsExecuted(project)

		then:
			def arguments = fileContent(project, 'jvmargs.javaService.txt')

			arguments.startsWith('-Xms64m -Xms256m ')
	}

	def 'It shall configure the runtime classpath argument'()
	{
		given:
			def project = newProject()

			createJavaService(project) {
				mainClass.set('ch.foo.Bar')
				servicePort.set(1234)
			}

		when:
			argFileTaskIsExecuted(project)

		then:
			def actualClasspath = fileContent(project, 'jvmargs.javaService.txt')
				.replaceAll('-cp ([^ ]*)', '$1')
				.split(File.pathSeparator) as Set
			def expectedClasspath = project
				.getExtensions()
				.getByType(JavaPluginExtension.class)
				.getSourceSets()
				.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
				.getRuntimeClasspath()
				.getFiles()
				.collect { file -> file.getAbsolutePath() } as Set

			actualClasspath == expectedClasspath
	}

	def 'It shall pass any given system property'()
	{
		given:
			def project = newProject()

			createJavaService(project) {
				mainClass.set('ch.foo.Bar')
				servicePort.set(1234)
				systemProperty('fooProperty', 'fooValue')
				systemProperty('barProperty', 'barValue')
			}

		when:
			argFileTaskIsExecuted(project)

		then:
			def arguments = fileContent(project, 'jvmargs.javaService.txt')

			arguments.endsWith(' -DfooProperty=fooValue -DbarProperty=barValue')
	}

	private static Project newProject()
	{
		def project = ProjectBuilder.builder().build()

		project.plugins.apply('java')
		project.plugins.apply('com.brunoritz.gradle.java-service-control')

		project.layout.buildDirectory.get().asFile.mkdirs()

		return project
	}

	private static void argFileTaskIsExecuted(Project project)
	{
		def task = project.tasks.getByPath('createArgumentsJavaService') as CreateArgumentsFileTask

		task.createFile()
	}

	private static String fileContent(Project project, String fileName)
	{
		return new File(project.layout.buildDirectory.get().asFile, fileName).text.trim()
	}
}
