package com.brunoritz.gradle.servicecontrol

import org.gradle.testkit.runner.GradleRunner
import spock.lang.IgnoreIf
import spock.lang.Specification

import static LivenessProbe.serverListeningOnPort
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class GenericServiceControlPluginSpec
	extends Specification
{
	// Note: If this test fails, consider disabling the local firewall. Some firewalls might prompt to allow incoming
	// connections, but the prompt might only stay visible for a few seconds.
	def 'It shall be possible to start an arbitrary generic service and wait for it to open a listening socket'()
	{
		given:
			def projectDirectory = File.createTempDir()
			def buildFile = new File(projectDirectory, 'build.gradle')

			buildFile << '''
				plugins {
					id 'com.brunoritz.gradle.generic-service-control'
				}

				genericServiceControl {
					netCat {
						executable.set('nc')
						servicePort.set(1983)
						args.set(['-k', '-l', '1983'])
					}
				}
			'''

		when:
			def result = GradleRunner.create()
				.withProjectDir(projectDirectory)
				.withArguments('--configuration-cache', 'startNetCat')
				.withPluginClasspath()
				.build()

		then:
			result.task(':startNetCat').outcome == SUCCESS
			serverListeningOnPort(1983)


		cleanup:
			println('***************')
			println(new File(projectDirectory, 'logs/stdout.netCat.log').text)
			println(new File(projectDirectory, 'logs/stderr.netCat.log').text)
	}

	def 'It shall be possible to start an arbitrary generic service and wait for it to log a specific message'()
	{
		given:
			def projectDirectory = File.createTempDir()
			def buildFile = new File(projectDirectory, 'build.gradle')

			buildFile << '''
				plugins {
					id 'com.brunoritz.gradle.generic-service-control'
				}

				genericServiceControl {
					pingService {
						executable.set('ping')
						args.set(['127.0.0.1'])

						startupLogMessage.set('127.0.0.1')
					}
				}
			'''

		when:
			def result = GradleRunner.create()
				.withProjectDir(projectDirectory)
				.withArguments('--configuration-cache', 'startPingService')
				.withPluginClasspath()
				.build()

		then:
			result.task(':startPingService').outcome == SUCCESS
	}

	// Note: If this test fails, consider disabling the local firewall. Some firewalls might prompt to allow incoming
	// connections, but the prompt might only stay visible for a few seconds.
	def 'It shall be possible to stop a running generic service'()
	{
		given:
			def projectDirectory = File.createTempDir()
			def buildFile = new File(projectDirectory, 'build.gradle')

			buildFile << '''
				plugins {
					id 'com.brunoritz.gradle.generic-service-control'
				}

				genericServiceControl {
					netCat {
						executable.set('nc')
						servicePort.set(1984)
						args.set(['-k', '-l', '1984'])
					}
				}
			'''

			GradleRunner.create()
				.withProjectDir(projectDirectory)
				.withArguments('--configuration-cache', 'startNetCat')
				.withPluginClasspath()
				.build()

		when:
			def result = GradleRunner.create()
				.withProjectDir(projectDirectory)
				.withArguments('--configuration-cache', 'stopNetCat')
				.withPluginClasspath()
				.build()

		then:
			result.task(':stopNetCat').outcome == SUCCESS
			!serverListeningOnPort(1984)

		cleanup:
			println('***************')
			println(new File(projectDirectory, 'logs/stdout.netCat.log').text)
			println(new File(projectDirectory, 'logs/stderr.netCat.log').text)
	}

	def 'It shall create controlling tasks for each defined service'()
	{
		given:
			def projectDirectory = File.createTempDir()
			def buildFile = new File(projectDirectory, 'build.gradle')

			buildFile << '''
				plugins {
					id 'java'
					id 'com.brunoritz.gradle.generic-service-control'
				}

				genericServiceControl {
					binFalse {
						executable.set('/bin/false')
					}

					binTrue {
						executable.set('/bin/true')
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
			result.output.contains('startBinFalse')
			result.output.contains('stopBinFalse')
			result.output.contains('restartBinFalse')

			result.output.contains('startBinTrue')
			result.output.contains('stopBinTrue')
			result.output.contains('restartBinTrue')

			result.task(':tasks').outcome == SUCCESS
	}
}
