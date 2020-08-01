/**
 * Multi-Place Tracker
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
    }
}

def setPersonId(String personId) {
    state.personId = personId    
}

def cancelTrip() {
    parent.cancelCurrentTripForPerson(state.personId)
}
