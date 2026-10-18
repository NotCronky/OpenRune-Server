plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.content.generic.genericLocs)
    implementation(projects.content.interfaces.bank)
    implementation(projects.content.quest)
    implementation(projects.content.skills.smithing)
}
