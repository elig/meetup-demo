package org.demo

/**
*  Created by elig on 01/09/2018.
*/
abstract class GlobalSettings implements Serializable{

    public static final String DIST_URL = 'https://distribution-eli-demo.jfrogdev.co'
    public static final String DEPLOY_REPO = 'libs-staging-local'
    public static final String INTEGRATION_REPO = 'libs-integration-local'
    public static final String RELEASE_REPO = 'libs-release-local'
    public static final String DISTRIBUTION_REPO = 'rpm-local'
    public static final List TESTS_LIST = ['fast-sanity','installer-test','rest-test','ui-test','load-test']
    public static final String ARTIFACTORY_SERVICE_ID = 'jfrt@01czjqtg7pmty506443qhy19nb'
}
