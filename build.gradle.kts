plugins {
    id("java")
}

group = "de.teamholy"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()

    maven {
        name = "github-teamholy"
        url = uri("https://maven.pkg.github.com/teamholy-network/holy-core")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
        }
        content { includeGroup("de.teamholy") }
    }

    maven {
        name = "cloudnet-releases"
        url = uri("https://repo.cloudnetservice.eu/releases/")
        metadataSources { gradleMetadata(); mavenPom(); artifact() }
        content {
            includeGroup("de.dytanic.cloudnet")
            includeGroup("eu.cloudnetservice")
        }
    }

    maven {
        name = "spigotmc-snapshots"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        content {
            includeGroup("org.spigotmc")
            includeGroup("net.md-5")
        }
    }

    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
        content { includeGroupByRegex("com\\.github\\..*") }
    }

    maven {
        name = "dmulloy2"
        url = uri("https://repo.dmulloy2.net/repository/public/")
        content { includeGroup("com.comphenix.protocol") }
    }

    maven {
        name = "koboo"
        url = uri("https://repo.koboo.eu/releases")
        content { includeGroup("eu.koboo") }
    }

    maven {
        name = "sonatype-snapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }

    maven(url = "https://libraries.minecraft.net")
    maven ( url  ="https://repo.glaremasters.me/repository/concuncan/" )
    maven(url = "https://repo.codemc.io/repository/maven-public/")
    maven(url = "https://maven.elmakers.com/repository/")
}

dependencies {
    compileOnly(libs.bundles.teamholy)
    compileOnly(libs.bundles.cloudnet)

    compileOnly(libs.spigot.api) { isTransitive = false }
    compileOnly(libs.bungeecord.api)
    compileOnly(libs.brigadier)
    compileOnly(libs.craftbukkit)

    compileOnly(libs.protocollib)
    compileOnly(libs.en2do)
    compileOnly(libs.netty.codec)
    compileOnly(libs.mongodb)
    compileOnly(libs.redisson)

    // must be installed to mavenLocal() — see slime-api / holographic-api in libs.versions.toml
    compileOnly(libs.slime.api)
    compileOnly(libs.holographic.api)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}
