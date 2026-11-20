# DairoDFS的SDK通过JNA反射访问,AAR里的proguard.txt已经包含规则,这里再写一次,避免AAR升级时遗漏
-dontwarn java.awt.*
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-keep class cn.dairo.file.sdk.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class cn.dairo.music.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
