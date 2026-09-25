Feature: Classification-based access control
  As a mission team member
  I want notes to be filtered by data classification
  So that operators only ever see information at or below their clearance

  Background:
    Given a clean workspace for team "alpha"

  Scenario: An operator with CUI clearance sees notes of every classification
    Given operator "ada" with clearance "CUI" creates a "PUBLIC" note titled "Public release checklist"
    And operator "ada" with clearance "CUI" creates a "INTERNAL" note titled "Internal integration plan"
    And operator "ada" with clearance "CUI" creates a "CUI" note titled "Controlled sensor assessment"
    When operator "ada" with clearance "CUI" lists notes for team "alpha"
    Then the response status should be 200
    And the note list should contain 3 notes

  Scenario: An operator with INTERNAL clearance cannot see CUI notes
    Given operator "ada" with clearance "CUI" creates a "CUI" note titled "Controlled sensor assessment"
    When operator "grace" with clearance "INTERNAL" lists notes for team "alpha"
    Then the response status should be 200
    And the note list should contain 0 notes

  Scenario: An operator with INTERNAL clearance is denied creating a CUI note
    When operator "grace" with clearance "INTERNAL" attempts to create a "CUI" note titled "Denied note"
    Then the response status should be 403
    And the error code should be "ACCESS_DENIED"

  Scenario: Fetching a note above the caller's clearance returns not found, not forbidden
    Given operator "ada" with clearance "CUI" creates a "CUI" note titled "Controlled sensor assessment"
    When operator "grace" with clearance "INTERNAL" fetches that note directly
    Then the response status should be 404
    And the error code should be "NOTE_NOT_FOUND"
