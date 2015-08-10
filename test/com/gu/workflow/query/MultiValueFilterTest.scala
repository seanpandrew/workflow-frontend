package com.gu.workflow.query

import com.gu.workflow.query.FilterTestOps._
import lib.TestData._
import models.{Flag, ContentItem, Section, Status}
import org.joda.time.DateTime
import org.scalatest.{Matchers, FreeSpec}
import test.WorkflowIntegrationSuite
import models.ContentItem._

class MultiValueFilterTest extends FreeSpec with WorkflowIntegrationSuite with Matchers{

  val testData = generateTestData()

  "No parameter set for a field" in {
    val dataInserted = testData.map(createContent(_)).flatten
    val query = WfQuery()
    query should selectSameResultsAs (FilterTest(noFilter, dataInserted))
  }

  "One parameter set for field" - {
    "field is status" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(status=Seq(Status("Writers")))
      val oneFilter = FilterTest(c => c.wcOpt.map(_.status) == Some(Status("Writers")), dataInserted)
      query should selectSameResultsAs (oneFilter)
    }

    "field is section" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(section=Seq(Section("Arts")))
      val oneFilter = FilterTest(c => c.stub.section == "Arts", dataInserted)
      query should selectSameResultsAs (oneFilter)
    }

    "field is contentType" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(contentType=Seq("article"))
      val oneFilter = FilterTest(c => contentTypeS(c).exists(_ == "article") && contentTypeWC(c).exists(_ == "article"), dataInserted)
      query should selectSameResultsAs (oneFilter)
    }

    "field is flags" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(flags=Seq(Flag.NotRequired))
      val oneFilter = FilterTest(c => c.stub.needsLegal == Flag.NotRequired, dataInserted)
      query should selectSameResultsAs (oneFilter)
    }

    "field is prodOffice" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(prodOffice=Seq("AU"))
      val oneFilter = FilterTest(c => c.stub.prodOffice == "AU", dataInserted)
      query should selectSameResultsAs (oneFilter)

    }

  }

  "Multiple paramets set for field" - {
    "field is status" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(status=Seq(Status("Writers"), Status("Desk")))
      val multiFilter = FilterTest(c => c.wcOpt.map(_.status) == Some(Status("Writers")) || c.wcOpt.map(_.status) == Some(Status("Desk")), dataInserted)
      query should selectSameResultsAs (multiFilter)

    }

    "field is section" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(section=Seq(Section("Arts"), Section("Business")))
      val multiFilter = FilterTest(c => c.stub.section == "Arts" || c.stub.section == "Business", dataInserted)
      query should selectSameResultsAs (multiFilter)
    }

    "field is contentType" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(contentType=Seq("article","gallery"))
      val multiFilter = FilterTest(c =>  contentTypeS(c).exists(_ == "article") && contentTypeWC(c).exists(_ == "article") ||  contentTypeS(c).exists(_ == "gallery") && contentTypeWC(c).exists(_ == "gallery"), dataInserted)
      query should selectSameResultsAs (multiFilter)
    }

    "field is flags" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(flags=Seq(Flag.NotRequired, Flag.Complete))
      val oneFilter = FilterTest(c => c.stub.needsLegal == Flag.NotRequired || c.stub.needsLegal == Flag.Complete, dataInserted)
      query should selectSameResultsAs (oneFilter)
    }

    "field is prodOffice" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(prodOffice=Seq("AU","UK"))
      val oneFilter = FilterTest(c => c.stub.prodOffice == "AU" || c.stub.prodOffice == "UK", dataInserted)
      query should selectSameResultsAs (oneFilter)
    }

  }

  "Invalid parameter set for a field" - {
    "field is status" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(status=Seq(Status("invalid")))
      query should selectSameResultsAs (FilterTest(noResults, dataInserted))
    }

    "field is section" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(section=Seq(Section("invalid")))
      query should selectSameResultsAs (FilterTest(noResults, dataInserted))
    }

    "field is contentType" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(contentType=Seq("invalid"))
      query should selectSameResultsAs (FilterTest(noResults, dataInserted))
    }

    "field is prodOffice" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(prodOffice=Seq("invalid"))
      query should selectSameResultsAs (FilterTest(noResults, dataInserted))
    }

  }

  "All parameters set for a field" - {
    "field is status" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(status=statuses)
      query should selectSameResultsAs (FilterTest(noFilter, dataInserted))
    }

    "field is section" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(section=sections.map(Section(_)))
      query should selectSameResultsAs (FilterTest(noFilter, dataInserted))
    }

    "field is contentType" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(contentType=contentTypes)
      query should selectSameResultsAs (FilterTest(noFilter, dataInserted))
    }

    "field is flags" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(flags=needsLegal)
      query should selectSameResultsAs (FilterTest(noFilter, dataInserted))
    }

    "field is prodOffice" in {
      val dataInserted = testData.map(createContent(_)).flatten
      val query = WfQuery(prodOffice=prodOffices)
      query should selectSameResultsAs (FilterTest(noFilter, dataInserted))

    }
  }
}
