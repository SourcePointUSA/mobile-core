Pod::Spec.new do |spec|
    spec.name                     = 'SPMobileCore'
    spec.version                  = '0.0.4'
    spec.homepage                 = 'https://github.com/SourcePointUSA/mobile-core.git'
    spec.source                   = { :http=> ''}
    spec.authors                  = 'André Herculano'
    spec.license                  = { :type => 'APACHE 2' }
    spec.summary                  = 'The internal Network & Data layers used by our mobile SDKs'
    spec.vendored_frameworks      = 'build/cocoapods/framework/SPMobileCore.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target = '10.0'
    spec.tvos.deployment_target = '10.0'
                
                
    if !Dir.exist?('build/cocoapods/framework/SPMobileCore.framework') || Dir.empty?('build/cocoapods/framework/SPMobileCore.framework')
        raise "

        Kotlin framework 'SPMobileCore' doesn't exist yet, so a proper Xcode project can't be generated.
        'pod install' should be executed after running ':generateDummyFramework' Gradle task:

            ./gradlew :core:generateDummyFramework

        Alternatively, proper pod installation is performed during Gradle sync in the IDE (if Podfile location is set)"
    end
                
    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':core',
        'PRODUCT_MODULE_NAME' => 'SPMobileCore',
    }
                
    spec.script_phases = [
        {
            :name => 'Build SPMobileCore',
            :execution_position => :before_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                  exit 0
                fi
                set -ev
                REPO_ROOT="$PODS_TARGET_SRCROOT"
                "$REPO_ROOT/../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:syncFramework \
                    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
                    -Pkotlin.native.cocoapods.archs="$ARCHS" \
                    -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
            SCRIPT
        }
    ]
                
end