/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//apply plugin: 'com.android.application'
//apply plugin: 'kotlin-android'
//apply plugin: 'kotlin-android-extensions'
//apply plugin: 'kotlin-kapt'

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("kotlin-kapt")
}

@Suppress("UnstableApiUsage")
android {

    compileSdk = 34
    //buildToolsVersion build_versions.build_tools

    defaultConfig {
        applicationId = "com.pedidos.android.persistence"
        minSdk = 25
        targetSdk = 34
        versionCode = 3
        versionName = "1.0"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }

        debug {
            isDebuggable = true
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")

        }
    }

    buildFeatures {
        dataBinding = true
    }

    flavorDimensions += "version"

    productFlavors {
        create("root") {
            dimension = "version"
            applicationIdSuffix = ".root"
            versionCode = 4
            versionName = "2.0.1"
            buildConfigField(
                "String",
                "URL_SERVER",
                "\"https://wsfacturacion.coolbox.com.pe:9443/SKM/\""
            )
            buildConfigField("String", "URL_PEDIDO_PAGAR_NUEVO", "\"pedidopagarnuevo\"")
            buildConfigField("String", "VERSION_APP", "\"27/03/2024\"")
        }

        create("skm") {
            dimension = "version"
            applicationIdSuffix = ".skm"
            versionCode = 138
            versionName = "2.1.4-test"
            buildConfigField(
                "String",
                "URL_SERVER",
                "\"https://wsfacturacion.coolbox.com.pe:9443/SKM/\""
            )
            buildConfigField("String", "URL_PEDIDO_PAGAR_NUEVO", "\"pedidopagarnuevoskmicg\"")
            buildConfigField("String", "VERSION_APP", "\"27/03/2024\"")
        }
    }


    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }
    namespace = "com.pedidos.android.persistence"
    lint {
        abortOnError = false
    }
}

dependencies {
    // Support libraries
    implementation(libs.support.annotations)

    implementation(libs.support.v4)
    implementation(libs.support.design)
    implementation(libs.support.cardview)

    // Architecture components
    implementation(libs.runtime)
    implementation(libs.lifecycle.extensions)
    kapt(libs.compiler)

    implementation(libs.constraint.layout)

    implementation(libs.room.runtime)
    kapt(libs.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    // Android Testing Support Library's runner and rules
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.rules)
    androidTestImplementation(libs.testing)
    androidTestImplementation(libs.core.testing)

    // Espresso UI Testing
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.contrib)
    androidTestImplementation(libs.espresso.intents)

    // Resolve conflicts between main and test APK:
    androidTestImplementation(libs.support.annotations)
    androidTestImplementation(libs.support.v4)
    androidTestImplementation(libs.support.cardview)
    androidTestImplementation(libs.support.design)

    implementation(libs.kotlin.stdlib.jdk7)

    implementation(libs.percent)
    //compile(name:'visanetMPOS.2.0', ext:'aar')

    // Z-Xing
    implementation(libs.zxing.core)

    implementation(files("libs/visanetMPOS.2.0.aar"))

    //todo: organize
    implementation(libs.rxbinding)

}
repositories {
    //mavenCentral()
    //flatDir {
    //  dirs("libs")
    //}
}

kapt {
    generateStubs = true
    correctErrorTypes = true
}
