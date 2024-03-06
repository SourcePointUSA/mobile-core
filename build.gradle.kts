plugins {
    //trick: for the same plugin versions in all sub-modules
    id("com.android.library").version("8.3.0").apply(false)
    kotlin("multiplatform").version("1.9.22").apply(false)
    id("com.github.ben-manes.versions").version("0.50.0").apply(true)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
