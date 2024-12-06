Feature: SOS Alert Management

  Background:
    Given I am a logged-in user
    And I'm part of one or more groups

  Rule: Triggering an SOS Alert
    Scenario:
      When I trigger an SOS alert
      Then all members of all my groups are notified that I am in danger
      And the alert must include the location where the alert was triggered

  Rule: Stopping an SOS Alert
    Scenario:
      When I stop an SOS alert
      Then all members of all my groups are notified that the alert has been stopped

  Rule: Automatic location tracking
    Scenario:
      When I trigger an SOS alert
      Then my live location should automatically start being shared with all members of all my groups
      And continue to be shared until the SOS alert is stopped

    Scenario:
      When I'm in SOS mode
      Then all my location updates are recorded to form a path
      And the path is shared with all members of all my groups until the SOS alert is stopped

    Scenario:
      When I'm in SOS mode
      And I stop sending location updates
      Then all members of all my groups are notified that I have stopped sending location updates while in SOS mode
