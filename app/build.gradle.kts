import org.gradle.api.tasks.compile.JavaCompile

plugins {
    alias(libs.plugins.android.application)
}

val stubClasses = layout.buildDirectory.dir("stub-classes")
val frameworkRoot = rootProject.layout.projectDirectory.dir("third_party/OpenMemories-Framework")

// JitPack cannot publish com.github.kungpaogao:OpenMemories-Framework:0.1.0.
// The tag build fails while downloading jasmin, and the repo has no maven-publish plugin.
// Compile the fork's sources instead, against stubs that stay off the APK.
val assembleStubs = tasks.register<Exec>("assembleStubs") {
    val source = frameworkRoot.dir("stubs/src/main/jasmin")
    val jasmin = frameworkRoot.file("compatibility/tools/jasmin.jar")
    val classes = stubClasses
    inputs.dir(source)
    inputs.file(jasmin)
    outputs.dir(classes)
    workingDir(source)
    val classesPath = classes.map { it.asFile.absolutePath }
    val jasminPath = jasmin.asFile.absolutePath
    doFirst {
        commandLine(
            "bash",
            "-lc",
            "mkdir -p '${classesPath.get()}' && find . -name '*.j' -print0 | xargs -0 java -jar '$jasminPath' -d '${classesPath.get()}'"
        )
    }
}

val stubJar = tasks.register<Jar>("stubJar") {
    dependsOn(assembleStubs)
    from(stubClasses)
    archiveFileName.set("sony-stubs.jar")
    destinationDirectory.set(layout.buildDirectory.dir("stubs"))
}

val sonyStubs = files(stubJar.flatMap { it.archiveFile }).builtBy(stubJar)

android {
    namespace = "org.andrewgao.sony.aspectratio"
    compileSdk {
        // Framework sources call Jelly Bean camera APIs. minSdk stays 10 so this still installs on the camera emulator.
        version = release(16)
    }

    defaultConfig {
        applicationId = "org.andrewgao.sony.aspectratio"
        minSdk = 10
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 10
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    sourceSets.named("main") {
        java.srcDir(frameworkRoot.dir("framework/src/main/java").asFile)
    }
}

dependencies {
    compileOnly(sonyStubs)
    testImplementation(libs.junit)
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(stubJar)
}
