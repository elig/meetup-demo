
/**
 * Just a dummy build to simulate short rest api build
 */
import static org.demo.BuildMarkerStatus.*

@Library('utils@ci-cd') _
pipeline {
    agent any

    environment {
        CREDENTIALS = credentials('jenkins')
        DOWNLOAD_REPO = 'libs-integration-local'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    parameters {
        string(name: 'BUILD_NAME', defaultValue: '', description: 'build name')
        string(name: 'BUILD_NUMBER', defaultValue: '', description: 'build number')
    }

    stages {
        stage('CleanUp') {
            steps {
                deleteDir()
            }
        }

        stage('Installer Tests') {
            when {
                allOf {
                    expression { params.BUILD_NAME != '' }
                    expression { params.BUILD_NUMBER != '' }
                }
            }
            steps {
                script {
                    log.info 'All INSTALLER TESTS HAVE PASSED'
                }
            }

        }
    }
    post {
        always {
            script {
                if (params.BUILD_NAME == '' || params.BUILD_NUMBER == '') {
                    log.error 'BUILD_NAME or BUILD NUMBER are not set'
                    currentBuild.result = 'FAILURE'
                }
            }
        }
        success {
            script {
                qaUtils.markTestStatus(passed)
            }
        }
        unstable {
            script {
                qaUtils.markTestStatus(failed)
            }
        }
    }
}