plugins {
    id("base-conventions")
}

dependencies {
    implementation(libs.netty.buffer)
    implementation(libs.rsprot.api)
    implementation(projects.api.account)
    implementation(projects.api.gameProcess)
    implementation(projects.api.net)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.realm)
    implementation(projects.api.registry)
    implementation(projects.api.utils.utilsSkills)
    implementation(projects.content.interfaces.bank)
    implementation(projects.content.skills.fishing)
    implementation(projects.content.skills.mining)
    implementation(projects.content.skills.woodcutting)
}
