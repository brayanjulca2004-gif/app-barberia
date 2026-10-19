plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false // <--- Agregas esta línea para registrar KSP a nivel de proyecto
}