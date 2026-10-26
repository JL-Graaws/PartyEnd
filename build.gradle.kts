plugins { java }

version = "0.1.1"

repositories { maven("https://repo.papermc.io/repository/maven-public/") }

dependencies {
    // Set this to the exact Paper API version you run (check papermc.io/downloads)
    compileOnly("io.papermc.paper:paper-api:26.2.build.121-stable")
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) } // match what Paper 26.2 requires
