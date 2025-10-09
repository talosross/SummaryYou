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

-keep class ai.koog.** { *; }

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

# Keep rules for KotlinX Serialization
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keep class **$$serializer { *; }

# Keep specific data models to be safe, though the above rules should cover them.
-keep class me.nanova.summaryexpressive.model.** { *; }
