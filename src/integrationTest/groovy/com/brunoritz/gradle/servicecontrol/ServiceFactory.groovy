package com.brunoritz.gradle.servicecontrol

import com.brunoritz.gradle.servicecontrol.java.JavaServiceDefinition
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

final class ServiceFactory
{
	private ServiceFactory()
	{
		throw new UnsupportedOperationException()
	}

	static JavaServiceDefinition createJavaService(
		Project project,
		@DelegatesTo(JavaServiceDefinition) Closure action
	)
	{
		def configuration = project.extensions.getByName('javaServiceControl')
			as NamedDomainObjectContainer<JavaServiceDefinition>
		def service = configuration.create('javaService')

		action.rehydrate(service, this, this).call()

		return service
	}
}
