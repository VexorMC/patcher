# ⚒️ Patcher

![Version](https://img.shields.io/badge/version-1.0--SNAPSHOT-blue)

A powerful Gradle plugin for decompiling, patching, and recompiling Minecraft source code, written with legacy Minecraft in mind.

## 📋 Requirements

- JDK 17 or higher
- Gradle 8.0+ (Gradle Wrapper included)
- Kotlin 2.0.20+

## ⚙️ Installation

Add the plugin to your project's build script:

```kotlin
// build.gradle.kts
plugins {
    id("dev.lunasa.patcher") version "1.0-SNAPSHOT"
}
```

## 🔧 Configuration

Configure the patcher in your build script:

```kotlin
patcher {
    minecraftVersion = "1.8.9"
}
```

## 🛠️ Usage

### Setup

To do anything on a Patcher project locally, it is crucial you set up the environment first by running the following command:

```bash
./gradlew setupEnv
```

After this, you can use the following commands to manage your patches and run the Minecraft client.

### Useful Commands

- `./gradlew applyPatches` - Apply patches to the source code
- `./gradlew generatePatches` - Generate patches from source modifications
- `./gradlew generateDeltas` - Generate the binary patches and bundle it with the project.
- `./gradlew runClient` - Launch the modified Minecraft client

### Binary Patches

#### General Information

When you run `generateDeltas`, it will build the project for you and bundle a `patches.zip` file. This file contains the binary patches that can be used to apply changes to the Minecraft JARs. It is intended that these patches be distributed with your JAR.  

The `patches.zip` file contains the XDelta binary patches between your modified source code and the original source code. This allows you to distribute only the changes instead of the entire modified source code.

#### Applying Patches

See the example test-project for how to apply these patches during runtime.

Generally, you will want to discover & load all patches from the `patches.zip` in your JAR root, and either redefine the classes using a Java Agent or transform the classes at load time. It is up to you how you want to apply these patches, but the Patcher plugin provides a convenient way to generate, maintain and bundle them.

## 🤝 Contributing

Contributions are always welcome! Feel free to submit issues or pull requests.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📜 License

This project is licensed under the GNU GPL 3.0 License - see the [LICENSE.md](LICENSE.md) file for details.

## 📚 Acknowledgements

- [Vineflower](https://github.com/Vineflower/Vineflower) for decompilation
- [Tiny-Remapper](https://github.com/FabricMC/tiny-remapper) for bytecode remapping
- [Legacy Fabric](https://legacyfabric.net/) for mappings
- All contributors and users of this tool