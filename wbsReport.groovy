package cz.tmobile.tests

import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter
import com.onresolve.scriptrunner.runner.customisers.WithPlugin

import cz.morosystems.jira.tm.pmo.cf.controllingobjects.service.ControllingObjectsService

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


cz.morosystems.jira.tm.pmo.cf.controllingobjects.service.ControllingObjectsService

@WithPlugin("cz.morosystems.jira.tmobile-pmo-cf-controllingobjects")

def controllingObjectService = ComponentAccessor.getOSGiComponentInstanceOfType(ControllingObjectsService)
SearchService searchService = ComponentAccessor.getComponent(SearchService.class)
ApplicationUser user = ComponentAccessor.getUserManager().getUserByName("service")
IssueManager issueManager = ComponentAccessor.getIssueManager()




String jqlSearch = '(project in (DEM, LA, AD, B2B, PRG) OR category = Tribe) AND ("CO CZ" is not EMPTY OR "CO SK" is not empty) AND resolution is EMPTY'

def parseResult = searchService.parseQuery(user, jqlSearch)
Set<Issue> issuesWithInactiveCO = []
Map<Issue, Set<String>> wrongObjects = [:]
int count = 1
if (parseResult.isValid()) {
    def searchResult = searchService.search(user, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
    def issues = searchResult.results.collect { issueManager.getIssueObject(it.id) }
    boolean  isActive

    for (Issue issue in issues) {
        log.error("Inspecting issue no: " + count + " from: " + issues.size())
        for (def controllingObject: controllingObjectService.getControllingObjectsForIssue(issue.id)) {
            isActive = controllingObjectService.getControllingObjectbyIdentifier(controllingObject.controllingObjectDto.identifier).get().controllingObjectStatusDto.name == "Y"
            if (!isActive) {
                issuesWithInactiveCO.add(issue)
                wrongObjects.keySet().contains(issue) ? wrongObjects.get(issue).add(controllingObject.controllingObjectDto.identifier?.toString()) :
                        wrongObjects.put(issue, [controllingObject.controllingObjectDto.identifier?.toString()])
                //break
            }
        }
        count++
    }

} else {
    log.error("Invalid JQL: " + jqlSearch)
    return (1)
}

final String DATE_PATTERN = "YYYYMMdd"
DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
String date = LocalDateTime.now().format(formatter);
File file = new File("/tmp/COsReport_" + date + ".csv" )
file.write("PROJECT;KEY;ISSUE TYPE;STATUS;INACTIVE COs\n")

issuesWithInactiveCO.each {iss ->
    log.error(iss.projectObject.key + ";" + iss.key + ";" + iss.issueType.name + ";" + iss.status.name + ";" + wrongObjects.get(iss).join(","))
    file.append(iss.projectObject.key + ";" + iss.key + ";" + iss.issueType.name + ";" + iss.status.name + ";" + wrongObjects.get(iss).join(",") + "\n")
}

log.error("Number of wrong issues: " + issuesWithInactiveCO.size())









long issueId = 370747

