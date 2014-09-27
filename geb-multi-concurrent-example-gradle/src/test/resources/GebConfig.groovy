import geb.driver.BrowserStackDriverFactory
import org.openqa.selenium.Platform
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import geb.report.ReportState
import geb.report.Reporter
import geb.report.ReportingListener
import org.openqa.selenium.remote.BrowserType
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver


waiting {
    timeout = 10
}

reportingListener = new ReportingListener() {
    void onReport(Reporter reporter, ReportState reportState, List<File> reportFiles) {
        reportFiles.each {
            println "[[ATTACHMENT|$it.absolutePath]]"
        }
    }
}


def browserStackBrowser = System.getProperty("geb.browserstack.browser")
if (browserStackBrowser) {
    driver = {
        def username = System.getenv("GEB_BROWSERSTACK_USERNAME")
        assert username
        def accessKey = System.getenv("GEB_BROWSERSTACK_AUTHKEY")
        assert accessKey
        new BrowserStackDriverFactory().create(browserStackBrowser, username, accessKey, ['browserstack.localIdentifier': System.getProperty('browserstack.localIdentifier')])
    }
}

environments {

    firefox {
        driver = { new FirefoxDriver() }
    }

    chrome {
        driver = { new ChromeDriver() }
    }

    chromeGrid {
        driver= {
            DesiredCapabilities capability = DesiredCapabilities.chrome()

            capability.setCapability("jenkins.label", "chrome")
            // Say you want a specific node to thread your request, just specify the node name (it must be running a selenium configuration though)
//        capability.setCapability("jenkins.nodeName", "(slave1)");
            new RemoteWebDriver(new URL(System.getProperty('seleniumServerUrl')), capability)
        }
    }
    firefoxGrid {
        driver= {
            DesiredCapabilities capability = DesiredCapabilities.firefox()
            capability.setCapability("jenkins.label", "firefox")
            new RemoteWebDriver(new URL(System.getProperty('seleniumServerUrl')), capability)
        }
    }

}

