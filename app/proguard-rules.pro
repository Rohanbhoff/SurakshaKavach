# Proguard rules
# Prevent R8 from stripping TensorFlow Lite classes and native symbols
-keep class org.tensorflow.lite.** { *; }
-keepclassmembers class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
