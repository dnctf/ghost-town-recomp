# JNI keep rules for Overcooked Recomp
-keepclassmembers class * {
    native <methods>;
}

-keep class com.overcookedrecomp.app.MainActivity {
    public int getJavaThermalStatus();
    native <methods>;
}

-keep class com.overcookedrecomp.app.GameStorageVerifier$** { *; }
-keep class com.overcookedrecomp.app.GameFileImporter$** { *; }
