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
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Apache POI
-dontwarn org.apache.**
-dontwarn org.openxmlformats.schemas.**
-dontwarn org.etsi.**
-dontwarn org.w3.**
-dontwarn com.microsoft.schemas.**
-dontwarn com.graphbuilder.**
-dontwarn javax.naming.**
-dontwarn java.lang.management.**
-dontwarn org.slf4j.impl.**
-dontwarn java.awt.**
-dontwarn net.sf.saxon.**
-dontwarn org.apache.batik.**
-dontwarn org.apache.logging.log4j.**

-dontnote org.apache.**
-dontnote org.openxmlformats.schemas.**
-dontnote org.etsi.**
-dontnote org.w3.**
-dontnote com.microsoft.schemas.**
-dontnote com.graphbuilder.**
-dontnote javax.naming.**
-dontnote java.lang.management.**
-dontnote org.slf4j.impl.**

-keeppackagenames org.apache.poi.ss.formula.function

-keep,allowoptimization,allowobfuscation class org.apache.logging.log4j.** { *; }
-keep,allowoptimization class org.apache.commons.compress.archivers.zip.** { *; }
-keep,allowoptimization class org.apache.poi.schemas.** { *; }
-keep,allowoptimization class org.apache.xmlbeans.** { *; }
-keep,allowoptimization class org.openxmlformats.schemas.** { *; }
-keep,allowoptimization class com.microsoft.schemas.** { *; }

-keep class ai.koog.** { *; }
-keep class me.nanova.summaryexpressive.llm.GeminiHandler { *; }
-keep class io.ktor.client.plugins.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
# FIXME: no idea why need those
-dontwarn io.micrometer.context.ContextAccessor
-dontwarn javax.enterprise.inject.spi.Extension
-dontwarn reactor.blockhound.integration.BlockHoundIntegration
-dontwarn io.micrometer.context.ContextRegistry
-dontwarn io.micrometer.context.ContextSnapshot$Scope
-dontwarn io.micrometer.context.ContextSnapshot
-dontwarn io.micrometer.context.ContextSnapshotFactory$Builder
-dontwarn io.micrometer.context.ContextSnapshotFactory
-dontwarn io.micrometer.context.ThreadLocalAccessor
-dontwarn io.netty.channel.epoll.Epoll
-dontwarn io.netty.channel.epoll.EpollChannelOption
-dontwarn io.netty.channel.epoll.EpollDatagramChannel
-dontwarn io.netty.channel.epoll.EpollDomainSocketChannel
-dontwarn io.netty.channel.epoll.EpollEventLoopGroup
-dontwarn io.netty.channel.epoll.EpollSocketChannel
-dontwarn io.netty.channel.kqueue.KQueue
-dontwarn io.netty.channel.kqueue.KQueueDatagramChannel
-dontwarn io.netty.channel.kqueue.KQueueDomainSocketChannel
-dontwarn io.netty.channel.kqueue.KQueueEventLoopGroup
-dontwarn io.netty.channel.kqueue.KQueueSocketChannel
-dontwarn io.netty.incubator.channel.uring.IOUring
-dontwarn io.netty.incubator.channel.uring.IOUringChannelOption
-dontwarn io.netty.incubator.channel.uring.IOUringDatagramChannel
-dontwarn io.netty.incubator.channel.uring.IOUringEventLoopGroup
-dontwarn io.netty.incubator.channel.uring.IOUringSocketChannel
-dontwarn io.netty.internal.tcnative.AsyncSSLPrivateKeyMethod
-dontwarn io.netty.internal.tcnative.AsyncTask
-dontwarn io.netty.internal.tcnative.Buffer
-dontwarn io.netty.internal.tcnative.CertificateCallback
-dontwarn io.netty.internal.tcnative.CertificateCompressionAlgo
-dontwarn io.netty.internal.tcnative.CertificateVerifier
-dontwarn io.netty.internal.tcnative.Library
-dontwarn io.netty.internal.tcnative.SSL
-dontwarn io.netty.internal.tcnative.SSLContext
-dontwarn io.netty.internal.tcnative.SSLPrivateKeyMethod
-dontwarn io.netty.internal.tcnative.SSLSessionCache
-dontwarn io.netty.internal.tcnative.SessionTicketKey
-dontwarn io.netty.internal.tcnative.SniHostNameMatcher
-dontwarn io.netty.resolver.dns.DefaultDnsCache
-dontwarn io.netty.resolver.dns.DefaultDnsCnameCache
-dontwarn io.netty.resolver.dns.DnsAddressResolverGroup
-dontwarn io.netty.resolver.dns.DnsCache
-dontwarn io.netty.resolver.dns.DnsCnameCache
-dontwarn io.netty.resolver.dns.DnsNameResolverBuilder
-dontwarn io.opentelemetry.api.incubator.logs.ExtendedLogger
-dontwarn io.opentelemetry.api.incubator.trace.ExtendedSpanBuilder
-dontwarn io.opentelemetry.api.incubator.trace.ExtendedTracer
-dontwarn jdk.jfr.Event
-dontwarn jdk.net.ExtendedSocketOptions
-dontwarn org.HdrHistogram.AbstractHistogram
-dontwarn org.HdrHistogram.Histogram
-dontwarn org.LatencyUtils.LatencyStats$Builder
-dontwarn org.LatencyUtils.LatencyStats
-dontwarn org.LatencyUtils.PauseDetector
-dontwarn org.LatencyUtils.SimplePauseDetector
-dontwarn org.eclipse.jetty.alpn.ALPN$ClientProvider
-dontwarn org.eclipse.jetty.alpn.ALPN$Provider
-dontwarn org.eclipse.jetty.alpn.ALPN$ServerProvider
-dontwarn org.eclipse.jetty.alpn.ALPN
-dontwarn org.eclipse.jetty.npn.NextProtoNego$ClientProvider
-dontwarn org.eclipse.jetty.npn.NextProtoNego$Provider
-dontwarn org.eclipse.jetty.npn.NextProtoNego$ServerProvider
-dontwarn org.eclipse.jetty.npn.NextProtoNego
