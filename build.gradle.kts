val xcfFrameworkDest = layout.projectDirectory.dir("core/build/cocoapods/publish/release")

tasks.register<Copy>("buildPodFramework") {
    dependsOn(":core:podPublishReleaseXCFramework")
    from(xcfFrameworkDest)
    into(layout.projectDirectory)
}
