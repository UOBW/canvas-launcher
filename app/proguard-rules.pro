# Disable obfuscation
# -dontobfuscate

# Strip log calls for performance
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Proto DataStore lite uses reflection
-keepclassmembers class * implements com.google.protobuf.MessageLite { <fields>; }
