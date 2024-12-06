package io.github.positionpal.location

import io.cucumber.scala.{EN, ScalaDsl}

class SOSAlertManagementSteps extends ScalaDsl with EN {

  When("I trigger an SOS alert") {}

  Then("all members of all my groups are notified that I am in danger") {}

  And("the alert must include the location where the alert was triggered") {}

  Then("my live location should automatically start being shared with all members of all my groups") {}

  And("continue to be shared until the SOS alert is stopped") {}

  When("I stop an SOS alert") {}

  Then("all members of all my groups are notified that the alert has been stopped") {}

  When("I'm in SOS mode") {}

  Then("all my location updates are recorded to form a path") {}

  And("the path is shared with all members of all my groups until the SOS alert is stopped") {}

  When("I stop sending location updates") {}

  Then("all members of all my groups are notified that I have stopped sending location updates while in SOS mode") {}
}
