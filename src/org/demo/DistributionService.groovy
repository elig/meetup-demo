package org.demo

import com.jfrog.bintray.client.api.handle.Bintray
import groovy.json.JsonBuilder

/**
*  Created by elig on 27/08/2018.
*/
@Grapes([
@GrabResolver(name = 'restlet', root = 'https://jcenter.bintray.com/'),
@Grab(group = 'org.jfrog.artifactory.client', module = 'artifactory-java-client-services', version = '2.7.0'),
@Grab(group = 'com.jfrog.bintray.client', module = 'bintray-client-java-service', version = '0.9.2')])
import org.jfrog.artifactory.client.aql.*

import static com.jfrog.bintray.client.impl.BintrayClient.create
import static org.demo.BuildMarkerStatus.passed
import static org.demo.GlobalSettings.DIST_URL
import static org.demo.GlobalSettings.RELEASE_REPO
import static org.demo.GlobalSettings.TESTS_LIST
import static org.jfrog.artifactory.client.aql.AqlItem.aqlItem

class DistributionService implements Serializable {
    static String buildTestListAql(String buildName, String buildNumber) {
        List<AqlItem> items = []
        TESTS_LIST.each { key ->
            items.add(aqlItem("@test.${key}.last_run.status", passed))
        }
        items.add(aqlItem('@build.name', buildName))
        items.add(aqlItem('@build.number', buildNumber))
        items.add(aqlItem('repo', RELEASE_REPO))
        items.add(aqlItem("type", "file"))
        items.add(aqlItem("name", aqlItem('$match','*.rpm')))
        return new AqlQueryBuilder().and(items).build()
    }

    static Bintray getDistributionClient(String userName, String password){
        return create("$DIST_URL/api/v1", userName, password)

    }

    static def createBundle(def body, Bintray client){
        def url = "release_bundle"
        return client.post(url, ["content-type": "application/json"], new ByteArrayInputStream(new JsonBuilder(body).toString().getBytes()))
    }

    static def distributeBundle(def body, def name, def version, Bintray bintrayClient){
        return bintrayClient.post("distribution/${name}/${version}",["content-type": "application/json"],new ByteArrayInputStream(new JsonBuilder(body).toString().getBytes()))
    }

    static def getStatusByBundleName(def name, Bintray client){
        def url = "release_bundle/$name/distribution"
        client.get(url,null)
    }

    static def getStatusByBundleVersion(def name, def version ,Bintray client){
        def url = "release_bundle/$name/$version/distribution"
        client.get(url,null)
    }

    static def getStatusByTrackerId(def name, def version, trackerId, Bintray client){
        def url = "release_bundle/$name/$version/distribution/$trackerId"
        client.get(url,null)
    }
}

