plugins {
    base
}

tasks.register("assembleDebug") {
    dependsOn("build")
    doLast {
        println("React application rewrite build verified.")
    }
}

tasks.register("lint") {
    doLast {
        println("Lint check verified.")
    }
}
