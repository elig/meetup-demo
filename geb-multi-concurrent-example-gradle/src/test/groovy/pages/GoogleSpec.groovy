package pages

import spock.lang.*
import geb.*
import geb.spock.*

class GoogleSpec extends GebReportingSpec {

    def "the first link should be wikipedia"() {
        when:
        to GoogleHomePage

        and:
        q = "wikipedia"

        then:
        at GoogleResultsPage

        and:
//        firstResultLink.text() == "Wikipedia"
        firstResultLink.text().contains('Wikipedia -fail')
        when:
        firstResultLink.click()

        then:
        waitFor { at WikipediaPage }
    }

    def "Hapoel is on the map"() {
        when:
        to GoogleHomePage

        and:
        q = "hapoel tel aviv"

        then:
        at GoogleResultsPage

        and:
        results != null
    }

}