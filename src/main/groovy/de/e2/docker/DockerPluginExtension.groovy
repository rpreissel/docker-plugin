package de.e2.docker

import org.gradle.api.Project

class DockerPluginExtension {
    public static final String LOCAL_BUILD_NUMBER = 'local'

    Project project

    DockerPluginExtension(Project project) {
        this.project = project
        this.buildDir = project.file("${project.buildDir}/docker")
    }

    File buildDir

    def appPort = 8080
    def yourkitPort = 10001
    def appName = "${-> project.jar.baseName}"
    def jarName = "${-> project.jar.archiveName}"
    def maxMem = "256m"
    def registryUrl = "localhost:5000"
    def imageName = "${-> appName}:${-> tagVersion}"
    def localContainerName = "${-> appName}"
    def localAppPort = 8080
    def localYourkitPort = 8080
    def localPortMappings = "-p ${-> localAppPort}:${-> appPort} -p ${-> localYourkitPort}:${-> yourkitPort}"
    def testHost ="testHost"
    def testAppPort = "${-> appPort}"
    def testPortMappings = "-p ${-> testAppPort}:${-> appPort} -p ${-> testAppPort.toInteger() + 1000}:${-> yourkitPort}"
    def testContainerName = "${-> appName}-test"
    def prodHost = "prodHost"
    def prodAppPort = "${-> appPort}"
    def prodPortMappings = "-p ${-> prodAppPort}:${-> appPort}  -p ${-> prodAppPort.toInteger() + 1000}:${-> yourkitPort}"
    def prodContainerName = "${-> appName}-prod"
    final def jenkinsBuildNumber = "${-> System.env.BUILD_NUMBER ?: LOCAL_BUILD_NUMBER}"
    final def tagVersion = "${-> System.env.BRANCH_NAME ?: System.properties['user.name']}-${-> jenkinsBuildNumber}"
}
