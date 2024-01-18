import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import java.net.URI

plugins {
	kotlin("multiplatform")
	alias(libs.plugins.android.library)
	alias(libs.plugins.kotlin.compose)
	id("maven-publish")
	id("signing")
}

val readableName = "Multiplatform File Picker"
val repoUrl = "https://github.com/Wavesonics/compose-multiplatform-file-picker"
group = "com.darkrockstudios"
description = "A multiplatform compose widget for picking files"
version = libs.versions.library.get()

extra.apply {
	set("isReleaseVersion", !(version as String).endsWith("SNAPSHOT"))
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    targetHierarchy.default()
	explicitApi()

	androidTarget {
		publishLibraryVariants("release")
	}
	jvm {
		compilations.all {
			kotlinOptions.jvmTarget = "17"
		}
	}
	js(IR) {
		browser()
		binaries.executable()
	}
	macosX64()
	listOf(
		iosX64(),
		iosArm64(),
		iosSimulatorArm64(),
	).forEach {
		it.binaries.framework {
			baseName = "MPFilePicker"
		}
	}

	sourceSets {
		val commonMain by getting {
			dependencies {
				api(compose.runtime)
				api(compose.foundation)
			}
		}
		val commonTest by getting {
			dependencies {
				implementation(kotlin("test"))
			}
		}
		val androidMain by getting {
			dependencies {
				api(compose.uiTooling)
				api(compose.preview)
				api(compose.material)
				api(libs.androidx.appcompat)
				api(libs.androidx.coreKtx)
				api(libs.compose.activity)
				api(libs.kotlinx.coroutines.android)
			}
		}
		val jvmMain by getting {
			dependencies {
				api(compose.uiTooling)
				api(compose.preview)
				api(compose.material)

				val lwjglVersion = "3.3.1"
				listOf("lwjgl", "lwjgl-tinyfd").forEach { lwjglDep ->
					implementation("org.lwjgl:${lwjglDep}:${lwjglVersion}")
					listOf(
						"natives-windows",
						"natives-windows-x86",
						"natives-windows-arm64",
						"natives-macos",
						"natives-macos-arm64",
						"natives-linux",
						"natives-linux-arm64",
						"natives-linux-arm32"
					).forEach { native ->
						runtimeOnly("org.lwjgl:${lwjglDep}:${lwjglVersion}:${native}")
					}
				}
			}
		}
		val jvmTest by getting
		val jsMain by getting
	}

	val javadocJar by tasks.registering(Jar::class) {
		archiveClassifier.set("javadoc")
	}

	publishing {
		repositories {
			maven {
				val releaseRepo = URI("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
				val snapshotRepo = URI("https://s01.oss.sonatype.org/content/repositories/snapshots/")
				url = if (extra["isReleaseVersion"] == true) releaseRepo else snapshotRepo
				credentials {
					username = System.getenv("OSSRH_USERNAME") ?: "Unknown user"
					password = System.getenv("OSSRH_PASSWORD") ?: "Unknown password"
				}
			}
		}
		publications {
			publications.withType<MavenPublication> {
				artifact(javadocJar.get())

				pom {
					name.set(readableName)
					description.set(project.description)
					inceptionYear.set("2023")
					url.set(repoUrl)
					developers {
						developer {
							name.set("Adam Brown")
							id.set("Wavesonics")
						}
					}
					licenses {
						license {
							name.set("MIT")
							url.set("https://opensource.org/licenses/MIT")
						}
					}
					scm {
						connection.set("scm:git:git://github.com/Wavesonics/compose-multiplatform-file-picker.git")
						developerConnection.set("scm:git:ssh://git@github.com/Wavesonics/compose-multiplatform-file-picker.git")
						url.set("https://github.com/Wavesonics/compose-multiplatform-file-picker")
					}
				}
			}
		}
	}
}

tasks.withType<AbstractPublishToMaven>().configureEach {
	val signingTasks = tasks.withType<Sign>()
	mustRunAfter(signingTasks)
}

signing {
	val signingKey: String? = System.getenv("SIGNING_KEY")
	val signingPassword: String? = System.getenv("SIGNING_PASSWORD")
	if (signingKey != null && signingPassword != null) {
		useInMemoryPgpKeys(null, signingKey, signingPassword)
		sign(publishing.publications)
	} else {
		println("No signing credentials provided. Skipping Signing.")
	}
}

android {
	namespace = "com.darkrockstudios.libraries.mpfilepicker"
	compileSdk = libs.versions.android.compile.sdk.get().toInt()
	sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
	defaultConfig {
		minSdk = libs.versions.android.min.sdk.get().toInt()
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
}
