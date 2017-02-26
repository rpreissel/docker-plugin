package de.e2.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.resources.ResourceException

class DockerPlugin implements Plugin<Project> {
    final static localExcludeFileName = 'docker.local.exclude'
    final static templatSuffix = '.template'
    final static renameClosure = {
        String fileName ->
            fileName.replace(templatSuffix, '')
    }

    void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);

        project.extensions.create("docker", DockerPluginExtension, project)
        project.configurations.create('docker')

        project.task('copyDockerFiles') { task ->
            group = 'docker'
            inputs.file { project.configurations.docker }
            inputs.file { project.jar.archivePath }
            outputs.dirs { project.docker.buildDir }
            inputs.property 'templateMap', {
                return createTemplateMap(project)
            }

            doLast {
                project.copy {
                    from project.jar.archivePath
                    into project.extensions.docker.buildDir
                }

                def templateMap = task.inputs.properties.templateMap.clone()
                boolean localBuild = project.docker.jenkinsBuildNumber == DockerPluginExtension.LOCAL_BUILD_NUMBER

                List<String> localExcludeContent = readLocalExcludePatterns(project)
                def copyTemplatesClosure=this.&copyTemplates.curry(project, templateMap, localBuild, localExcludeContent);

                project.configurations.docker.filter {file -> isArchive(file)} forEach copyTemplatesClosure
                project.configurations.docker.filter {file -> !isArchive(file)} forEach copyTemplatesClosure
            }
        }
    }

    private copyTemplates(project, templateMap, boolean localBuild, List<String> localExcludeContent, file) {
        def srcFiles = isArchive(file) ? project.zipTree(file) : project.fileTree(file)

        project.copy {
            from srcFiles
            into project.extensions.docker.buildDir

            expand templateMap
            include '*' + templatSuffix
            if (localBuild) {
                exclude localExcludeContent
            }
            exclude localExcludeFileName
            rename renameClosure
        }

        project.copy {
            from srcFiles
            into project.extensions.docker.buildDir
            exclude '*' + templatSuffix
            if (localBuild) {
                exclude localExcludeContent
            }
            exclude localExcludeFileName
        }
    }


    private isArchive(File file) {
        file.getName().endsWith(".jar") || file.getName().endsWith(".zip")
    }

    private List<String> readLocalExcludePatterns(Project project) {
        List localExcludeContent = project.configurations.docker.findResults { file ->
            if (isArchive(file)) {
                try {
                    return project.resources.text.fromArchiveEntry(file, localExcludeFileName).asReader().readLines()
                } catch (ResourceException exc) {
                    return null;
                }
            } else if (file.getName().equals(localExcludeFileName)) {
                return project.resources.text.fromFile(file).asReader().readLines();
            }

            return null;
        }.flatten()

        return localExcludeContent
    }

    private Map createTemplateMap(Project project) {
        def rawProperties = convertValuesToStrings(
                project.docker.properties.findAll { k, v ->
                    return v instanceof Serializable
                })

        def rawExtraProperties = convertValuesToStrings(
                project.docker.ext.properties.findAll { k, v ->
                    return v instanceof Serializable
                })

        def properties = rawProperties + rawExtraProperties
        return properties
    }

    private Map convertValuesToStrings(Map map) {
        return map.collectEntries { k, v ->
            if (v instanceof Map) {
                [(k): convertValuesToStrings(v)]
            } else {
                [(k): v?.toString()]
            }
        }
    }
}
