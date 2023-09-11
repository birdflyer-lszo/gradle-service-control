package com.brunoritz.gradle.servicecontrol

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import static LivenessProbe.serverListeningOnPort
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class JavaServiceControlPluginSpec
	extends Specification
{
	def 'It shall be possible to start an arbitrary Java service and wait for it to open a listening socket'()
	{
		given:
			def projectDirectory = File.createTempDir()
			def buildFile = new File(projectDirectory, 'build.gradle')
			def javaSource = new File(projectDirectory, 'src/main/java/testservice/Main.java')

			buildFile << '''
				plugins {
					id 'java'
					id 'com.brunoritz.gradle.java-service-control'
				}

				javaServiceControl {
					testService {
						mainClass.set('testservice.Main')
						servicePort.set(1981)
					}
				}

				startTestService.dependsOn classes
			'''

			javaSource.parentFile.mkdirs()
			javaSource << '''
				package testservice;

				import java.io.IOException;
				import java.net.ServerSocket;

				public class Main
				{
					public static void main(String... args)
						throws IOException
					{
						try (ServerSocket dummyServer = new ServerSocket(1981)) {
							while (true) {
								dummyServer.accept();
							}
						}
					}
				}
			'''

		when:
			def result = GradleRunner.create()
				.withProjectDir(projectDirectory)
				.withArguments('--configuration-cache', 'startTestService')
				.withPluginClasspath()
				.build()

		then:
			result.task(':startTestService').outcome == SUCCESS
			serverListeningOnPort(1981)
	}

	def 'It shall be possible to start an arbitrary Java service and wait for it to log a specific message'()
	{
		given:
			def projectDirectory = File.createTempDir()
			def buildFile = new File(projectDirectory, 'build.gradle')
			def javaSource = new File(projectDirectory, 'src/main/java/testservice/Main.java')

			buildFile << '''
				plugins {
					id 'java'
					id 'com.brunoritz.gradle.java-service-control'
				}

				javaServiceControl {
					testService {
						mainClass.set('testservice.Main')
						startupLogMessage.set('The eagle has landed')
					}
				}

				startTestService.dependsOn classes
			'''

			javaSource.parentFile.mkdirs()
			javaSource << '''
				package testservice;

				public class Main
				{
					public static void main(String... args)
						throws InterruptedException
					{
						System.out.println("The eagle has landed");
						Thread.currentThread().join();
					}
				}
			'''

		when:
			def result = GradleRunner.create()
				.withProjectDir(projectDirectory)
				.withArguments('--configuration-cache', 'startTestService')
				.withPluginClasspath()
				.build()

		then:
			result.task(':startTestService').outcome == SUCCESS
			serverListeningOnPort(1981)
	}

	def 'It shall be posssible to stop a running Java service'()
	{
		given:
			def projectDirectory = File.createTempDir()
			def buildFile = new File(projectDirectory, 'build.gradle')
			def javaSource = new File(projectDirectory, 'src/main/java/testservice/Main.java')

			buildFile << '''
				plugins {
					id 'java'
					id 'com.brunoritz.gradle.java-service-control'
				}

				javaServiceControl {
					testService {
						mainClass.set('testservice.Main')
						servicePort.set(1982)
					}
				}

				startTestService.dependsOn classes
			'''

			javaSource.parentFile.mkdirs()
			javaSource << '''
				package testservice;

				import java.io.IOException;
				import java.net.ServerSocket;

				public class Main
				{
					public static void main(String... args)
						throws IOException
					{
						try (ServerSocket dummyServer = new ServerSocket(1982)) {
							while (true) {
								dummyServer.accept();
							}
						}
					}
				}
			'''

			GradleRunner.create()
				.withProjectDir(projectDirectory)
				.withArguments('--configuration-cache', 'startTestService')
				.withPluginClasspath()
				.build()

		when:
			def result = GradleRunner.create()
				.withProjectDir(projectDirectory)
				.withArguments('--configuration-cache', 'stopTestService')
				.withPluginClasspath()
				.build()

		then:
			result.task(':stopTestService').outcome == SUCCESS
			!serverListeningOnPort(1982)
	}

	def 'It shall create controlling tasks for each defined service'()
	{
		given:
			def projectDirectory = File.createTempDir()
			def buildFile = new File(projectDirectory, 'build.gradle')

			buildFile << '''
				plugins {
					id 'java'
					id 'com.brunoritz.gradle.java-service-control'
				}

				javaServiceControl {
					fooService {
						mainClass.set('ch.foobar.Main')
					}

					barService {
						mainClass.set('ch.bar.Main')
					}
				}
			'''

		when:
			def result = GradleRunner.create()
				.withProjectDir(projectDirectory)
				.withArguments('--configuration-cache', 'tasks')
				.withPluginClasspath()
				.build()

		then:
			result.output.contains('startFooService')
			result.output.contains('stopFooService')
			result.output.contains('restartFooService')

			result.output.contains('startBarService')
			result.output.contains('stopBarService')
			result.output.contains('restartBarService')

			result.task(':tasks').outcome == SUCCESS
	}

	def 'If a main class is defined, it shall not be changed, even if the application defines another one'()
	{
		given:
			def projectDirectory = File.createTempDir()
			def buildFile = new File(projectDirectory, 'build.gradle')

			buildFile << '''
				plugins {
					id 'application'
					id 'com.brunoritz.gradle.java-service-control'
				}

				application {
					mainClass.set('ch.unexpected.Unexpected')
				}

				javaServiceControl {
					fooService {
						mainClass.set('ch.foobar.Main')
					}
				}

				project.afterEvaluate {
					println("Resulting main class: ${javaServiceControl.fooService.mainClass.get()}")
				}
			'''

		when:
			def result = GradleRunner.create()
				.withProjectDir(projectDirectory)
				.withArguments('--configuration-cache', 'tasks')
				.withPluginClasspath()
				.build()

		then:
			result.output.contains('Resulting main class: ch.foobar.Main')
			result.task(':tasks').outcome == SUCCESS
	}

	def 'If no main class is defined, it shall use the one of the application'()
	{
		given:
			def projectDirectory = File.createTempDir()
			def buildFile = new File(projectDirectory, 'build.gradle')

			buildFile << '''
				plugins {
					id 'application'
					id 'com.brunoritz.gradle.java-service-control'
				}

				application {
					mainClass.set('ch.application.MainClass')
				}

				javaServiceControl {
					fooService {
					}
				}

				project.afterEvaluate {
					println("Resulting main class: ${javaServiceControl.fooService.mainClass.get()}")
				}
			'''

		when:
			def result = GradleRunner.create()
				.withProjectDir(projectDirectory)
				.withArguments('--configuration-cache', 'tasks')
				.withPluginClasspath()
				.build()

		then:
			result.output.contains('Resulting main class: ch.application.MainClass')
			result.task(':tasks').outcome == SUCCESS
	}

	def 'It shall always regenerate the arguments file'()
	{
		given:
			def projectDirectory = File.createTempDir()
			def buildFile = new File(projectDirectory, 'build.gradle')

			buildFile << '''
				plugins {
					id 'java'
					id 'com.brunoritz.gradle.java-service-control'
				}

				javaServiceControl {
					fooService {
						mainClass.set('ch.foobar.Main')
					}
				}
			'''

		when:
			def initialRun = GradleRunner.create()
				.withProjectDir(projectDirectory)
				.withArguments('--configuration-cache', '--build-cache', 'createArgumentsFooService')
				.withPluginClasspath()
				.build()

		then:
			initialRun.task(':createArgumentsFooService').outcome == SUCCESS

		when:
			def unchangedRun = GradleRunner.create()
				.withProjectDir(projectDirectory)
				.withArguments('--configuration-cache', '--build-cache', 'createArgumentsFooService')
				.withPluginClasspath()
				.build()

		then:
			unchangedRun.task(':createArgumentsFooService').outcome == SUCCESS
	}

	def 'It shall fail, if the project does not have the Java plugin applied'()
	{
		given:
			def projectDirectory = File.createTempDir()
			def buildFile = new File(projectDirectory, 'build.gradle')

			buildFile << '''
				plugins {
					id 'com.brunoritz.gradle.java-service-control'
				}
			'''

		when:
			GradleRunner.create()
				.withProjectDir(projectDirectory)
				.withArguments('--configuration-cache', '--build-cache', 'tasks')
				.withPluginClasspath()
				.build()

		then:
			thrown(Exception)
	}
}
