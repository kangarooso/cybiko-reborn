// Pure-Java (no Android, no Swing) Cybiko Classic emulation core.
//
// The H8S/2241 CPU, address bus, HD66421 LCD, AT45DB041 SPI flash, timers and RTC
// come unmodified from the upstream MIT-licensed emulator, pinned as the git submodule
// third_party/cybiko-java-emulator. Only the files listed in upstream-core-files.txt
// are compiled; desktop-only classes (Swing UI, javax.sound, PTY, CLI) are excluded.
plugins { `java-library` }

val upstreamDir = rootProject.file("third_party/cybiko-java-emulator/emulator/src/main/java")
val upstreamClasses = file("upstream-core-files.txt").readLines()
    .map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }

val syncUpstreamCore by tasks.registering(Sync::class) {
    doFirst {
        if (!upstreamDir.isDirectory) throw GradleException(
            "Upstream emulator source missing. Run: git submodule update --init --recursive")
    }
    from(upstreamDir) { include(upstreamClasses.map { "com/github/daberkow/$it.java" }) }
    into(layout.buildDirectory.dir("generated/upstream-core"))
}

sourceSets { main { java { srcDir(syncUpstreamCore) } } }

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
