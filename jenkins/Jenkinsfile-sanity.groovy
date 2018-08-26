import static org.demo.BuildMarkerStatus.*

/**
 * Just a dummy build to simulate short test build
 */
@Library('utils@ci-cd') _
pipeline {
    agent any

    environment {
        CREDENTIALS = credentials('jenkins')
        DOWNLOAD_REPO = 'libs-staging-local'
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

        stage('Sanity Tests') {
            when {
                allOf {
                    expression { params.BUILD_NAME != '' }
                    expression { params.BUILD_NUMBER != '' }
                }
            }
            steps {
                script {
                    server = Artifactory.server 'artifactory-demo'
                    server.username = CREDENTIALS_USR
                    server.password = CREDENTIALS_PSW

                    def downloadSpec = """{
 "files": [
  {
      "pattern": "$DOWNLOAD_REPO/**/*.rpm",
      "target": "$WORKSPACE/",
      "props": "build.name=$BUILD_NAME;build.number=$BUILD_NUMBER",
      "flat":"true"
    }
 ]
}"""
                    server.download(downloadSpec)
                    sh 'ls -al *.rpm'
                    log.info 'All SANITY TESTS HAVE PASSED'
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