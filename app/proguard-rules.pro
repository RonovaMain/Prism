-keepattributes *Annotation*, SourceFile, LineNumberTable
# libVLC обращается к Java-классам из нативного кода
-keep class org.videolan.libvlc.** { *; }
-keep class org.videolan.** { *; }
-dontwarn org.videolan.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.**
