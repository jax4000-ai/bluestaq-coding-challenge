Feature: Audit trail
  As a mission team lead
  I want every mutation recorded in the audit trail
  So that I can answer who changed what, and when, after the fact

  Background:
    Given a clean workspace for team "alpha"

  Scenario: Creating a note writes an audit record
    When operator "ada" with clearance "CUI" creates a "INTERNAL" note titled "Sprint retrospective"
    Then an audit record with action "NOTE_CREATED" should exist for that note

  Scenario: Archiving and deleting a note each write their own audit record
    Given operator "ada" with clearance "CUI" creates a "INTERNAL" note titled "Old plan"
    When operator "ada" with clearance "CUI" archives that note
    And operator "ada" with clearance "CUI" deletes that note
    Then an audit record with action "NOTE_ARCHIVED" should exist for that note
    And an audit record with action "NOTE_DELETED" should exist for that note
