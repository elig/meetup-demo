import org.demo.DistributionService
import static org.demo.GlobalSettings.DEPLOY_REPO
import static org.demo.GlobalSettings.INTEGRATION_REPO
import static org.demo.GlobalSettings.RELEASE_REPO

/**
 *  Created by elig on 26/08/2018.
 */
@Library('utils@ci-cd') _
pipeline {
    agent any

    environment {
        CREDENTIALS = credentials('jenkins')
        DIST_CREDS = credentials('distribution')
        BUNDLE_VERSION = "${JOB_NAME}-${BUILD_NUMBER}"
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    tools {
        maven 'maven-3.5.3'
        jdk '8u181'
    }

    stages {

        stage('CleanUp') {
            steps {
                deleteDir()
            }
        }

        stage('Clone') {
            steps {
                git branch: 'ci-cd', url: 'https://github.com/elig/meetup-demo.git'
            }
        }

        stage('Build Configuration') {
            steps {
                script {
                    //plugin configuration
                    server = Artifactory.server 'artifactory-demo'
                    server.username = CREDENTIALS_USR
                    server.password = CREDENTIALS_PSW
                    rtMaven = Artifactory.newMavenBuild()
                    rtMaven.deployer releaseRepo: DEPLOY_REPO, snapshotRepo: 'libs-snapshot-local', server: server
                    rtMaven.resolver releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot', server: server
                    buildInfo = Artifactory.newBuildInfo()
                    buildInfo.env.capture = true

                    //Change source version
                    descriptor = Artifactory.mavenDescriptor()
                    rootPom = "$WORKSPACE/maven-example/pom.xml" as String
                    descriptor.pomFile = rootPom
                    descriptor.version = "${BUILD_NUMBER}" as String
                    descriptor.failOnSnapshot = true
                    descriptor.transform()

                }
            }
        }

        stage('Exec Maven Build') {
            steps {
                script {
                    rtMaven.run pom: rootPom, goals: 'clean install -T 3 -Dmaven.repo.local=${WORKSPACE}/.repo', buildInfo: buildInfo
                }
            }
            post {
                always {
                    junit allowEmptyResults: false, testResults: '**/target/surefire-reports/TEST-*.xml'
                }
            }
        }

        stage('Build RPM installer') {
            steps {
                script {
                    sh "cd maven-example && ./rpmscript elig $BUILD_NUMBER $WORKSPACE/maven-example/rpmbuild/"
                }
            }
        }

        stage('Publish RPM') {
            steps {
                script {
                    def uploadSpec = """{
  "files": [
    {
      "pattern": "${WORKSPACE}/**/RPMS/*/*.rpm",
      "target": "${DEPLOY_REPO}/org/jfrog/test/multi3/$BUILD_NUMBER/"
    }
 ]
}"""
                    def buildInfo2 = server.upload(uploadSpec)
                    buildInfo.append buildInfo2
                }
            }
        }

        stage('Publish Build Info') {
            steps {
                script {
                    server.publishBuildInfo buildInfo
                }
            }
        }

        stage('Xray scan') {
            steps {
                script {
                    def scanConfig = [
                            'buildName'  : JOB_NAME,
                            'buildNumber': BUILD_NUMBER,
                            'failBuild'  : false
                    ]
                    def scanResult = server.xrayScan scanConfig
                    echo scanResult as String
                }
            }
        }

        stage('Sanity Test') {
            steps {
                build job: 'Tests/fast-sanity', parameters: [string(name: 'BUILD_NAME', value: JOB_NAME), string(name: 'BUILD_NUMBER', value: BUILD_NUMBER)]
            }
        }

        stage('Promote To Integration Repo') {
            input {
                message "Should we continue?"
                ok "Yes, we should."
            }

            steps {
                script {
                    qaUtils.buildPromote(buildInfo, INTEGRATION_REPO, 'Staging', 'Staging build to integration repository', CREDENTIALS_USR, CREDENTIALS_PSW)
                }
            }
        }

        stage('Execute full test suit') {
            parallel {
                stage('UI Test') {
                    steps {
                        build job: 'Tests/ui-test', parameters: [string(name: 'BUILD_NAME', value: JOB_NAME), string(name: 'BUILD_NUMBER', value: BUILD_NUMBER)]
                    }
                }
                stage('Rest Test') {
                    steps {
                        build job: 'Tests/rest-test', parameters: [string(name: 'BUILD_NAME', value: JOB_NAME), string(name: 'BUILD_NUMBER', value: BUILD_NUMBER)]
                    }
                }
                stage('Installer Test') {
                    steps {
                        build job: 'Tests/installer-test', parameters: [string(name: 'BUILD_NAME', value: JOB_NAME), string(name: 'BUILD_NUMBER', value: BUILD_NUMBER)]
                    }
                }
                stage('Load Test') {
                    steps {
                        build job: 'Tests/load-test', parameters: [string(name: 'BUILD_NAME', value: JOB_NAME), string(name: 'BUILD_NUMBER', value: BUILD_NUMBER)]
                    }
                }
            }
        }

        stage('Promote To Release Repo') {
            steps {
                script {
                    qaUtils.buildPromote(buildInfo, RELEASE_REPO, 'Released', 'Promoting build to release repository', CREDENTIALS_USR, CREDENTIALS_PSW)
                }
            }
        }

        stage('Generate Release Bundle') {
            input {
                message "Should we continue?"
                ok "Yes, we should."
                parameters {
                    string(name: 'BUILD_NUMBER', defaultValue: BUILD_NUMBER, description: 'BUILD_NUMBER')
                }
            }
            steps {
                script {
                    String queries = DistributionService.buildTestListAql(JOB_NAME, BUILD_NUMBER)
                    def distClient = DistributionService.getDistributionClient(DIST_CREDS_USR, DIST_CREDS_PSW)
                    qaUtils.createBundle(BUNDLE_VERSION, 'I hope you are enjoying the MeetUp, we are RECRUITING!!!.', queries, 'rpm-meetup-pipe', BUILD_NUMBER, false, distClient)

                }
            }
        }

        stage('Distribute Bundle') {
            steps {
                script {
                    def distClient = DistributionService.getDistributionClient(DIST_CREDS_USR, DIST_CREDS_PSW)
                    qaUtils.executeDistribution(JOB_NAME, BUNDLE_VERSION, 'artifactory-edge*', distClient, false)
                }
            }
        }

        stage('Wait for edge deployment') {
            steps {
                retry(2) {
                    script {
                        def distClient = DistributionService.getDistributionClient(DIST_CREDS_USR, DIST_CREDS_PSW)
                        qaUtils.waitForDistributionsToFinish(JOB_NAME, BUNDLE_VERSION, distClient)
                    }
                }
            }
        }

    }


}