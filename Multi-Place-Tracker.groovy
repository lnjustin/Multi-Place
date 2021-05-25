/**
 * Multi-Place Tracker
 *
 * V 1.0.0
 *
 * Copyright 2020 Justin Leonard
 *
 * Multi-Place has been licensed to you. By downloading, installing, and/or executing this software you hereby agree to the terms and conditions set forth in the Multi-Place license agreement.
 * <https://raw.githubusercontent.com/lnjustin/Multi-Place/master/License.md>
**/

metadata
{
    definition(name: "Multi-Place Tracker", namespace: "lnjustin", author: "Justin Leonard", importUrl: "")
    {
        capability "PresenceSensor"
        attribute "tracker", "string"
        attribute "place", "string"
        attribute "vehicle", "string"
        
        command "cancelTrip"
        command "startUpcomingTrip"   // command to start a trip early. Trip must be in the departure window or in the pre-departure phase shortly before the specified departure window

    }
}

def setPersonId(String personId) {
    state.personId = personId    
}

def cancelTrip() {
    parent.cancelCurrentTripForPerson(state.personId)
}

def startUpcomingTrip() {
    parent.startUpcomingTripForPerson(state.personId)    
}
