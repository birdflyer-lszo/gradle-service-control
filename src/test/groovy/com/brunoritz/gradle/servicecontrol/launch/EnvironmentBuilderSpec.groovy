package com.brunoritz.gradle.servicecontrol.launch

import io.vavr.collection.HashMap
import io.vavr.collection.List
import spock.lang.Specification

class EnvironmentBuilderSpec
	extends Specification
{
	def 'It shall be possible to read environment variables from a file'()
	{
		given:
			def inputFile = File.createTempFile('evironment', 'variables')
			def builder = EnvironmentBuilder.empty()

			inputFile.text = '''
				FOO=FOO_VALUE
				BAR=BAR_VALUE
				'''

		when:
			def result = builder.append(List.of(inputFile))

		then:
			result.isSuccess()
			result.get().environment().toJavaMap() == [
				FOO: 'FOO_VALUE',
				BAR: 'BAR_VALUE'
			]
	}

	def 'It shall be possible to set environment variables via a map'()
	{
		given:
			def builder = EnvironmentBuilder.empty()

		when:
			def result = builder.append(HashMap.of(
				'FOO', 'FOO_VALUE',
				'BAR', 'BAR_VALUE'
			))

		then:
			result.environment().toJavaMap() == [
				FOO: 'FOO_VALUE',
				BAR: 'BAR_VALUE'
			]
	}

	def 'Later added environment variables (via map) shall override existing ones'()
	{
		given:
			def inputFile = File.createTempFile('evironment', 'variables')
			def builder = EnvironmentBuilder.empty()

			inputFile.text = '''
				FOO=FOO_VALUE
				BAR=BAR_VALUE
				'''

		when:
			def result = builder.append(List.of(inputFile)).get()
				.append(HashMap.of(
					'FOO', 'FOO_VALUE2',
					'BAR', 'BAR_VALUE2'
				))

		then:
			result.environment().toJavaMap() == [
				FOO: 'FOO_VALUE2',
				BAR: 'BAR_VALUE2'
			]
	}

	def 'Later added environment variables (via file) shall override existing ones'()
	{
		given:
			def inputFile = File.createTempFile('evironment', 'variables')
			def builder = EnvironmentBuilder.empty()

			inputFile.text = '''
				FOO=FOO_VALUE2
				BAR=BAR_VALUE2
				'''

		when:
			def result = builder
				.append(HashMap.of(
					'FOO', 'FOO_VALUE',
					'BAR', 'BAR_VALUE'
				))
				.append(List.of(inputFile))

		then:
			result.isSuccess()
			result.get().environment().toJavaMap() == [
				FOO: 'FOO_VALUE2',
				BAR: 'BAR_VALUE2'
			]
	}

	def 'It shall merge multiple files together with those further behind in the list taking precedence'()
	{
		given:
			def inputFile1 = File.createTempFile('evironment', 'variables')
			def inputFile2 = File.createTempFile('evironment2', 'variables')
			def builder = EnvironmentBuilder.empty()

			inputFile1.text = '''
				FOO=FOO_VALUE
				BAR=BAR_VALUE
				'''

			inputFile2.text = '''
				FOO=FOO_VALUE2
				BAZ=BAZ_VALUE
				'''

		when:
			def result = builder.append(List.of(inputFile1, inputFile2))

		then:
			result.isSuccess()
			result.get().environment().toJavaMap() == [
				FOO: 'FOO_VALUE2',
				BAR: 'BAR_VALUE',
				BAZ: 'BAZ_VALUE'
			]
	}

	def 'It shall fail upon trying to read from an inexistent file'()
	{
		given:
			def builder = EnvironmentBuilder.empty()

		when:
			def result = builder.append(List.of(new File('/inexistent')))

		then:
			result.isFailure()
	}
}
