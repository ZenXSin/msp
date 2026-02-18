@file:Import("org.jetbrains.exposed:exposed-java-time:0.40.1", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-jdbc:0.40.1", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-core:0.40.1", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-dao:0.40.1", mavenDepends = true)
@file:Import("javax.activation:activation:1.1.1", mavenDepends = true)
@file:Import("com.sun.mail:javax.mail:1.6.2", mavenDepends = true)
@file:Import("org.json:json:20210307", mavenDepends = true)

@file:Import("org.jetbrains.exposed:exposed-core:0.53.0", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-crypt:0.53.0", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-dao:0.53.0", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-jdbc:0.53.0", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-jodatime:0.53.0", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-json:0.53.0", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-money:0.53.0", mavenDepends = true)
@file:Import("org.postgresql:postgresql:0.53.0", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-spring-boot-starter:0.53.0", mavenDepends = true)
@file:Import("org.slf4j:slf4j-simple:1.7.30", mavenDepends = true)
@file:Import("mysql:mysql-connector-java:8.0.23", mavenDepends = true)

@file:Import("net.mamoe:mirai-core-jvm:2.15.0-M1", mavenDepends = true)
@file:Import("net.mamoe.mirai.event.*", defaultImport = true)
@file:Import("net.mamoe.mirai.event.events.*", defaultImport = true)
@file:Import("net.mamoe.mirai.message.*", defaultImport = true)
@file:Import("net.mamoe.mirai.message.data.*", defaultImport = true)
@file:Import("net.mamoe.mirai.contact.*", defaultImport = true)

@file:Import("io.ktor:ktor-jvm:2.3.0", mavenDepends = true)
@file:Import("io.ktor:ktor-client-jvm:2.3.0", mavenDepends = true)
@file:Import("io.ktor:ktor-server-jvm:2.3.0", mavenDepends = true)
@file:Import("io.ktor:ktor-server-jetty-jvm:2.3.0", mavenDepends = true)
@file:Import("io.ktor:ktor-server-core-jvm:2.3.0", mavenDepends = true)
@file:Import("io.ktor:ktor-server-content-negotiation-jvm:2.3.0", mavenDepends = true)

@file:Import("org.asynchttpclient:async-http-client:3.0.0.Beta3", mavenDepends = true)

@file:Depends("wayzer/vote", "投票实现")
@file:Depends("wayzer/user/ext/profileBind")
@file:Depends("wayzer/maps", "地图管理")

@file:Depends("coreMindustry/menu")
@file:Depends("coreLibrary/DBApi")
@file:Depends("coreMindustry/utilTextInput")
@file:Depends("coreMindustry")
@file:Depends("wayzer")
@file:Depends("wayzer/user/level")
@file:Depends("mirai/send")
package zxs

onEnable {
    println("MindustryOS in starting.....")
}