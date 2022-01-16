rootProject.name = "parent"
include(":adapterapi")
include(":redissync-example")
include(":MonumentaRedisSync")
include(":v1_16_R3")
include(":v1_17_R1")
project(":redissync-example").projectDir = file("example")
project(":MonumentaRedisSync").projectDir = file("plugin")
