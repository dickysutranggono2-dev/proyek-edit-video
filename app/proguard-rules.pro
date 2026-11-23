# Media3, AdMob, Coil dan Compose membawa consumer-rules sendiri.
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.checkerframework.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# ONNX Runtime dipanggil lewat JNI
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**
