# Keep the libxposed entry point (it is loaded by name from java_init.list).
-keep public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
    public void on*(...);
}
-keep class io.github.libxposed.service.XposedProvider { *; }
-dontwarn io.github.libxposed.api.**
