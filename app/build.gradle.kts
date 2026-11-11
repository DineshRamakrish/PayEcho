plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseKeyStorePath = providers.environmentVariable("PAYECHO_KEYSTORE_PATH").orNull
val releaseStorePassword = providers.environmentVariable("PAYECHO_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("PAYECHO_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("PAYECHO_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(
    releaseKeyStorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.payecho.app"
    compileSdk = 35

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeyStorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    defaultConfig {
        applicationId = "com.payecho.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}
