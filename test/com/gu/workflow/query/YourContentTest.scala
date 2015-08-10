package com.gu.workflow.query

import lib.PostgresDB
import lib.TestData._
import models.{ContentItem, User}
import FilterTestOps._
import test._

import org.scalatest.{Matchers, FreeSpec}

class YourContentTest extends FreeSpec with WorkflowIntegrationSuite with Matchers {

  val testEmail = "guardian@example.com"
  val testUser = User(testEmail, "Test", "Bunny")

  val assignedToField: FieldGetter[Option[String]] = _.stub.assigneeEmail

  def matchCollaboratorsTest(email: String) =
    fieldTest(_.stub.composerId,
              optTest[String](composerId => DatabaseManager.hasCollaborator(composerId, email)))

  def assignedToTest(pattern: String) = fieldTest(_.stub.assigneeEmail,
                                                  optTest[String](_ == pattern))


  val testData = generateTestData()

  val withCollaborators = testData.map(c => ContentItemWithCollaborators(c, generateRandomSizeCollaborators()))

  "YourContent query" - {
    "should correctly find assigned content" in withTestData(testData) { insertedData =>
      (WfQuery(assignedToEmail = List(testEmail))
         should selectSameResultsAs (FilterTest(assignedToTest(testEmail), insertedData)))
    }
    "should correctly find touched content" in withCollaboratorTestData(withCollaborators) { insertedData =>
      (WfQuery(touched = List(testEmail))
         should selectSameResultsAs (FilterTest(matchCollaboratorsTest(testEmail), insertedData)))
    }
  }
}