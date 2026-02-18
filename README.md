# Service Control Plugin

[![build](https://github.com/birdflyer-lszo/gradle-service-control/actions/workflows/build.yaml/badge.svg?branch=master&event=push)](https://github.com/birdflyer-lszo/gradle-service-control/actions/workflows/build.yaml)
[![License](https://img.shields.io/github/license/node-gradle/gradle-node-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
![Version](https://img.shields.io/badge/Version-2.0.0-orange.svg)

## About

The _Service Control_ plugin allows starting, stopping and restarting of Java services as well as generic services. The
services are kept alive for as long as the _Gradle Daemon_ is running. The plugin was inspired by the
`gradle-execfork-plugin`. That plugin had the limitation that a forked process was only kept alive for as long as at
least another task was running in the current build.

Developers complained that with the `gradle-execfork-plugin` no individual services could be restarted when changes were
made. As a consequence, the entire application had to be stopped and restarted which resulted in a significant loss of
time.

### Function

For each service that is started, the plugin records its PID to a file. That PID is later used for stopping/restarting
the service. If a PID file already exists when starting a service, the startup fails. The standard output and standard
error streams of each running service will be redirected into a file for later consumption.

Services will be kept running in the background until they are stopped using a task or when the _Gradle Daemon_
terminates.

Once all services have been started, the build ends. This can be particularly helpful in a CI environment where the
services being tested can be started once whereafter a variety of test suites can be executed against the running
services.

### Java Services

Java services allow running a Java project as a service. The requirement is that a main class be present in the project
that can be started.

### Generic Services

Generic services allow executing arbitrary binaries as a service. This is particularly helpful for running NodeJS based
frontends and other tools that are not Java based.

## Installation

The plugin can be applied to any project using the `plugins` closure.

```groovy
plugins {
	id 'com.brunoritz.gradle.java-service-control' version '<version>' // For Java projects
	id 'com.brunoritz.gradle.generic-service-control' version '<version>' // For any other projects (NodeJS)
}
```

## Configuration

### Java Services

Java based services can be controlled using the `javaServiceControl` extension. The plugin will automatically configure
the classpath using the `main` configuration's runtime classpath. The plugin waits for specific amount of time for the
service's TCP port to become available. If no listening socket is created on the given port within the specified time,
the startup fails.

```groovy
javaServiceControl {
	foobarService {
		mainClass.set('com.foobar.Main')
		environmentFiles.set(file('application-local.env'))
		servicePort.set(9300)
	}
}
```

If the project uses the _Application_ plugin, the `mainClass` property can be left empty. `java-service-control` plugin
will use the application's main class name as configured in `application`. This only happens. if no explicit name is set
in the `javaServiceControl` extension.

The service startup can be configured using the following properties. Those marked with an asterisk are mandatory.
Details on the default values and further behavior can be found in the Javadoc documentation.

| Name                | Description                                                               |
|---------------------|---------------------------------------------------------------------------|
| `mainClass`*        | The fully qualified name of the class to start                            |
| `environmentFiles`  | Environment variables to set for the application (properties file format) |
| `args`              | Arguments to pass to the application                                      |
| `systemProperties`  | System properties to pass to the application                              |
| `environment`       | Environment variables so set for the application                          |
| `servicePort`       | The port on which the service runs.                                       |
| `startupLogMessage` | The message to expect in the log output when the serivce has started.     |
| `debugPort`         | The port on which to setup a remote debugger                              |
| `workingDirectory`  | The working director of the application.                                  |
| `pidFile`           | The file into which to store the service's process ID                     |
| `standardOutputLog` | The file into which to store the service's stdout output                  |
| `errorOutputLog`    | The file into which to store the service's stderr output                  |
| `startTimeout`      | The time the plugin allows the service to start                           |
| `agent`             | The configuration containing the single agent library to attach           |
| `agentArgs`         | The arguments to pass to the agent. Ignored, if no agent is configured.   |

Exactly one of the `servicePort` or `startupLogMessage` properties have to be set. If both are set, the log message is
ignored.

### Generic Services

Arbitrary services can be controlled using the `genericServiceControl` extension. The plugin waits for specific amount
of time for the service's TCP port to become available. If no listening socket is created on the given port within the
specified time, the startup fails.

Care has to be taken when setting the executable name since such names are platform dependent (Windows vs. Unix naming).

The `genericServiceControl` extension allows defining multiple services.

```groovy
genericServiceControl {
	npmService {
		executable.set('npm')

		args.set([
			'run', 'start', '--',
			'--watch=false',
			'--aot',
			'--progress=false',
			'--live-reload=false'
		])

		environment.set([
			PATH: "${System.getenv('PATH')}${File.pathSeparator}" +
				"${nodePath}${File.pathSeparator}" +
				"${projectDir}/node_modules/.bin"
		])

		servicePort.set(1234)
	}

	someOtherService {
		// ...
	}
}
```

The service can be configured using the following properties. Those marked with an asterisk are mandatory. Details on
the default values and further behavior can be found in the Javadoc documentation.

| Name                | Description                                                                             |
|---------------------|-----------------------------------------------------------------------------------------|
| `executable`*       | The application to run. Platform dependent name is required.                            |
| `args`              | Arguments to pass to the application                                                    |
| `environmentFiles`  | Environment variables to set for the application (properties file format)               |
| `environment`       | Environment variables so set for the application                                        |
| `servicePort`       | The port on which the service runs.                                       |
| `startupLogMessage` | The message to expect in the log output when the serivce has started.     |
| `workingDirectory`  | The working director of the application.                                                |
| `pidFile`           | The file into which to store the service's process ID                                   |
| `standardOutputLog` | The file into which to store the service's stdout output                                |
| `errorOutputLog`    | The file into which to store the service's stderr output                                |
| `startTimeout`      | The time the plugin allows the service to start                                         |

Exactly one of the `servicePort` or `startupLogMessage` properties have to be set. If both are set, the log message is
ignored.

## Usage

### Starting Services

Services can be started using the `start<service-name>` task, where `<service-name>` depends on the service definiton.
Upon invocation the task checks for the presence of a PID file. If such a file exists, the service is considered
running, and the task fails. When no PID file exists, one is created, and the service process created. The process ID of
the started task is recorded in the PID file for later use by the stopping tasks. After starting a service process, the
start task waits for the service to open a listening socket on the specified TCP port.

It is recommended to add the PID files to the SCM ignore list.

### Stopping Services

Services can be stopped using the `stop<service-name>` task, where `<service-name>` depends on the service definiton.
Alternatively, the _Gradle Daemon_ can be stopped, which will also result in the termination of any running services.
Upon invocation the task checks for the presence of a PID file. If that file exists, the process' ID is read from the
file and the process identified by that ID is requested to terminate. When no PID file exists, the service is considered
stopped and no further action is taken.

The plugin allows a process 30 seconds for termination. After these 30 seconds, if the process is still running, the
task will fail.

### Restarting Services

Services can be restarted using the `restart<service-name>` task, where `<service-name>` depends on the service
definiton. This task does nothing more than just running the stop task followed by the start task. Since the stopping
tasks do not fail if a service is not running, the restart tasks can be used for starting services the first time.

## Development Documentation

* [Code Style](doc/code-style.md)
* [Change Log](doc/changelog.md)
