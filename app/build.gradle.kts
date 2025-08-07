import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.ktlint)
    jacoco
}

android {
    namespace = "com.example.flexioffice"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.flexioffice"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// KSP Configuration
ksp {
    arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
}

ktlint {
    android = true
    ignoreFailures = false
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
        reporter(ReporterType.SARIF)
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)

    // ViewModel and Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Calendar
    implementation(libs.kizitonwose.calendar.compose)

    // LocalBroadcastManager for in-app notifications
    implementation(libs.androidx.localbroadcastmanager)

    // Google Play Services Location for Geofencing
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // UI Automator for comprehensive gesture testing
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("androidx.test:rules:1.5.0")

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(kotlin("test"))
}

// Jacoco configuration for code coverage
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter =
        listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",
            "**/Lambda$*.class",
            "**/Lambda.class",
            "**/*Lambda.class",
            "**/*Lambda*.class",
            "**/*\$ViewInjector*.*",
            "**/*\$ViewBinder*.*",
            "**/*_MembersInjector.class",
            "**/Dagger*Component*.class",
            "**/*Module_*Factory.class",
            "**/di/**",
            "**/*_Factory*.*",
            "**/*Module*.*",
            "**/*Dagger*.*",
            "**/*Hilt*.*",
            "**/hilt_aggregated_deps/**",
            "**/*_HiltModules.class",
            "**/*_Provide*Factory*.*",
            "**/*Extensions*.*",
            // UI and Compose related exclusions
            "**/*Activity*.*",
            "**/*Fragment*.*",
            "**/ui/**",
            "**/compose/**",
            "**/navigation/**",
            "**/*Screen*.*",
            "**/*Composable*.*",
            "**/theme/**",
        )

    val debugTree =
        fileTree("${layout.buildDirectory.get().asFile}/tmp/kotlin-classes/debug") {
            exclude(fileFilter)
        }

    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get().asFile) {
            include("**/*.exec", "**/*.ec")
        },
    )
}
