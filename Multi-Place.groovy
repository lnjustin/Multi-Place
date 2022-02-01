
/**
 * Multi-Place
 * v1.0.0 - Initial Release
 * v1.1.0 - Added failed arrival automations

 *
 * Copyright 2020 Justin Leonard
 *
 * Multi-Place has been licensed to you. By downloading, installing, and/or executing this software you hereby agree to the terms and conditions set forth in the Multi-Place license agreement.
 * <https://raw.githubusercontent.com/lnjustin/Multi-Place/master/License.md>
 *
 * Attribution
 * Icons made by:
 * <a href="https://www.flaticon.com/free-icon/dumbbell_1159873" title="Kiranshastry">Kiranshastry</a> f
 * <a href="https://www.flaticon.com/authors/pixel-perfect" title="Pixel perfect">Pixel perfect</a> 
 * <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> 
 * <a href="https://www.flaticon.com/authors/vitaly-gorbachev" title="Vitaly Gorbachev">Vitaly Gorbachev</a>
 * All from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
 *
 *

 */


import groovy.json.*
import java.text.SimpleDateFormat
import groovy.json.JsonBuilder
import groovy.transform.Field

definition(
    name: "Multi-Place",
    namespace: "lnjustin",
    author: "Justin Leonard",
    description: "Multi-Place Presence Tracker with Travel Advisor",
    category: "Presence",
    iconUrl: getLogoPath(),
    iconX2Url: getLogoPath(),
    iconX3Url: getLogoPath(),
    singleInstance: true,
    oauth: [displayName: "Multi-Place"],
    usesThirdPartyAuthentication: true)

@Field Integer fetchIntervalDefault = 5    // default number of minutes between checks of travel conditions during the departure window (does not apply to early checks before departure window)
@Field Integer tripPreCheckMinsDefault = 30    // default number of minutes before earliest departure time to check travel conditions
@Field Integer postArrivalDisplayMinsDefault = 10 // default number of minutes to display trip after arrival
@Field Integer cacheValidityDurationDefault = 120  // default number of seconds to cache directions/routes
@Field Integer optionsCacheValidityDurationDefault = 900
@Field Integer LateNotificationMinsDefault = 10
@Field Integer FailedArrivalNotificationMinsDefault = 30
@Field Integer geofenceRadiusDefault = 250
@Field String timeFormatDefault = "12 Hour"
@Field Boolean isPreferredRouteDisplayedDefault = false
@Field String circleBackgroundColorDefault = "#808080"
@Field String textColorDefault = "#000000"
@Field Integer trafficDelayThresholdDefault = 10
@Field Integer sleepMetricsDisplayMinsDefault = 60
@Field Integer avatarScaleDefault = 100
@Field Integer circleScaleDefault = 100

@Field Integer iterationCount = 1

@Field String checkMark = "https://raw.githubusercontent.com/lnjustin/App-Images/master/checkMark.svg"
@Field String xMark = "https://raw.githubusercontent.com/lnjustin/App-Images/master/xMark.svg"

mappings
{
    path("/multiplace/:personId/:type") { action: [ GET: "fetchTracker"] }
    path("/multiplaceHTML/:personId") { action: [ GET: "fetchHTMLTracker"] }
    path("/multiplaceSleep/:personId") { action: [ GET: "fetchSleepTracker"] }
}


def getTrackerEndpoint(String personId, trackerType='svg') {
    return getFullApiServerUrl() + "/multiplace/${personId}/${trackerType}?access_token=${state.accessToken}"
}

def getTrackerHTMLEndpoint(String personId) {
    return getFullApiServerUrl() + "/multiplaceHTML/${personId}?access_token=${state.accessToken}"
}

def getSleepTrackerEndpoint(String personId) {
    return getFullApiServerUrl() + "/multiplaceSleep/${personId}?access_token=${state.accessToken}"
}

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
     page name: "PeoplePage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "PeopleAccessoryPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "VehiclesPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "PlacesPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "TripsPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "RestrictionsPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "GoogleAPIPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "TrackerPage", title: "", install: false, uninstall: false, nextPage: "mainPage"
     page name: "AdvancedPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
}

String logo(String width='75') {
    return '<img width="' + width + 'px" style="display: block;margin-left: auto;margin-right: auto;margin-top:0px;" border="0" src="' + getLogoPath() + '">'
}

String MP() {
    return '<img width="125px" style="display: block;margin-left: auto;margin-right: auto; margin-top: 3px;" border="0" src="' + getMPPath() + '">'
}

def header() {
    paragraph logo() + MP()
}

def footer() {
    paragraph getInterface("line", "") + '<div style="display: block;margin-left: auto;margin-right: auto;text-align:center"><img width="25px" border="0" src="' + getLogoPath() + '"> &copy; 2020 Justin Leonard.<br>' + getInterface("link", "Readme. Attribution.", "https://github.com/lnjustin/Multi-Place/blob/master/README.md") + getInterface("link", " License.", "https://github.com/lnjustin/Multi-Place/blob/master/License.md")
}

def mainPage() {
    dynamicPage(name: "mainPage") {
            section {
                header()
                if (!api_key) {
                    paragraph getInterface("header", " Google API")
                    href(name: "GoogleAPIPage", title: getInterface("boldText", "Configure Google API Access"), description: "Google API Access Required for Travel Advisor and Recommended for Life360 Free.", required: false, page: "GoogleAPIPage", image: xMark)
                }
                paragraph getInterface("header", " Core Setup")
                if (state.people) {
                    href(name: "PeoplePage", title: getInterface("boldText", "People"), description: getPeopleDescription(), required: false, page: "PeoplePage", image: checkMark)
                    href(name: "PeopleAccessoryPage", title: getInterface("boldText", "People Accessories"), description: "Manage Accessories for People", required: false, page: "PeopleAccessoryPage", image: (state.accessories && state.accessories.size() > 0 ? checkMark : xMark))
                    href(name: "VehiclesPage", title: getInterface("boldText", "Vehicles"), description: (state.vehicles ? getVehiclesDescription() : "Click to Add Vehicles"), required: false, page: "VehiclesPage", image: (state.vehicles ? checkMark : xMark))
                    href(name: "PlacesPage", title: getInterface("boldText", "Places"), description: (state.places ? getPlacesDescription() : "Click to Add Places"), required: false, page: "PlacesPage", image: (state.places ? checkMark : xMark))
                }
                else {
                    href(name: "PeoplePage", title: getInterface("boldText", "People"), description: "Add a person to get started.", required: false, page: "PeoplePage", image: xMark)
                    href(name: "PeopleAccessoryPage", title: getInterface("boldText", "People Accessories"), description: "Add at least one person before managing accessories for people", required: false, page: "", image: xMark)
                    href(name: "VehiclesPage", title: getInterface("boldText", "Vehicles"), description: "Add at least one person before managing vehicles.", required: false, page: "", image: xMark)
                    href(name: "PlacesPage", title: getInterface("boldText", "Places"), description: "Add at least one person before managing places.", required: false, page: "", image: xMark)
                }
                
            }
        
            if (api_key) {
                section {
                    paragraph getInterface("header", " Travel Advisor")
                    href(name: "TripsPage", title: getInterface("boldText", "Trips"), description: (state.trips ? getTripDescriptionList() : "No trips configured"), required: false, page: "TripsPage", image: (state.trips ? checkMark : xMark))
                    href(name: "RestrictionsPage", title: getInterface("boldText", "Mode-Based Restrictions"), description: (restrictedModes ? getRestrictedModesDescription() : "No restrictions configured. Restrict Travel Advisor by Hub Mode."), required: false, page: "RestrictionsPage", image: (restrictedModes ? checkMark : xMark))
                }
            }
        
            section {
                paragraph getInterface("header", " Manage Settings")
                if (api_key) {
                    href(name: "GoogleAPIPage", title: getInterface("boldText", "Configure Google API Access"), description: "API Key: ${api_key}", required: false, page: "GoogleAPIPage", image: checkMark)                    
                }
                    href(name: "TrackerPage", title: getInterface("boldText", "Tracker Settings"), description: "Colors, Time Format, Traffic Thresholds, Display Timing", required: false, page: "TrackerPage")
                    href(name: "AdvancedPage", title: getInterface("boldText", "Advanced Settings"), description: "Cache Duration Settings, Enable Debug Logging", required: false, page: "AdvancedPage")
			}
        section {
         footer()   
        }
    }

}

def GoogleAPIPage() {
    dynamicPage(name: "GoogleAPIPage") {
        section {
            header()
            paragraph getInterface("header", " Google API Access")
            href(name: "GoogleApiLink", title: "Get Google API Key", required: false, url: "https://developers.google.com/maps/documentation/directions/get-api-key", style: "external")
            input name: "api_key", type: "text", title: "Enter Google API key", required: false, submitOnChange: true
        }
        section {
          footer()   
        }
    }
}

def formatAvatarPreview(String imageUrl) {
    return '<div><img width="80px" border="0" style="max-width:100px" src="' + imageUrl + '"></div>'   
}

def PeoplePage() {
    dynamicPage(name: "PeoplePage") {
        section() {
            header()
                paragraph getInterface("header", " Manage People")
                if (state.people) {
                    paragraph getInterface("note", "Click a person's avatar for accessing his or her graphical tracker via a cloud endpoint. May need to refresh after clicking 'Done' on the app's main page.")
                    state.people.each { id, person ->
                        def avatar = getPersonAvatar(id)
                        def avatarPreview = avatar ? avatar : getPathOfStandardIcon("Dark", "Unknown")
                        paragraph '<table width="100%"><tr><td align="center"><font style="font-size:20px;font-weight: bold"><a href="' + getTrackerHTMLEndpoint(id) + '" target="_blank"  style="color:black;">' + formatAvatarPreview(avatarPreview) + settings["person${id}Name"] + '</a></font></td></tr></table>', width:3
                    }
                    paragraph getInterface("line", "")
                }
                
            if (state.addingPerson) {
                paragraph getInterface("subHeader", " Add Person")
                input name: "person${state.lastPersonID}Name", type: "text", title: "Unique Name", submitOnChange: false, required: true
                
                input name: "person${state.lastPersonID}Avatar", type: "enum", options: getIconsEnum("People"), title: "Avatar", submitOnChange: true, multiple: false, required: true, width: 4
                def avatar = settings["person${state.lastPersonID}Avatar"]
                if (avatar != null) {
                    avatarPath = avatar == "Custom" ? settings["person${state.lastPersonID}AvatarCustom"] : getPathOfStandardIcon(avatar, "People")
                    avatarPath = avatarPath != null ? avatarPath : getPathOfStandardIcon("Dark", "Unknown")
                    paragraph formatAvatarPreview(avatarPath), width: 2
                }
                if (settings["person${state.lastPersonID}Avatar"] == "Custom") {
                    input name: "person${state.lastPersonID}AvatarCustom", type: "text", title: "URL to Custom Avatar", submitOnChange: true, required: true
                }
                paragraph getInterface("link", "SVG Avatar Creator", "https://avatarmaker.com/")
                
                input name: "person${state.lastPersonID}Life360", type: "device.LocationTrackerUserDriver", title: "Life360 with States Device", submitOnChange: false, multiple: false, required: false
                input name: "person${state.lastPersonID}SleepSensor", type: "device.WithingsSleepSensor", title: "Withings Sleep Sensor", submitOnChange: false, multiple: false, required: false
                if (settings["person${state.lastPersonID}SleepSensor"]) {
                    input name: "person${state.lastPersonID}SleepScoreBias", type: "number", title: "Sleep Score Bias", submitOnChange: false, required: false, defaultValue: 0
                    input name: "person${state.lastPersonID}SleepScoreBiasNeg", type: "bool", title: "Negative?", submitOnChange: false, required: false, defaultValue: false
                }
                if (state.vehicles || state.places) paragraph getInterface("subHeader", " Presence Sensors for Person")
                if (state.vehicles) {
                    for (vehicleId in state.vehicles) {
                        input name: "vehicle${vehicleId}Person${state.lastPersonID}Sensor", type: "capability.presenceSensor", title: settings["vehicle${vehicleId}Name"] + " Presence Sensor", description: "Presence Sensor for this person's presence in the " + settings["vehicle${vehicleId}Name"] + " vehicle", submitOnChange: true, multiple: false, required: false
                    }
                }
                
                if (state.places) {
                    state.places.each { placeId, place ->
                        input name: "place${placeId}Person${state.lastPersonID}Sensor", type: "capability.presenceSensor", title: "${settings["place${placeId}Name"]} Presence Sensor", description: "Presence Sensor for this person's presence at " + settings["place${placeId}Name"], submitOnChange: true, multiple: false, required: false
                    }
                }
                input name: "person${state.lastPersonID}Stars", type: "bool", title: "Track Stars?", submitOnChange: true, required: true
                if (settings["person${state.lastPersonID}Stars"] == true) {
                   input name: "person${state.lastPersonID}NumStars", type: "number", title: "Num Stars?", submitOnChange: false, required: true
                   input name: "person${state.lastPersonID}StarColor", type: "enum", options: ["Blue", "Red", "Yellow"], title: "Select Star Color", submitOnChange: false, required: true
                }
                paragraph "<br>"
                input name: "submitNewPerson", type: "button", title: "Submit", width: 3
                input name: "cancelAddPerson", type: "button", title: "Cancel", width: 3
            }
            else if (state.deletingPerson) {
                paragraph getInterface("subHeader", " Delete Person")
                input name: "personToDelete", type: "enum", title: "Person to Delete:", options: getPeopleEnumList(), multiple: false, submitOnChange: true
                if (personToDelete) input name: "submitDeletePerson", type: "button", title: "Confirm Delete", width: 3
                input name: "cancelDeletePerson", type: "button", title: "Cancel", width: 3
            }
            else if (state.editingPerson) {
                paragraph getInterface("subHeader", " Edit Person")
                input name: "personToEdit", type: "enum", title: "Edit Person:", options: getPeopleEnumList(), multiple: false, submitOnChange: true
                if (personToEdit) {
                    def id = null
                    id = getIdOfPersonWithName(personToEdit)
                    if (id != null) {
                        state.editedPersonId = id    // save the ID and name of the person being edited in state
                        state.editedPersonName = settings["person${id}Name"]
                    }
                    else {
                        // just edited the person's name so that personToEdit no longer holds the same name as in settings["person${id}Name"]. Need to update that.
                        id = state.editedPersonId
                        app.updateSetting("personToEdit",[type:"enum",value:settings["person${id}Name"]]) 
                        updateAfterPersonNameEdit()
                        state.editedPersonName = settings["person${id}Name"]
                    }
                    input name: "person${id}Name", type: "text", title: "Unique Name", submitOnChange: true, required: true

                    input name: "person${id}Avatar", type: "enum", options: getIconsEnum("People"), title: "Avatar", submitOnChange: true, multiple: false, required: true, width: 4
                    def avatar = settings["person${id}Avatar"]
                    if (avatar != null) {
                        avatarPath = avatar == "Custom" ? settings["person${id}AvatarCustom"] : getPathOfStandardIcon(avatar, "People")
                        avatarPath = avatarPath != null ? avatarPath : getPathOfStandardIcon("Dark", "Unknown")
                        paragraph formatAvatarPreview(avatarPath), width: 2
                    }
                    if (settings["person${id}Avatar"] == "Custom") {
                        input name: "person${id}AvatarCustom", type: "text", title: "URL to Custom Avatar", submitOnChange: true, required: true
                    }
                    paragraph getInterface("link", "SVG Avatar Creator", "https://avatarmaker.com/")
                    
                    input name: "person${id}Life360", type: "device.LocationTrackerUserDriver", title: "Life360 with States Device", submitOnChange: true, multiple: false, required: false
                    input name: "person${id}SleepSensor", type: "device.WithingsSleepSensor", title: "Withings Sleep Sensor", submitOnChange: false, multiple: false, required: false
                    if (settings["person${id}SleepSensor"]) {
                    input name: "person${id}SleepScoreBias", type: "number", title: "Sleep Score Bias", submitOnChange: false, required: false, defaultValue: 0
                    input name: "person${id}SleepScoreBiasNeg", type: "bool", title: "Negative?", submitOnChange: false, required: false, defaultValue: false
                }                   
                    if (state.vehicles || state.places) paragraph getInterface("subHeader", " Presence Sensors for Person")
                    if (state.vehicles) {
                        for (vehicleId in state.vehicles) {
                            input name: "vehicle${vehicleId}Person${id}Sensor", type: "capability.presenceSensor", title: settings["vehicle${vehicleId}Name"] + " Presence Sensor", description: "Presence Sensor for this person's presence in the " + settings["vehicle${vehicleId}Name"] + " vehicle", submitOnChange: true, multiple: false, required: false
                        }
                    }
                
                    if (state.places) {
                        state.places.each { placeId, place ->
                            input name: "place${placeId}Person${id}Sensor", type: "capability.presenceSensor", title: "${settings["place${placeId}Name"]} Presence Sensor", description: "Presence Sensor for this person's presence at " + settings["place${placeId}Name"], submitOnChange: true, multiple: false, required: false
                        }
                    }
                    input name: "person${id}Stars", type: "bool", title: "Track Stars?", submitOnChange: true, required: true
                    if (settings["person${id}Stars"] == true) {
                        input name: "person${id}NumStars", type: "number", title: "Num Stars?", submitOnChange: false, required: true
                        input name: "person${id}StarColor", type: "enum", options: ["Blue", "Red", "Yellow"], title: "Select Star Color", submitOnChange: false, required: true
                    }
                    paragraph "<br>"
                    input name: "submitEditPerson", type: "button", title: "Done", width: 3
                }
                
            }            
            else {     

                input name: "addPerson", type: "button", title: "Add Person", width: 3                
                if (state.people) input name: "editPerson", type: "button", title: "Edit Person", width: 3
                app.clearSetting("personToEdit")
                if (state.people) input name: "deletePerson", type: "button", title: "Delete Person", width: 3
                app.clearSetting("personToDelete")
            } 
            footer() 
        }
    }
}

def hasOnlySleepSensor(personId) {
    Boolean hasOnlySleepSensor = null
    if (settings["person${personId}SleepSensor"]) {
        if (!settings["person${personId}Life360"]) {
            if (state.vehicles || state.places) {
                if (state.vehicles) {
                    for (vehicleId in state.vehicles) {
                         if (settings["vehicle${vehicleId}Person${personId}Sensor"]) hasOnlySleepSensor = false
                    }
                }
                if (state.places) {
                    state.places.each { placeId, place ->
                         if (settings["place${placeId}Person${personId}Sensor"]) hasOnlySleepSensor = false
                    }
                }
                if (hasOnlySleepSensor == null) hasOnlySleepSensor = true
            }
            else hasOnlySleepSensor = true
        }
        else hasOnlySleepSensor = false
    }
    else hasOnlySleepSensor = false
    return hasOnlySleepSensor
}

def updateAfterPersonNameEdit() {
    state.trips.each { tripId, trip ->
        def tripPeople = []
        for (tripPersonName in settings["trip${tripId}People"]) {
            if (tripPersonName == state.editedPersonName) tripPeople.add(settings["person${state.editedPersonId}Name"])
            else tripPeople.add(tripPersonName)
        }
        app.updateSetting("trip${tripId}People",[type:"enum",value:tripPeople]) 
    }
    updateTrackerName(state.editedPersonId)
}

def getPeopleIds() {
    return state.people?.keySet()
}

def getPeopleEnumList() {
    def list = []
    if (state.people) {
        state.people.each { id, person ->
            list.add(settings["person${id}Name"])
        }
    }
    return list
}

def getPeopleDescription() {
    def people = getPeopleEnumList()
    def description = ""
    for (i=0; i < people.size(); i++) {
         description += people[i]   
        if (i != people.size-1) description += ", "
    }
    return description
}

def addPerson(String id) {
    if (!state.people) state.people = [:]
    def currentPlaceMap = [name: null, id: null, arrival: null]
    def currentVehicleMap = [id: null, arrival: null]
    def currentTripMap = [id: null, departureTime: null, recommendedRoute: null, eta: null, hasPushedLateNotice: null] // map for details about active trip
    def previousTripMap = [id: null, departureTime: null, arrivalTime: null] // map for details about last trip
    def previousPlaceMap = [name: null, id: null, arrival: null, departure: null]
    def previousVehicleMap = [id: null, arrival: null, departure: null]
    def currentMap = [place: currentPlaceMap, vehicle: currentVehicleMap, trip: currentTripMap]
    def previousMap = [place: previousPlaceMap, vehicle: previousVehicleMap, trip: previousTripMap]
    def life360Map = [address: null, latitude: null, longitude: null, placeIdAtAddress: null, placeIdWithName: null, placeIdAtCoordinates: null, atTime: null, isDriving: null]
    def sleepMap = [presence: null, presenceAtTime: null, score: null, quality: null, sleepDataAtTime: null, winner: false, weekWinCount: 0, weekWinner: false, monthWinCount: 0, monthWinner: false]
    def starsMap = [earned:null, completedCount: null, earnedToday: null]
    def accessoryMap = [earned: [], active:[]]
    def personMap = [current: currentMap, previous: previousMap, life360: life360Map, places: null, vehicles: null, sleep: sleepMap, sleepOnlySensor: null, stars: starsMap, accessories:accessoryMap]
        // so use like state.people.persondId.current.place.name
    state.people[id] = personMap
}

def getSleepData(String personId) {
    return state.people[personId]?.sleep
}

def isWithinSleepDisplayWindow(String personId) {
    def inWindow = false
    def sleepLastUpdate = state.people[personId]?.sleep.sleepDataAtTime
    if (sleepLastUpdate) {
        def secsPassed = getSecondsSince(sleepLastUpdate)
        if (secsPassed <= getSleepMetricsDisplayMinsSetting()*60) inWindow = true
        logDebug("Sleep display window ${inWindow ? "is valid" : "has passed"} with secsPassed = ${secsPassed} for personID: ${personId}", "Sleep")
    }
    return inWindow
}

def isInBed(String personId) {
    def isInBed = false
    if (state.people[personId]?.sleep.presence == "present") isInBed = true
    return isInBed
}

def hasSleepSensor(String personId) {
    def hasSensor = false
    if (settings["person${personId}SleepSensor"]) hasSensor = true
    return hasSensor
}

def haveMultipleSleepSensors() {
    def haveMultiple = false
    def sensorCount = 0
    state.people.each { personId, person ->
        if (hasSleepSensor(personId)) sensorCount++
    }
    if (sensorCount >= 2) haveMultiple = true
    return haveMultiple
}

def getPlaceOfPresenceById(String personId) {
    logDebug("Getting place of presence for person ${personId}", "Places")
    return state.people[personId].current.place.id
}

def getPlaceOfPresenceByName(String personId) {
    return state.people[personId].current.place.name
}

def getNameOfVehiclePresentIn(String personId) {
    def vehicleName = ""
    if (state.people[personId].current.vehicle.id) {
        vehicleName = getNameOfVehicleWithId(state.people[personId].current.vehicle.id)
    }
    return vehicleName
}

def getIdOfVehiclePresentIn(String personId) {
    return state.people[personId].current.vehicle.id
}


def getEtaOfCurrentTrip(String personId) {
    return state.people[personId].current.trip.eta
}

Boolean isInVehicle(String personId) {
    return state.people[personId].current.vehicle.id ? true : false
}

String getIdOfPersonWithName(String name) {
    def list = []
    state.people?.each { id, person ->
        if (settings["person${id}Name"] == name) list.add(id)
    }
    if (list.size() > 1) log.warn "Non-unique names used. Behavior not accounted for."
    else if (list.size() == 0) {
        log.warn "No Person Found With the Name: ${name}"
        return null
    }
    else return list[0]
}


String getNameOfPersonWithId(String personId) {
    return settings["person${personId}Name"]
}

String getPersonAvatar(String personId) {
    def avatar = null
    if (settings["person${personId}Avatar"] == "Custom") avatar = settings["person${personId}AvatarCustom"]
    else if (settings["person${personId}Avatar"] != null) avatar = getPathOfStandardIcon(settings["person${personId}Avatar"], "People")
    return avatar
}


Boolean isDriving(String personId) {
    if (settings["person${id}Life360"]) return settings["person${id}Life360"].isDriving
    else return null
}


def deletePerson(String nameToDelete) {
    def idToDelete = getIdOfPersonWithName(nameToDelete)
    if (idToDelete && state.people) {       
        state.people.remove(idToDelete)
        state.images.people[idToDelete] = null
        clearPersonSettings(idToDelete)
        deleteTracker(idToDelete)
    }
}

def clearPersonSettings(String personId) {
    def personNameToDelete = getNameOfPersonWithId(personId)
    
    app.removeSetting("person${personId}Name")
    app.removeSetting("person${personId}Avatar")
    app.removeSetting("person${personId}AvatarCustom")
    app.removeSetting("person${personId}Life360")
    app.removeSetting("person${personId}SleepSensor") 
    
    if (state.vehicles) {
        for (vehicleId in state.vehicles) { 
         //   app.updateSetting("vehicle${vehicleId}Person${personId}Sensor",[type:"capability",value:[]])
            app.removeSetting("vehicle${vehicleId}Person${personId}Sensor")
        }
    }
    
    if (state.places) {
        state.places.each { placeId, place ->
           //  app.updateSetting("place${placeId}Person${personId}Sensor",[type:"capability",value:[]])  
            app.removeSetting("place${placeId}Person${personId}Sensor")
        }
    }
    
    if (state.trips) {
        state.trips.each { tripId, trip ->
            def newTripPeople = []
            for (tripPerson in settings["trip${tripId}People"]) {
                if (tripPerson != personNameToDelete) newTripPeople.add(tripPerson)
            }
            app.updateSetting("trip${tripId}People",[type:"enum",value:(newTripPeople.size() > 0 ? newTripPeople : null)])
        }
    }
    
}

def formatAccessoryPreview(accessoryId, personId = null) {
    if (personId == null) return '<div><img width="140px" border="0" style="max-width:140px" src="' + settings["accessory${accessoryId}Path"] + '"></div>'   
    else {
        def content = ''
        content += '<style>'
        content += 'body { margin: 0; vertical-align: top; }'
        content += '.tracker' + personId + ' { width: 100%; padding-top: 100%; vertical-align: top; position:relative; display:block;'
        content +=            'background-repeat: no-repeat; '

        def avatarSize = (getAvatarScaleSetting() / 100) *65
        
        content +=            'background-position:'
        def accessory = getAccessory(accessoryId)
        if (accessory.people[personId]?.top) content += "top ${accessory.people[personId]?.top ? accessory.people[personId]?.top : 0}% "
        else if (accessory.people[personId]?.bottom) content += "bottom ${accessory.people[personId]?.bottom ? accessory.people[personId]?.bottom : 0}% "
        if (accessory.people[personId]?.left) content += "left ${accessory.people[personId]?.left ? accessory.people[personId]?.left : 0}%,"
        else if (accessory.people[personId]?.right) content += "right ${accessory.people[personId]?.right ? accessory.people[personId]?.right : 0}%,"
        content +=            'center;'
        content +=            'background-size:'
        content += "${accessory.people[personId]?.scale ? accessory.people[personId]?.scale : 0}%,"  
        content +=            avatarSize + '%;'
        content +=            'background-image:' 
        content += 'url("' + accessory.path + '"),'             
        content +=            'url("' + getPersonAvatar(personId) + '");'       
        content += '}'
        content += '</style>'
        content += '<div class="tracker' + personId + '">'
        content += '</div>'  
        return content
    }
}

def clearAccessorySettings(accessoryId) {
    app.removeSetting("accessory${accessoryId}Name")
    app.removeSetting("accessory${accessoryId}Category")
    app.removeSetting("accessory${accessoryId}Path")
    app.removeSetting("accessory${accessoryId}NumStarsRequired")
     if (state.people) {
          state.people.each { id, person ->
              app.removeSetting("accessory${accessoryId}TopPerson${id}")
              app.removeSetting("accessory${accessoryId}BottomPerson${id}")
              app.removeSetting("accessory${accessoryId}LeftPerson${id}")
              app.removeSetting("accessory${accessoryId}RightPerson${id}")
              app.removeSetting("accessory${accessoryId}ScalePerson${id}")
              app.removeSetting("isAccessory${accessoryId}EarnedByPerson${id}")             
              app.removeSetting("isNextAccessory${accessoryId}ForPerson${id}")
          }
     }
}

def PeopleAccessoryPage() {
    dynamicPage(name: "PeopleAccessoryPage") {
        section() {
            header()
                paragraph getInterface("header", " Manage People Accessories")
                if (state.accessories) {
                    for (id in state.accessories) {
                        paragraph '<table width="80%"><tr><td align="center"><font style="font-size:20px;font-weight: bold">' + formatAccessoryPreview(id) + settings["accessory${id}Name"] + '</font></td></tr></table>', width:3
                    }
                    paragraph getInterface("line", "")
                }
                
            if (state.addingAccessory) {
                paragraph getInterface("subHeader", " Add Accessory")                
                    input name: "accessory${state.lastAccessoryID}Name", type: "text", title: "Unique Name", submitOnChange: false, required: true
                    input name: "accessory${state.lastAccessoryID}Category", type: "text", title: "Category", description: "Existing categories: ${getAccessoryCategoryEnumList()}", submitOnChange: false, required: true
                    input name: "accessory${state.lastAccessoryID}Path", type: "text", title: "Accessory Image Path", submitOnChange: true, required: true
                    input name: "accessory${state.lastAccessoryID}NumStarsRequired", type: "number", title: "Number of Stars to Earn Accessory", submitOnChange: false, required: true
                
                     if (state.people) {
                         state.people.each { id, person ->
                              paragraph '<div width="100%" align="center"><font style="font-size:20px;font-weight: bold">' + formatAccessoryPreview(state.lastAccessoryID, id) + settings["person${id}Name"] + '</font></div>', width:12

                              input name: "accessory${state.lastAccessoryID}TopPerson${id}", type: "text", title: "Top % Alignment", submitOnChange: true, required: false, width: 3
                              input name: "accessory${state.lastAccessoryID}BottomPerson${id}", type: "text", title: "Bottom % Alignment", submitOnChange: true, required: false, width: 3
                              input name: "accessory${state.lastAccessoryID}LeftPerson${id}", type: "text", title: "Left % Alignment", submitOnChange: true, required: false, width: 3
                              input name: "accessory${state.lastAccessoryID}RightPerson${id}", type: "text", title: "Right % Alignment", submitOnChange: true, required: false, width: 3
                              input name: "accessory${state.lastAccessoryID}ScalePerson${id}", type: "text", title: "Scale %", submitOnChange: true, required: false, width: 3
                        
                              input name: "isAccessory${state.lastAccessoryID}EarnedByPerson${id}", type: "bool", title: "Is accessory already earned by ${settings["person${id}Name"]}?", submitOnChange: false, required: false, width: 3
                              input name: "isAccessory${state.lastAccessoryID}ActiveForPerson${id}", type: "bool", title: "Is accessory active for ${settings["person${id}Name"]}?", submitOnChange: false, required: false, width: 3

                             input name: "isNextAccessory${state.lastAccessoryID}ForPerson${id}", type: "bool", title: "Set as next accessory to be earned for ${settings["person${id}Name"]}?", submitOnChange: false, required: false, width: 3
                          }
                      }   
                paragraph "<br>"
                input name: "submitNewAccessory", type: "button", title: "Submit", width: 3
                input name: "cancelAddAccessory", type: "button", title: "Cancel", width: 3
            }
            else if (state.deletingAccessory) {
                paragraph getInterface("subHeader", " Delete Accessory")
                input name: "accessoryToDelete", type: "enum", title: "Accessory to Delete:", options: getAccessoriesEnumList(), multiple: false, submitOnChange: true
                if (accessoryToDelete) input name: "submitDeleteAccessory", type: "button", title: "Confirm Delete", width: 3
                input name: "cancelDeleteAccessory", type: "button", title: "Cancel", width: 3
            }
            else if (state.editingAccessory) {
                paragraph getInterface("subHeader", " Edit Accessory")
                input name: "accessoryToEdit", type: "enum", title: "Edit Accessory:", options: getAccessoriesEnumList(), multiple: false, submitOnChange: true
                if (accessoryToEdit) {
                    def accessoryId = getIdOfAccessoryWithName(accessoryToEdit)
                    if (accessoryId != null) {
                        state.editedAccessoryId = accessoryId    // save the ID and name of the accessory being edited in state
                        state.editedAccessoryName = settings["accessory${accessoryId}Name"]
                    }
                    else {
                        // just edited the accessory's name so that accessoryToEdit no longer holds the same name as in settings["accessory${accessoryId}Name"]. Need to update that.
                        accessoryId = state.editedAccessoryId
                        app.updateSetting("accessoryToEdit",[type:"enum",value:settings["accessory${accessoryId}Name"]]) 
                        state.editedAccessoryName = settings["accessory${accessoryId}Name"]
                    }
                    input name: "accessory${accessoryId}Name", type: "text", title: "Unique Name", submitOnChange: false, required: true
                    input name: "accessory${accessoryId}Category", type: "text", title: "Category", description: "Existing categories: ${getAccessoryCategoryEnumList()}", submitOnChange: false, required: true
                    input name: "accessory${accessoryId}Path", type: "text", title: "Accessory Image Path", submitOnChange: true, required: true
                    input name: "accessory${accessoryId}NumStarsRequired", type: "number", title: "Number of Stars to Earn Accessory", submitOnChange: false, required: true
                
                     if (state.people) {
                        state.people.each { id, person ->

                              paragraph '<div width="100%" align="center"><font style="font-size:20px;font-weight: bold">' + formatAccessoryPreview(accessoryId, id) + settings["person${id}Name"] + '</font></div>', width:12

                              input name: "accessory${accessoryId}TopPerson${id}", type: "text", title: "Top % Alignment", submitOnChange: true, required: false, width: 3
                              input name: "accessory${accessoryId}BottomPerson${id}", type: "text", title: "Bottom % Alignment", submitOnChange: true, required: false, width: 3
                              input name: "accessory${accessoryId}LeftPerson${id}", type: "text", title: "Left % Alignment", submitOnChange: true, required: false, width: 3
                              input name: "accessory${accessoryId}RightPerson${id}", type: "text", title: "Right % Alignment", submitOnChange: true, required: false, width: 3
                              input name: "accessory${accessoryId}ScalePerson${id}", type: "text", title: "Scale %", submitOnChange: true, required: false, width: 3
                        
                              input name: "isAccessory${accessoryId}EarnedByPerson${id}", type: "bool", title: "Is accessory already earned by ${settings["person${id}Name"]}?", submitOnChange: false, required: false, width: 3
                              input name: "isAccessory${accessoryId}ActiveForPerson${id}", type: "bool", title: "Is accessory active for ${settings["person${id}Name"]}?", submitOnChange: false, required: false, width: 3
                            input name: "isNextAccessory${accessoryId}ForPerson${id}", type: "bool", title: "Set as next accessory to be earned for ${settings["person${id}Name"]}?", submitOnChange: false, required: false, width: 3
                          }
                      }   
                    paragraph "<br>"
                    input name: "submitEditAccessory", type: "button", title: "Done", width: 3
                }
                
            }            
            else {     

                input name: "addAccessory", type: "button", title: "Add Accessory", width: 3                
                if (state.accessories && state.accessories.size() > 0) input name: "editAccessory", type: "button", title: "Edit Accessory", width: 3
                app.clearSetting("accessoryToEdit")
                if (state.accessories && state.accessories.size() > 0) input name: "deleteAccessory", type: "button", title: "Delete Accessory", width: 3
                app.clearSetting("accessoryToDelete")
            } 
            footer() 
        }
    }
}

def getAccessoriesEnumList() {
    def list = []
    if (state.accessories) {
        for (id in state.accessories) {
            list.add(settings["accessory${id}Name"])
        }
    }
    return list
}

def getAccessoryCategoryEnumList() {
    def list = []
    if (state.accessories) {
        for (id in state.accessories) {
            if (!list.contains(settings["accessory${id}Category"])) list.add(settings["accessory${id}Category"])
        }
    }
    return list
}

String getIdOfAccessoryWithName(String name) {
    def accessoryId = null
    for (id in state.accessories) {
        if (settings["accessory${id}Name"] == name) accessoryId = id
    }
    if (accessoryId == null) log.warn "No Accessory Found With the Name: ${name}"
    return accessoryId
}

def addAccessory(id) {
    if (!state.accessories) state.accessories = []
    state.accessories.add(id)
    def accessory = getAccessory(id)
    if (state.people) {
          state.people.each { personId, person ->
              def earnedAccessories = getEarnedAccessoryIDs(personId)
              if (accessory.people[personId]?.isEarned == true && !earnedAccessories.contains(id)) state.people[personId].accessories?.earned.add(id)
              def activeAccessories = getActiveAccessoryIDs(personId)
              if (accessory.people[personId]?.isActive == true && !activeAccessories.contains(id)) state.people[personId].accessories?.active.add(id)
              if (state.accessories && settings["isNextAccessory${id}ForPerson${personId}"] == true) {
                  // if set this accessory as the next accessory, remove any other accessories as next
                  for (accessoryId in state.accessories) {
                      if (accessoryId != id && settings["isNextAccessory${accessoryId}ForPerson${personId}"] != null) app.updateSetting("isNextAccessory${accessoryId}ForPerson${personId}",[value:"false",type:"bool"])
                  }
              }
          }
    }
}

def editAccessory(id) {
    def accessory = getAccessory(id)
    if (state.people) {
          state.people.each { personId, person ->
              def earnedAccessories = getEarnedAccessoryIDs(personId)
              if (accessory.people[personId]?.isEarned == true && !earnedAccessories.contains(id)) state.people[personId].accessories?.earned.add(id)
              else if (accessory.people[personId]?.isEarned == false && earnedAccessories.contains(id)) state.people[personId].accessories?.earned.removeElement(id)
              def activeAccessories = getActiveAccessoryIDs(personId)
              if (accessory.people[personId]?.isActive == true && !activeAccessories.contains(id)) state.people[personId].accessories?.active.add(id)
              else if (accessory.people[personId]?.isActive == false && activeAccessories.contains(id)) state.people[personId].accessories?.active.removeElement(id)
              if (state.accessories && settings["isNextAccessory${id}ForPerson${personId}"] == true) {
                  // if set this accessory as the next accessory, remove any other accessories as next
                  for (accessoryId in state.accessories) {
                      if (accessoryId != id && settings["isNextAccessory${accessoryId}ForPerson${personId}"] != null) app.updateSetting("isNextAccessory${accessoryId}ForPerson${personId}",[value:"false",type:"bool"])
                  }
              }
          }
    }    
}

def getAccessory(accessoryId) {
    if (accessoryId == null) return null
     def peopleMap = [:]
     if (state.people) {
          state.people.each { id, person ->
              def personMap = [top: settings["accessory${accessoryId}TopPerson${id}"], bottom: settings["accessory${accessoryId}BottomPerson${id}"], left: settings["accessory${accessoryId}LeftPerson${id}"], right: settings["accessory${accessoryId}RightPerson${id}"], scale: settings["accessory${accessoryId}ScalePerson${id}"], isEarned: settings["isAccessory${accessoryId}EarnedByPerson${id}"], isActive: settings["isAccessory${accessoryId}ActiveForPerson${id}"], isNext: settings["isNextAccessory${accessoryId}ForPerson${id}"]]
              peopleMap[id] = personMap
          }
     }
    return [category: settings["accessory${accessoryId}Category"], path: settings["accessory${accessoryId}Path"], numStarsRequired: settings["accessory${accessoryId}NumStarsRequired"], people: peopleMap]     
}


def deleteAccessory(String accessoryToDelete) {
    def idToDelete = getIdOfAccessoryWithName(accessoryToDelete)
    if (idToDelete && state.accessories) {       
        state.accessories.removeElement(idToDelete)
        clearAccessorySettings(idToDelete)    
        if (state.people) {
            state.people.each { id, person ->
                state.people[id].accessories?.earned?.removeElement(idToDelete)
                state.people[id].accessories?.active?.removeElement(idToDelete)
            }
        }
    }
}

def getNextAccessoryIdForPerson(personId, excludeEarned = true) {
    def nextAccessoryId = null
    if (state.accessories) {
        for (accessoryId in state.accessories) {
            def accessory = getAccessory(accessoryId)
            if (accessory.people[personId] && accessory.people[personId].isNext == true) {
                nextAccessoryId = accessoryId
            }
        }
    }
    if (excludeEarned == true && state.people[personId].accessories?.earned.contains(nextAccessoryId)) nextAccessoryId = null // already earned the accessory designated as the next accessory - need to set up new accessory to be next
    return nextAccessoryId    
}

def getLastEarnedAccessoryIdForPerson(personId) {
    return state.people[personId].accessories?.lastEarned  
}

def getNextAccessoryForPerson(personId, excludeEarned = true) {
    def nextAccessoryId = getNextAccessoryIdForPerson(personId, excludeEarned)
    return getAccessory(nextAccessoryId)
}

def getNumStarsNeededForNextAccessory(personId, excludeEarned = true) {
    def nextAccessory = getNextAccessoryForPerson(personId, excludeEarned)
    if (nextAccessory == null) {
        log.warn "No next accessory for person ${personId} set up yet."
        return null
    }
    if (nextAccessory != null) return nextAccessory.numStarsRequired ? nextAccessory.numStarsRequired : 0
}

def initializeStarsAccessories() {
    if (state.people) {
        state.people.each { personId, person ->
            if (state.people && state.people[personId]) {
                if (!state.people[personId].stars) {
                    def starsMap = [earned: null, completedCount: null, earnedToday: null]
                    state.people[personId].stars = starsMap
                }
               if (state.people[personId].stars.earned == null) state.people[personId].stars.earned = 0
                if (state.people[personId].stars.earnedToday == null) state.people[personId].stars.earnedToday = false
                if (!state.people[personId].accessories) {
                    def accessoryMap = [earned: [], active:[], lastEarned: null]
                    state.people[personId].accessories = accessoryMap
               }
                if (!state.people[personId].accessories.lastEarned) state.people[personId].accessories.lastEarned = null
                if (state.people[personId].hats) state.people[personId].hats = null
             }
        }
    }
}

def handleTap(personId) {
    if (state.people && state.people[personId]) {
        if ((getNumStarsNeededForNextAccessory(personId) == null || state.people[personId].stars.earned < getNumStarsNeededForNextAccessory(personId)) && state.people[personId].stars.earnedToday == false) {
            addStar(personId)
        }
        else if (state.people[personId].stars.earnedToday == true) {
            cycleActiveAccessory(personId)  // TO DO: figure out how to handle cycling between different category accessories via taps
        }
    }   
}

def cycleActiveAccessory(personId, category = null) {       
    def earnedAccessories = getEarnedAccessoryIDs(personId, category)
    def activeAccessories = getActiveAccessoryIDs(personId, category)
    logDebug("For personId ${personId}, found earned accessories ${earnedAccessories} and active accessories ${activeAccessories}", "Accessories")
    
    if (activeAccessories.size() == 0 && earnedAccessories.size() > 0) {
        state.people[personId].accessories?.active.add(earnedAccessories[0]) // no active accessory (in the category), so set the first earned accessory (in the category) to active
        logDebug("Setting active accessory for personId ${personId} to first earned accessory", "Accessories")
    }
    else {
        def foundActive = false
        for (def i = 0; i < earnedAccessories.size(); i++) {
            if (activeAccessories.contains(earnedAccessories[i]) && foundActive == false) {
                foundActive = true
                 state.people[personId].accessories?.active.removeElement(earnedAccessories[i])
                logDebug("Removing active accessory ${i} for personId ${personId}", "Accessories")
                if (i < earnedAccessories.size() - 1) {
                    state.people[personId].accessories?.active.add(earnedAccessories[i+1])
                    logDebug("Setting active accessory for personId ${personId} to earned accessory ${i+1}", "Accessories")
                }
                // don't cycle around back to i = 0 so that can "remove" an accessory
            }
        }   
     }
    updateTracker(personId)
}

def replaceActiveAccessory(personId, newActiveAccessoryId) {
    def newAccessory = getAccessory(newActiveAccessoryId) 
    deactivateAccessories(personId, newAccessory.category)
    state.people[personId].accessories?.active.add(newActiveAccessoryId)  
}

def deactivateAccessories(personId, category = null) {
    if (category == null) state.people[personId].accessories?.active = []
    else {
        for (def i = 0; i < state.people[personId].accessories?.active?.size(); i++) {
            def accessory = getAccessory(state.people[personId].accessories?.active[i]) 
            if (accessory.category == category) state.people[personId].accessories?.active.removeElement(state.people[personId].accessories?.active[i])
        }
    }    
}

def awardNextAccessory(personId) {
     def nextAccessoryId = getNextAccessoryIdForPerson(personId)  
     if (nextAccessoryId != null) {
         state.people[personId].accessories?.earned.add(nextAccessoryId)
         state.people[personId].accessories?.lastEarned = nextAccessoryId
         app.updateSetting("isNextAccessory${nextAccessoryId}ForPerson${personId}",[value:"false",type:"bool"])
         replaceActiveAccessory(personId, nextAccessoryId)
     }
}

def takeBackLastEarnedAccessory(personId) {
     def lastEarnedAccessoryId = getLastEarnedAccessoryIdForPerson(personId) 
     if (lastEarnedAccessoryId != null) {
         state.people[personId].accessories?.earned.removeElement(lastEarnedAccessoryId)
         state.people[personId].accessories?.lastEarned = null
         app.updateSetting("isNextAccessory${lastEarnedAccessoryId}ForPerson${personId}",[value:"true",type:"bool"])
         if (state.people[personId].accessories?.active.contains(lastEarnedAccessoryId)) {
             state.people[personId].accessories?.active.removeElement(lastEarnedAccessoryId)
             def lastEarnedAccessory = getAccessory(lastEarnedAccessoryId) 
             def accessoriesInCategory = getEarnedAccessoryIDs(personId, lastEarnedAccessory.category)
             if (accessoriesInCategory.size() > 0) {
                 state.people[personId].accessories?.active.add(accessoriesInCategory[0]) // replace deactivated accessory with the first accessory in the category
             }
         }
     }    
}

def getEarnedAccessoryIDs(personId, category = null) {
    def accessoryIDs = []  
    if (category == null) accessoryIDs = state.people[personId].accessories?.earned
    else {
        for (def i = 0; i < state.people[personId].accessories?.earned?.size(); i++) {
            def accessory = getAccessory(state.people[personId].accessories?.earned[i]) 
            if (accessory.category == category) accessoryIDs.add(state.people[personId].accessories?.earned[i])
        }
    }
    return accessoryIDs
}

def getActiveAccessoryIDs(personId, category = null) {
    def accessoryIDs = []  
    if (category == null) accessoryIDs = state.people[personId].accessories?.active
    else {
        for (def i = 0; i < state.people[personId].accessories?.active?.size(); i++) {
            def accessory = getAccessory(state.people[personId].accessories?.active[i]) 
            if (accessory.category == category) accessoryIDs.add(state.people[personId].accessories?.active[i])
        }
    }
    return accessoryIDs
}

def addStar(personId) {
    state.people[personId].stars.earned++
    state.people[personId].stars.earnedToday = true
    if (state.people[personId].stars.earned == getNumStarsNeededForNextAccessory(personId)) {
        awardNextAccessory(personId)
        state.people[personId].stars.earned = 0 // reset stars earned
    }
    updateTracker(personId)
}

def subtractStar(personId) {
    if (state.people[personId].stars.earned == 0) {
        // undesired star addition resulted in awarding of the next accessory. Undo that.
        takeBackLastEarnedAccessory(personId)
        state.people[personId].stars.earned = getNumStarsNeededForNextAccessory(personId, false) - 1
    }
    else state.people[personId].stars.earned--
    state.people[personId].stars.earnedToday = false
    updateTracker(personId)
}

void resetAddEditState() {
    state.addingPerson = false
    state.editingPerson = false
    state.deletingPerson = false
    state.addingAccessory = false
    state.editingAccessory = false
    state.deletingAccessory = false
    state.addingVehicle = false
    state.editingVehicle = false
    state.deletingVehicle = false
    state.addingPlace = false
    state.editingPlace = false
    state.deletingPlace = false
    state.addingTrip = false
    state.editingTrip = false
    state.deletingTrip = false
}

void appButtonHandler(btn) {
   switch (btn) {
      case "addPerson":
         state.addingPerson = true
         if (!state.lastPersonID) state.lastPersonID = 0
         state.lastPersonID ++
         break
      case "submitNewPerson":
         addPerson(state.lastPersonID.toString()) 
         state.addingPerson = false
         break
      case "cancelAddPerson":
         state.addingPerson = false
         clearPersonSettings(state.lastPersonID.toString())
         state.lastPersonID --
         break
      case "editPerson":
         state.editingPerson = true
         state.editPersonSubmitted = false
         break
      case "submitEditPerson":
         state.editingPerson = false
         state.editPersonSubmitted = true
         state.editedPersonId = null
         break
      case "cancelEditPerson":
         state.editingPerson = false
         break
      case "deletePerson":
         state.deletingPerson = true
         break
      case "submitDeletePerson":
         if (personToDelete) deletePerson(personToDelete)
         state.deletingPerson = false
         break
      case "cancelDeletePerson":
         state.deletingPerson = false
         break
      case "addAccessory":
         state.addingAccessory = true
         if (!state.lastAccessoryID) state.lastAccessoryID = 0
         state.lastAccessoryID++
         break
      case "submitNewAccessory":
         addAccessory(state.lastAccessoryID.toString())
         state.addingAccessory = false
         break
      case "cancelAddAccessory":
         state.addingAccessory = false
         clearAccessorySettings(state.lastAccessoryID.toString())
         state.lastAccessoryID--
         break
      case "editAccessory":
         state.editingAccessory = true
         state.editAccessorySubmitted = false
         break
      case "submitEditAccessory":
         editAccessory(state.editedAccessoryId.toString())
         state.editingAccessory = false
         state.editAccessorySubmitted = true
         state.editedAccessoryId = null
         break
      case "cancelEditAccessory":
         state.editingAccessory = false
         break
      case "deleteAccessory":
         state.deletingAccessory = true
         break
      case "submitDeleteAccessory":
         if (accessoryToDelete) deleteAccessory(accessoryToDelete)
         state.deletingAccessory = false
         break
      case "cancelDeleteAccessory":
         state.deletingAccessory = false
         break       
      case "addVehicle":
         state.addingVehicle = true
         if (!state.lastVehicleID) state.lastVehicleID = 0
         state.lastVehicleID ++
         break
      case "submitNewVehicle":
         addVehicle(state.lastVehicleID.toString())
         state.addingVehicle = false
         break
      case "cancelAddVehicle":
         state.addingVehicle = false
         clearVehicleSettings(state.lastVehicleID.toString())
         state.lastVehicleID --
         break
      case "editVehicle":
         state.editingVehicle = true
         break
      case "submitEditVehicle":
         state.editingVehicle = false
         state.editedVehicleId = null
         break
      case "cancelEditVehicle":
         state.editingVehicle = false
         break
      case "deleteVehicle":
         state.deletingVehicle = true
         break
      case "submitDeleteVehicle":
         if (vehicleToDelete) deleteVehicle(vehicleToDelete)
         state.deletingVehicle = false
         break
      case "cancelDeleteVehicle":
         state.deletingVehicle = false
         break
      case "addPlace":
         state.addingPlace = true
         if (!state.lastPlaceID) state.lastPlaceID = 0
         state.lastPlaceID ++
         break
      case "submitNewPlace":
         addPlace(state.lastPlaceID.toString())
         state.addingPlace = false
         break
      case "cancelAddPlace":
         state.addingPlace = false
         clearPlaceSettings(lastPlaceID)
         state.lastPlaceID --
         break
      case "editPlace":
         state.editingPlace = true
         break
      case "submitEditPlace":
         state.editingPlace = false
         state.editedPlaceId = null
         break
      case "cancelEditPlace":
         state.editingPlace = false
         break
      case "deletePlace":
         state.deletingPlace = true
         break
      case "submitDeletePlace":
         if (placeToDelete) deletePlace(placeToDelete)
         state.deletingPlace = false
         break
      case "cancelDeletePlace":
         state.deletingPlace = false
         break
      case "addTrip":
         state.addingTrip = true
         if (!state.lastTripID) state.lastTripID = 0
         state.lastTripID ++
         break
      case "submitNewTrip":
         addTrip(state.lastTripID.toString()) 
         state.addingTrip = false
         break
      case "cancelAddTrip":
         state.addingTrip = false
         clearTripSettings(state.lastTripID.toString())
         state.lastTripID --
         break
      case "editTrip":
         state.editingTrip = true
         break
      case "submitEditTrip":
         state.editingTrip = false
         state.editedTripId = null
         break
      case "cancelEditTrip":
         state.editingTrip = false
         break
      case "deleteTrip":
         state.deletingTrip = true
         break
      case "submitDeleteTrip":
         if (tripToDelete) deleteTrip(tripToDelete)
         state.deletingTrip = false
         break
      case "cancelDeleteTrip":
         state.deletingTrip = false
      default:
         log.warn "$btn press not handled"
         break
   }    
}

def VehiclesPage() {
    dynamicPage(name: "VehiclesPage") {
        section {
            header()
            paragraph getInterface("header", " Manage Vehicles")
            if (state.vehicles) {  
                paragraph 
                for (id in state.vehicles) {
                    paragraph '<table width="100%" border="0" style="float:right;"><tr><td align="center">' + formatImagePreview(getVehicleIconById(id)) + '<font style="font-size:20px;font-weight: bold">' + settings["vehicle${id}Name"] + '</font></td></tr></table>', width: 3
                }
                paragraph getInterface("line", "")
            }
            
            if (state.addingVehicle) {
                input name: "vehicle${state.lastVehicleID}Name", type: "text", title: "Unique Name", submitOnChange: false, required: true

                input name: "vehicle${state.lastVehicleID}Icon", type: "enum", options: getIconsEnum("Vehicles"), title: "Vehicle Icon", submitOnChange: true, multiple: false, required: true, width: 5
                def vehicleIcon = settings["vehicle${state.lastVehicleID}Icon"]
                if (vehicleIcon != null) {
                    vehiclePath = vehicleIcon == "Custom" ? settings["vehicle${state.lastVehicleID}IconCustom"] : getPathOfStandardIcon(vehicleIcon, "Vehicles")
                    paragraph formatImagePreview(vehiclePath), width: 2
                }
                if (settings["vehicle${state.lastVehicleID}Icon"] == "Custom") {
                    input name: "vehicle${state.lastVehicleID}IconCustom", type: "text", title: "URL to Custom Icon", submitOnChange: true, required: true
                }
                paragraph getInterface("link", "Get Icons", "https://www.flaticon.com/")
                
                
                if (state.people) {
                    state.people.each { personId, person ->
                        def personDisplay = "<table border=0 margin=0><tr>"
                        personDisplay+= "<td align=center><img border=0 style='max-width:100px' src='${getPersonAvatar(personId)}'>"
                        personDisplay += "<br><font style='font-size:20px;font-weight: bold'>${settings["person${personId}Name"]}</font></td>"
                        personDisplay += "</tr></table>"
                      //  paragraph personDisplay
                        input name: "vehicle${state.lastVehicleID}Person${personId}Sensor", type: "capability.presenceSensor", title: "${settings["person${personId}Name"]} Presence Sensor", description: "Presence Sensor for ${settings["person${personId}Name"]}'s presence in vehicle", submitOnChange: false, multiple: false, required: false, width: 4
                    }                        
                }
                paragraph "<br>"
                input name: "submitNewVehicle", type: "button", title: "Submit", width: 3
                input name: "cancelAddVehicle", type: "button", title: "Cancel", width: 3
            }
            else if (state.deletingVehicle) {
                input name: "vehicleToDelete", type: "enum", title: "Delete Vehicle:", options: getVehiclesEnumList(), multiple: false, submitOnChange: true
                if (vehicleToDelete) input name: "submitDeleteVehicle", type: "button", title: "Confirm Delete", width: 3
                input name: "cancelDeleteVehicle", type: "button", title: "Cancel", width: 3
            }
            else if (state.editingVehicle) {
                input name: "vehicleToEdit", type: "enum", title: "Edit Vehicle:", options: getVehiclesEnumList(), multiple: false, submitOnChange: true
                if (vehicleToEdit) {
                    def vehicleId = getIdOfVehicleWithName(vehicleToEdit)
                    if (vehicleId != null) {
                        state.editedVehicleId = vehicleId    // save the ID and name of the vehicle being edited in state
                        state.editedVehicleName = settings["vehicle${vehicleId}Name"]
                    }
                    else {
                        // just edited the vehicle's name so that vehicleToEdit no longer holds the same name as in settings["vehicle${vehicleId}Name"]. Need to update that.
                        vehicleId = state.editedVehicleId
                        app.updateSetting("vehicleToEdit",[type:"enum",value:settings["vehicle${vehicleId}Name"]]) 
                        updateAfterVehicleNameEdit()
                        state.editedVehicleName = settings["vehicle${vehicleId}Name"]
                    }
                    
                    input name: "vehicle${vehicleId}Name", type: "text", title: "Unique Name", submitOnChange: true, required: true
                    
                    input name: "vehicle${vehicleId}Icon", type: "enum", options: getIconsEnum("Vehicles"), title: "Vehicle Icon", submitOnChange: true, multiple: false, required: true, width: 5
                    def vehicleIcon = settings["vehicle${vehicleId}Icon"]
                    if (vehicleIcon != null) {
                        vehiclePath = vehicleIcon == "Custom" ? settings["vehicle${vehicleId}IconCustom"] : getPathOfStandardIcon(vehicleIcon, "Vehicles")
                        paragraph formatImagePreview(vehiclePath), width: 2
                    }
                    if (settings["vehicle${vehicleId}Icon"] == "Custom") {
                        input name: "vehicle${vehicleId}IconCustom", type: "text", title: "URL to Custom Icon", submitOnChange: true, required: true
                    }
                    paragraph getInterface("link", "Get Icons", "https://www.flaticon.com/")
                    
                    if (state.people) {
                        state.people.each { personId, person ->
                            def personDisplay = "<table border=0 margin=0><tr>"
                            personDisplay+= "<td align=center><img border=0 style='max-width:100px' src='${getPersonAvatar(personId)}'>"
                            personDisplay += "<br><font style='font-size:20px;font-weight: bold'>${settings["person${personId}Name"]}</font></td>"
                            personDisplay += "</tr></table>"
                          //  paragraph personDisplay
                            input name: "vehicle${vehicleId}Person${personId}Sensor", type: "capability.presenceSensor", title: "${settings["person${personId}Name"]} Presence Sensor", description: "Presence Sensor for ${settings["person${personId}Name"]}'s presence in vehicle", submitOnChange: false, multiple: false, required: false, width: 4
                        }    
                        
                    }
                    paragraph "<br>"
                    input name: "submitEditVehicle", type: "button", title: "Done", width: 3
                }
                
               // input name: "cancelEditVehicle", type: "button", title: "Cancel", width: 3
            }            
            else {               
                input name: "addVehicle", type: "button", title: "Add Vehicle", width: 3                
                if (state.vehicles) input name: "editVehicle", type: "button", title: "Edit Vehicle", width: 3
                app.clearSetting("vehicleToEdit")
                if (state.vehicles) input name: "deleteVehicle", type: "button", title: "Delete Vehicle", width: 3
                app.clearSetting("vehicleToDelete")
            } 
            footer()   
        }
    }
}


def updateAfterVehicleNameEdit() {
    state.trips.each { tripId, trip ->
        def tripVehicles = []
        for (tripVehicleName in settings["trip${tripId}Vehicles"]) {
            if (tripVehicleName == state.editedVehicleName) tripVehicles.add(settings["vehicle${state.editedVehicleId}Name"])
            else tripVehicles.add(tripVehicleName)
        }
        app.updateSetting("trip${tripId}Vehicles",[type:"enum",value:tripVehicles]) 
    }
}

def clearVehicleSettings(String vehicleId) {
    def vehicleNameToDelete = getNameOfVehicleWithId(vehicleId)
    
    app.removeSetting("vehicle${vehicleId}Name")
    app.removeSetting("vehicle${vehicleId}Icon")
    app.removeSetting("vehicle${vehicleId}IconCustom")
    if (state.people) {
        state.people.each { personId, person ->
            app.removeSetting("vehicle${vehicleId}Person${personId}Sensor")
        }
    }
    
    if (state.trips) {
        state.trips.each { tripId, trip ->
            def newTripVehicles = []
            for (tripVehicle in settings["trip${tripId}Vehicles"]) {
                if (tripVehicle != vehicleNameToDelete) newTripVehicles.add(tripVehicle)
            }
            app.updateSetting("trip${tripId}Vehicles",[type:"enum",value:newTripVehicles])
        }
    }
}

def getVehicles() {
     return state.vehicles
}

def getVehiclesEnumList() {
    def list = []
    if (state.vehicles) {
        for (id in state.vehicles) {
            list.add(settings["vehicle${id}Name"])
        }
    }
    return list
}

def getVehiclesDescription() {
    def vehicles = getVehiclesEnumList()
    def description = ""
    for (i=0; i < vehicles.size(); i++) {
         description += vehicles[i] 
        if (i != vehicles.size()-1) description += ", "
    }
    return description
}

String getVehicleIcon(String name) {
    def id = getIdOfVehicleWithName(name)
    return getVehicleIconById(id)
}

String getVehicleIconById(String vehicleId) {
    def vehicleIcon = null
    if (settings["vehicle${vehicleId}Icon"] == "Custom") vehicleIcon = settings["vehicle${vehicleId}IconCustom"]
    else if (settings["vehicle${vehicleId}Icon"] != null) vehicleIcon = getPathOfStandardIcon(settings["vehicle${vehicleId}Icon"], "Vehicles")
    return vehicleIcon
}

def addVehicle(String id) {
    if (!state.vehicles) state.vehicles = []
    state.vehicles.add(id)
}

String getNameOfVehicleWithId(String vehicleId) {
    return settings["vehicle${vehicleId}Name"]
}

String getIdOfVehicleWithName(String name) {
    def vehicleId = null
    for (id in state.vehicles) {
        if (settings["vehicle${id}Name"] == name) vehicleId = id
    }
    if (vehicleId == null) log.warn "No Vehicle Found With the Name: ${name}"
    return vehicleId
}

def deleteVehicle(String nameToDelete) {
    def idToDelete = getIdOfVehicleWithName(nameToDelete)
    if (idToDelete && state.vehicles) {       
        state.vehicles.removeElement(idToDelete)
        state.images.vehicles[idToDelete] = null
        clearVehicleSettings(idToDelete)
    }
}

def formatImagePreview(String imageUrl) {
    return '<div style="width:80px; height:80px; background:gray; border-radius:50%; border: solid 4px black"><img width="80px" height="80px" style="transform:scale(.7);" src="' + imageUrl + '"></div>'   
}

def PlacesPage() {
    dynamicPage(name: "PlacesPage") {
        section {
            header()
            paragraph getInterface("header", " Manage Places")
            if (state.places) {                
                state.places.each { id, place ->
                    paragraph '<table width=100% border=0 style="float:right;"><tr><td align=center>' + formatImagePreview(getPlaceIconById(id)) + '<font style="font-size:20px;font-weight: bold">' + settings["place${id}Name"] + '</font></td></tr></table>', width: 3
                }
                paragraph getInterface("line", "")
            }
            
            if (state.addingPlace) {
                paragraph getInterface("subHeader", "Add New Place")
                input name: "place${state.lastPlaceID}Name", type: "text", title: "Unique Name", submitOnChange: false, required: true
                                
                input name: "place${state.lastPlaceID}Icon", type: "enum", options: getIconsEnum("Places"), title: "Place Icon", submitOnChange: true, multiple: false, required: true, width: 5
                def placeIcon = settings["place${state.lastPlaceID}Icon"]
                if (placeIcon != null) {
                    def iconPath = placeIcon == "Custom" ? settings["place${state.lastPlaceID}IconCustom"] : getPathOfStandardIcon(settings["place${state.lastPlaceID}Icon"], "Places")
                    paragraph formatImagePreview(iconPath), width: 2
                }
                if (settings["place${state.lastPlaceID}Icon"] == "Custom") {
                    input name: "place${state.lastPlaceID}IconCustom", type: "text", title: "URL to Custom Icon", submitOnChange: true, required: true
                }
                paragraph getInterface("link", "Get Icons", "https://www.flaticon.com/")
                                
                input name: "place${state.lastPlaceID}Address", type: "text", title: "Full Address", submitOnChange: false, required: false, description: "*Address Required for Travel Advisor"
                paragraph getInterface("note", "Specify addresses in accordance with the format used by the national postal service. Additional address elements such as business names and unit, suite or floor numbers should be avoided.")
                if (state.people) {
                        state.people.each { personId, person ->
                            def personDisplay = "<table border=0 margin=0><tr>"
                            personDisplay+= "<td align=center><img border=0 style='max-width:100px' src='${getPersonAvatar(personId)}'>"
                            personDisplay += "<br><font style='font-size:20px;font-weight: bold'>${settings["person${personId}Name"]}</font></td>"
                            personDisplay += "</tr></table>"
                         //  paragraph personDisplay
                            input name: "place${state.lastPlaceID}Person${personId}Sensor", type: "capability.presenceSensor", title: "${settings["person${personId}Name"]} Presence Sensor", description: "Presence Sensor for ${settings["person${personId}Name"]}'s presence at place",  submitOnChange: false, multiple: false, required: false, width: 4
                        }                       
                }
                paragraph "Select garage door(s), contact sensor(s), and/or switch(es) that change when someone is about to depart. When this place is the origin of a trip, the changing of any of these devices during a time window for departure on the trip will trigger a proactive check of travel conditions, to advise you of the best route to take even before actual departure."
                input name: "place${state.lastPlaceID}GarageDoor", type: "capability.garageDoorControl", title: "Garage Door(s)", submitOnChange: false, multiple: true, required: false, width: 4
                input name: "place${state.lastPlaceID}ContactSensor", type: "capability.contactSensor", title: "Contact Sensor(s)", submitOnChange: false, multiple: true, required: false, width: 4
                input name: "place${state.lastPlaceID}Switch", type: "capability.switch", title: "Switch(es)", submitOnChange: false, multiple: true, required: false, width: 4
                paragraph "<div><br></div>"
                paragraph "<br>"
                input name: "submitNewPlace", type: "button", title: "Submit", width: 3
                input name: "cancelAddPlace", type: "button", title: "Cancel", width: 3
            }
            else if (state.deletingPlace) {
                paragraph getInterface("subHeader", "Delete Place")
                input name: "placeToDelete", type: "enum", title: "Delete Place:", options: getPlacesEnumList(), multiple: false, submitOnChange: true
                if (placeToDelete) input name: "submitDeletePlace", type: "button", title: "Confirm Delete", width: 3
                input name: "cancelDeletePlace", type: "button", title: "Cancel", width: 3
            }
            else if (state.editingPlace) {
                paragraph getInterface("subHeader", "Edit Place")
                input name: "placeToEdit", type: "enum", title: "Edit Place:", options: getPlacesEnumList(), multiple: false, submitOnChange: true
                if (placeToEdit) {
                    def placeId = getIdOfPlaceWithName(placeToEdit)
                    if (placeId != null) {
                        state.editedPlaceId = placeId    // save the ID and name of the vehicle being edited in state
                        state.editedPlaceName = settings["place${placeId}Name"]
                    }
                    else {
                        // just edited the place's name so that placeToEdit no longer holds the same name as in settings["place${placeId}Name"]. Need to update that.
                        placeId = state.editedPlaceId
                        app.updateSetting("placeToEdit",[type:"enum",value:settings["place${placeId}Name"]]) 
                        updateAfterPlaceNameEdit()
                        state.editedPlaceName = settings["place${placeId}Name"]
                    }
                    
                    input name: "place${placeId}Name", type: "text", title: "Unique Name", submitOnChange: true, required: true
                    
                    input name: "place${placeId}Icon", type: "enum", options: getIconsEnum("Places"), title: "Place Icon", submitOnChange: true, multiple: false, required: true, width: 5
                    def placeIcon = settings["place${placeId}Icon"]
                    if (placeIcon != null) {
                        def iconPath = placeIcon == "Custom" ? settings["place${placeId}IconCustom"] : getPathOfStandardIcon(settings["place${placeId}Icon"], "Places")
                       paragraph formatImagePreview(iconPath), width: 2
                    }
                    if (settings["place${placeId}Icon"] == "Custom") {
                        input name: "place${placeId}IconCustom", type: "text", title: "URL to Custom Icon", submitOnChange: true, required: true
                    }
                    paragraph getInterface("link", "Get Icons", "https://www.flaticon.com/")
                    
                    input name: "place${placeId}Address", type: "text", title: "Full Address", submitOnChange: true, required: false, description: "*Address Required for Travel Advisor"
                    paragraph getInterface("note", "Specify addresses in accordance with the format used by the national postal service. Additional address elements such as business names and unit, suite or floor numbers should be avoided.")
                    if (state.people) {
                        state.people.each { personId, person ->
                            def personDisplay = "<table border=0 margin=0><tr>"
                            personDisplay+= "<td align=center><img border=0 style='max-width:100px' src='${getPersonAvatar(personId)}'>"
                            personDisplay += "<br><font style='font-size:20px;font-weight: bold'>${settings["person${personId}Name"]}</font></td>"
                            personDisplay += "</tr></table>"
                           // paragraph personDisplay
                            input name: "place${placeID}Person${personId}Sensor", type: "capability.presenceSensor", title: "${settings["person${personId}Name"]} Presence Sensor", description: "Presence Sensor for ${settings["person${personId}Name"]}'s presence at place",  submitOnChange: false, multiple: false, required: false, width: 4
                        }   
                        
                    }
                    paragraph "Select garage door(s), contact sensor(s), and/or switch(es) that change when someone is arriving or departing."
                    input name: "place${placeId}GarageDoor", type: "capability.garageDoorControl", title: "Garage Door(s)", submitOnChange: true, multiple: true, required: false, width: 4
                    input name: "place${placeId}ContactSensor", type: "capability.contactSensor", title: "Contact Sensor(s)", submitOnChange: true, multiple: true, required: false, width: 4
                    input name: "place${placeId}Switch", type: "capability.switch", title: "Switch(es)", submitOnChange: true, multiple: true, required: false, width: 4
                    paragraph "<br>"
                    input name: "submitEditPlace", type: "button", title: "Done", width: 3
                }
                
              //  input name: "cancelEditPlace", type: "button", title: "Cancel", width: 3
            }            
            else {               
                input name: "addPlace", type: "button", title: "Add Place", width: 3                
                if (state.places) input name: "editPlace", type: "button", title: "Edit Place", width: 3
                app.clearSetting("placeToEdit")
                if (state.places) input name: "deletePlace", type: "button", title: "Delete Place", width: 3
                app.clearSetting("placeToDelete")
            } 
            footer()
        }
    }
}

def clearPlaceSettings(String placeId) {
    def placeName = getNameOfPlaceWithId(placeId)
    
    app.removeSetting("place${placeId}Name")
    app.removeSetting("place${placeId}Icon")
    app.removeSetting("place${placeId}IconCustom")
    app.removeSetting("place${placeId}Address")
    
    if (state.people) {
       state.people.each { personId, person ->
            app.removeSetting("place${placeId}Person${personId}Sensor")                
        }
    }

    app.removeSetting("place${placeId}GarageDoor")
    app.removeSetting("place${placeId}ContactSensor")
    app.removeSetting("place${placeId}Switch")
    
    if (state.trips) {
        state.trips.each { tripId, trip ->
            if (settings["trip${tripId}Origin"] == placeName) app.clearSetting("trip${tripId}Origin")
            if (settings["trip${tripId}Destination"] == placeName) app.clearSetting("trip${tripId}Destination")
        }
    }
}

def updateAfterPlaceNameEdit() {
    state.trips.each { tripId, trip ->
        if (settings["trip${tripId}Origin"] == state.editedPlaceName) {
            app.updateSetting("trip${tripId}Origin",[type:"enum",value:settings["place${state.editedPlaceId}Name"]]) 
        }
        if (settings["trip${tripId}Destination"] == state.editedPlaceName) {
            app.updateSetting("trip${tripId}Destination",[type:"enum",value:settings["place${state.editedPlaceId}Name"]]) 
        }
    }
}

def getPlaces() {
     return state.places
}

def getPlacesEnumList() {
    def list = []
    if (state.places) {
        state.places.each { id, place ->
            if (settings["place${id}Name"]) list.add(settings["place${id}Name"])
        }
    }
    return list
}


String getPlacesDescription() {
    def places = getPlacesEnumList()
    def description = ""
    for (i=0; i < places.size(); i++) {
         description += places[i] 
        if (i != places.size()-1) description += ", "
    }
    return description
}

String getPlaceAddress(String name) {
    def id = getIdOfPlaceWithName(name)
    return settings["place${id}Address"]
}


String getPlaceAddressById(String id) {
    return settings["place${id}Address"]
}

String getPlaceIcon(String name) {
    def id = getIdOfPlaceWithName(name)
    return getPlaceIconById(id)
}

String getPlaceIconById(String placeId) {
    def placeIcon = null
    if (settings["place${placeId}Icon"] == "Custom") placeIcon = settings["place${placeId}IconCustom"]
    else if (settings["place${placeId}Icon"] != null) placeIcon = getPathOfStandardIcon(settings["place${placeId}Icon"], "Places")
    return placeIcon
}

def addPlace(String id) {
    if (!state.places) state.places = [:]
    def placeMap = [latitude: null, longitude: null]
    state.places[id] = placeMap
}

String getIdOfPlaceWithName(String name) {
    def id = null
    state.places.each { placeId, place ->
        if (settings["place${placeId}Name"] == name) id = placeId
    }
    return id
}

String getNameOfPlaceWithId(String id) {
    return settings["place${id}Name"]
}


String getIdOfPlaceWithAddress(String address) {
    def id = null
    state.places.each { placeId, place ->
        if (settings["place${placeId}Address"] == address) id = placeId
    }
    return id
}

def deletePlace(String nameToDelete) {
    def idToDelete = getIdOfPlaceWithName(nameToDelete)  
    logDebug("In delete places with idToDelete = ${idToDelete} and state.places = ${state.places}", "Places")
    if (idToDelete && state.places) {    
        logDebug("Deleting place id ${idToDelete}", "Places")
        state.places.remove(idToDelete)
        state.images.places[idToDelete] = null
        clearPlaceSettings(idToDelete)
    }
}



def RestrictionsPage() {
    dynamicPage(name: "RestrictionsPage") {
        section {
            header()
            paragraph getInterface("header", " Manage Restrictions")
            paragraph "Do not check travel conditions when..."
            input name: "restrictedModes",type: "mode", title: "The Location Mode is", multiple: true, required: false, width: 6
            input name: "restrictedSwitch", type: "capability.switch", title: "Any of these switches are ON", submitOnChange: false, multiple: true, required: false, width: 4
            paragraph getInterface("note", "Example Use Case: Install <a href='https://community.hubitat.com/t/release-holiday-switcher/26136'>Holiday Switcher app</a> to configure a virtual switch to turn ON on desired holidays.") 
        }
    }
}

String getRestrictedModesDescription() {
    def description = ""
    if (restrictedModes) {
         for (i=0; i < restrictedModes.size(); i++) {
             description += restrictedModes[i] 
            if (i != restrictedModes.size()-1) description += ", "
        }
    }
    return description
}

Boolean isRestricted() {
    Boolean isRestricted = false     
    
    if (restrictedModes) {
        if(restrictedModes.contains(location.mode)) {
            logDebug("Mode Check Failed.")
            isRestricted = true
        }
    }
    
    if (restrictedSwitch) {
        restrictedSwitch.each { theSwitch ->
            if (theSwitch.currentValue("switch") == "on") {
                isRestricted = true
                logDebug("Restricted due to switch being on.")
            }
        }
        
    }
    
    return isRestricted  
}

def AdvancedPage() {
    dynamicPage(name: "AdvancedPage") {
        section {
            header()
            paragraph getInterface("header", " Advanced Settings")
            
            paragraph "${app.name} caches the response from Google Directions and considers the cached response valid for the duration selected here. Increasing the validity time reduces the number of API calls. Decreasing the validity time increases responsiveness to traffic fluctuations."
            input name: "cacheValidityDuration", type: "number", title: "Duration of Directions Cache (Secs)", required: false, defaultValue: cacheValidityDurationDefault
            paragraph "${app.name} also caches the response from Google Directions for showing the route options available in the app. Set for as long as a configuration session may last."
            input name: "optionsCacheValidityDuration", type: "number", title: "Duration of Options Cache (Secs)", required: false, defaultValue: optionsCacheValidityDurationDefault
            input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
            input name: "logTimed", type: "bool", title: "Disable debug logging in 30 minutes?", defaultValue: false
            input name: "logTypes", type: "enum", title: "Debug Logging Types", options: ["All", "Person", "Vehicles", "Places", "Accessories", "Sleep", "Trips"], required: false, defaultValue: "All"

        }
        section {
           footer()   
        }
    }   
}

def TrackerPage() {
    dynamicPage(name: "TrackerPage") {
        section {
            header()
            paragraph getInterface("header", " Tracker Settings")
            input name: "textColor", type: "text", title: "Text color", required: false, defaultValue: textColorDefault
            input name: "circleBackgroundColor", type: "text", title: "Circle background color", required: false, defaultValue: circleBackgroundColorDefault
            input name: "avatarScale", type: "number", title: "Avatar Scale (%)", required: false, defaultValue: avatarScaleDefault
             
            input name: "geofenceRadius", type: "number", title: "Geofence Radius (meters)", required: false, defaultValue: geofenceRadiusDefault
            
            input name: "timeFormat", type: "enum", title: "Time Format", options: ["12 Hour", "24 Hour"], required: false, defaultValue: timeFormatDefault
            input name: "trafficDelayThreshold", type: "number", title: "Consider traffic bad when traffic delays arrival by how many more minutes than usual?", required: false, defaultValue: trafficDelayThresholdDefault
            input name: "isPreferredRouteDisplayed", type: "bool", title: "Display recommended route even if recommend preferred route?", required: false, defaultValue: isPreferredRouteDisplayedDefault
            input name: "fetchInterval", type: "number", title: "Interval (mins) for Checking Travel Conditions", required: false, defaultValue: fetchIntervalDefault
            input name: "tripPreCheckMins", type: "number", title: "Number of Minutes Before Earliest Departure Time to Check Travel Conditions", required: false, defaultValue: tripPreCheckMinsDefault
            input name: "postArrivalDisplayMins", type: "number", title: "Number of Minutes to Display Trip After Arrival", required: false, defaultValue: postArrivalDisplayMinsDefault
            input name: "sleepMetricsDisplayMins", type: "number", title: "Number of Minutes to Display New Sleep Score", required: false, defaultValue: sleepMetricsDisplayMinsDefault
        }
        section {
           footer()   
        }
    }   
}



def installed() {
    initialize()
}


def updated() {
    initialize()
}


def initialize() {
    resetAddEditState()
    unsubscribe()
    unschedule()
    initializeDebugLogging()
    instantiateToken()
    initializePlaces()
    subscribeTriggers()
    scheduleTimeTriggers()
    initializePresence()
    initializeSleep()
    initializeStarsAccessories()
   // updateSleepCompetition()   // uncomment to debug sleep competition
    initializeSVGImages()
    initializeTrackers()
}

def initializeDebugLogging() {
    if (logEnable && logTimed) runIn(1800, disableDebugLogging)
    
}

def disableDebugLogging() {
    logDebug("Disabling Debug Logging")
    app.updateSetting("logEnable",[value:"false",type:"bool"])
    app.updateSetting("logTimed",[value:"false",type:"bool"])
}

def initializePlaces() {
    state.places.each { placeId, place ->
        def geocode = geocode(placeId)
      //  logDebug("geocode response = ${geocode}.")
        if (geocode) {
            state.places[placeId]?.latitude = geocode.results?.geometry?.location?.lat[0]
            state.places[placeId]?.longitude = geocode.results?.geometry?.location?.lng[0]
        }
    }
}

def initializeSleep() {
    schedule("01 00 00 ? * *", resetSleepCompetition)	// check each day if need to reset sleep competition at the beginning of the week or month    
    if (state.people) {
        state.people.each { personId, person ->
            state.people[personId]?.sleepOnlySensor = hasOnlySleepSensor(personId) 
            def sleepDevice = settings["person${personId}SleepSensor"]
            if (sleepDevice) {
                if (!state.people[personId]?.sleep.presence) {
                    state.people[personId]?.sleep.presence = sleepDevice.currentValue("presence")
                    state.people[personId]?.sleep.presenceAtTime = new Date().getTime()
                }
                if (!state.people[personId]?.sleep.score) {
                    def score = (sleepDevice.currentValue("sleepScore") as Integer) + getSleepScoreBias(personId)
                    state.people[personId]?.sleep.score = Math.min(score, 100)
                    state.people[personId]?.sleep.sleepDataAtTime = new Date().getTime()
                }
                if (!state.people[personId]?.sleep.quality) {
                    state.people[personId]?.sleep.quality = sleepDevice.currentValue("sleepQuality")
                    state.people[personId]?.sleep.sleepDataAtTime = new Date().getTime()
                }            
            }
        }
    }
}

def getSleepScoreBias(personId) {
    def bias = 0
    if (settings["person${personId}SleepScoreBias"]) {
        bias = settings["person${personId}SleepScoreBias"] as Integer
        if (settings["person${personId}SleepScoreBiasNeg"] == true) {
            bias = bias * -1
        }
    }
    return bias
}

def initializePresence() {
    if (state.people) {
        state.people.each { personId, person ->
            setPersonPlace(personId) 
            setPersonVehicle(personId)
            
        }
    }
}

Boolean isSVG(String url) {
   String extension = "";
    def isSVG = false
    if (url != null) {
       int i = url.lastIndexOf('.');
       if (i > 0) {
           extension = url.substring(i+1).toLowerCase()
       }

        if (extension == "svg") isSVG = true
    }
    return isSVG
}



def initializeSVGImages() {
    // prefetch SVG text from files
    logDebug("Refetching SVG images.")
    
    state.trackerType = 'svg'
    
    state.images = [:]
    if (state.people) {
        state.images.people = [:]
        state.people.each { personId, person ->
            def imageUrl = getPersonAvatar(personId)
            if (isSVG(imageUrl)) {
                state.images.people[personId] = sanitizeSvg(imageUrl.toURL().text)
            }
            else state.trackerType = 'html'
        }
    }
    
    if (state.places) {
        state.images.places = [:]
        state.places.each { placeId, place ->
            def imageUrl = getPlaceIconById(placeId)
            if (isSVG(imageUrl)) {
                state.images.places[placeId] = sanitizeSvg(imageUrl.toURL().text)
            }
            else state.trackerType = 'html'
        }
    }
    
    if (state.vehicles) {
        state.images.vehicles = [:]
        for (vehicleId in state.vehicles) {
            def imageUrl = getVehicleIconById(vehicleId)
            if (isSVG(imageUrl)) {
                state.images.vehicles[vehicleId] = sanitizeSvg(imageUrl.toURL().text)
            }
            else state.trackerType = 'html'
        }
    }    
    
    state.images.sleep = [:]
    standardSleepIcons.each { name, subPath ->
        state.images.sleep[name] = sanitizeSvg(getPathOfStandardIcon(name,"Sleep").toURL().text)
    }
    
    state.images.unknown = [:]
    standardUnknownIcons.each { name, subPath ->
        state.images.unknown[name] = sanitizeSvg(getPathOfStandardIcon(name,"Unknown").toURL().text)
    }
    
}

String sanitizeSvg(String svg) {
    String cleanSvg = svg
  //  logDebug("unsanitized svg: ${groovy.xml.XmlUtil.escapeXml(svg)}")
    def xmlDecStart = svg.indexOf("<?xml")
    if (xmlDecStart > -1) {
        def xmlDecEnd = svg.indexOf("?>")
        cleanSvg = svg.substring(xmlDecEnd+2)
    }
  //  logDebug("svg without xml declaration: ${groovy.xml.XmlUtil.escapeXml(cleanSvg)}")
    def svgStart = cleanSvg.indexOf("<svg")
  //  logDebug("svgStart is ${svgStart}")
    if (svgStart > -1) {
        def svgEnd = cleanSvg.indexOf('>', svgStart)
      //  logDebug("svgEnd is ${svgEnd}")
        if (svgEnd > -1) {
            String svgTag = cleanSvg.substring(svgStart, svgEnd)  
         //   logDebug("SVG Tag is ${groovy.xml.XmlUtil.escapeXml(svgTag)}")
            svgTag = svgTag.replaceAll('width="[^"]*', 'width="100%')
            svgTag = svgTag.replaceAll('height="[^"]*', 'height="100%')
          //  logDebug("Changed SVG Tag to ${groovy.xml.XmlUtil.escapeXml(svgTag)}")
            cleanSvg = cleanSvg.replaceFirst("(<svg)([^>]*)", svgTag)
         //   logDebug("sanitized svg: ${groovy.xml.XmlUtil.escapeXml(cleanSvg)}")
        }
    }
    
    return cleanSvg
}

def scheduleTimeTriggers() {
    if (state.trips) {
        state.trips.each { tripId, trip ->
            def earliestDeparture = settings["trip${tripId}EarliestDepartureTime"]
            def latestDeparture = settings["trip${tripId}LatestDepartureTime"]
            if (earliestDeparture) {
                def tripPreCheckTime = getPreCheckTime(tripId)
                schedule(tripPreCheckTime, startTripPreCheckHandler, [data: [tripId: tripId], overwrite: false])
            }
            if (earliestDeparture) {
                schedule(earliestDeparture, startDepartureWindowHandler, [data: [tripId: tripId], overwrite: false])
            }
            if (latestDeparture) {
                schedule(latestDeparture, endDepartureWindowHandler, [data: [tripId: tripId], overwrite: false])
            }   
        }
    }
     if (state.people) {
        state.people.each { personId, person ->
            if (settings["person${personId}Stars"] == true) {
                schedule("00 00 09 ? * *", allowAddStar)
                schedule("00 00 00 ? * *", disableAddStar)
                // allows cycling accessories between 12AM - 9AM
            }
        }
     }
}

def allowAddStar() {
    logDebug("Running allowAddStar()")
    if (state.people) {
        state.people.each { personId, person ->
            logDebug("checking if stars set for person id ${personId}", "Accessories")
            if (state.people[personId].stars != null && state.people[personId].stars.earnedToday != null) {
                logDebug("setting earnedToday = false for person id ${personId}", "Accessories")
                state.people[personId].stars.earnedToday = false
            }
        }
    }
}

def disableAddStar() {
    logDebug("Running disableAddStar()")
    if (state.people) {
        state.people.each { personId, person ->
            if (state.people[personId].stars != null && state.people[personId].stars.earnedToday != null) {
                state.people[personId].stars.earnedToday = true
                // set to disable allowing stars
            }
        }
    }
}

def resetStars(data) {
    def personId = data.personId
    if (state.people && state.people[personId]) {
        state.people[personId].stars.earned = 0
    }
}

@Field daysOfWeekMap = ["Sunday":1, "Monday":2, "Tuesday":3, "Wednesday":4, "Thursday":5, "Friday":6, "Saturday":7]

def subscribeTriggers() {
    subscribePeople()
    subscribeVehicles()
    subscribePlaces()
}

def isGoogleAPIConfigured() {
    return api_key ? true : false    
}

def subscribePeople() {
    if (state.people) {
        state.people.each { id, person ->
             if (settings["person${id}Life360"]) { 
                 if (isGoogleAPIConfigured()) {
                     def timeOfPresence = (state.people[id].life360.address != null && state.people[id].life360.address.equals(life360Address) && state.people[id].life360.atTime != null) ? state.people[id].life360.atTime : new Date().getTime() 
                     updateLife360(id, timeOfPresence)
                     subscribe(settings["person${id}Life360"], "latitude", life360CoordinatesHandler)
                     subscribe(settings["person${id}Life360"], "longitude", life360CoordinatesHandler)
                 }
                 else subscribe(settings["person${id}Life360"], "address", life360AddressHandler) 
                state.people[id].life360.isDriving = settings["person${id}Life360"].currentValue("isDriving")
                subscribe(settings["person${id}Life360"], "isDriving", life360DrivingHandler)
            }    
            if (settings["person${id}SleepSensor"]) {
                subscribe(settings["person${id}SleepSensor"], "sleepScore", sleepScoreHandler)
                subscribe(settings["person${id}SleepSensor"], "presence", bedPresenceHandler)
            }
        }
    }
}

def life360CoordinatesHandler(evt) {    
    state.people?.each { personId, person ->
        if (isLife360DeviceForPerson(personId, evt.getDevice())) {
            logDebug("Before update life360 from life360CoorinatesHandler, State of person per place is ${state.people[personId].places}", "Places")
            updateLife360(personId, evt.getDate().getTime())
            logDebug("After update life360, State of person per place is ${state.people[personId].places}", "Places")
            logDebug("Calling setPersonPlace from life360CoordinatesHandler", "Places")
            setPersonPlace(personId)
        }
    }

}

def updateLife360(String personId, timestamp) {
    def address2 = settings["person${personId}Life360"].currentValue("address2")
    state.people[personId]?.life360.address = settings["person${personId}Life360"].currentValue("address1") + (address2 != null ? ", " + address2 : "")
    state.people[personId].life360.placeIdAtAddress = getIdOfPlaceWithAddress(state.people[personId]?.life360.address)
    
    if (isGoogleAPIConfigured()) {
        state.people[personId].life360.latitude = settings["person${personId}Life360"].currentValue("latitude")
        state.people[personId].life360.longitude = settings["person${personId}Life360"].currentValue("longitude")
        logDebug("Life360 Lat/Long for person ${personId} is ${state.people[personId].life360.latitude} lat, ${state.people[personId].life360.longitude} long", "Places")
        if (state.people[personId].life360.latitude && state.people[personId].life360.longitude) {
            def placeIdByCoordinates = getPlaceIdForCoordinates(state.people[personId].life360.latitude, state.people[personId].life360.longitude)
            state.people[personId].life360.placeIdAtCoordinates = placeIdByCoordinates
        }
    }
    
    state.people[personId].life360.placeIdWithName = getIdOfPlaceWithName(settings["person${personId}Life360"].currentValue("address1"))
    
    state.people[personId]?.life360.atTime = timestamp
}

def sleepScoreHandler(evt) {
    
    def eventDevice = evt.getDevice()
    def eventDate = evt.getDate()
    def eventTime = eventDate.getTime()
    def eventValue = evt.value
    logDebug("In sleepScoreHandler with event: ${evt}. Device is ${eventDevice}. Date is ${eventDate}. Time is ${eventTime}. Unbiased event value is ${eventValue}", "Sleep")
    state.people?.each { personId, person ->
        def isSleepDeviceForPerson = isSleepDeviceForPerson(personId, eventDevice)
        logDebug("Sleep Device for person ${personId} is ${isSleepDeviceForPerson}", "Sleep")
        if (isSleepDeviceForPerson) {
            def midnight = new Date().clearTime()
            def midnightUtc = midnight.getTime()
            def newSleepScore = (eventValue as Integer) + getSleepScoreBias(personId)
            newSleepScore = Math.min(newSleepScore, 100)
            def existingSleepScore = state.people[personId]?.sleep.score
            def asOf = state.people[personId]?.sleep.sleepDataAtTime
            if (existingSleepScore && asOf >= midnightUtc) {
                // if already have a sleep score for today, then set the score to the maximum for today
                if (newSleepScore >= existingSleepScore) state.people[personId]?.sleep.score = newSleepScore
                else state.people[personId]?.sleep.score = existingSleepScore
            }
            else state.people[personId]?.sleep.score = newSleepScore
            // set timestamp to latest event time no matter whether latest score is the max score for the day
            state.people[personId]?.sleep.sleepDataAtTime = eventTime
            def sleepQuality = settings["person${personId}SleepSensor"]?.currentValue("sleepQuality")
            logDebug("Sleep data for person ${personId} is: score = ${newSleepScore}. quality = ${sleepQuality}", "Sleep")
            if (sleepQuality) state.people[personId]?.sleep.quality = sleepQuality   // grab sleep quality on assumption that updated at the same time
            if (haveMultipleSleepSensors()) updateSleepCompetition()  
           // logDebug("Updated sleep competition. Now update tracker.", "Sleep")
            updateTracker(personId)
            def secsUntilClearSleep = (getSleepMetricsDisplayMinsSetting()*60) + 10 // add 10 second buffer to make sure clearing right after window passes
            runIn(secsUntilClearSleep, clearSleepDisplay, [data: [personId: personId], overwrite: false])
        }
    }
}

def clearSleepDisplay(data) {
    def personId = data.personId
    updateTracker(personId)
}

def isSleepDeviceForPerson(String personId, device) {
     if (settings["person${personId}SleepSensor"] && settings["person${personId}SleepSensor"].getDeviceNetworkId() == device.getDeviceNetworkId()) return true
    return false
}

def bedPresenceHandler(evt) {
    state.people?.each { personId, person ->
        if (isSleepDeviceForPerson(personId, evt.getDevice())) {
            state.people[personId]?.sleep.presence = evt.value
            state.people[personId]?.sleep.presenceAtTime = evt.getDate().getTime()
            updateTracker(personId)
        }
    }
}

def resetSleepCompetition() {
    
    state.people?.each { personId, person ->
        state.people[personId]?.sleep.winner = false
    }
    
    Calendar cal = Calendar.getInstance()
    cal.setTimeZone(location.timeZone)
    cal.setTime(new Date())
    def dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    def dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
    def lastDayOfMonth = cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH)
    def isWeekStart = dayOfWeek == Calendar.SATURDAY ? true : false
    def isTrophyResetDay = dayOfWeek == Calendar.MONDAY ? true : false
    def isMonthStart = dayOfMonth == 1 ? true : false
    logDebug("Checking sleep competition day. dayOfWeek=${dayOfWeek} with isWeekStart=${isWeekStart}. dayOfMonth=${dayOfMonth} with isMonthStart=${isMonthStart}", "Sleep")
        
    state.hasSleepCompetitionCompletedToday = false
    
    if (isWeekStart) {
        // make sure it's been at least 24 hours since cleared scores last, so that scores aren't cleared by a nap at the start of the week
        if (state.weekSleepWinsLastCleared) {
           if (getSecondsSince(state.weekSleepWinsLastCleared) > 86400) {
               clearWeeklyWinCount() 
           }
        }
        else clearWeeklyWinCount()
    }
    if (isMonthStart) {
        // make sure it's been at least 24 hours since cleared scores last, so that scores aren't cleared by a nap at the start of the month
        if (state.monthSleepWinsLastCleared) {
            if (getSecondsSince(state.monthSleepWinsLastCleared) > 86400) {
                  clearMonthlyWinCount() 
            }
        }
        else clearMonthlyWinCount()
    }    
    if (isTrophyResetDay) { // show trophy through the weekend and then hide
        state.people?.each { personId, person ->
            state.people[personId]?.sleep.weekWinner = false
            state.people[personId]?.sleep.monthWinner = false
        }
    }
}

def updateSleepCompetition() {
    // check if all sleep sensors have been updated today before competing 
    logDebug("Updating Sleep Competition", "Sleep")
    state.sleepCompetitionUpdating = true
    def allScoresUpdated = true
    def winner = [score: 0, personList: []] // list for ties
    
    def midnight = new Date().clearTime()
    def midnightUtc = midnight.getTime()
    state.people?.each { personId, person ->
        if (hasSleepSensor(personId)) {
            def isInBed = state.people[personId]?.sleep.presence == "present" ? true : false
            def asOf = state.people[personId]?.sleep.presenceAtTime
            if (!inBed && asOf < midnightUtc) { // did not sleep in bed last night, so exclude from sleep competition
                    logDebug("Person ${personId} did not sleep in their bed last night. Excluding from sleep competition.", "Sleep")
                    // nothing to do here, just don't set allScoresUpdated to false
            }
            else {
                def sleepDataAtTime = state.people[personId]?.sleep.sleepDataAtTime
                if (sleepDataAtTime) {  
                    logDebug("Person ${personId} sleep data last updated at time ${sleepDataAtTime}. ")
                    def scoreInt = person.sleep.score as Integer
                    if (sleepDataAtTime < midnightUtc) {    // sleep data not updated today
                        logDebug("No sleep data today for person ${personId}. Aborting update to sleep competition.", "Sleep")
                        allScoresUpdated = false
                    }
                    else {
                        if (scoreInt && scoreInt > winner.score) {
                            logDebug("Tentatively setting sleep competition winner to person ${personId}", "Sleep")                       
                            winner = [score: scoreInt, personList: [personId]]
                        }
                        else if (scoreInt && scoreInt == winner.score) {
                            winner.personList.add(personId)
                        }
                    }
                }
                else {
                    allScoresUpdated = false    // no sleep data at all yet
                    logDebug("No sleep data yet for person ${personId}. Aborting update to sleep competition.", "Sleep")
                }    
            }
        }
    }
  //  logDebug("allScoresUpdated value is ${allScoresUpdated}. Winner is person ${winner.personList}")
    if (allScoresUpdated) {
        logDebug("All sleep scores updated for people competiting. Setting competition winner", "Sleep")
        Calendar cal = Calendar.getInstance()
        cal.setTimeZone(location.timeZone)
        cal.setTime(new Date())
        def dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        def dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        def lastDayOfMonth = cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH)
        def isWeekStart = dayOfWeek == Calendar.SATURDAY ? true : false
        def isWeekEnd = dayOfWeek == Calendar.FRIDAY ? true : false
        def isMonthStart = dayOfMonth == 1 ? true : false
        def isMonthEnd = dayOfMonth == lastDayOfMonth ? true : false
      //logDebug("Checking sleep competition day. dayOfWeek=${dayOfWeek} with isWeekStart=${isWeekStart} and isWeekEnd=${isWeekEnd}. dayOfMonth=${dayOfMonth} with isMonthStart=${isMonthStart} and isMonthEnd=${isMonthEnd}", "Sleep")    
        
        if (state.hasSleepCompetitionCompletedToday == true) {
             // sleep competition already updated today, so undo in case results have changed
            state.people?.each { personId, person ->
                if (state.people[personId]?.sleep.winner == true) {
                    state.people[personId]?.sleep.winner = false
                    state.people[personId]?.sleep.weekWinCount = (state.people[personId]?.sleep.weekWinCount == 1) ? null : state.people[personId]?.sleep.weekWinCount - 1
                    state.people[personId]?.sleep.monthWinCount = (state.people[personId]?.sleep.monthWinCount == 1) ? null : state.people[personId]?.sleep.monthWinCount - 1
                }
            }            
        }
        
        // now set winner of sleep competition
        state.people?.each { personId, person ->
            if (winner.personList.contains(personId)) {
                logDebug("Setting person ${personId} as the official winner of the sleep competition", "Sleep")
                state.people[personId]?.sleep.winner = true
                state.people[personId]?.sleep.weekWinCount = (state.people[personId]?.sleep.weekWinCount == null) ? 1 : state.people[personId]?.sleep.weekWinCount + 1
                state.people[personId]?.sleep.monthWinCount = (state.people[personId]?.sleep.monthWinCount == null) ? 1 : state.people[personId]?.sleep.monthWinCount + 1
            }
            else state.people[personId]?.sleep.winner = false
            updateTracker(personId)
        }
        
        state.hasSleepCompetitionCompletedToday = true
        
        if (isWeekEnd) {
            def weekWinner = [winCount: 0, personList: []] // list for ties
            state.people?.each { personId, person ->
                if (state.people[personId]?.sleep.weekWinCount && state.people[personId]?.sleep.weekWinCount > weekWinner.winCount) {
                        weekWinner = [winCount: state.people[personId]?.sleep.weekWinCount, personList: [personId]]
                }
                else if (state.people[personId]?.sleep.weekWinCount && state.people[personId]?.sleep.weekWinCount == weekWinner.winCount) {
                    weekWinner.personList.add(personId)
                }
           }
            
            state.people?.each { personId, person ->
                if (weekWinner.personList.contains(personId)) {
                    state.people[personId]?.sleep.weekWinner = true
                }
                else state.people[personId]?.sleep.weekWinner = false
                updateTracker(personId)
            }
        }
        
        if (isMonthEnd) {
            def monthWinner = [winCount: 0, personList: []] // list for ties
            state.people?.each { personId, person ->
                if (state.people[personId]?.sleep.monthWinCount && state.people[personId]?.sleep.monthWinCount > monthWinner.winCount) {
                        monthWinner = [winCount: state.people[personId]?.sleep.monthWinCount, personList: [personId]]
                }
                else if (state.people[personId]?.sleep.monthWinCount && state.people[personId]?.sleep.monthWinCount == monthWinner.winCount) {
                    monthWinner.personList.add(personId)
                }
           }
            
            state.people?.each { personId, person ->
                if (monthWinner.personList.contains(personId)) {
                    state.people[personId]?.sleep.monthWinner = true
                }
                else state.people[personId]?.sleep.monthWinner = false
                updateTracker(personId)
            }
        }        
    }
}

def clearWeeklyWinCount() {
    state.weekSleepWinsLastCleared = new Date().getTime()
    state.people?.each { personId, person ->
        state.people[personId]?.sleep.weekWinCount = 0
        state.people[personId]?.sleep.weekWinner = false
    }
}

def clearMonthlyWinCount() {
    state.monthSleepWinsLastCleared = new Date().getTime()
    state.people?.each { personId, person ->
        state.people[personId]?.sleep.monthWinCount = 0
        state.people[personId]?.sleep.monthWinner = false
    }
}

def subscribeVehicles() {
    if (state.vehicles && state.people) {  
         for (vehicleId in state.vehicles) {
            state.people.each { personId, person ->
                if (settings["vehicle${vehicleId}Person${personId}Sensor"]) {
                    if (!state.people[personId].vehicles) state.people[personId].vehicles = [:]
                    def vehiclePresence = settings["vehicle${vehicleId}Person${personId}Sensor"].currentValue("presence")
                    def timeOfPresence = (state.people[personId].vehicles[vehicleId] != null && state.people[personId].vehicles[vehicleId].presence != null && state.people[personId].vehicles[vehicleId].presence.equals(vehiclePresence) && state.people[personId].vehicles[vehicleId].atTime != null) ? state.people[personId].vehicles[vehicleId].atTime : new Date().getTime()
                    def vehicleMap = [presence: vehiclePresence, atTime: timeOfPresence]                    
                    state.people[personId].vehicles[vehicleId] = vehicleMap
                    subscribe(settings["vehicle${vehicleId}Person${personId}Sensor"], "presence", vehiclePresenceSensorHandler)
                }
            }
         }
    }
}
             

def subscribePlaces() {
    if (state.places && state.people) {                
           state.places.each { placeId, place ->
               if (settings["place${placeId}GarageDoor"]) subscribe(settings["place${placeId}GarageDoor"], "door", garageDoorHandler)
               if (settings["place${placeId}ContactSensor"]) subscribe(settings["place${placeId}ContactSensor"], "contact", contactSensorHandler)
               if (settings["place${placeId}Switch"]) subscribe(settings["place${placeId}Switch"], "switch", switchHandler)
               
               state.people.each { personId, person ->
                   if (settings["place${placeId}Person${personId}Sensor"]) {
                       if (!state.people[personId].places) state.people[personId].places = [:]
                       def placePresence = settings["place${placeId}Person${personId}Sensor"].currentValue("presence")
                       def timeOfPresence = (state.people[personId].places[placeId] != null && state.people[personId].places[placeId].presence != null && state.people[personId].places[placeId].presence.equals(placePresence) && state.people[personId].places[placeId].atTime != null) ? state.people[personId].places[placeId].atTime : new Date().getTime()
                       def placeMap = [presence: placePresence, atTime: timeOfPresence]                       
                       state.people[personId].places[placeId] = placeMap
                       subscribe(settings["place${placeId}Person${personId}Sensor"], "presence", placePresenceSensorHandler)
                   }
               }
           }
    }    
}

def garageDoorHandler(evt) {
    if (evt.value == "open" || evt.value == "opening") {
        state.places.each { placeId, place ->
            if (isGarageDoorForPlace(placeId, evt.getDevice())) {
                handlePossiblePreDeparture(placeId)
            }
        }        
    }
}

def handlePossiblePreDeparture(String placeId) {
    state.trips.each { tripId, trip ->
         if (isPlaceTripOrigin(placeId, tripId) && areDepartureConditionsMet(tripId) && !hasTripStarted(tripId)) {
             performPreDepartureActionsForTrip(tripId)        
         }
    }    
}

def hasTripStarted(String tripId) {
    def isTripStarted = false
    for (personName in settings["trip${tripId}People"]) {
        def personId = getIdOfPersonWithName(personName)
        if (isPersonOnTrip(personId, tripId)) {
            isTripStarted = true
        }
    }   
    return isTripStarted
}

def isPlaceTripOrigin(String placeId, String tripId) {
    if (settings["place${placeId}Name"] && settings["trip${tripId}Origin"] && settings["place${placeId}Name"] == settings["trip${tripId}Origin"]) return true
    return false
}

                    
def isGarageDoorForPlace(String placeId, device) {
     if (settings["place${placeId}GarageDoor"] && settings["place${placeId}GarageDoor"] == device) return true
    return false
}

def contactSensorHandler(evt) {
    state.places.each { placeId, place ->
        if (isContactSensorForPlace(placeId, evt.getDevice())) {
            handlePossiblePreDeparture(placeId)
        }
    }        
}
                    
def isContactSensorForPlace(String placeId, device) {
     if (settings["place${placeId}ContactSensor"] && settings["place${placeId}ContactSensor"] == device) return true
    return false
}

def switchHandler(evt) {
    state.places.each { placeId, place ->
        if (isSwitchForPlace(placeId, evt.getDevice())) {
            handlePossiblePreDeparture(placeId)
        }
    }        
}
                    
def isSwitchForPlace(String placeId, device) {
     if (settings["place${placeId}Switch"] && settings["place${placeId}Switch"] == device) return true
    return false
}

def startTripPreCheckHandler(data) {
    def tripId = data.tripId
    logDebug("In Start Trip Pre-Check for trip ${tripId}", "Trips")
    if (areDepartureConditionsMet(tripId, true) && !hasTripStarted(tripId)) {
        logDebug("Departure conditions met & trip has not started for trip ${tripId}", "Trips")
        for (personName in settings["trip${tripId}People"]) {
            def personId = getIdOfPersonWithName(personName)
            if (!atDestinationOfTrip(personId, tripId)) {
                state.people[personId]?.current.trip.id = tripId
                state.people[personId]?.current.trip.departureTime = null      
                state.people[personId]?.current.trip.recommendedRoute = null
                state.people[personId]?.current.trip.eta = null
                state.people[personId]?.current.trip.hasPushedLateNotice = false
                updateTripPreCheck([tripId: tripId])
            }
        }
    }
}

def updateTripPreCheck(data) {
    def tripId = data.tripId
    logDebug("In Update Trip Pre-Check for trip ${tripId}", "Trips") 
    if (areDepartureConditionsMet(tripId, true) && !hasTripStarted(tripId)) {
        // perform check throughout the predeparture and departure window, until the trip starts (i.e., a person departs). Once a person departs on the trip, stop updating from this function.
         logDebug("Updating Trip Pre-Check for trip ${tripId}", "Trips")   
        updateTrackersForTripPeople(tripId)
        pushLateNotification(tripId)
        badTrafficNotification(tripId)
        runIn(getFetchIntervalSetting()*60, "updateTripPreCheck", [data: [tripId: tripId]])
    }
    else {
        logDebug("Either departure window ended or trip started. No more updates for now.", "Trips")
    }
}

def isBadTraffciNotificationConfigured(String tripId) {
    if (settings["trip${tripId}BadTrafficPushDevices"]) return true
    else return false
}

def badTrafficNotification(String tripId) {
    if (isBadTraffciNotificationConfigured(tripId)) {
        def bestRoute = getBestRoute(tripId)
        if (bestRoute.relativeTrafficDelay > gettrafficDelayThresholdSetting()) {
            def trafficDelayMins = Math.round(bestRoute.relativeTrafficDelay / 60)
            def trafficDelayStr = (trafficDelayMins == 1) ? trafficDelayMins.toString() + " min" : trafficDelayMins.toString() + " mins"
            settings["trip${tripId}BadTrafficPushDevices"].deviceNotification("Bad traffic on your trip from ${settings["trip${tripId}Origin"]} to ${settings["trip${tripId}Destination"]}. Allow ${trafficDelayStr} more than usual. Best route as of now is ${bestRoute.summary}, for ${bestRoute.eta} arrival.")
            
            settings["trip${tripId}BadTrafficSwitches"].each { theSwitch ->
                theSwitch.on()
            }
        }
    }
}

def updateTrackersForTripPeople(String tripId) {
    for (personName in settings["trip${tripId}People"]) {
        def personId = getIdOfPersonWithName(personName)
        updateTracker(personId)
    }    
}

def startTripForPerson(String personId, String tripId) {
    logDebug("Starting trip ${tripId} for person ${personId}", "Trips")
    state.people[personId]?.current.trip.id = tripId
    state.people[personId]?.current.trip.departureTime = new Date().getTime()
    
    def bestRoute = getBestRoute(tripId, true) // force update of route information 
    state.people[personId]?.current.trip.eta = getETADate(bestRoute.duration).getTime()
     
    performDepartureActionsForTrip(personId, tripId)
    
    def tripTimeOut = bestRoute.duration * 2
    runIn(tripTimeOut, "timeOutTrip", [data: [personId: personId, tripId: tripId, originalDuration: bestRoute.duration]])
    
    if (isFailedArrivalNotificationConfigured(tripId)) {
        def targetArrival = toDateTime(settings["trip${tripId}TargetArrivalTime"])
        Calendar cal = Calendar.getInstance()
        cal.setTimeZone(location.timeZone)
        cal.setTime(targetArrival)
        def secsLate = getFailedArrivalNotificationMinsSetting(tripId)*60
        cal.add(Calendar.SECOND, secsLate)
        def failedArrivalTime = cal.getTime()
        runOnce(failedArrivalTime, "pushFailedArrivalNotification", [data: [personId: personId, tripId: tripId]])
    }
}

def timeOutTrip(data) {
    def tripId = data.tripId
    def personId = data.personId
    def originalDuration = data.originalDuration
    if (isPersonOnTrip(personId, tripId)) {
        if (isInVehicle(personId) || isDriving(personId)) {
            logDebug("Trip Timeout Postponed: Person ${personId} has not yet arrived at the destination of trip ${tripId} but is still  driving. Trip abort postponed.", "Trips")
            runIn(originalDuration, "timeOutTrip", [data: [personId: personId, tripId: tripId, originalDuration: originalDuration]])
        }
        else {
            logDebug("Trip Timeout: Person ${personId} did not arrive at the destination of trip ${tripId} and is not currently driving. Trip being aborted.", "Trips")
            abortCurrentTripForPerson(personId)
            updateTracker(personId)
        }
    }
}

def pushFailedArrivalNotification(data) {
    def tripId = data.tripId
    def personId = data.personId
    if (isPersonOnTrip(personId, tripId)) {     
        // person is still on trip, so failed to arrive
         if (isInVehicle(personId) || isDriving(personId)) {
             settings["trip${tripId}FailedArrivalPushDevices"].deviceNotification("${personName} started to travel to ${settings["trip${tripId}Destination"]} but failed to arrive as expected. ${personName} is still in the car.")
         }
         else {
             settings["trip${tripId}FailedArrivalPushDevices"].deviceNotification("${personName} started to travel to ${settings["trip${tripId}Destination"]} but failed to arrive as expected. ${personName} is no longer in the car.")
         }
        
        if (settings["trip${tripId}FailedArrivalSwitches"] && settings["trip${tripId}FailedArrivalSwitchesOnorOff"]) {
            def cmd = settings["trip${tripId}FailedArrivalSwitchesOnorOff"]
            settings["trip${tripId}FailedArrivalSwitches"].each { theSwitch ->
                theSwitch."${cmd}"()
            }
        }
    }
}

def isFailedArrivalNotificationConfigured(String tripId) {
    if (settings["trip${tripId}FailedArrivalPushDevices"] && settings["trip${tripId}TargetArrivalTime"]) return true
    else return false
} 

def endCurrentTripForPerson(String personId) {
    logDebug("Ending trip ${state.people[personId]?.current.trip.id} for person ${personId}", "Trips")
    performArrivalActionsForTrip(personId, state.people[personId]?.current.trip.id) 
    
    state.people[personId]?.previous.trip.id = state.people[personId]?.current.trip.id
    state.people[personId]?.previous.trip.departureTime = state.people[personId]?.current.trip.departureTime
    state.people[personId]?.previous.trip.arrivalTime = new Date().getTime()            
    state.people[personId]?.current.trip.id = null
    state.people[personId]?.current.trip.eta = null
    state.people[personId]?.current.trip.departureTime = null      
    state.people[personId]?.current.trip.recommendedRoute = null
    state.people[personId]?.current.trip.hasPushedLateNotice = null
    
    runIn(getPostArrivalDisplayMinsSetting()*60, "stopPostArrivalDisplay", [data: [personId: personId]])
}

def abortCurrentTripForPerson(String personId) {
    logDebug("Aborting trip ${state.people[personId]?.current.trip.id} for person ${personId}", "Trips")
             
    state.people[personId]?.current.trip.id = null
    state.people[personId]?.current.trip.eta = null
    state.people[personId]?.current.trip.departureTime = null      
    state.people[personId]?.current.trip.recommendedRoute = null
    state.people[personId]?.current.trip.hasPushedLateNotice = null
}

def stopPostArrivalDisplay(data) {
    String personId = data.personId
    updateTracker(personId)
}

def startDepartureWindowHandler(data) {
    def tripId = data.tripId
    // reserved for future use in case need to do something here
}

def endDepartureWindowHandler(data) {
    def tripId = data.tripId
    for (personName in settings["trip${tripId}People"]) {
        def personId = getIdOfPersonWithName(personName)
        if (state.people[personId]?.current.trip.id == tripId && state.people[personId]?.current.trip.departureTime == null) {
         // person never left on the trip, so cancel trip
        cancelCurrentTripForPerson(personId) 
        }
    }
}

def cancelCurrentTripForPerson(String personId) {
    state.people[personId]?.current.trip.id = null
    state.people[personId]?.current.trip.departureTime = null      
    state.people[personId]?.current.trip.recommendedRoute = null 
    state.people[personId]?.current.trip.eta = null 
    state.people[personId]?.current.trip.hasPushedLateNotice = false  
    updateTracker(personId)
}

def getTripIdListForPerson(String personId) {
    def tripIdList = []
    state.trips.each { tripId, trip ->
        if (isTripPerson(personId, tripId)) {
            tripIdList.add(tripId)
        }
    }
    return tripIdList  
}

String getUpcomingTripForPerson(String personId) {
    // a trip is "upcoming" if it's pre-departure window is open despite not being in the specified departure window, or of course if it's within the departure window
    def nextUpcomingTripId = null
    def tripsForPerson = getTripIdListForPerson(personId)
    for (tripId in tripsForPerson) {
        if (areDepartureConditionsMet(tripId, true) && !isPersonOnTrip(personId, tripId)) {
            if (!nextUpcomingTripId) {
                def tripDeparture = toDateTime(settings["trip${tripId}EarliestDepartureTime"])
                if (tripDeparture && tripDeparture.after(new Date())) {
                    nextUpcomingTripId = tripId
                }
            }
            else {
                def tripDeparture = toDateTime(settings["trip${tripId}EarliestDepartureTime"])
                def soonestSoFar = toDateTime(settings["trip${nextUpcomingTripId}EarliestDepartureTime"])
                if (tripDeparture && tripDeparture.after(new Date()) && tripDeparture.before(soonestSoFar)) {
                    nextUpcomingTripId = tripId
                }
            }
        }
    }
    return nextUpcomingTripId
}

def startUpcomingTripForPerson(String personId) {
    def tripId = getUpcomingTripForPerson(personId)    
    if (tripId) {
        startTripForPerson(personId, tripId)
        updateTracker(personId)
    }
    else logDebug("No upcoming trips for person ${personId}. Nothing to start.", "Trips")
}

def performPreDepartureActionsForTrip(String tripId) {
    pushRouteRecommendation(tripId)
}

def performDepartureActionsForTrip(String personId, String tripId) {
    logDebug("Performing Departure Actions for trip ${tripId}", "Trips")
    pushRouteRecommendation(tripId, personId)
    pushLateNotification(tripId)
    controlDepartureSwitches(tripId)
}

def controlDepartureSwitches(String tripId) {
    if (settings["trip${tripId}DepartureSwitches"] && settings["trip${tripId}DepartureSwitchesOnorOff"]) {
        def cmd = settings["trip${tripId}DepartureSwitchesOnorOff"]
        settings["trip${tripId}DepartureSwitches"].each { theSwitch ->
                theSwitch."${cmd}"()
        }
    }
}

def performArrivalActionsForTrip(String personId, String tripId) {
    controlArrivalSwitches(tripId)
    pushArrivalNotification(tripId, personId)
}

def controlArrivalSwitches(String tripId) {
    if (settings["trip${tripId}ArrivalSwitches"] && settings["trip${tripId}ArrivalSwitchesOnorOff"]) {
        def cmd = settings["trip${tripId}ArrivalSwitchesOnorOff"]
        settings["trip${tripId}ArrivalSwitches"].each { theSwitch ->
                theSwitch."${cmd}"()
        }
    }
}

def pushLateNotification(String tripId) {
    // this method is called before the trip has started, or upon the trip starting, but not after the trip has started
    if (isLateNotificationConfigured(tripId)) {
        def bestRoute = getBestRoute(tripId)
        if (bestRoute) {
            def bestRouteDuration = bestRoute.duration
            def eta = getETADate(bestRoute.duration)
            def etaStr = extractTimeFromDate(eta)
            def targetArrival = toDateTime(settings["trip${tripId}TargetArrivalTime"])
            if (eta.after(targetArrival)) {
                def secondsLate = getSecondsBetweenDates(targetArrival, eta)
                def secsLateThreshold = getLateNotificationMinsSetting(tripId)*60
                if (secondsLate >= secsLateThreshold) {
                    for (personName in settings["trip${tripId}People"]) {
                        def personId = getIdOfPersonWithName(personName)
                        if (state.people[personId].current.trip.hasPushedLateNotice == false) {
                            if (state.people[personId]?.current.trip.eta != null) {
                                settings["trip${tripId}PushDevicesIfLate"].deviceNotification("${personName} just left ${settings["trip${tripId}Origin"]}, but is running late. ${personName} expects to arrive at ${settings["trip${tripId}Destination"]} around ${etaStr}.")
                            }
                            else {
                                settings["trip${tripId}PushDevicesIfLate"].deviceNotification("${personName} has not yet left ${settings["trip${tripId}Origin"]}, and is running late. If ${personName} were able to leave now for ${settings["trip${tripId}Destination"]}, estimated time of arrival would be around ${etaStr}.")
                            }
                            state.people[personId].current.trip.hasPushedLateNotice = true 
                        }
                    }
                }
            }
        }
    }
}

def isLateNotificationConfigured(String tripId) {
    if (settings["trip${tripId}PushDevicesIfLate"] && settings["trip${tripId}TargetArrivalTime"]) return true
    else return false
}   

def pushRouteRecommendation(String tripId, String personId = null) {
    if (isDeparturePushConfigured(tripId) && isDeparturePushAllowed(tripId, personId)) {
        def bestRoute = getBestRoute(tripId)
        if (bestRoute) {
            def etaDate = getETADate(bestRoute.duration)
            def eta = extractTimeFromDate(etaDate)
            def isBestPreferred = isPreferredRoute(tripId, bestRoute.summary)
        
            def nextBestRoute = getSecondBestRoute(tripId)
            def isNextBestPreferred = isPreferredRoute(tripId, nextBestRoute.summary)
            def fasterBy = Math.round((nextBestRoute.duration - bestRoute.duration)/60)

            if (isBestPreferred && fasterBy < 0) {
                settings["trip${tripId}DeparturePushDevices"].deviceNotification("Take ${bestRoute.summary} as the preferred route, for ${eta} arrival. But ${nextBestRoute.summary} is ${fasterBy*-1} mins faster.")
            }
            else if (isBestPreferred || !isNextBestPreferred || (!isBestPreferred && isNextBestPreferred && fasterBy >= settings["trip${tripId}PreferredRouteBias"])) {
                settings["trip${tripId}DeparturePushDevices"].deviceNotification("Take ${bestRoute.summary} for ${eta} arrival. Faster than ${nextBestRoute.summary} by ${fasterBy} mins.")
            }
        
            if (personId != null) state.people[personId].current.trip.recommendedRoute = bestRoute.summary
            else {
                 // predeparture recommendation that is not specific to a person
                for (personName in settings["trip${tripId}People"]) {
                    def id = getIdOfPersonWithName(personName)
                    state.people[id].current.trip.recommendedRoute = bestRoute.summary
               }            
            }
        }
    }
}

def isDeparturePushConfigured(String tripId) {
    if (settings["trip${tripId}DeparturePushDevices"]) return true
    else return false
}

def isDeparturePushAllowed(String tripId, String personId = null) {
    def isAllowed = true
    
    def bestRoute = getBestRoute(tripId)  
    
    if (settings["trip${tripId}OnlyPushIfNonPreferred"] && settings["trip${tripId}PreferredRoute"]) {
        if (isPreferredRoute(tripId, bestRoute.summary)) {
            isAllowed = false
            log.info "Preferred Route Best. No Push Notification To Be Sent."      
        }                
    }
    
    if (personId != null) {
        if (state.people[personId].current.trip.recommendedRoute != null && state.people[personId].current.trip.recommendedRoute == bestRoute.summary) {
            isAllowed = false
            log.info "Route already recommended to person. No Push Notification To Be Sent."
        }
    }
    else {
        // predeparture push taking place. Only send push if no person has already been recommended a route.
        for (personName in settings["trip${tripId}People"]) {
            def id = getIdOfPersonWithName(personName)
            if (state.people[id].current.trip.recommendedRoute != null && state.people[id].current.trip.recommendedRoute == bestRoute.summary) {
                isAllowed = false
                log.info "Route already recommended. No Push Notification To Be Sent."
           }
        }
    }
    return isAllowed
}


def pushArrivalNotification(String tripId, String personId) {
    if (isArrivalPushConfigured(tripId)) {
        def destination = settings["trip${tripId}Destination"]
        def arrivalTime = extractTimeFromDate(new Date())
        def personName = getNameOfPersonWithId(personId)
        settings["trip${tripId}ArrivalPushDevices"].deviceNotification("${personName} arrived at ${destination} at ${arrivalTime}.")
    }
}


def isArrivalPushConfigured(String tripId) {
    if (settings["trip${tripId}ArrivalPushDevices"]) return true
    else return false
}

def isInPostArrivalDisplayWindow(String personId) {
    def inPostArrivalDisplayWindow = false
    if (state.people[personId]?.current.trip.id == null && state.people[personId]?.previous.trip.id != null && state.people[personId]?.previous.trip.arrivalTime != null) {
        def postArrivalSecs = getPostArrivalDisplayMinsSetting()*60
        def secsSinceArrival = getSecondsSince(state.people[personId]?.previous.trip.arrivalTime)
        logDebug("PersonId ${personId} Arrived on tripID ${state.people[personId]?.previous.trip.id} ${secsSinceArrival} seconds ago.", "Trips")
        if (secsSinceArrival < postArrivalSecs) {
            def destinationId = getDestinationIdOfTrip(state.people[personId]?.previous.trip.id)
            if (destinationId == state.people[personId]?.current.place.id) {
                // only display post arrival if still at destination of trip
                inPostArrivalDisplayWindow = true
            }
        }
    }
    return inPostArrivalDisplayWindow
}

def getDestinationIdOfTrip(String tripId) {
    def destinationName = getDestination(tripId)
    return getIdOfPlaceWithName(destinationName)    
}

def handlePlaceChange(String personId) {
    // first update state based on impact of change to trip status
    
    // Did the place change start a trip?
    state.trips.each { tripId, trip ->
        if (isTripPerson(personId, tripId) && didDepartOrigin(personId, tripId) && areDepartureConditionsMet(tripId) && inTripVehicle(personId, tripId) && !isPersonOnTrip(personId, tripId) && !atDestinationOfTrip(personId, tripId)) {
            // trip just started because (i) person scheduled for trip (ii) left origin; (iii) while departure conditions met; (iv) within a vehicle specified for the trip; (iv) trip was not already in progress; and (v) not already at destination (can't start a trip to the destination if already there)
            startTripForPerson(personId, tripId)
        }
    }
    
    // Did the place change end the person's current trip by arrival at the destination?
    if (isPersonOnTrip(personId) && atDestinationOfCurrentTrip(personId)) {
        // trip just ended   
        logDebug("Ending current trip for person ${personId}", "Trips")
        endCurrentTripForPerson(personId)        
    }
    
        
    // Did the place change abort the person's current trip by arrival at the origin?
    if (isPersonOnTrip(personId) && atOriginOfCurrentTrip(personId)) {
        logDebug("Aborting current trip for person ${personId} since arrived back at the origin", "Trips")
        abortCurrentTripForPerson(personId)        
    }
    
    // then update Tracker device (which will indicate any tripId stored at state.people[personId]?.current.trip.id as well as store, for a period of time, any tripId stored at state.people[personId]?.previous.trip.id)
    updateTracker(personId)
}

Boolean isPersonOnTrip(String personId, String tripId=null) {
    if (tripId != null) {
        return (state.people[personId]?.current.trip.id == tripId && state.people[personId]?.current.trip.departureTime != null) ? true : false  
    }
    else return (state.people[personId]?.current.trip.id != null && state.people[personId]?.current.trip.departureTime != null) ? true : false
}

Boolean isTripPerson(String personId, String tripId) {
    def personName = getNameOfPersonWithId(personId)
	if (settings["trip${tripId}People"] && settings["trip${tripId}People"].contains(personName)) {
	    return true
	}    
    else return false    
}

def inTripVehicle(String personId, String tripId) {
    def returnValue = false
    if (!settings["trip${tripId}Vehicles"]) returnValue = true  // no vehicle specified for the trip, so assume no restriction on vehicle to be used
    else if (state.people[personId]?.current?.vehicle?.id) {
        for (tripVehicleName in settings["trip${tripId}Vehicles"]) {
            def tripVehicleId = getIdOfVehicleWithName(tripVehicleName)
            if (state.people[personId]?.current?.vehicle?.id == tripVehicleId) returnValue = true
        }
    }    
    return returnValue
}

boolean didDepartOrigin(String personId, String tripId) {
    if (state.people[personId]?.previous?.place?.id && state.people[personId]?.previous?.place?.id == getIdOfPlaceWithName(settings["trip${tripId}Origin"])) {
        // Just left origin of trip
        return true
    }
    else return false
}
                
                
Boolean areDepartureConditionsMet(String tripId, Boolean isTripPreCheckIncluded=false) {
    def isInDepartureWindow = inDepartureWindow(tripId, isTripPreCheckIncluded)
    logDebug("Checking whether departure conditions are met for trip ${tripId}. isInDepartureWindow = ${isInDepartureWindow}", "Trips")
    if (isInDepartureWindow && !isRestricted()) return true
    else return false
}

Boolean inDepartureWindow(String tripId, Boolean isTripPreCheckIncluded=false) {
    Boolean inWindow = true
    
    // Day of Week Travel Check
    if(!isTripDay(tripId)) {
        logDebug("Trip Day Check Failed.", "Trips")
	    inWindow = false
    }
    
    // Time Window
    if (settings["trip${tripId}EarliestDepartureTime"] && settings["trip${tripId}LatestDepartureTime"]) {
        def tripPreCheckTime = getPreCheckTime(tripId)
        logDebug("Trip PreCheck time for trip ${tripId} is ${tripPreCheckTime}. isTripPreCheckIncluded value is ${isTripPreCheckIncluded}", "Trips")
        def windowStart = isTripPreCheckIncluded ? tripPreCheckTime : toDateTime(settings["trip${tripId}EarliestDepartureTime"])
        def windowEnd = toDateTime(settings["trip${tripId}LatestDepartureTime"])
        logDebug("Checking if in departure window which starts at ${windowStart} and ends at ${windowEnd} for trip ${tripId}", "Trips")
        if(!timeOfDayIsBetween(windowStart, windowEnd, new Date(), location.timeZone)) {
            logDebug("Time Check Failed.", "Trips")
            inWindow = false
        }
        else logDebug("Time Check Passed.", "Trips")
    }    
    return inWindow
}

Date getPreCheckTime(String tripId) {
    Integer preCheckSecs = getTripPreCheckMinsSetting()*60*-1
    return adjustTimeBySecs(settings["trip${tripId}EarliestDepartureTime"], preCheckSecs)
}
                
                
def isTripDay(String tripId) {
    def isTripDay = false
    def dateFormat = new SimpleDateFormat("EEEE")
    def dayOfTheWeek = dateFormat.format(new Date())
    logDebug("Trip days for tripId ${tripId} are ${settings["trip${tripId}Days"]}.", "Trips")
    if (settings["trip${tripId}Days"] == null) {
        logDebug("No trip days specified.", "Trips")
        isTripDay = true // if no trip days specified, assume trip day
    }
	if (settings["trip${tripId}Days"].contains(dayOfTheWeek)) {
        logDebug("Trip days include ${dayOfTheWeek}.", "Trips")
	    isTripDay = true
	}    
    logDebug("isTripDay is ${isTripDay}", "Trips")
    return isTripDay
}
     
boolean atDestinationOfTrip(String personId, String tripId) {
    def atDest = false
    if (state.people[personId]?.current?.place?.id == getDestinationIdOfTrip(tripId)) {
        atDest = true
    }
    logDebug("Person ${personId} is ${(atDest) ? "" : "not "}at the destination of trip ${tripId}.", "Trips")
    return atDest
}   
       
boolean atOriginOfTrip(String personId, String tripId) {
    def atOrigin = false
    if (state.people[personId]?.current?.place?.id == getOriginIdOfTrip(tripId)) {
        atOrigin = true
    }
    logDebug("Person ${personId} is ${(atOrigin) ? "" : "not "}at the origin of trip ${tripId}.", "Trips")
    return atOrigin
}   

boolean atDestinationOfCurrentTrip(personId) {
    def atDest = false
    if (state.people[personId]?.current?.place?.id && state.people[personId]?.current?.trip?.id) {
        def tripId = state.people[personId]?.current?.trip?.id
        if (state.people[personId]?.current?.place?.id == getIdOfPlaceWithName(settings["trip${tripId}Destination"])) {
            atDest = true
        }
    } 
    logDebug("Person ${personId} is ${(atDest) ? "" : "not "}at the destination of their current trip.", "Trips")
    return atDest
}
    
boolean atOriginOfCurrentTrip(personId) {
    def atOrigin = false
    if (state.people[personId]?.current?.place?.id && state.people[personId]?.current?.trip?.id) {
        def tripId = state.people[personId]?.current?.trip?.id
        if (state.people[personId]?.current?.place?.id == getIdOfPlaceWithName(settings["trip${tripId}Origin"])) {
            atOrigin = true
        }
    } 
    logDebug("Person ${personId} is ${(atDest) ? "" : "not "} at the origin of their current trip.", "Trips")
    return atOrigin
}

def handleVehicleChange(String personId) {
    // first update state based on impact of change to trip status
    logDebug("Handling Vehicle Change for person ${personId}", "Vehicles")

    // did vehicle change start a trip?
    state.trips.each { tripId, trip ->
         if (isTripPerson(personId, tripId) && inTripVehicle(personId, tripId) && atOriginOfTrip(personId, tripId) && areDepartureConditionsMet(tripId) && !isPersonOnTrip(personId, tripId) && !atDestinationOfTrip(personId, tripId)) {
             // assume trip just started because (i) got in a vehicle specified for the trip (ii) at the origin of the trip (iii) while departure conditions met; (iv) trip was not already in progress; and (v) person is not already at the destination of the trip
             startTripForPerson(personId, tripId)             
         }
    }
    // did vehicle change end a trip?
    def currentTripId = state.people[personId]?.current.trip.id
    if (currentTripId != null && isTripPerson(personId, tripId) && !inTripVehicle(personId, currentTripId) && !mostPreferredArrivalTriggersConfigured(personId, currentTripId) && acceptableArrivalTriggersConfigured(personId, currentTripId)) {
        // Exiting of a vehicle specified for the trip while the trip was in progress COULD mean that the trip just ended. But it could also mean that the person stopped on the way to the destination, but has not yet arrived. So, only detect arrival based on vehicle presence if it is the most acceptable way possible given the current sensor configuration.                
        endCurrentTripForPerson(personId) 
    }
    // then update Tracker device (which will indicate any tripId stored at state.people[personId]?.current.trip.id as well as store, for a period of time, any tripId stored at state.people[personId]?.previous.trip.id)
    updateTracker(personId)
}


Boolean mostPreferredArrivalTriggersConfigured(String  personId, String tripId) {
    def isConfigured = false
    
    def destinationId = getIdOfPlaceWithName(settings["trip${tripId}Destination"])
    if (settings["place${destinationId}Person${personId}Sensor"]) isConfigured = true

    if (settings["place${destinationId}GarageDoor"]) isConfigured = true
    if (settings["place${destinationId}ContactSensor"]) isConfigured = true
    if (settings["place${destinationId}Switch"]) isConfigured = true
    
    return isConfigured
}


Boolean acceptableArrivalTriggersConfigured(String  personId, String tripId) {
    def isConfigured = false
    
    if (settings["person${personId}Life360"]) isConfigured = true
    
    for (vehicleName in settings["trip${tripId}Vehicles"]) {
        def vehicleId = getIdOfVehicleWithName(vehicleName)
        if (settings["vehicle${vehicleId}Person${personId}Sensor"]) isConfigured = true
    }    

    return isConfigured
}

def handleDrivingChange(String personId) {        // isDriving is only set by life360, not by presence in a vehicle
    // first update state based on impact of change to trip status
    
    // did driving change start a trip?
    if (isDriving(personId)) {  // person just started driving
        state.trips.each { tripId, trip ->
             if (isTripPerson(personId, tripId) && areDepartureConditionsMet(tripId) && !isPersonOnTrip(personId, tripId)) {
                 // assume trip just started because (i) person scheduled for the trip just started driving (ii) while departure conditions met; (iii) and trip was not already in progress
                 startTripForPerson(personId, tripId)             
             }
        }
    }
    else {   // person just stopped driving
    // did driving change end a trip?
        def currentTripId = state.people[personId]?.current.trip.id
        if (currentTripId != null && isTripPerson(personId, currentTripId) && !mostPreferredArrivalTriggersConfigured(personId, currentTripId) && acceptableArrivalTriggersConfigured(personId, currentTripId)) {
            // Stopping of driving state while the trip was in progress COULD mean that the trip just ended. But it could also mean that the person stopped on the way to the destination, but has not yet arrived. So, only detect arrival based on driving state stopping if it is the most acceptable way possible given the current sensor configuration.                
            endCurrentTripForPerson(personId) 
        }
    }
    
    // then update Tracker device (which will indicate any tripId stored at state.people[personId]?.current.trip.id as well as store, for a period of time, any tripId stored at state.people[personId]?.previous.trip.id)
    updateTracker(personId)
}

String getPlaceIdForCoordinates(latitude, longitude) {
    def placesPresentAt = []
    def closestPlace = [placeId: null, distance: null]
    state.places.each { placeId, place ->
        if (latitude != null && longitude != null && place.latitude != null && place.longitude != null) {
            def distance = getDistanceBetweenCoordinates(latitude, longitude, place.latitude, place.longitude)  
            logDebug("Distance = ${distance}", "Places")
            if (distance <= getGeofenceRadiusSetting()) {
                placesPresentAt.add(placeId)
                if (closestPlace.placeId == null) closestPlace = [placeId: placeId, distance: distance]
                else {
                    if (distance < closestPlace.distance) {
                        closestPlace = [placeId: placeId, distance: distance]
                    }
                }
            }
        }
        else {
            log.warn "Warning: Null latitude or longitude for placeID=${placeId}. Latitude=${latitude}, Longitude=${longitude}, placeLatitude=${place.latitude}, placeLongitude=${place.longitude}."
        }
    }
    if (placesPresentAt.size() > 1) {
        log.warn "Present at multiple places according to Life360 lat/long. Only reporting presence at closest place."
    }
    return closestPlace.placeId
}

def getDistanceBetweenCoordinates(lat1, lon1, lat2, lon2) {
    def radius = 6371    // radius of the earth
    // In kilometers
    def latDistance = Math.toRadians(lat2 - lat1)
    def lonDistance = Math.toRadians(lon2 - lon1)
 
    def a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2) 
    def c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    def distance = radius * c * 1000     // convert to meters
        
    return distance
}

def setPersonPlace(String personId) {
    // ** set person's current/previous presence after per place presence updated in state in response to presence event **
    def placesPresent = []
    def lastChanged = null
    
    iterationCount++
      //  logDebug("Iteration# ${iterationCount}: In setPersonPlace for personId " + personId)
  //  logDebug("Iteration# ${iterationCount}: State of person per place is ${state.people[personId].places}")
    def placesList = []
    state.people[personId].places.each { placeId, presenceInfo ->
        placesList.add(placeId)
      //  logDebug("Iteration# ${iterationCount}: Presence Info for person ${personId} at placeId ${placeId} is ${presenceInfo.presence}.")
        if (presenceInfo.presence.equals("present")) {
         //   logDebug("Iteration# ${iterationCount}: Adding placeId ${placeId} to placesPresent")
            placesPresent.add(placeId)
        }
        if (lastChanged == null || state.people[personId].places[lastChanged].atTime == null) lastChanged = placeId
        else if (presenceInfo.atTime > state.people[personId].places[lastChanged].atTime) lastChanged = placeId
            
      //  logDebug("Iteration# ${iterationCount}: Checking for last changed presence event. Presence info for placeId ${placeId} last updated at ${presenceInfo.atTime}. LatestChanged event considered so far was for placeId ${lastChanged} at ${state.people[personId].places[lastChanged].atTime}")
    }
  //  logDebug("Iteration# ${iterationCount}: places list is ${placesList}")
  //  logDebug("Iteration# ${iterationCount}: placeIds of places where present: ${placesPresent}. placeId of lastChanged: ${lastChanged}")
    if (placesPresent.size() == 0) {
        // just left a place and not present at any other place that has a presence sensor. Set to life360 address if available, or else set to null to indicate presence is unknown
        
        // NOTE: setting to life360 presence means a person won't be able to leave a place until life360 detects departure. But trip can be started sooner with detection that present in vehicle or with garage door
        
        def placeIdAtAddress = state.people[personId].life360?.placeAtAddress
        def placeIdAtCoordinates = state.people[personId].life360?.placeAtCoordinates
        def placeIdWithName = state.people[personId].life360?.placeWithName
     //   logDebug("Iteration# ${iterationCount}: No presence sensors of places present.")
        if (placeIdWithName && didChangePlaceById(personId, placeIdWithName)) {
            changePersonPlaceById(personId, placeIdWithName)
        }
        else if (placeIdAtCoordinates && didChangePlaceById(personId, placeIdAtCoordinates)) {
            changePersonPlaceById(personId, placeIdAtCoordinates)
        }
        else if (placeIdAtAddress && didChangePlaceById(personId, placeIdAtAddress)) {
            changePersonPlaceById(personId, placeIdAtAddress)
        }
        else if (state.people[personId].life360?.address && didChangePlaceByName(personId, state.people[personId].life360?.address)) {
            // life360 address that doesn't correspond to a place
            changePersonPlaceByName(personId, state.people[personId].life360?.address)
        }
        else {
            // no life360 address available, and not present anywhere. Set everything to null if wasn't already null
            if (state.people[personId]?.current.place.id != null || state.people[personId]?.current.place.name != null) {
                changePersonPlaceByName(personId, null)
            }
        }
    }
    else if (placesPresent.size() == 1) {
        // either (i) just arrived at a place after not being present anywhere or (ii) just left a place after multiple presence sensors present
        
        // check if last presence sensor event was arrival or departure to detect case (i) or case (ii), respectively
      //  logDebug("Iteration# ${iterationCount}: Person ${personId} Only present at 1 place")
        if (state.people[personId].places[lastChanged]?.presence.equals("present")) {
            // case (i)
         //   logDebug("Iteration# ${iterationCount}: Presence at that place is the last presence event to occur")
            if (lastChanged != placesPresent[0]) logDebug("Mismatch from places presence sensor logic. Needs debugging.")
            
            if (state.people[personId].life360?.address != getNameOfPlaceWithId(placesPresent[0]) && state.people[personId].life360?.address !=  getPlaceAddressById(placesPresent[0])) {
                log.warn "Iteration# ${iterationCount}: Mismatch in presence for ${getNameOfPersonWithId(personId)}. Life360 indicates presence at ${state.people[personId].life360?.address} but presence sensor indicates he or she is present at ${getNameOfPlaceWithId(placesPresent[0])}"
            }
            // prioritize presence sensor presence over any life360 state, since presence sensor capable of combining info from multiple sources
            if (didChangePlaceById(personId, placesPresent[0])) {
            //    logDebug("Iteration# ${iterationCount}: Place of presence has changed, so updating current place in person's state to placeId ${placesPresent[0]}")
                changePersonPlaceById(personId, placesPresent[0])
            }
        }
        else if (state.people[personId].places[lastChanged]?.presence.equals("not present")) {
            // case (ii)    
          //  log.warn "Iteration# ${iterationCount}: Check to see if presence sensor for placeId ${placesPresent[0]} is stuck."
            if (didChangePlaceById(personId, placesPresent[0])) {
             //   logDebug("Iteration# ${iterationCount}: Place of presence has changed, so updating current place in person's state to placeId ${placesPresent[0]}")
                changePersonPlaceById(personId, placesPresent[0])
             }
        }
    }
    else {
        // present at multiple places. Must resolve conflict between multiple presence sensors in the present state
         //   log.warn "Iteration# ${iterationCount}: Present at multiple places, including placeIds ${placesPresent}. Setting presence to the last place where presence changed to present."
            if (didChangePlaceById(personId, lastChanged)) {
            //    logDebug("Iteration# ${iterationCount}: Place of presence has changed, so updating current place in person's state to placeId ${lastChanged}")
                changePersonPlaceById(personId, lastChanged)
             }
    }
}

def didChangePlaceByName(String personId, String placeName) {

    def placeIdByName = getIdOfPlaceWithName(placeName)
    def placeIdByAddress = getIdOfPlaceWithAddress(placeName)    // if life360 being used, placeName could be just an address
    
    logDebug("In didChangePlaceByName() for person ${personId} with placeName ${placeName}. placeIdByName is ${placeIdByName}. placeIdByAddress is ${placeIdByAddress}. And current place id is ${state.people[personId]?.current.place.id}", "Places")
    // check if place presence actually changed
    if (state.people[personId]?.current.place.id) {
        if (state.people[personId]?.current.place.id == placeIdByName || state.people[personId]?.current.place.id == placeIdByAddress) {
            // location hasn't changed
            logDebug("Current Place Id hasn't changed", "Places")
            return false
        }
    }
    else if (state.people[personId]?.current.place.name && state.people[personId]?.current.place.name.equals(placeName)) {
        // location hasn't changed
        logDebug("Current place name hasn't changed", "Places")
        return false
    }
    else if (state.people[personId]?.current.place.name == null && placeName == null) {
        // still at unknown place; no update to be done
        logDebug("Current place name is still null", "Places")
        return false
    }    
    return true
}

def didChangePlaceById(String personId, String placeId) {
    // check if place presence actually changed
    if (state.people[personId]?.current.place.id && state.people[personId]?.current.place.id == placeId) {
        // location hasn't changed
        logDebug("didChangePlaceById returning false", "Places")
        return false
    }    
    return true
}

def changePersonPlaceByName(String personId, String placeName) {    
    def placeIdByName = getIdOfPlaceWithName(placeName)
    def placeIdByAddress = getIdOfPlaceWithAddress(placeName)    // if life360 being used, placeName could be just an address
    
    state.people[personId]?.previous.place.id = state.people[personId]?.current.place.id
    state.people[personId]?.previous.place.name = state.people[personId]?.current.place.name
    state.people[personId]?.previous.place.arrival = state.people[personId]?.current.place.arrival
    
    state.people[personId]?.previous.place.departure = new Date().getTime()
    
    if (placeIdByName) {
        state.people[personId]?.current.place.id = placeIdByName
        state.people[personId]?.current.place.name = placeName
        state.people[personId]?.current.place.arrival = new Date().getTime()
    }
    else if (placeIdByAddress) {
        state.people[personId]?.current.place.id = placeIdByAddress
        state.people[personId]?.current.place.name = getNameOfPlaceWithId(placeIdByAddress)
        state.people[personId]?.current.place.arrival = new Date().getTime()
    }
    else {    // place name is either an address of an unknown place (from life360) or is null (indicating completely unknown presence)
        state.people[personId]?.current.place.id = null
        state.people[personId]?.current.place.name = placeName
        state.people[personId]?.current.place.arrival = (placeName) ? new Date().getTime() : null
    }
    handlePlaceChange(personId)
}

def changePersonPlaceById(String personId, String placeId) {    
    state.people[personId]?.previous.place.id = state.people[personId]?.current.place.id
    state.people[personId]?.previous.place.name = state.people[personId]?.current.place.name
    state.people[personId]?.previous.place.arrival = state.people[personId]?.current.place.arrival
    state.people[personId]?.previous.place.departure = new Date().getTime()
    state.people[personId]?.current.place.arrival = new Date().getTime()
    state.people[personId]?.current.place.id = placeId
    state.people[personId]?.current.place.name = getNameOfPlaceWithId(placeId)
    handlePlaceChange(personId)
}

def setPersonVehicle(String personId) {
    // ** set person's current/previous presence after per vehicle presence updated in state in response to presence event **
    def vehiclesPresent = []
    def lastChanged = null
    logDebug("State of person per vehicle is ${state.people[personId].vehicles}", "Vehicles")
    def vehiclesList = []
    state.people[personId].vehicles.each { vehicleId, presenceInfo ->
        vehiclesList.add(vehicleId)
        logDebug("Presence Info for person ${personId} at vehicleId ${vehicleId} is ${presenceInfo.presence}.", "Vehicles")
        if (presenceInfo.presence.equals("present")) {
            logDebug("Adding vehicleId ${vehicleId} to vehiclesPresent", "Vehicles")
            vehiclesPresent.add(vehicleId)
        }
        if (lastChanged == null || state.people[personId].vehicles[lastChanged].atTime == null) lastChanged = vehicleId
        else if (presenceInfo.atTime > state.people[personId].vehicles[lastChanged].atTime) lastChanged = vehicleId
            
        logDebug("Checking for last changed presence event. Presence info for vehicleId ${vehicleId} last updated at ${presenceInfo.atTime}. LatestChanged event considered so far was for vehicleeId ${lastChanged} at ${state.people[personId].vehicles[lastChanged].atTime}", "Vehicles")
    }
    logDebug("vehicles list is ${vehiclesList}", "Vehicles")
    logDebug("vehicleIds of vehicles where present: ${vehiclesPresent}. vehicleId of lastChanged: ${lastChanged}")
    if (vehiclesPresent.size() == 0) {
        // just left a vehicle and not present at any other vehicle that has a presence sensor. Set to current vehicle null to indicate not present anywhere, if not already null

        logDebug("No presence sensors of vehicles present.", "Vehicles")
        if (didChangeVehicle(personId, null)) {
            logDebug("changing presence of personId: ${personId} to no vehicle", "Vehicles")
            changePersonVehicle(personId, null)
        }
    }
    else if (vehiclesPresent.size() == 1) {
        // either (i) just arrived at a vehicle after not being present anywhere or (ii) just left a vehicle after multiple presence sensors present
        
        // check if last presence sensor event was arrival or departure to detect case (i) or case (ii), respectively
        logDebug("Person ${personId} Only present at 1 vehicle", "Vehicles")
        if (state.people[personId].vehicles[lastChanged]?.presence.equals("present")) {
            // case (i)
            logDebug("Presence at that vehicle is the last presence event to occur", "Vehicles")
            if (lastChanged != vehiclesPresent[0]) log.warn "Mismatch from vehicles presence sensor logic. Debug."
            
            if (didChangeVehicle(personId, vehiclesPresent[0])) {
                logDebug("Vehicle of presence has changed, so updating current vehicle in person's state to vehicleId ${vehiclesPresent[0]}", "Vehicles")
                changePersonVehicle(personId, vehiclesPresent[0])
            }
        }
        else if (state.people[personId].vehicles[lastChanged]?.presence.equals("not present")) {
            // case (ii) 
            log.warn "Check to see if presence sensor for vehicleId ${vehiclesPresent[0]} is stuck."
            if (didChangeVehicle(personId, vehiclesPresent[0])) {
                logDebug("Vehicle of presence has changed, so updating current vehicle in person's state to vehicleId ${vehiclesPresent[0]}", "Vehicles")
                changePersonVehicle(personId, vehiclesPresent[0])
            }
        }
    }
    else {
        // present at multiple vehicles. Must resolve conflict between multiple presence sensors in the present state
        log.warn "Present at multiple vehicles, including vehicleIds ${vehiclesPresent}. Setting presence to the last vehicle where presence changed to present."
        if (didChangeVehicle(personId, lastChanged)) {
                logDebug("Vehicle of presence has changed, so updating current vehicle in person's state to vehicleId ${lastChanged}", "Vehicles")
                changePersonVehicle(personId, lastChanged)
            }
    }    
}

def didChangeVehicle(String personId, String vehicleId) {
    // check if vehicle presence actually changed
    if (state.people[personId]?.current.vehicle.id && state.people[personId]?.current.vehicle.id == vehicleId) {
        // location hasn't changed
        logDebug("Vehicle ID has not changed. Returning false from didChangeVehicle", "Vehicles")
        return false
    }
    else if (state.people[personId]?.current.vehicle.id == null && vehicleId == null) {
        logDebug("Still not present at any vehicle. Returning false from didChangeVehicle", "Vehicles")
        return false
    }
    else return true
}

def changePersonVehicle(String personId, String vehicleId) {    
    state.people[personId].previous.vehicle.id = state.people[personId]?.current.vehicle.id
    state.people[personId].previous.vehicle.arrival = state.people[personId]?.current.vehicle.arrival
    state.people[personId].previous.vehicle.departure = new Date().getTime()
    state.people[personId].current.vehicle.arrival = (vehicleId) ? new Date().getTime() : null
    state.people[personId].current.vehicle.id = (vehicleId) ? vehicleId : null
    logDebug("Changed Person ${personId} current vehicleID to ${(state.people[personId].current.vehicle.id) ? state.people[personId].current.vehicle.id : 'null'} with arrival at ${(state.people[personId].current.vehicle.arrival) ? state.people[personId].current.vehicle.arrival : 'null'}", "Vehicles")
    handleVehicleChange(personId)
}

def isLife360DeviceForPerson(String personId, device) {
     if (settings["person${personId}Life360"] && settings["person${personId}Life360"].getDeviceNetworkId() == device.getDeviceNetworkId()) return true
    return false
}

def life360AddressHandler(evt) {
    state.people?.each { personId, person ->
        if (isLife360DeviceForPerson(personId, evt.getDevice())) {
            logDebug("Before update life360 from life360AddressHandler, State of person per place is ${state.people[personId].places}", "Places")
            updateLife360(personId, evt.getDate().getTime()) 
            logDebug("After update life360 from life360AddressHandler, State of person per place is ${state.people[personId].places}", "Places")
            logDebug("Calling setPersonPlace from life360AddressHandler", "Places")
            setPersonPlace(personId)
        }
    }
}

def life360DrivingHandler(evt) {
    state.people?.each { personId, person ->
        if (isLife360DeviceForPerson(personId, evt.getDevice())) {
            state.people[personId]?.life360.isDriving = evt.value
            state.people[personId]?.life360.atTime = evt.getDate().getTime()
            handleDrivingChange(personId)
        }
    }    
}
                    
def isVehicleDeviceForPerson(String personId, String vehicleId, device) {
     if (settings["vehicle${vehicleId}Person${personId}Sensor"] && settings["vehicle${vehicleId}Person${personId}Sensor"].getDeviceNetworkId() == device.getDeviceNetworkId()) return true
    return false
}

def vehiclePresenceSensorHandler(evt) {
    for (vehicleId in state.vehicles) {
        state.people?.each { personId, person ->
            if (isVehicleDeviceForPerson(personId, vehicleId, evt.getDevice())) {
                state.people[personId].vehicles[vehicleId].presence = evt.value
                state.people[personId].vehicles[vehicleId].atTime = evt.getDate().getTime()
                setPersonVehicle(personId)
            }
        }
    }
}
                    
def isPlaceDeviceForPerson(personId, placeId, device) {
    logDebug("In isPlaceDeviceForPerson with sensor for person: ${settings["place${placeId}Person${personId}Sensor"]} and device: ${device}", "Places")
     if (settings["place${placeId}Person${personId}Sensor"] && settings["place${placeId}Person${personId}Sensor"].getDeviceNetworkId() == device.getDeviceNetworkId()) {
         log.debug "returning true from isPlaceDeviceForPerson"
         return true
     }
    else return false
}

def placePresenceSensorHandler(evt) {
    logDebug("In place presence sensor handler for event: ${evt.value} with device ${evt.getDevice()}", "Places")
    state.places.each { placeId, place ->
        state.people?.each { personId, person ->
            if (isPlaceDeviceForPerson(personId, placeId, evt.getDevice())) {
                state.people[personId].places[placeId].atTime = evt.getDate().getTime()
                state.people[personId].places[placeId].presence = evt.value
                logDebug("Set state for personId ${personId} to have a presence value of '${evt.value}' at placeId ${placeId} as of ${evt.getDate().getTime()}. Calling setPersonPlace from placePresenceSensorHandler", "Places")
                setPersonPlace(personId)
            }
        }
    }    
}

def uninstalled() {
    unsubscribe()
    unschedule()
    deleteAllTrackers()
}

def TripsPage() {
    dynamicPage(name: "TripsPage") {
        
         section {
             header()
            paragraph getInterface("header", " Manage Trips")
            if (state.trips) {
                state.trips.each { tripId, trip ->
                    paragraph '<table align=left border=0 margin=0 width=100%><tr><td align=center style:"width=50%;">' + formatImagePreview(getOriginIcon(tripId)) + '</td><td align=center style:"width=50%;">' + formatImagePreview(getDestinationIcon(tripId)) + '</td></tr><tr><td align=center style:"width=100%;" colspan=2><font style="font-size:20px;font-weight: bold">' + getNameOfTripWithId(tripId) + '</font></td></tr><tr><td align=center style:"width=100%;" colspan=2><font style="font-size:15px;font-weight: normal">' + getDayStringOfTripWithId(tripId) + ' ' + getDepartureTimeOfTripWithId(tripId) + '</font></td></tr></table>', width: 4
                }
                paragraph getInterface("line", "")
            }
            
            if (state.addingTrip) {
                
                tripInput(state.lastTripID.toString())
                paragraph "<br>"
                input name: "submitNewTrip", type: "button", title: "Submit", width: 3
                input name: "cancelAddTrip", type: "button", title: "Cancel", width: 3
            }
            else if (state.deletingTrip) {
                input name: "tripToDelete", type: "enum", title: "Delete Trip:", options: getTripEnumList(), multiple: false, submitOnChange: true
                if (tripToDelete) input name: "submitDeleteTrip", type: "button", title: "Confirm Delete", width: 3
                input name: "cancelDeleteTrip", type: "button", title: "Cancel", width: 3
            }
            else if (state.editingTrip) {
                input name: "tripToEdit", type: "enum", title: "Edit Trip:", options: getTripEnumList(), multiple: false, submitOnChange: true
                if (tripToEdit) {
                    def tripId = getIdOfTripWithListName(tripToEdit)
                    if (tripId != null) {
                        state.editedTripId = tripId    // save the ID and name of the vehicle being edited in state
                        state.editedTripName = tripToEdit
                    }
                    else {
                        // just edited the trip's origin or destination so that tripToEdit no longer holds the same trip name. Need to update that.
                        tripId = state.editedTripId
                        def newTripName = getListNameOfTripWithId(tripId)
                        app.updateSetting("tripToEdit",[type:"enum",value:newTripName]) 
                        state.editedTripName = newTripName
                    }
                    tripInput(tripId)
                    paragraph "<br>"
                    input name: "submitEditTrip", type: "button", title: "Submit", width: 3            
                }
                
               // input name: "cancelEditTrip", type: "button", title: "Cancel", width: 3
            }            
            else {               
                input name: "addTrip", type: "button", title: "Add Trip", width: 3                
                if (state.trips) input name: "editTrip", type: "button", title: "Edit Trip", width: 3
                app.clearSetting("tripToEdit")
                if (state.trips) input name: "deleteTrip", type: "button", title: "Delete Trip", width: 3
                app.clearSetting("tripToDelete")
            } 
             footer()
        }
    }
}

def tripInput(String tripId) {
    paragraph getInterface("subHeader", " Configure Trip Logistics")
                input name: "trip${tripId}Origin", type: "enum", title: "Origin of Trip", required: true, submitOnChange: true, options: getPlacesEnumList()
                input name: "trip${tripId}Destination", type: "enum", title: "Destination of Trip", required: true, submitOnChange: true, options: getPlacesEnumList()    
    
                input name: "trip${tripId}People", type: "enum", title: "Traveler(s)", required: true, submitOnChange: true, options: getPeopleEnumList(), multiple: true 
                input name: "trip${tripId}Vehicles", type: "enum", title: "Vehicle(s)", required: false, submitOnChange: true, options: getVehiclesEnumList(), multiple: true 
                input name: "trip${tripId}Days", type: "enum", title: "Day(s) of Week", required: true, multiple:true, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]  
                input name: "trip${tripId}EarliestDepartureTime", type: "time", title: "Earliest Departure Time", required: true, width: 4, submitOnChange: false
    
                input name: "trip${tripId}LatestDepartureTime", type: "time", title: "Latest Departure Time", required: true, width: 4
                input name: "trip${tripId}TargetArrivalTime", type: "time", title: "Target Arrival", required: false, width: 4
                
                if (settings["trip${tripId}Origin"] && settings["trip${tripId}Destination"]) {
                    def routeOptions = getRouteOptions(tripId)
                    if (routeOptions != null) {
                        def optionEnum = []
                        routeOptions.each { index, option ->
                            optionEnum.add(option.route)
                         }
                        paragraph getInterface("subHeader", " Configure Preferred Route Biasing")
                         paragraph "Select a preferred route in order to bias route recommendations in favor of your preferred route."
                         input name: "trip${tripId}PreferredRoute", type: "enum", title: "Preferred Route", required: false, options: optionEnum, submitOnChange: true
                     if (settings["trip${tripId}PreferredRoute"]) {      
                         def routeOption = null
                         routeOptions.each { index, option ->
                             if (option.route.contains(settings["trip${tripId}PreferredRoute"])) routeOption = option
                         }
                         paragraph routeOption.steps
                    }
                    input name: "trip${tripId}PreferredRouteBias", type: "number", title: "Preferred Route Bias (mins)", required: false, width: 4
                    paragraph "Preferred Route Bias is how many minutes faster an alternate route must be in order to be recommended over your preferred route."
                    
                    }
                    else {
                        log.error "Error: Unable to Retrieve Route Options."
                        paragraph getInterface("error", "Error: Unable to Retrieve Route Options. Please check your Internet connection.")
                    }
                }

                paragraph getInterface("subHeader", " Configure Departure Automations")
               input name: "trip${tripId}BadTrafficPushDevices", type: "capability.notification", title: "Send Push Notification to these devices if there is bad traffic.", required: false, multiple: true, submitOnChange: true 
                input name: "trip${tripId}BadTrafficSwitches", type: "capability.switch", title: "Turn on these switches if there is bad traffic.", required: false, multiple: true, submitOnChange: true, width: 4 
                input name: "trip${tripId}DeparturePushDevices", type: "capability.notification", title: "Send Push Notification with recommended route to these devices upon departure", required: false, multiple: true, submitOnChange: true  
               input name: "trip${tripId}OnlyPushIfNonPreferred", type: "bool", title: "But Only if Non-Preferred Route Recommended?", required: false, submitOnChange: false 
                
                input name: "trip${tripId}DepartureSwitches", type: "capability.switch", title: "Upon departure, turn these Switches", required: false, multiple: true, submitOnChange: true, width: 4  
                input name: "trip${tripId}DepartureSwitchesOnorOff", type: "enum", title: "", options: ["on", "off"], required: false, multiple: false, submitOnChange: true, width: 3

                paragraph getInterface("subHeader", " Configure Arrival Automations")
                input name: "trip${tripId}ArrivalPushDevices", type: "capability.notification", title: "Send Push Notification about arrival to these devices", required: false, multiple: true, submitOnChange: true
                input name: "trip${tripId}ArrivalSwitches", type: "capability.switch", title: "Upon arrival, turn these switches", required: false, multiple: true, submitOnChange: true   
                input name: "trip${tripId}ArrivalSwitchesOnorOff", type: "enum", title: "", options: ["on", "off"], required: false, multiple: false, submitOnChange: true  
                
                paragraph getInterface("subHeader", " Configure Late Arrival Automations")
                
                input name: "trip${tripId}PushDevicesIfLate", type: "capability.notification", title: "Send Push Notification to these devices if going to be at least X minutes later than target arrival time", required: false, multiple: true, submitOnChange: true 
                input name: "trip${tripId}LateNotificationMins", type: "number", title: "Number of minutes late that triggers notification", required: false, submitOnChange: false     
    
                paragraph getInterface("subHeader", " Configure Failure to Arrive Automations")
                input name: "trip${tripId}FailedArrivalNotificationMins", type: "number", title: "Number (Y) of minutes late that triggers failed arrival automations", required: false, submitOnChange: false        
                input name: "trip${tripId}FailedArrivalPushDevices", type: "capability.notification", title: "Send Push Notification to these devices upon failure to arrive", required: false, multiple: true, submitOnChange: true                   
                input name: "trip${tripId}FailedArrivalSwitches", type: "capability.switch", title: "Upon failure to arrive, turn these switches", required: false, multiple: true, submitOnChange: true   
                input name: "trip${tripId}FailedArrivalSwitchesOnorOff", type: "enum", title: "", options: ["on", "off"], required: false, multiple: false, submitOnChange: true  
}

def clearTripSettings(String tripId) {
    app.removeSetting("trip${tripId}Origin")
    app.removeSetting("trip${tripId}Destination")
    app.removeSetting("trip${tripId}People")
    app.removeSetting("trip${tripId}Vehicles")
    app.removeSetting("trip${tripId}Days")
    app.removeSetting("trip${tripId}EarliestDepartureTime")
    app.removeSetting("trip${tripId}LatestDepartureTime")
    app.removeSetting("trip${tripId}TargetArrivalTime")   
    app.removeSetting("trip${tripId}PreferredRoute")  
    app.removeSetting("trip${tripId}PreferredRouteBias")  
    app.removeSetting("trip${tripId}BadTrafficPushDevices")  
    app.removeSetting("trip${tripId}BadTrafficSwitches")  
    app.removeSetting("trip${tripId}DeparturePushDevices")  
    app.removeSetting("trip${tripId}DepartureSwitchesOnorOff")  
    app.removeSetting("trip${tripId}ArrivalPushDevices")  
    app.removeSetting("trip${tripId}ArrivalSwitches")  
    app.removeSetting("trip${tripId}ArrivalSwitchesOnorOff")  
    app.removeSetting("trip${tripId}PushDevicesIfLate")  
    app.removeSetting("trip${tripId}LateNotificationMins")  
    app.removeSetting("trip${tripId}FailedArrivalNotificationMins") 
    app.removeSetting("trip${tripId}FailedArrivalPushDevices")  
    app.removeSetting("trip${tripId}FailedArrivalSwitches")  
    app.removeSetting("trip${tripId}FailedArrivalSwitchesOnorOff")  
}

def getTripEnumList() {
    def list = []
    if (state.trips) {
        state.trips.each { tripId, trip ->
            def tripName = getListNameOfTripWithId(tripId)
            list.add(tripName)
        }
    }
    return list
}


def getTripDescriptionList() {
    def list = ""
    if (state.trips) {
        state.trips.each { tripId, trip ->
            def tripName = getListNameOfTripWithId(tripId)
            list += tripName + "\n\t"
        }
    }
    if (list != "" && list.length() > 4) list = list.substring(0, list.length() - 2)
    return list
}

def getIdOfTripWithListName(name) {
    def id = null
    state.trips.each { tripId, trip ->
        def tripName = getListNameOfTripWithId(tripId)
        if (tripName == name) id = tripId
    }
    logDebug("Returning id = ${id} for trip list name ${name}", "Trips")
    if (id == null) log.warn "No Trip Found With the List Name: ${name}"
    return id       
}

def getListNameOfTripWithId(tripId) {
    return settings["trip${tripId}Origin"] + " to " + settings["trip${tripId}Destination"] + ': ' + getDayStringOfTripWithId(tripId) + ' ' + getDepartureTimeOfTripWithId(tripId)
}

def getIdOfTripWithName(name) {
    def id = null
    state.trips.each { tripId, trip ->
        def tripName = getNameOfTripWithId(tripId)
        if (tripName == name) id = tripId
    }
    logDebug("Returning id = ${id} for trip name ${name}", "Trips")
    if (id == null) log.warn "No Trip Found With the Name: ${name}"
    return id   
}

def addTrip(String id) {
    if (!state.trips) state.trips = [:]
    def tripMap = [routes: null, areRoutesBiased: null, routesAsOf: null, averageTrafficDelay: null, numSamplesForAverage: null]
    state.trips[id] = tripMap
}

def getNameOfTripWithId(String id) {
    return settings["trip${id}Origin"] + " to " + settings["trip${id}Destination"]
}


def getDepartureTimeOfTripWithId(String id) {
    def dateObj = toDateTime(settings["trip${id}EarliestDepartureTime"])
    def depTime = extractTimeFromDate(dateObj)
    return depTime
}

def getDayStringOfTripWithId(String id) {
    def days = ""
    if (settings["trip${id}Days"].contains("Sunday")) days += "Sun"
    if (settings["trip${id}Days"].contains("Monday")) days += (days == "") ? "Mon" : "-Mon"
    if (settings["trip${id}Days"].contains("Tuesday")) days += (days == "") ? "Tue" : "-Tue"
    if (settings["trip${id}Days"].contains("Wednesday")) days += (days == "") ? "Wed" : "-Wed"
    if (settings["trip${id}Days"].contains("Thursday")) days += (days == "") ? "Thu" : "-Thu"
    if (settings["trip${id}Days"].contains("Friday")) days += (days == "") ? "Fri" : "-Fri"
    if (settings["trip${id}Days"].contains("Saturday")) days += (days == "") ? "Sat" : "-Sat"
    return days
}

String getOrigin(String id) {
    return settings["trip${id}Origin"]
}

String getOriginIdOfTrip(String tripId) {
    def originName = getOrigin(tripId)
    def originId = getIdOfPlaceWithName(originName)
    return originId
}

String getDestination(String tripId) {
    return settings["trip${tripId}Destination"]
}


String isPreferredRouteSet(String tripId) {
    return (settings["trip${tripId}PreferredRoute"]) ? true : false
}

def deleteTrip(nameToDelete) {
    def idToDelete = getIdOfTripWithListName(nameToDelete)
    if (idToDelete && state.trips) {       
        state.trips.remove(idToDelete)
        clearTripSettings(idToDelete)
    }
}

def getGeofenceRadiusSetting() {
     return geofenceRadius ? geofenceRadius : geofenceRadiusDefault   
}

def getFetchIntervalSetting() {
    return (fetchInterval) ? fetchInterval : fetchIntervalDefault
}

def getTripPreCheckMinsSetting() {
    return (tripPreCheckMins) ? tripPreCheckMins : tripPreCheckMinsDefault
}

def getPostArrivalDisplayMinsSetting() {
    return (postArrivalDisplayMins) ? postArrivalDisplayMins : postArrivalDisplayMinsDefault
}

def getCacheValidityDurationSetting() {
    return (cacheValidityDuration) ? cacheValidityDuration : cacheValidityDurationDefault
}

def getIsPreferredRouteDisplayedSetting() {
    return (isPreferredRouteDisplayed) ? isPreferredRouteDisplayed : isPreferredRouteDisplayedDefault
}

def getCircleBackgroundColorSetting() {
    return (circleBackgroundColor) ? circleBackgroundColor : circleBackgroundColorDefault
}

def getCircleScaleSetting() {
    return (circleScale) ? circleScale : circleScaleDefault
}

def getAvatarScaleSetting() {
    return (avatarScale) ? avatarScale : avatarScaleDefault
}

def getSleepMetricsDisplayMinsSetting() {
    return (sleepMetricsDisplayMins) ? sleepMetricsDisplayMins : sleepMetricsDisplayMinsDefault
}


def getTextColorSetting() {
    return (textColor) ? textColor : textColorDefault
}
                   
Integer gettrafficDelayThresholdSetting() {
    return (trafficDelayThreshold) ? trafficDelayThreshold*60 : trafficDelayThresholdDefault*60             
}
                   
Integer getLateNotificationMinsSetting(tripId) {
    return (settings["trip${tripId}LateNotificationMins"]) ? settings["trip${tripId}LateNotificationMins"] : LateNotificationMinsDefault             
}

Integer getFailedArrivalNotificationMinsSetting(tripId) {
    return (settings["trip${tripId}FailedArrivalNotificationMins"]) ? settings["trip${tripId}FailedArrivalNotificationMins"] : FailedArrivalNotificationMinsDefault             
}


def getTimeFormatSetting() {
    return (timeFormat) ? timeFormat : timeFormatDefault
}
def getApiKey() {
     return api_key   
}

def instantiateToken() {
     if(!state.accessToken){	
         //enable OAuth in the app settings or this call will fail
         createAccessToken()	
     }   
}

def initializeTrackers() {
    if (state.people) {
        state.people.each { personId, person ->
            def networkID = getTrackerId(personId)
            def child = getChildDevice(networkID)
            if (!child) createTracker(personId, settings["person${personId}Name"])
            updateTracker(personId)
        }
    }
}

def getTrackerId(String personId) {
    return "MultiPlaceTracker${personId}"
}

def createTracker(String personId, personName)
{
    def networkID = getTrackerId(personId)
    def child = addChildDevice("lnjustin", "Multi-Place Tracker", networkID, [label:"${personName} Multi-Place Tracker", isComponent:true, name:"${personName} Multi-Place Tracker"])
    if (child) child.setPersonId(personId)
}

def updateTrackerName(String personId) {
     def networkID = getTrackerId(personId)
    def child = getChildDevice(networkID)
    if (child) {
        def personName = getNameOfPersonWithId(personId)
        def newName = "${personName} Multi-Place Tracker"
        child.name = newName
        child.label = newName
    }  
}

def getTracker(personId) {
    def networkID = getTrackerId(personId)
    return getChildDevice(networkID)
}

def deleteTracker(personId)
{
    def networkID = getTrackerId(personId)
    def child = getChildDevice(networkID)
    if (child) {
        deleteChildDevice(networkID)
    }
}

def deleteAllTrackers() {
    if (state.people) {
        state.people.each { personId, person ->
            deleteTracker(personId)
        }
    }
}

def getSleepDataSvg(String personId) {
    def svg = ""
    def sleepData = getSleepData(personId)
    if (sleepData?.score) {
        def sleepColor = "black"
        logDebug("Sleep quality: " + sleepData?.quality, "Sleep")
        if (sleepData?.quality == "Restless") sleepColor = "#ee6c5c"  // red
        else if (sleepData?.quality == "Average") sleepColor = "#f2b14d"       // orange
        else if (sleepData?.quality == "Restful") sleepColor = "#73d49f"   // green
        logDebug("Sleep color: " + sleepColor, "Sleep")
            
        def coloredSleep = state.images.sleep["Moon"]
        def colored = coloredSleep.replace("currentColor",sleepColor)
        
      //  logDebug("colored sleep svg is ${groovy.xml.XmlUtil.escapeXml(colored)}", "Sleep")
            
        svg += '<svg width="20" height="38" z-index="5" x="5" y="-7" viewBox="0 0 20 20" overflow="visible" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">'
        svg += '<style>'
        svg += '.moon {fill:' + sleepColor + '}'
        svg += '</style>'
        svg += '<g style="path { fill:' + sleepColor + '}">'
        svg += colored
        svg += '</g>'
        svg += '<text x="14" y="9" alignment-baseline="middle" style="font:bold 11px Oswald,sans-serif;" text-anchor="middle" fill="' + getTextColorSetting() + '">' + sleepData?.score + '</text>'
        if (sleepData.winner) {
            svg += '<svg x="0.5" y="14" viewBox="0 0 20 20">'
            svg += state.images.sleep["Ribbon"]
            svg += '</svg>'
        }
        svg += '</svg>'
   }   
 //   logDebug("sleep svg is ${groovy.xml.XmlUtil.escapeXml(svg)}", "Sleep")
    return svg
}

def fetchSleepTracker() {
    logDebug("Fetching Sleep Tracker", "Sleep")
    def personId = params.personId
    def svg = getSleepDataSvg(personId)
    render contentType: "image/svg+xml", data: svg, status: 200
}

def fetchTracker() {
  //  logDebug("Fetching Tracker")
    def personId = params.personId
    def trackerType = params.type    // tracker type of 'svg' nests all images inside an svg. tracker type of 'html' does not (outside images handled by calling method)
    
    if(!state.people.containsKey(personId)) return null
    
    Integer yOffset = 0
    Integer maskOffset = -45
    Integer yViewportOffset = 0
    if (trackerType == 'svg') {
        yOffset = 45
        yViewportOffset = 65
    }

    def circleColor = getCircleBackgroundColorSetting()
    def txtColor = getTextColorSetting()
        
    def placeOfPresenceById = getPlaceOfPresenceById(personId)
    def placeOfPresenceByName = getPlaceOfPresenceByName(personId)

    def presenceIcon = getPresenceIcon(personId, trackerType)  

    String svg = '<svg viewBox="0 0 120 ' + (55+yViewportOffset) + '" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">'
    
    svg += '<style>'
    svg +=   '.large { font: bold 14px Oswald,sans-serif; fill:' +  txtColor + '; }'
    svg +=   '.small { font: bold 11px Oswald,sans-serif; fill:' +  txtColor + '; }'
    svg += '</style>'
    svg += '<defs>'
    svg +=    '<mask id="mask1" x="0" y="0" width="100" height="110" >'
    svg +=         '<rect x="25" y="0" width="80" height="110" style="stroke:none; fill:white" />'
    svg +=     '</mask>'
    svg += '</defs>'
    
    def isInSleepWindow = isWithinSleepDisplayWindow(personId)
    
    if (!state.people[personId]?.sleepOnlySensor || (state.people[personId]?.sleepOnlySensor && isInSleepWindow) || settings["person${personId}Stars"] == true) {
        
        if (trackerType == 'svg') {
            svg += '<svg x="20" width="80" height="80" z-index="1">'
            svg += state.images.people[personId]
            svg += '</svg>'
        
            if (isInSleepWindow) {
                svg += getSleepDataSvg(personId) 
            }
            def sleepData = getSleepData(personId)
            if (sleepData.monthWinner) {            // display monthly trophy the whole day, irrespective of whether only in sleep display window
                svg += '<svg x="90" y="-12" width="25" height="50" z-index="1">'
                svg += state.images.sleep["Month Trophy"]
                svg += '</svg>'
            }
            else if (sleepData.weekWinner) {            // display weekly trophy the whole day, irrespective of whether only in sleep display window
                svg += '<svg x="90" y="-12" width="25" height="50" z-index="1">'
                svg += state.images.sleep["Week Trophy"]
                svg += '</svg>'
            }
        }
    }
    
    
    if (!state.people[personId]?.sleepOnlySensor && state.people[personId]?.current.trip.id != null) {
    // show current trip
           
        def tripId = state.people[personId]?.current.trip.id
        logDebug("Tracker for personId ${personId} with active tripID ${tripId}", "Trips")
        def bestRoute = getBestRoute(tripId)
           
        def isPreferred = isPreferredRoute(tripId, bestRoute.summary)
        def isPreferredRouteSet = isPreferredRouteSet(tripId)
           
        def routeAlert = false
        if (bestRoute.relativeTrafficDelay > gettrafficDelayThresholdSetting()) routeAlert = true
        if (isPreferredRouteSet && !isPreferred) routeAlert = true
        
        def conditionColor = "green"
        if (routeAlert) conditionColor = "red"
           
        svg += '<g mask="url(#mask1)" ' + (trackerType =="svg" ? '' : 'transform="translate(0,' + maskOffset+ ')"') + '>'
        svg +=     '<polygon z-index="1" points="0,50 20,64, 0,78 0,68 8,64 0,60" fill="' + conditionColor + '">'
        svg +=     '<animateTransform attributeName="transform" type="translate" attributeType="XML" calcMode="spline" values="0, 0; 0, 0; 120, 0; 120, 0" keyTimes="0; .2; .8; 1" keySplines=".5,.12,.36,.93;.5,.12,.36,.93;.5,.12,.36,.93" dur="3.5s" repeatCount="indefinite" additive="sum"/>'
        svg +=     '</polygon>'
        svg += '</g>'

        svg += '<circle z-index="2" cx="20" cy="' + (20+yOffset) + '" r="18" stroke-width="1" stroke="black" fill="gray"/>'
        
        if (trackerType == 'svg') {
            svg += '<svg z-index="3" x="7.5" y="51" width="25" height="25">'
            svg += presenceIcon
            
            svg += '</svg>'
        }
        
        svg += '<circle z-index="2" cx="100" cy="' + (20+yOffset) + '" r="18" stroke-width="1" stroke="black" fill="gray"/>'
        
        if (trackerType == 'svg') {
            svg += '<svg z-index="3" x="87.5" y="51" width="25" height="25">'
            svg += getDestinationIcon(tripId, trackerType)
            svg += '</svg>'
        }

        svg += '<circle z-index="2" cx="100" cy="' + (20+yOffset) + '" r="18" stroke-width="1" stroke="' + conditionColor + '" fill="none">'
        svg += '<animate attributeName="stroke-dasharray" values="56.5, 0, 56.5, 0; 0, 113, 0, 0; 0, 113, 0, 0" keyTimes="0; 0.5; 1" dur="3.5s" repeatCount="indefinite" />'
        svg += '</circle>'
  
        svg += '<circle z-index="2" cx="100" cy="' + (20+yOffset) + '" r="18" stroke-width="1" stroke="' + conditionColor + '" fill="none">'
        svg += '<animate attributeName="stroke-dasharray" values="0, 56.5, 0, 56.5; 0, 56.5, 0, 56.5; 0, 0, 113, 0" dur="3.5s" keyTimes="0; 0.5; 1" repeatCount="indefinite" />'
        svg += '</circle>'
          
           
        if (isPersonOnTrip(personId)) {
            // display ETA if already departed on trip

            def etaUtc = getEtaOfCurrentTrip(personId)
            def etaDate = new Date(etaUtc)
               
            def lateAlert = false
            def target = settings["trip${tripId}TargetArrivalTime"]
            if (target) {
                def targetArrival = toDateTime(target)
                if (etaDate.after(targetArrival)) {
                    def secondsLate = getSecondsBetweenDates(targetArrival, etaDate)
                    def secsLateThreshold = getLateNotificationMinsSetting(tripId)*60
                    if (secondsLate >= secsLateThreshold) lateAlert = true
                }
            }
            svg += '<text height="10" width="120" text-align="center" text-anchor="middle" x="60" y="' + (55+yOffset) + '" class="large" fill="' + ((lateAlert) ? " red" : "black") + '">' + extractTimeFromDate(etaDate) + '</text>'
        }
        else {
            // otherwise display expected duration of trip
            svg += '<text height="10" width="120" text-align="center" text-anchor="middle" x="100" y="' + (trackerType == 'svg' ? '45' : '-20') + '" class="small"  fill="' + ((routeAlert) ? " red" : "black") + '">' + formatTimeMins(bestRoute.duration) + '</text>'

               if (!isPreferredRouteSet || (isPreferredRouteSet && !isPreferred) || (isPreferredRouteSet && isPreferred && getIsPreferredRouteDisplayedSetting())) {
                   svg += '<text height="10" width="120" text-align="center" text-anchor="middle" x="60" y="' + (55+yOffset) + '" class="small" fill="' + ((routeAlert) ? " red" : "black") + '">' + bestRoute.summary + '</text>'

               }      
           }
    }
     else if (!state.people[personId]?.sleepOnlySensor) { 
         def isPostArrival = isInPostArrivalDisplayWindow(personId)
         // in post arrival display window for a period of time after arrive at the destination of a trip, as long as the person is still at that destination.
         if (isPostArrival) {
             // while in post arrival display window, prioritize display of presence at the trip's destination, even if the person is still in the vehicle. 
             def destinationName = getDestination(state.people[personId]?.previous.trip.id)
             def destinationId = getIdOfPlaceWithName(destinationName)
             presenceIcon = state.images.places[destinationId]
         }
         svg += '<circle z-index="2" cx="100" cy="' + (20+yOffset) + '" r="18" stroke-width="1" stroke="black" fill="' + ((isPostArrival) ? "green" : "gray") + '"/>'
         
         if (trackerType == 'svg') {
             svg += '<svg z-index="3" x="87.5" y="51" width="25" height="25">'
             svg += presenceIcon
             svg += '</svg>'
         }
         
            
          if (isPostArrival) {
              // show arrival at destination of any previous trip for X minutes according to getPostArrivalDisplayMinsSetting()*60
              def tripId = state.people[personId]?.previous.trip.id
              def arrivalUTCTime = state.people[personId]?.previous.trip.arrivalTime
              def arrivalDateTime = new Date(arrivalUTCTime)
              def lateAlert = false
              def target = settings["trip${tripId}TargetArrivalTime"]
              if (target) {
                  def targetArrival = toDateTime(target)
                  if (arrivalDateTime.after(targetArrival)) {
                      def secondsLate = getSecondsBetweenDates(targetArrival, arrivalDateTime)
                      def secsLateThreshold = getLateNotificationMinsSetting(tripId)*60
                      if (secondsLate >= secsLateThreshold) lateAlert = true
                  }
              }
             svg += '<text height="10" width="120" text-align="center" text-anchor="middle" x="60" y="' + (55+yOffset) + '" class="large" fill="' + ((lateAlert) ? " red" : "black") + '">' + extractTimeFromDate(arrivalDateTime) + '</text>'
         }
         else if (placeOfPresenceById == null && placeOfPresenceByName != null) {
           //  svg += '<rect height="10" width="120" x="60" y="' + (55+yOffset) + '" style="fill:black"/>'
             svg += '<text height="10" width="120" text-align="center" text-anchor="middle" x="60" y="' + (55+yOffset) + '" class="small">' + placeOfPresenceByName + '</text>'
             
         }
     }
     svg += '</svg>'
  //   logDebug("svg is ${groovy.xml.XmlUtil.escapeXml(svg)}")
     render contentType: "image/svg+xml", data: svg, status: 200
}

def getPresenceIcon(String personId, trackerType=null) {
    def presenceIcon = null
    
    def placeOfPresenceById = getPlaceOfPresenceById(personId)
    def vehicleIdPresentIn = getIdOfVehiclePresentIn(personId)
    def isInBed = isInBed(personId)
    
    // generally, prioritize showing presence in bed, then vehicle, then place. Except when in post arrival display window, which is handled below
    if (isInBed) presenceIcon = trackerType == 'svg' ? state.images.sleep["Bed"] : getPathOfStandardIcon("Bed", "Sleep")
    else if (placeOfPresenceById != null) presenceIcon = trackerType == 'svg' ? state.images.places[placeOfPresenceById] : getPlaceIconById(placeOfPresenceById)    
    else if (vehicleIdPresentIn != null) presenceIcon = trackerType == 'svg' ? state.images.vehicles[vehicleIdPresentIn] : getVehicleIconById(vehicleIdPresentIn)     
    else presenceIcon = trackerType == 'svg' ? state.images.places["Generic"] : getPathOfStandardIcon("Generic", "Places")
    
    def isPostArrival = isInPostArrivalDisplayWindow(personId)
    // in post arrival display window for a period of time after arrive at the destination of a trip, as long as the person is still at that destination.
    if (isPostArrival) {
       // while in post arrival display window, prioritize display of presence at the trip's destination, even if the person is still in the vehicle. 
       def destinationName = getDestination(state.people[personId]?.previous.trip.id)
       def destinationId = getIdOfPlaceWithName(destinationName)
       presenceIcon = trackerType == 'svg' ? state.images.places[destinationId] : getPlaceIconById(destinationId)
    }
    
    return presenceIcon
}

def getDestinationIcon(String tripId, trackerType=null) {
    def destinationIcon = null
    def destinationName = getDestination(tripId)
    def destinationId = getIdOfPlaceWithName(destinationName)
    if (destinationId != null) destinationIcon = trackerType == 'svg' ? state.images.places[destinationId] : getPlaceIconById(destinationId)
    return destinationIcon
}


def getOriginIcon(String tripId, trackerType=null) {
    def originIcon = null
    def originName = getOrigin(tripId)
    def originId = getIdOfPlaceWithName(originName)
    if (originId != null) originIcon = trackerType == 'svg' ? state.images.places[originId] : getPlaceIconById(originId)
    return originIcon
}

def updateTracker(String personId) {
    //  The real update happens when the svg is fetched from the cloud endpoint. This just forces the hub into seeing an "event" in the Tracker device attribute that references the svg at the cloud enpdoint.
    if (!state.refreshNum) state.refreshNum = 0
    state.refreshNum++
        
    def personAvatar = getPersonAvatar(personId)
    def presenceIcon = getPresenceIcon(personId)
    def tripId = state.people[personId]?.current.trip.id
    def destinationIcon = null
    if (tripId != null) destinationIcon = getDestinationIcon(tripId)
    
    def trackerType = state.trackerType        // sets tracker type based on whether all images are SVGs
    
    // commented-out-code sets tracker type based on whether images used at any given time are all SVGs. Pending delete if decide not to do it this way.
  //  if(!isSVG(personAvatar) || !isSVG(presenceIcon) || (destinationIcon != null && !isSVG(destinationIcon))) trackerType = 'html'
        
  //  trackerType = 'html'  // uncomment line for debugging; forces display of either svg or html version of tracker
    
    def trackerUrl = getTrackerEndpoint(personId, trackerType) + '&version=' + state.refreshNum
    
    String content = ""
    if (trackerType == 'svg') {
        content += '<div style="width:100%;position:relative; z-index:0; text-align: center; display: block;">'
        content +=     '<div style="positive:absolute; width: 100%; height: 100%; z-index:1;text-align: center;display: block;">'
        content +=         '<img src="' + trackerUrl + '"/>'
        content +=     '</div>'
        content += '</div>'
    }
    else if (trackerType == 'html') {
        logDebug("Outputing HTML version of tracker")
      //  content += '<head><meta http-equiv="refresh" content="10"></head>'
        content += '<style>'
        content += 'body { margin: 0; vertical-align: top; }'
        content += '.tracker { width: 100%; padding-top: 100%; vertical-align: top; position:relative; display:block;'
        content +=            'background-repeat: no-repeat; '

        def isInSleepWindow = isWithinSleepDisplayWindow(personId)
        def sleepData = getSleepData(personId)
        def isWeekWinner = sleepData.weekWinner
        def isMonthWinner = sleepData.monthWinner
        def isWinner = isWeekWinner || isMonthWinner

        // DEBUG CODE
     //   isInSleepWindow = true
     //   isWinner = true
      //  isWeekWinner = true
     //   isMonthWinner = true
        
        def avatarSize = (getAvatarScaleSetting() / 100) * 65
        
        def trophy = ""
        if (isMonthWinner) trophy = "Month Trophy"
        else if (isWeekWinner) trophy = "Week Trophy"
        
        logDebug("isInSleepWindow: ${isInSleepWindow} isWinner: ${isWinner} for personID: ${personId}", "Sleep")
        
        if (state.people[personId]?.sleepOnlySensor ||  settings["person${personId}Stars"] == true) {
            if (isInSleepWindow || settings["person${personId}Stars"] == true) {
                content +=            'background-position:'
                if (isInSleepWindow) content += 'top 18% left 4%,'
                if (isWinner) content += 'top 25% right 8%,'
                for (def i = 0; i < state.people[personId]?.stars?.earned; i++) {
                    def starSize = 24
                    def starting = 50 - (starSize * state.people[personId]?.stars?.earned / 2) + (starSize/2)
                    def perc = starting + (i*starSize) // (i < halfway) ? 50-((i+1)*5) : 50+((i+1)*5)
                    content += "bottom 0% left ${perc}%,"
                }
                for (def i = 0; i < state.people[personId]?.accessories?.active.size(); i++) {
                    def accessory = getAccessory(state.people[personId]?.accessories?.active[i])
                    if (accessory.people[personId]?.top) content += "top ${accessory.people[personId]?.top}% "
                    else if (accessory.people[personId]?.bottom) content += "bottom ${accessory.people[personId]?.bottom}% "
                    if (accessory.people[personId]?.left) content += "left ${accessory.people[personId]?.left}%,"
                    else if (accessory.people[personId]?.right) content += "right ${accessory.people[personId]?.right}%,"
                }
                content +=            'center;'
                content +=            'background-size:'
                if (isInSleepWindow) content += '20%,'
                if (isWinner)  content += '20%,'
                for (def i = 0; i < state.people[personId]?.stars?.earned; i++) {
                    content += "17%,"
                }
                for (def i = 0; i < state.people[personId]?.accessories?.active.size(); i++) {
                    def accessory = getAccessory(state.people[personId]?.accessories?.active[i])
                    content += "${accessory.people[personId]?.scale}%,"
                }     
                content +=            avatarSize + '%;'
                content +=            'background-image:' 
                if (isInSleepWindow) {
                    content += 'url("' + getSleepTrackerEndpoint(personId) + '&version=' + state.refreshNum + '"),'
                }
                if (isWinner) {
                    content +=  'url("' + getPathOfStandardIcon(trophy,"Sleep") + '"),'
                }   
                for (def i = 0; i < state.people[personId]?.stars?.earned; i++) {
                    def icon = "Earned Star"
                    if (settings["person${personId}StarColor"] == "Red") {
                        icon = "Red Earned Star"
                    }
                    content += 'url("' + getPathOfStandardIcon(icon,"Stars") + '"),' 
                }
                for (def i = 0; i < state.people[personId]?.accessories?.active.size(); i++) {
                    def accessory = getAccessory(state.people[personId]?.accessories?.active[i])
                    content += 'url("' + accessory.path + '"),' 
                }             
                content +=            'url("' + personAvatar + '");'       
            }
        }
        else {
            if (tripId != null) {
                content +=            'background-position:'
                if (isInSleepWindow && isWinner) content += 'top 18% left 4%, top 25% right 8%,'
                else if (isInSleepWindow) content += 'top 18% left 4%,'
                else if (isWinner) content += 'top 25% right 8%,'
                for (def i = 0; i < state.people[personId]?.accessories?.active.size(); i++) {
                    def accessory = getAccessory(state.people[personId]?.accessories?.active[i])
                    if (accessory.people[personId]?.top) content += "top ${accessory.people[personId]?.top}% "
                    else if (accessory.people[personId]?.bottom) content += "bottom ${accessory.people[personId]?.bottom}% "
                    if (accessory.people[personId]?.left) content += "left ${accessory.people[personId]?.left}%,"
                    else if (accessory.people[personId]?.right) content += "right ${accessory.people[personId]?.right}%,"
                }
                content +=            'bottom 24% right 7.5%, bottom 24% left 8%, bottom 0% right 50%, center;'
                content +=            'background-size:'
                if (isInSleepWindow && isWinner) content += '20%,20%,'
                else if (isInSleepWindow) content += '20%,'
                else if (isWinner) content += '20%,'
                for (def i = 0; i < state.people[personId]?.accessories?.active.size(); i++) {
                    def accessory = getAccessory(state.people[personId]?.accessories?.active[i])
                    content += "${accessory.people[personId]?.scale}%,"
                } 
                content +=            '21%, 21%, 100% auto,' + avatarSize + '%;'
                content +=            'background-image:'
                if (isInSleepWindow && isWinner) {
                    content += 'url("' + getSleepTrackerEndpoint(personId) + '&version=' + state.refreshNum + '"),'
                    content += 'url("' + getPathOfStandardIcon(trophy,"Sleep") + '"),'
                }
                else if (isInSleepWindow) {
                    content += 'url("' + getSleepTrackerEndpoint(personId) + '&version=' + state.refreshNum + '"),'
                }
                else if (isWinner) {
                    content += 'url("' + getPathOfStandardIcon(trophy,"Sleep") + '"),'  
                }
                for (def i = 0; i < state.people[personId]?.accessories?.active.size(); i++) {
                    def accessory = getAccessory(state.people[personId]?.accessories?.active[i])
                    content += 'url("' + accessory.path + '"),' 
                }
                content +=            'url("' + destinationIcon + '"),'
                content +=            'url("' + presenceIcon + '"),'
                content +=            'url("' + trackerUrl + '"),' 
                content +=            'url("' + personAvatar + '");'
            }
            else {
                content +=            'background-position:'
                if (isInSleepWindow && isWinner) content += 'top 18% left 4%, top 25% right 8%,'
                else if (isInSleepWindow) content += 'top 18% left 4%,'
                else if (isWinner) content += 'top 25% right 8%,'
                for (def i = 0; i < state.people[personId]?.accessories?.active.size(); i++) {
                    def accessory = getAccessory(state.people[personId]?.accessories?.active[i])
                    if (accessory.people[personId]?.top) content += "top ${accessory.people[personId]?.top}% "
                    else if (accessory.people[personId]?.bottom) content += "bottom ${accessory.people[personId]?.bottom}% "
                    if (accessory.people[personId]?.left) content += "left ${accessory.people[personId]?.left}%,"
                    else if (accessory.people[personId]?.right) content += "right ${accessory.people[personId]?.right}%,"
                }
                content +=            'bottom 24% right 7.5%, bottom 0% right 50%, center;'
                content +=            'background-size:'
                if (isInSleepWindow && isWinner) content += '20%,20%,'
                else if (isInSleepWindow) content += '20%,'
                else if (isWinner) content += '22%,'
                for (def i = 0; i < state.people[personId]?.accessories?.active.size(); i++) {
                    def accessory = getAccessory(state.people[personId]?.accessories?.active[i])
                    content += "${accessory.people[personId]?.scale}%,"
                } 
                content +=            '21%, 100% auto,' + avatarSize + '%;'
                content +=            'background-image:'
                 if (isInSleepWindow && isWinner) {
                    content += 'url("' + getSleepTrackerEndpoint(personId) + '&version='
                    content += state.refreshNum + '"), url("' + getPathOfStandardIcon(trophy,"Sleep") + '"),'
                }
                else if (isInSleepWindow) {
                    content += 'url("' + getSleepTrackerEndpoint(personId) + '&version=' + state.refreshNum + '"),'
                }
                else if (isWinner) {
                    content += 'url("' + getPathOfStandardIcon(trophy,"Sleep") + '"),'  
                }
                for (def i = 0; i < state.people[personId]?.accessories?.active.size(); i++) {
                    def accessory = getAccessory(state.people[personId]?.accessories?.active[i])
                    content += 'url("' + accessory.path + '"),' 
                }
                content +=            'url("' + presenceIcon + '"),'
                content +=            'url("' + trackerUrl + '"),'
                content +=            'url("' + personAvatar + '");'   

            }
        }
        content += '}'
        content += '</style>'
        content += '<div class="tracker">'
        content += '</div>'
    }
  //  logDebug("html is ${groovy.xml.XmlUtil.escapeXml(content)}")
    def tracker = getTracker(personId)
    if (tracker) {
        tracker.sendEvent(name: 'tracker', value: content, displayed: true, isStateChange: true)   
        def placeOfPresenceByName = getPlaceOfPresenceByName(personId)
        tracker.sendEvent(name: 'place', value: placeOfPresenceByName)  
        def vehicleOfPresenceByName = getNameOfVehiclePresentIn(personId)
        def vehicle = vehicleOfPresenceByName ? vehicleOfPresenceByName : "None"
        tracker.sendEvent(name: 'vehicle', value: vehicle) 
        
        if (tripId != null) {
            def bestRoute = getBestRoute(tripId)
            if (bestRoute.relativeTrafficDelay > gettrafficDelayThresholdSetting()) {
                def durationMins = Math.round(bestRoute.duration / 60)
                def durationStr = (durationMins == 1) ? durationMins.toString() + " min" : durationMins.toString() + " mins"
                tracker.sendEvent(name: 'travelAlert', value: "${settings["trip${tripId}Origin"]} to ${settings["trip${tripId}Destination"]}: ${durationStr}. Take ${bestRoute.summary}, for ${bestRoute.eta} arrival.")
            }
            else tracker.sendEvent(name: 'travelAlert', value: "No Travel Alert")
        }
        else tracker.sendEvent(name: 'travelAlert', value: "No Travel Alert")
    }
}

def fetchHTMLTracker() {
    logDebug("Fetching HTML Tracker")
    def personId = params.personId
    
    if(!state.people.containsKey(personId)) return null
    
    def tracker = getTracker(personId)
    if (!tracker) return null
    def trackerValue = tracker.currentValue("tracker")
    render contentType: "text/html", data: trackerValue, status: 200


}

def getInterface(type, txt="", link="") {
    switch(type) {
        case "line": 
            return "<hr style='background-color:#555555; height: 1px; border: 0;'></hr>"
            break
        case "header": 
            return "<div style='color:#ffffff;font-weight: bold;background-color:#555555;border: 1px solid;box-shadow: 2px 3px #A9A9A9'> ${txt}</div>"
            break
        case "error": 
            return "<div style='color:#ff0000;font-weight: bold;'>${txt}</div>"
            break
        case "note": 
            return "<div style='color:#333333;font-size: small;'>${txt}</div>"
            break
        case "subField":
            return "<div style='color:#000000;background-color:#ededed;'>${txt}</div>"
            break     
        case "subHeader": 
            return "<div style='color:#000000;font-weight: bold;background-color:#ededed;border: 1px solid;box-shadow: 2px 3px #A9A9A9'> ${txt}</div>"
            break
        case "subSection1Start": 
            return "<div style='color:#000000;background-color:#d4d4d4;border: 0px solid'>"
            break
        case "subSection2Start": 
            return "<div style='color:#000000;background-color:#e0e0e0;border: 0px solid'>"
            break
        case "subSectionEnd":
            return "</div>"
            break
        case "boldText":
            return "<b>${txt}</b>"
            break
        case "link":
            return '<a href="' + link + '" target="_blank" style="color:#51ade5">' + txt + '</a>'
            break
    }
} 

def adjustTimeBySecs(time, Integer secs) {
    Calendar cal = Calendar.getInstance()
    cal.setTimeZone(location.timeZone)
    def dateTime = toDateTime(time)
    cal.setTime(dateTime)
    cal.add(Calendar.SECOND, secs)
    Date newDate = cal.getTime()
    return newDate
}


def getDateObjectFromUTCDt(utcDt) {
    return Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", utcDt)
}


def logDebug(msg, type="All") 
{
    if (logEnable && (logTypes == null || logTypes.contains(type)))
    {
        log.debug(msg)
    }
}

// ## Google Directions ##

def getGoogleMapsApiUrl() {
    return "https://maps.googleapis.com/maps/api/"
}

Boolean areRoutesToBeBiased(tripId) {
    if (settings["trip${tripId}PreferredRoute"] && settings["trip${tripId}PreferredRouteBias"]) {
        return true
    }
    else return false
}

def getTripWithRoutes(String tripId, Boolean doForceUpdate=false) {
    logDebug("Getting Routes.", "Trips")
    if (!isCacheValid(tripId) || doForceUpdate) { // if cache is valid, that means state.trips[tripId].routes already holds valid routes data. If the cache is invalid, or if an update is forced,  fetch and populate state.trips[tripId].routes with new routes data
        logDebug("Either Cached routes are invalid, or forcing update. Fetching routes from Google.", "Trips")
        def response = fetchRoutes(tripId)
        if (response) {
           state.trips[tripId].routes = [:]
            def routes = response.routes
            if (areRoutesToBeBiased(tripId)) {
                routes = biasRoutes(tripId, routes)
                state.trips[tripId].areRoutesBiased = true
            }
            else {
                state.trips[tripId].areRoutesBiased = false
            }
            for (Integer i=0; i<routes.size(); i++) {
                def route = routes[i]
                def summary = route.summary
                def duration = route.legs[0].duration_in_traffic?.value
                def trafficDelay = (route.legs[0].duration_in_traffic?.value - route.legs[0].duration?.value)
                def distance = route.legs[0].distance.text
                state.trips[tripId].routes[i.toString()] = [summary: summary, duration: duration, trafficDelay: trafficDelay, distance: distance]
            }
            state.trips[tripId].routesAsOf = new Date().getTime()
            def bestRouteTrafficDelay = state.trips[tripId].routes['0'].trafficDelay
            if (!state.trips[tripId].averageTrafficDelay) {
                state.trips[tripId].averageTrafficDelay = bestRouteTrafficDelay
                state.trips[tripId].numSamplesForAverage = 1
            }
            else {
                def oldAverage = state.trips[tripId].averageTrafficDelay
                def newSampleNum = state.trips[tripId].numSamplesForAverage + 1
                state.trips[tripId].averageTrafficDelay = ((oldAverage + ((bestRouteTrafficDelay - oldAverage) / newSampleNum)) as double).round(2)
                state.trips[tripId].numSamplesForAverage = newSampleNum
            }
            def relativeTrafficDelay = state.trips[tripId].routes['0'].trafficDelay - state.trips[tripId].averageTrafficDelay
            state.trips[tripId].routes['0'].relativeTrafficDelay = relativeTrafficDelay
            def etaDate = getETADate(state.trips[tripId].routes['0'].duration)
            state.trips[tripId].routes['0'].eta = extractTimeFromDate(etaDate)
        }
        else {
            log.warn "No response from Google Traffic API. Check connection."
        }
    }
    // now have state.trips[tripId] populated with routes, if wasn't already or if was stale
    return state.trips[tripId]
}

def getBestRoute(String tripId, Boolean doForceUpdate=false) {
    def routes = getTripWithRoutes(tripId, doForceUpdate).routes
    return routes['0']
}


def getSecondBestRoute(String tripId, Boolean doForceUpdate=false) {
    def routes = getTripWithRoutes(tripId, doForceUpdate).routes
    return routes['1']
}

// ### ACT METHODS ###
def act() {
    if (isPushNotification) sendPush()   
}



// ###  Route Info Methods  ###
def getRequiredDeparture(String tripId, duration) {
    if (settings["Trip${tripId}TargetArrivalTime"]) {
        def target = toDateTime(settings["Trip${tripId}TargetArrivalTime"])
        Calendar cal = Calendar.getInstance()
        cal.setTimeZone(location.timeZone)
        cal.set(Calendar.SECOND,(cal.get(Calendar.SECOND)-duration));
        //cal.subtract(Calendar.SECOND, duration)
        return cal.getTime() 
    }    
    else return null
}

def getETA(duration) {
    Calendar cal = Calendar.getInstance()
    cal.setTimeZone(location.timeZone)
    cal.add(Calendar.SECOND, duration)
    def arrival = cal.getTime()
    return arrival.toString()
}


def getETADate(duration) {
    Calendar cal = Calendar.getInstance()
    cal.setTimeZone(location.timeZone)
    cal.add(Calendar.SECOND, duration)
    return cal.getTime()
}

def biasRoutes(String tripId, unbiasedRoutes) {
    def preferredRouteIndex = -1
    def routeMap = [:]
    def biasedRoutes = [:]
    for (Integer i=0; i<unbiasedRoutes.size(); i++) {
         def route = unbiasedRoutes[i]
         def summary = route.summary
        if (isPreferredRoute(tripId, summary)) preferredRouteIndex = i
        routeMap[i] = route.legs[0].duration_in_traffic?.value
    }
    if (preferredRouteIndex <= 0) {
    // if preferred route is either not listed or is already the best route, no bias needed, so return unbiased routes
        return unbiasedRoutes
    }
    else if (preferredRouteIndex > 0) {
    // If preferred route is listed but is not the best route, bias ordering according to bias
        logDebug("Unbiased routes: ${routeMap}", "Trips")
        routeMap[preferredRouteIndex] -= (settings["trip${tripId}PreferredRouteBias"]*60)    // bias preferred route duration for ranking
        routeMap = routeMap.sort {it.value}                        // rank routes after biasing
        logDebug("Biased route map: ${routeMap}", "Trips")
        Integer rank = 0
        routeMap.each { i, j ->
            biasedRoutes[rank] = unbiasedRoutes[i]                // reorder routes according to new rank
            rank++
        }
        return biasedRoutes
    }
}

def isPreferredRoute(String tripId, route) {
    // TO DO: any better way to check for preferred route?
    if (settings["trip${tripId}PreferredRoute"] && route.contains(settings["trip${tripId}PreferredRoute"])) return true
    else return false
}

def getRouteOptions(String tripId) {
    def routeOptions = null
    def response = fetchRouteOptions(tripId)
    if(response) {
        routeOptions = [:]
        def routes = response.routes
        for (Integer i=0; i<routes.size(); i++) {
            def steps = routes[i].legs[0].steps
            def stepsText = ""
            def j=1
            for (step in steps) {
                def text = step.html_instructions.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", "")
                stepsText += j + "." + text + " "
                j++
            }
            routeOptions[i] = [route: routes[i].summary, steps: stepsText]
            logDebug("Route option ${i} = ${routeOptions[i]}", "Trips")
        }

    }
    return routeOptions
}

// ### API Methods ###

def fetchRoutes(String tripId) {
    def subUrl = "directions/json?origin=${getPlaceAddress(getOrigin(tripId))}&destination=${getPlaceAddress(getDestination(tripId))}&key=${getApiKey()}&alternatives=true&mode=driving&departure_time=now"   
    def response = httpGetExec(subUrl)
    return response
}

def fetchRouteOptions(tripId) {
    def subUrl = "directions/json?origin=${getPlaceAddress(getOrigin(tripId))}&destination=${getPlaceAddress(getDestination(tripId))}&key=${getApiKey()}&alternatives=true&mode=driving"   // Don't need traffic info for route options, so exclude for lower billing rate
    def response = httpGetExec(subUrl)
    return response
}

def geocode(String placeId) {
    
    def encodedAddress = java.net.URLEncoder.encode(getPlaceAddressById(placeId), "UTF-8")
    
    def subUrl = "geocode/json?address=${encodedAddress}&key=${getApiKey()}"   
    def response = httpGetExec(subUrl)
    return response
}

def httpGetExec(subUrl)
{
    try
    {
        getString = getGoogleMapsApiUrl() + subUrl
        httpGet(getString.replaceAll(' ', '%20'))
        { resp ->
            if (resp.data)
            {
                return resp.data
            }
            else {
                log.warn "No response from Google Traffic API. Check connection."
            }
        }
    }
    catch (Exception e)
    {
        log.warn "httpGetExec() failed: ${e.message}"
    }
}

// ### Cache Methods ###
def isCacheValid(String tripId) {
    def isCacheValid = false
    if (state.trips[tripId].routes && state.trips[tripId].routesAsOf && (getSecondsSince(state.trips[tripId].routesAsOf) <= getCacheValidityDurationSetting())) {
        logDebug("Routes cache is within the validity time window.", "Trips")
        if (areRoutesToBeBiased(tripId) && state.trips[tripId].areRoutesBiased) {
           isCacheValid = true
            logDebug("Cached routes are biased, and routes are to be biased, so return cache valid", "Trips")
        }
        else if (!areRoutesToBeBiased(tripId) && state.trips[tripId].areRoutesBiased == false) {
            isCacheValid = true
            logDebug("Cached routes are not biased, and routes are not to be biased, so return cache valid", "Trips")
        }
    }
    return isCacheValid
}

// ### Utility Methods ###
def getSecondsBetweenDates(Date startDate, Date endDate) {
    try {
        def difference = endDate.getTime() - startDate.getTime()
        return Math.round(difference/1000)
    } catch (ex) {
        log.error "getSecondsBetweenDates Exception: ${ex}"
        return 1000
    }
}

def getSecondsBetweenUTC(startDateUTC, endDateUTC) {
    try {
        def difference = endDateUTC - startDateUTC
        return Math.round(difference/1000)
    } catch (ex) {
        log.error "getSecondsBetweenUTC Exception: ${ex}"
        return 1000
    }
}

def getSecondsSince(utcDate) {
    try {
        def difference = new Date().getTime() - utcDate
        return Math.round(difference/1000)
    } catch (ex) {
        log.error "getSecondsSince Exception: ${ex}"
        return 1000
    }
}

def formatTime(duration) {
    def hours = (duration / 3600).intValue()
    def mins = ((duration % 3600) / 60).intValue()
    def secs = (duration % 60).intValue()
    return (hours > 0) ? String.format("%02d:%02d:%02d", hours, mins, secs) : String.format("%02d:%02d", mins, secs)
}


def formatTimeMins(duration) {
    def hours = (duration / 3600).intValue()
    def mins = ((duration % 3600) / 60).intValue()
    return String.format("%01d:%02d", hours, mins)
}


def extractTimeFromDate(Date date) {
    if (getTimeFormatSetting() == "12 Hour") return date.format("h:mm a")
    else if (getTimeFormatSetting() == "24 Hour") return date.format("H:mm")
}

def getImagePath() {
    return "https://raw.githubusercontent.com/lnjustin/App-Images/master/Multi-Place"
}

String getLogoPath() {
    return getImagePath() + "/logo.png"
}

String getLogoWithWordsPath() {
    return getImagePath() + "/MPlogo.png"
}


String getMPPath() {
    return getImagePath() + "/MP.png"
}

def getIconsEnum(String type) {
    def list = []
    def icons = null
    if (type == "Places") icons = standardPlacesIcons
    else if (type == "Vehicles") icons = standardVehiclesIcons
    else if (type == "People") icons = standardPeopleIcons
    icons.each { name, path ->
       list.add(name)
    }
    list.add("Custom")
    return list                  
}


def getPathOfStandardIcon(String name, type) {
    def iconPath = null
    if (type == "Places") iconPath = standardPlacesIcons[name]
    else if (type == "Vehicles") iconPath = standardVehiclesIcons[name]
    else if (type == "People") iconPath = standardPeopleIcons[name]
    else if (type == "Sleep") iconPath = standardSleepIcons[name]
    else if (type == "Unknown") iconPath = standardUnknownIcons[name]
    else if (type == "Stars") iconPath = standardStarIcons[name]
    else if (type == "Hats") iconPath = '/Hats/' + state.hatFiles[name]
    
    return getImagePath() + iconPath
}
            
@Field static standardPlacesIcons = [
	'Home': '/Places/home.svg',
	'Work': '/Places/work.svg',
    'School': '/Places/school.svg',
    'Church': '/Places/church.svg',
    'Gym': '/Places/gym.svg',
    'Generic': '/Places/generic.svg',
]

            
@Field static standardVehiclesIcons = [
	'Car': '/Vehicles/car.svg',
	'Minivan': '/Vehicles/minivan.svg',
]
      
@Field static standardPeopleIcons = [
	'Man': '/People/man.svg',
	'Woman': '/People/woman.svg',
]

@Field static standardSleepIcons = [
   'Bed': '/Sleep/bed.svg',
    'Moon': '/Sleep/moon.svg',
    'Ribbon': '/Sleep/ribbon.svg',
    'Week Trophy': '/Sleep/weekTrophy.svg',
    'Month Trophy': '/Sleep/monthTrophy.svg',
]

@Field static standardUnknownIcons = [
    'Light': '/Unknown/questionLight.svg',
    'Dark': '/Unknown/questionDark.svg',
    ]

@Field static standardStarIcons = [
   'Star': '/Stars/star.svg',
    'Earned Star': '/Stars/earnedStar.svg',
       'Red Star': '/Stars/starRed.svg',
    'Red Earned Star': '/Stars/earnedStarRed.svg'
]

