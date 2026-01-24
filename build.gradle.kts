import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask

plugins {
	id("java-gradle-plugin")
	id("jvm-test-suite")
	id("org.checkerframework")
	id("groovy")

	id("com.gradle.plugin-publish")

	id("com.github.spotbugs")
	id("checkstyle")
	id("pmd")

	id("me.qoomon.git-versioning")

	id("idea")
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17

	consistentResolution {
		useCompileClasspathVersions()
	}
}

checkerFramework {
	checkers = listOf(
		"org.checkerframework.checker.nullness.NullnessChecker",
		"org.checkerframework.common.initializedfields.InitializedFieldsChecker",
		"org.checkerframework.checker.formatter.FormatterChecker",
		"org.checkerframework.common.value.ValueChecker"
	)

	extraJavacArgs = listOf(
		"-AskipUses=io.vavr|java.io.BufferedReader"
	)
}

testing {
	suites {
		val test by getting(JvmTestSuite::class) {
			useSpock()
		}

		register<JvmTestSuite>("integrationTest") {
			useSpock()

			dependencies {
				implementation(project())
			}
		}

		register<JvmTestSuite>("functionalTest") {
			useSpock()
		}
	}
}

tasks.named<Test>("functionalTest") {
	testLogging {
		showStandardStreams = true
	}
}

gradlePlugin {
	website = "https://github.com/birdflyer-lszo/gradle-service-control"
	vcsUrl = "https://github.com/birdflyer-lszo/gradle-service-control"

	plugins {
		create("javaServiceControl") {
			id = "com.brunoritz.gradle.java-service-control"
			implementationClass = "com.brunoritz.gradle.servicecontrol.JavaServiceControlPlugin"

			displayName = "Service Control Plugin"
			description = "Allows starting, stopping and restarting of Java services in developer environments"
			tags = listOf(
				"java",
				"service",
				"testing"
			)
		}

		create("genericServiceControl") {
			id = "com.brunoritz.gradle.generic-service-control"
			implementationClass = "com.brunoritz.gradle.servicecontrol.GenericServiceControlPlugin"

			displayName = "Service Control Plugin"
			description = "Allows starting, stopping and restarting of generic services in developer environments"
			tags = listOf(
				"generic",
				"service",
				"testing"
			)
		}
	}

	testSourceSets(
		sourceSets["integrationTest"],
		sourceSets["functionalTest"]
	)
}

configurations["integrationTestImplementation"].extendsFrom(configurations["testImplementation"])

dependencies {
	implementation("com.github.spotbugs:spotbugs-annotations:4.9.8")
	implementation("commons-io:commons-io:2.21.0")
	implementation("io.vavr:vavr:0.11.0")
	implementation("net.jcip:jcip-annotations:1.0")

	testImplementation("cglib:cglib-nodep:3.3.0")
	testImplementation("org.spockframework:spock-core")
}

spotbugs {
	excludeFilter = file("config/spotbugs-exclusions.xml")
	showStackTraces = false

	effort = Effort.MAX
}

tasks.named<SpotBugsTask>("spotbugsMain") {
	reports.create("xml")
}

tasks.named("spotbugsIntegrationTest") {
	enabled = false
}

tasks.named("spotbugsFunctionalTest") {
	enabled = false
}

pmd {
	toolVersion = "6.33.0"
	isConsoleOutput = true
	isIgnoreFailures = true

	ruleSets = listOf()
	reportsDir = layout.buildDirectory.dir("reports/pmd").get().asFile
	ruleSetFiles = files("${rootDir}/config/pmd-rules.xml")

}

group = "com.brunoritz.gradle"

gitVersioning.apply {
	rev {
		version = "\${describe.tag.version}-dev+\${commit.short}"
	}

	refs {
		tag("v(?<version>.*)") {
			version = "\${ref.version}"
		}
	}
}

tasks.wrapper {
	gradleVersion = "9.3.0"
	distributionType = Wrapper.DistributionType.ALL
}

idea {
	module {
		testSources.from(
			sourceSets["integrationTest"].java.srcDirs,
			sourceSets["functionalTest"].java.srcDirs
		)

		testResources.from(
			sourceSets["integrationTest"].resources.srcDirs,
			sourceSets["functionalTest"].resources.srcDirs
		)
	}
}
