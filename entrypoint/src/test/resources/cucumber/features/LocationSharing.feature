Feature: Location Sharing

  Background:
    Given I am a logged-in user
    And I'm part of one or more groups

  Scenario: Start sharing location with a group
    When I start sharing my location with a group
    Then all connected group members are able to receive location updates

  Scenario: Stop sharing location
    When I stop sharing my location with the group
    Then all connected group members are no longer able to receive location updates
    But they can still get my last known location and state
