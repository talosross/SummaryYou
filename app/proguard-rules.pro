# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keepattributes Signature,*Annotation*

# 1. Keep the ai.koog library classes, as they are used extensively via reflection.
-keep class ai.koog.** { *; }

# 2. Preserve the kotlin.Metadata annotation, which is required for reflection to work.
# This is a more memory-efficient way than keeping all annotated classes, which caused an OOM error.
-keepattributes kotlin.Metadata
-keep class kotlin.Metadata { *; }

# 3. Keep the entire Kotlin Reflection package. This is critical.
# The reflection implementation depends on classes like `KVisibility` from the root package,
# not just the internal classes. Failing to keep the whole package causes the `getVisibility` crash.
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

-keep class io.ktor.client.engine.android.** { *; }
-keep class io.ktor.client.plugins.contentnegotiation.** { *; }
-keep class io.ktor.serialization.kotlinx.** { *; }

# Ktor and its dependencies (Netty, Reactor, etc.) have optional references
# to classes not present in Android. We must tell R8 not to warn about them.
-dontwarn io.micrometer.context.**
-dontwarn io.netty.**
-dontwarn io.opentelemetry.api.incubator.**
-dontwarn javax.enterprise.inject.spi.Extension
-dontwarn jdk.jfr.**
-dontwarn jdk.net.**
-dontwarn org.HdrHistogram.**
-dontwarn org.LatencyUtils.**
-dontwarn org.eclipse.jetty.**
-dontwarn reactor.blockhound.integration.BlockHoundIntegration
-dontwarn java.lang.management.**
-dontwarn com.fasterxml.jackson.core.JsonFactory
-dontwarn com.fasterxml.jackson.core.JsonGenerator

# Suppress warnings for missing classes in jsoup (optional re2j support)
-dontwarn com.google.re2j.**
-dontwarn org.jsoup.helper.Re2jRegex**

# Suppress warnings for Java 9+ ProcessHandle used in ai.koog (not available on Android)
-dontwarn java.lang.ProcessHandle
-dontwarn ai.koog.agents.ext.tool.shell.JvmShellCommandExecutor

# Keep rules for KotlinX Serialization
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keep class **$$serializer { *; }

# Keep specific data models to be safe, though the above rules should cover them.
-keep class com.talosross.summaryyou.model.** { *; }
