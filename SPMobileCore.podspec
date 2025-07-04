Pod::Spec.new do |spec|
    spec.name                     = 'SPMobileCore'
    spec.version                  = '0.1.8'
    spec.homepage                 = 'https://github.com/SourcePointUSA/mobile-core.git'
    spec.source                   = { :git => 'https://github.com/SourcePointUSA/mobile-core.git', :tag => '0.1.8' }
    spec.authors                  = 'André Herculano'
    spec.license                  = { :type => 'APACHE 2' }
    spec.summary                  = 'The internal Network & Data layers used by our mobile SDKs'
    spec.vendored_frameworks      = 'SPMobileCore.xcframework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target    = '10.0'
    spec.tvos.deployment_target    = '10.0'
                
                
                
                
                
                
end