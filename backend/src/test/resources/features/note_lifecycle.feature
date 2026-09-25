Feature: Note lifecycle
  As a mission team member
  I want to archive, restore, and delete notes
  So that I can manage a note's lifecycle without losing history unintentionally

  Background:
    Given a clean workspace for team "alpha"

  Scenario: A note can be archived and then restored
    Given operator "ada" with clearance "CUI" creates a "INTERNAL" note titled "Launch plan"
    When operator "ada" with clearance "CUI" archives that note
    Then the note's status should be "ARCHIVED"
    When operator "ada" with clearance "CUI" restores that note
    Then the note's status should be "ACTIVE"

  Scenario: Deleting a note removes it permanently
    Given operator "ada" with clearance "CUI" creates a "INTERNAL" note titled "Temporary note"
    When operator "ada" with clearance "CUI" deletes that note
    Then the response status should be 204
    And fetching that note as "ada" with clearance "CUI" should return 404
