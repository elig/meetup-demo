#!groovy
import groovy.json.JsonSlurper

import org.demo.DistributionService
import org.demo.BuildMarker
import org.demo.BuildMarkerStatus
import org.demo.BundleBuilder
import static org.demo.BuildMarkerStatus.*
import static org.demo.GlobalSettings.ARTIFACTORY_SERVICE_ID
import static org.demo.GlobalSettings.DISTRIBUTION_REPO
import static org.demo.GlobalSettings.RELEASE_REPO


def buildPromote(def buildInfo, String targetRepo, String status, String comment, String artUser, String artPassword) {
    def server = Artifactory.server 'artifactory-demo'
    server.username = artUser
    server.password = artPassword
    def promotionConfig = [
            // Mandatory parameters
            'buildName'          : buildInfo.name,
            'buildNumber'        : buildInfo.number,
            'targetRepo'         : targetRepo,
            'status'             : status,
            'comment'            : comment,
            'includeDependencies': false,
            'copy'               : false,
            'failFast'           : true
    ]

// Promote build
    server.promote promotionConfig
}


def markTestStatus(BuildMarkerStatus test_status) {
    stage('Set test property on artifacts') {
        BuildMarker marker = new BuildMarker(stripFolderName(env.JOB_NAME as String), env.BUILD_NUMBER as String, test_status, env.BUILD_URL as String, params.BUILD_NAME as String, params.BUILD_NUMBER as String, env.DOWNLOAD_REPO as String)
        log.info marker.toString()
        def server = Artifactory.server 'artifactory-demo'
        String artUrl = server.url
        for (build in marker.builds) {
            for (repository in marker.repositories) {
                log.info "Setting properties on $build in $repository."
                String cliCmd = "./jfrog rt sp --url $artUrl --user ${env.CREDENTIALS_USR} --password ${env.CREDENTIALS_PSW} --build ${build}/${marker.artifact_build_number} ${repository} 'test.${marker.build_name}.last_run.status=${marker.test_status};test.${marker.build_name}.last_run.url=${marker.build_url}"
                if (test_status == passed) {
                    cliCmd += ";test.${marker.build_name}.last_successful_run.url=${marker.build_url}"
                }
                cliCmd += "'"
                sh "docker run -e JFROG_CLI_OFFER_CONFIG=false elig-docker-registry.bintray.io/jfrog-cli-alpine:1.12.1 ${cliCmd}"
            }

        }

    }

}

def createBundle(String version, String releaseNotes, def queries, String bundleName, String buildNumber, boolean dryRun, def distUsr) {
    def body = new BundleBuilder()
            .sourceArtifactoryId(ARTIFACTORY_SERVICE_ID)
            .version(version)
            .name(bundleName)
            .dryRun(dryRun)
            .description('Release bundle demo')
            .signImmediately(true)
            .addMapping("${RELEASE_REPO}/(.+)/(.+)", "${DISTRIBUTION_REPO}/meetup-demo/${buildNumber}/\$2")
            .addQuery(queries)
            .build()
    log.info body as String
    def responseCode = DistributionService.createBundle(body, distUsr)
    if (!(responseCode.code ==~ /(200|201|202)/)) {
        error("Build failed because command returned $responseCode and not 200 or 201 or 202")
    }
}

String executeDistribution(String bundleName, String bundleVersion, String serviceExp,
                           def distUser, boolean dryRun = false) {
    Map body = [
            dry_run             : dryRun,
            "distribution_rules": [["service_name": "$serviceExp"]]
    ]
    def responseCode = DistributionService.distributeBundle(body, bundleName, bundleVersion, distUser)
    if (!(responseCode.code ==~ /(200|202)/)) {
        error("Build failed because command returned $responseCode and not 200 or 202")
    }
    return responseCode
}

@NonCPS
def waitForDistributionsToFinish(String bundleName, String bundleVersion, def distUser) {
    int counter = 5
    boolean success = false
    while (counter != 0) {
        def trackerResponse = new JsonSlurper().parse(DistributionService.getStatusByBundleVersion(bundleName, bundleVersion, distUser).getEntity().getContent())
        def trResponse = trackerResponse.findAll { a -> a."release_bundle_name" == bundleName && a."release_bundle_version" == bundleVersion }
        log.info("status for tracker bundle name ${trResponse.release_bundle_name} bundle version ${trResponse.release_bundle_version} is ${trResponse.status.get(0)}")
        if (trResponse.status.get(0) == "Completed") {
            log.info 'Distribution completed'
            success = true
            break
        }
        counter -= 1
        sleep(5000)
    }
    if (!success) error('Build failed because distribution was not completed in time')
}

def stripFolderName(String buildName) {
    return buildName.tokenize('/').last()
}

return this