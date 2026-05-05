plugins {
    id("java")
}

group = "de.teamholy"
version = "1.0-SNAPSHOT"

val coreVersion = "2.7.2"

repositories {
    mavenLocal()
    mavenCentral()

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
        content {
            includeGroupByRegex("com\\.github\\..*")
        }
    }

    maven {
        name = "dmulloy2"
        url = uri("https://repo.dmulloy2.net/repository/public/")
        content {
            includeGroup("com.comphenix.protocol")
        }
    }
}

dependencies {
    compileOnly("de.teamholy:holy-core-api:${coreVersion}")
    compileOnly("de.teamholy:bukkit-core-api:${coreVersion}")
    compileOnly("de.teamholy:bungee-core-api:${coreVersion}")
    compileOnly("de.teamholy:bukkit-markupapi:${coreVersion}")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
    compileOnly("eu.koboo:en2do:3.1.9")
    compileOnly("io.netty:netty-codec:4.1.97.Final")
    compileOnly("net.md-5:bungeecord-api:1.19-R0.1-SNAPSHOT")
    compileOnly("net.md-5:brigadier:1.0.16-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT") { isTransitive = false }
    compileOnly("com.github.azbh111:craftbukkit-1.8.8:R")
    compileOnly("org.mongodb:mongo-java-driver:3.12.8")
    compileOnly("org.redisson:redisson:3.19.1")
    compileOnly("de.dytanic.cloudnet:cloudnet-wrapper-jvm:3.4.0-RELEASE")
    compileOnly("de.dytanic.cloudnet:cloudnet-syncproxy:3.4.0-RELEASE")
    compileOnly("de.dytanic.cloudnet:cloudnet-bridge:3.4.0-RELEASE")
    // slimeworldmanager-api and holographicdisplays-api must be installed to mavenLocal()
    // from the server's plugins folder: mvn install:install-file -Dfile=<jar> -DgroupId=... -DartifactId=... -Dversion=... -Dpackaging=jar
    compileOnly("com.grinderwolf:slimeworldmanager-api:2.2.1")
    compileOnly("com.gmail.filoghost.holographicdisplays:holographicdisplays-api:2.4.9")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}

tasks.test {
    useJUnitPlatform()
}
