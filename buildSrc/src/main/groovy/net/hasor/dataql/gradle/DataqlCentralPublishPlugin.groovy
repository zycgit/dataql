package net.hasor.dataql.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.plugins.signing.SigningExtension

class DataqlCentralPublishPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.pluginManager.apply('maven-publish')
        project.pluginManager.apply('signing')

        project.pluginManager.withPlugin('java') {
            configureJavaArtifacts(project)
            configurePublishing(project)
            configureSigning(project)
        }
    }

    private static void configureJavaArtifacts(Project project) {
        project.extensions.configure(JavaPluginExtension) { JavaPluginExtension java ->
            java.withSourcesJar()
            java.withJavadocJar()
        }
        project.tasks.withType(Javadoc).configureEach {
            failOnError = false
        }
    }

    private static void configurePublishing(Project project) {
        project.extensions.configure('publishing') { publishing ->
            publishing.publications {
                mavenJava(MavenPublication) {
                    from project.components.java
                    pom {
                        name = project.providers.provider { project.findProperty('pomName') ?: project.name }
                        description = project.providers.provider { project.description ?: project.name }
                        url = 'https://www.hasor.net/'
                        licenses {
                            license {
                                name = 'The Apache Software License, Version 2.0'
                                url = 'https://www.apache.org/licenses/LICENSE-2.0.txt'
                            }
                        }
                        developers {
                            developer {
                                name = '赵永春(Mr.Zhao)'
                                email = 'zyc@hasor.net'
                            }
                        }
                        scm {
                            connection = 'scm:git:git@gitee.com:zycgit/dataql.git'
                            developerConnection = connection
                            url = 'https://gitee.com/zycgit/dataql'
                            tag = 'HEAD'
                        }
                        withXml {
                            removeNonPublishedDependencies(project, asNode())
                        }
                    }
                }
            }
            if (project.findProperty('centralBundleDir')) {
                publishing.repositories {
                    maven {
                        name = 'centralBundle'
                        url = project.uri(project.findProperty('centralBundleDir'))
                    }
                }
            }
        }

        project.afterEvaluate {
            if (!project.plugins.hasPlugin('com.gradleup.shadow')) {
                return
            }
            def publication = project.extensions.publishing.publications.mavenJava
            publication.setArtifacts([])
            publication.artifact(project.tasks.shadowJar) {
                classifier = null
            }
            publication.artifact(project.tasks.sourcesJar)
            publication.artifact(project.tasks.javadocJar)
        }
    }

    private static void configureSigning(Project project) {
        project.extensions.configure(SigningExtension) { SigningExtension signing ->
            signing.required { project.findProperty('centralRelease') == 'true' }
            def key = project.findProperty('maven.central.signing_key')
            if (key) {
                signing.useInMemoryPgpKeys(key.replace('\\n', '\n'), project.findProperty('maven.central.signing_password'))
            }
            signing.sign project.extensions.publishing.publications.mavenJava
        }
    }

    private static void removeNonPublishedDependencies(Project project, Node pomNode) {
        def excluded = new LinkedHashSet()
        ['compileOnly', 'testImplementation', 'testRuntimeOnly', 'shaded', 'antlr', 'antlrTool'].each { configName ->
            def configuration = project.configurations.findByName(configName)
            if (configuration) {
                excluded.addAll(configuration.dependencies)
            }
        }
        if (excluded.isEmpty()) {
            return
        }

        def dependenciesNode = pomNode.dependencies[0]
        excluded.each { dep ->
            def groupId = dep.group ?: project.group
            dependenciesNode?.dependency?.findAll {
                it.groupId.text() == groupId.toString() && it.artifactId.text() == dep.name
            }?.each {
                dependenciesNode.remove(it)
            }
        }
    }
}
