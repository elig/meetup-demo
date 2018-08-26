package org.demo


/**
*  Created by elig on 01/09/2018.
*/
class DistributionServiceTest extends GroovyTestCase {
    void testAqlBuilder() {
        String aql = DistributionService.buildTestListAql('rpm-meetup-pipe', '10')
        assertToString(aql, 'items.find({"$and":[{"@test.fast-sanity.last_run.status":"passed"},{"@test.installer-test.last_run.status":"passed"},' +
                '{"@test.rest-test.last_run.status":"passed"},{"@test.ui-test.last_run.status":"passed"},{"@test.load-test.last_run.status":"passed"},' +
                '{"@build.name":"rpm-meetup-pipe"},{"@build.number":"10"}]})')
    }


}
