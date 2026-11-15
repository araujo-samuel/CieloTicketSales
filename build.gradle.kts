// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
}

// JavaPoet 1.13.0 é exigido pelo plugin Hilt para funcionar com AGP 8.x
// A versão no classpath do AGP 8.13.2 não inclui ClassName.canonicalName()
buildscript {
    configurations.all {
        resolutionStrategy.force("com.squareup:javapoet:1.13.0")
    }
}
