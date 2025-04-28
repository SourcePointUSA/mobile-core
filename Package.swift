// swift-tools-version:5.3
import PackageDescription

let package = Package(
    name: "SPMobileCore",
    platforms: [
        .iOS(.v10),
        .tvOS(.v10)
    ],
    products: [
        .library(
            name: "SPMobileCore",
            targets: ["SPMobileCore"]
        ),
    ],
    targets: [
        .binaryTarget(
            name: "SPMobileCore",
            path: "./SPMobileCore.xcframework"
        )
    ]
)
