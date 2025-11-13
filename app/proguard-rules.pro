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
-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated, SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

#-keep public class * {
#    public protected *;
#}


-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}


-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep class com.icmp10.**  { *; }
-keep class com.simcore.**  { *; }
-keep class com.sacbpp.**  { *; }

-keep class com.simant.sacbpp_sample_softpos.** { *; }
-keep class com.simant.m.b.e { *; }
-keep class com.simant.dp.cpe { *; }
-keepnames class com.provisionpay.softpos.navigation.args.*

-keep class retrofit2.* { *; }
-keep class com.google.** { *; }
-keep class android.** { *; }
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keep class flexjson.** { *; }

-keep class okhttp3.** { *; }
-keep class com.android.** { *; }
-keep class com.androidx.** { *; }
-keep class com.airbnb.** { *; }
-keep class com.facebook.** { *; }
-keep class com.github.** { *; }
-keep class com.google.** { *; }
-keep class com.facebook.** { *; }
-keep class com.tbuonomo.** { *; }
-keep class io.reactivex.** { *; }
-keep class com.github.** { *; }
-keep class retrofit2.** { *; }
-keep class com.crashlytics.** { *; }
-keep class io.fabric.** { *; }
-keep class info.androidhive.** { *; }
-keep class com.tapadoo.** { *; }
-keep class com.squareup.** { *; }
-keep class javax.** { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keep class okio.** { *; }
-keep class org.intellij.** { *; }
-keep class org.jetbrains.** { *; }
-keep class org.reactivestreams.** { *; }
-keep class com.visa.** { *; }
-keep class org.bouncycastle.** { *; }
-keep class org.apache.** { *; }
-keep class org.openxmlformats.**  { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class schemaorg_apache_xmlbeans.system.sD023D6490046BA0250A839A9AD24C443.TypeSystemHolder { public final static *** typeSystem; }
-keep class com.microsoft.** { *; }
-keep class com.graphbuilder.** { *; }
-keep class org.etsi.** { *; }
-keep class org.w3.** { *; }
-keep class org.w3c.** { *; }
-keep class org.xml.** { *; }
-keep class org.xmlpull.** { *; }
-keep class com.itextpdf.** { *; }

-dontwarn org.ksoap2.**
-dontwarn com.icmp10.**
-dontwarn com.simcore.**
-dontwarn com.sacbpp.**
-dontwarn com.google.**
-dontwarn okhttp3.**
-dontwarn org.reactivestreams.**
-dontwarn kotlinx.coroutines.**
-dontwarn org.bouncycastle.**

-dontwarn android.support.v4.**
-dontwarn android.support.v7.**
-keep class android.support.** { *; }

-keep class org.slf4j.**  { *; }

-keep class com.shockwave.**

-keep class com.payten.whitelabel.dto.** { *; }
-keepclassmembers class com.payten.whitelabel.dto.** { *; }
-keepclassmembers enum com.payten.whitelabel.** { *; }

-dontshrink
-dontoptimize
#-dontpreverify
