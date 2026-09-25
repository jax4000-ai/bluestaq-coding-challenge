Feature: Optimistic concurrency and validation
  As a mission team member
  I want conflicting or invalid writes rejected
  So that no one silently loses another operator's changes or creates ambiguous data

  Background:
    Given a clean workspace for team "alpha"

  Scenario: A stale update is rejected with a conflict instead of overwriting
    Given operator "ada" with clearance "CUI" creates a "INTERNAL" note titled "Architecture notes"
    And operator "ada" with clearance "CUI" updates that note's content to "Version two"
    When operator "ada" with clearance "CUI" attempts to update the original version of that note with content "Stale edit"
    Then the response status should be 409
    And the error code should be "NOTE_CONFLICT"

  Scenario: Duplicate titles within the same team are rejected case-insensitively
    Given operator "ada" with clearance "CUI" creates a "INTERNAL" note titled "Retrospective"
    When operator "ada" with clearance "CUI" attempts to create a "INTERNAL" note titled "retrospective"
    Then the response status should be 422
    And the error code should be "NOTE_VALIDATION_FAILED"

  Scenario: The same title is allowed in a different team
    Given operator "ada" with clearance "CUI" creates a "INTERNAL" note titled "Retrospective"
    When operator "grace" with clearance "CUI" creates a "INTERNAL" note titled "Retrospective" for team "beta"
    Then the response status should be 201
