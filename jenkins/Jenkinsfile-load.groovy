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
        RESTV1_RESULT = null
        RESTV2_RESULT = null
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
        stage('Execute load tests') {
            when {
                allOf {
                    expression { params.BUILD_NAME != '' }
                    expression { params.BUILD_NUMBER != '' }
                }
            }

            parallel {
                stage('REST-V1 load test') {
                    steps {
                        blazeMeterTest(
                                credentialsId: 'blazemeter',
                                testId: '6285412',
                                notes: "Testing build = ${params.BUILD_NAME}, Build Number=${params.BUILD_NUMBER}",
                                sessionProperties: '',
                                reportLinkName: 'REST-V1',
                                jtlPath: '',
                                junitPath: 'blazemeter',
                                getJtl: false,
                                getJunit: true
                        )
                    }
                    post {
                        success {
                            script {
                                RESTV1_RESULT = 'SUCCESS'
                            }
                        }
                        failure {
                            script {
                                RESTV1_RESULT = 'FAIL'
                            }
                        }
                    }
                }

                stage('REST-V2 load test') {
                    steps {
                        blazeMeterTest(
                                credentialsId: 'blazemeter',
                                testId: '6285418',
                                notes: "Testing build = ${params.BUILD_NAME}, Build Number=${params.BUILD_NUMBER}",
                                sessionProperties: '',
                                reportLinkName: 'REST-V2',
                                jtlPath: '',
                                junitPath: 'blazemeter',
                                getJtl: false,
                                getJunit: true
                        )
                    }
                    post {
                        success {
                            script {
                                RESTV2_RESULT = 'SUCCESS'
                            }
                        }
                        failure {
                            script {
                                RESTV2_RESULT = 'FAIL'
                            }
                        }
                    }
                }


            }
        }

        stage('Update Status Monitor') {
            steps {
                script {
                    htmlRaw = '''
            <!doctype html><html lang="en"> <head> <title>Blazemeter tests results</title>
            <meta charset="utf-8"> <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
            <meta name="description" content=""> <meta name="author" content="">
            <link rel="stylesheet" href="/userContent/css/bootstrap.min.css" crossorigin="anonymous">
            <link rel="stylesheet" href="/userContent/css/fa.css" crossorigin="anonymous">
            <link rel="stylesheet" href="/userContent/css/fa-solid.css" crossorigin="anonymous">
            <style>html,body{height: 90%;}body{display: -ms-flexbox; display: -webkit-box; display: flex; -ms-flex-align: center; -ms-flex-pack: center; -webkit-box-align: center; align-items: center; -webkit-box-pack: center; justify-content: center; padding-top: 150px; padding-bottom: 140px; background-color: #f5f5f5;}</style>
            </head>
            <body class="text-center"><form class="form-signin">
            <h1 class="h3 mb-3 font-weight-normal">Artifactory: tests results</h1>
        '''
                    restv1ButtonCssClass = RESTV1_RESULT == "SUCCESS" ? 'btn-success' : 'btn-danger'
                    restv2ButtonCssClass = RESTV2_RESULT == "SUCCESS" ? 'btn-success' : 'btn-danger'

                    restv1HtmlRaw = '<div class="btn-lg ' + restv1ButtonCssClass + ' btn-block text-left" role="button">REST-V1: ' + ((RESTV1_RESULT != null) ? RESTV1_RESULT : "N/A. SKIPPED") + '</div>'
                    restv2HtmlRaw = '<div class="btn-lg ' + restv2ButtonCssClass + ' btn-block text-left" role="button">REST-V2: ' + ((RESTV1_RESULT != null) ? RESTV2_RESULT : "N/A. SKIPPED") + '</div>'

                    htmlRaw += '<span class="badge badge-info" style="line-height: 1.5;">BUILD_NAME: ' + BUILD_NAME + ', BUILD_NUMBER: ' + BUILD_NUMBER + '</span><br/><br/>'
                    htmlRaw += restv1HtmlRaw + restv2HtmlRaw
                    htmlRaw += '</form></body></html>'

                    writeFile(
                            file: "index.html",
                            text: htmlRaw
                    )

                    publishHTML([
                            allowMissing         : true,
                            alwaysLinkToLastBuild: false,
                            includes             : '**/index.html',
                            keepAll              : true,
                            reportDir            : '',
                            reportFiles          : 'index.html',
                            reportName           : 'Blazemeter Test Monitor',
                            reportTitles         : 'Blazemeter Test Monitor'
                    ])
                }

            }
        }
    }

    post {
        always {
            junit allowEmptyResults: false, testResults: '**/*.xml'
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













