/*

    - Multi-Place Presence Tracker with Travel Advisor, Powered by Google Directions

    - 

 * TO DO: 
    - organize, comment code
     - handle scenario where user clicks "Add Place/Person/Vehicle", fills part of the form out, and then never clicks Submit or Cancel. That adds to the value of lastPerson/Place, etc. and causes problems when app loaded again
    - research issue of whether sleep competition scores would be cleared by a nap on Monday, after scores already cleared that morning
    - issue with delay when hit button
    - resize moon icon
    - decide if should start pre-trip check if already at the destinatoin - probably not
    - make submit button more prominent, so don't accidentally click Next page
    - link to JPG to SVG converter
    - link to SVG avatar creator
    - re-enable disabling of debug logging after 30 minutes

  * Future versions
    - add snoring indicator

  * TO CHECK:
    - check display when in pre-trip interval for displaying duration of trip and route
    - check bad traffic notification

  * TO DOCUMENT
    - repository for icons, so people can push built icons for sharing
    - README
    - instructions in app?
    - SVG vs. JPG, including JPG size restrictions, SVG margin requirements
    - note that SVGs are imported so hit DONE in app to re-import if SVG changed
    - Tracker options
        - use only SVG files. Compatible with Sharptools. Avoids long attribute values. Not limited to 1024 characters.
            - use cloud endpoint AND tracker (cloud endpoint requires refresh on the remote end, whereas tracker refreshes automatically)
        - use non-SVG files. Compatible with any dashboard that supports HTML (native Hubitat or Sharply). Limited to 1024 characters. Longer attribute values.
            - don't use cloud endpoint. pointless. use attribute instead.

    - License Terms
        - Permit derivative works for personal use, but not for distribution


    Attribution
    - Gym: Icons made by <a href="https://www.flaticon.com/free-icon/dumbbell_1159873" title="Kiranshastry">Kiranshastry</a> from <a href="https://www.flaticon.com/" title="Flaticon"> www.flaticon.com</a>

    - Work: <div>Icons made by <a href="https://www.flaticon.com/authors/pixel-perfect" title="Pixel perfect">Pixel perfect</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>

    - Home: <div>Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>

    - Church: <div>Icons made by <a href="http://www.freepik.com/" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>

    - School: <div>Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>

    - minivan: <div>Icons made by <a href="http://www.freepik.com/" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>

    - car: <div>Icons made by <a href="http://www.freepik.com/" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>

    - man & woman: <div>Icons made by <a href="https://www.flaticon.com/authors/vitaly-gorbachev" title="Vitaly Gorbachev">Vitaly Gorbachev</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>

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
    category: "Green Living",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    singleInstance: true,
    oauth: [displayName: "Multi-Place"],
    usesThirdPartyAuthentication: true)

@Field Integer fetchIntervalDefault = 5    // default number of minutes between checks of travel conditions during the departure window (does not apply to early checks before departure window)
@Field Integer tripPreCheckMinsDefault = 30    // default number of minutes before earliest departure time to check travel conditions
@Field Integer postArrivalDisplayMinsDefault = 10 // default number of minutes to display trip after arrival
@Field Integer cacheValidityDurationDefault = 120  // default number of seconds to cache directions/routes
@Field Integer optionsCacheValidityDurationDefault = 900
@Field String timeFormatDefault = "12 Hour"
@Field Boolean isPreferredRouteDisplayedDefault = false
@Field String circleBackgroundColorDefault = "#808080"
@Field String textColorDefault = "#000000"
@Field Integer trafficDelayThresholdDefault = 10
@Field Integer sleepMetricsDisplayMinsDefault = 60

@Field String checkMark = "https://raw.githubusercontent.com/lnjustin/App-Images/master/checkMark.svg"
@Field String xMark = "https://raw.githubusercontent.com/lnjustin/App-Images/master/xMark.svg"

mappings
{
    path("/multiplace/:personId/:type") { action: [ GET: "fetchTracker"] }
    path("/multiplaceSleep/:personId") { action: [ GET: "fetchSleepTracker"] }
}

def getTrackerEndpoint(String personId, trackerType='svg') {
    return getFullApiServerUrl() + "/multiplace/${personId}/${trackerType}?access_token=${state.accessToken}"
}

def getSleepTrackerEndpoint(String personId) {
    return getFullApiServerUrl() + "/multiplaceSleep/${personId}?access_token=${state.accessToken}"
}

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
     page name: "PeoplePage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "VehiclesPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "PlacesPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "TripsPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "RestrictionsPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "TravelAPIPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "TrackerPage", title: "", install: false, uninstall: false, nextPage: "mainPage"
     page name: "AdvancedPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
}

def mainPage() {
    dynamicPage(name: "mainPage") {
            section {
                
                if (state.people) {
                    href(name: "PeoplePage", title: "Manage People", description: getPeopleDescription(), required: false, page: "PeoplePage", image: checkMark)
                    href(name: "VehiclesPage", title: "Manage Vehicles", description: getVehiclesDescription(), required: false, page: "VehiclesPage", image: checkMark)
                    href(name: "PlacesPage", title: "Manage Places", description: getPlacesDescription(), required: false, page: "PlacesPage", image: checkMark)
                }
                else {
                    href(name: "PeoplePage", title: "Manage People", description: "Add a person to get started.", required: false, page: "PeoplePage", image: xMark)
                    href(name: "VehiclesPage", title: "Manage Vehicles", description: "Add at least one person before managing vehicles.", required: false, page: "", image: xMark)
                    href(name: "PlacesPage", title: "Manage Places", description: "Add at least one person before managing places.", required: false, page: "", image: xMark)
                }
                
            }
            
  			section {
                paragraph getInterface("header", " Manage Travel Advisor")
                if (!api_key) {
                    href(name: "TravelAPIPage", title: "Set Up Travel API Access", description: "Add API Access before managing trips.", required: false, page: "TravelAPIPage", image: xMark)
                }
                else {
                    href(name: "TripsPage", title: "Manage Trips", description: (state.trips ? getTripEnumList() : "No trips configured"), required: false, page: "TripsPage", image: (state.trips ? checkMark : xMark))
                    href(name: "RestrictionsPage", title: "Manage Mode-Based Restrictions", description: (restrictedModes ? getRestrictedModesDescription() : "No restrictions configured. Restrict Travel Advisor by Hub Mode."), required: false, page: "RestrictionsPage", image: (restrictedModes ? checkMark : xMark))
                    href(name: "TravelAPIPage", title: "Manage Travel API Access", description: "Travel API Access configured.", required: false, page: "TravelAPIPage", image: checkMark)
                }
            }
            section {
                paragraph getInterface("header", " Manage Settings")
                    href(name: "TrackerPage", title: "Manage Tracker Settings", required: false, page: "TrackerPage")
                    href(name: "AdvancedPage", title: "Manage Advanced Settings", required: false, page: "AdvancedPage")
			}
    }

}

def TravelAPIPage() {
    dynamicPage(name: "TravelAPIPage") {
        section {
            paragraph getInterface("header", " Manage Travel API Access")
            href(name: "GoogleApiLink", title: "Get Google API Key", required: false, url: "https://developers.google.com/maps/documentation/directions/get-api-key", style: "external")
            input name: "api_key", type: "text", title: "Enter Google API key", required: false, submitOnChange: true
        }
    }
}

def formatAvatarPreview(String imageUrl) {
    return '<div><img width="80px" border="0" style="max-width:100px" src="' + imageUrl + '"></div>'   
}

def PeoplePage() {
    dynamicPage(name: "PeoplePage") {
        section() {
                paragraph getInterface("header", " Manage People")
                if (state.people) {
                    paragraph getInterface("note", "After clicking 'Done' from the app's main page to apply any updates, click a person's avatar for accessing his or her graphical tracker via a cloud endpoint.")
                    state.people.each { id, person ->
                        def avatar = getPersonAvatar(id)
                        def avatarPreview = avatar ? avatar : getPathOfStandardIcon("Dark", "Unknown")
                        paragraph '<table width="100%"><tr><td align="center"><font style="font-size:20px;font-weight: bold"><a href="' + getTrackerEndpoint(id) + '" target="_blank"  style="color:black;">' + formatAvatarPreview(avatarPreview) + settings["person${id}Name"] + '</a></font></td></tr></table>', width:3
                    }
                }
                paragraph getInterface("line", "")
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
                
                input name: "person${state.lastPersonID}Life360", type: "device.Life360User", title: "Life360 Device", submitOnChange: false, multiple: false, required: false
                paragraph getInterface("note", "If the names of Places in ${app.name} are the same as those in Life360, this Person's presence in ${app.name} will follow his/her presence in Life360.")
                input name: "person${state.lastPersonID}SleepSensor", type: "device.WithingsSleepSensor", title: "Withings Sleep Sensor", submitOnChange: false, multiple: false, required: false
                paragraph getInterface("subHeader", " Presence Sensors for Person")
                if (state.vehicles) {
                    for (vehicleId in state.vehicles) {
                        input name: "vehicle${vehicleId}Person${state.lastPersonID}Sensor", type: "capability.presenceSensor", title: settings["vehicle${vehicleId}Name"] + " Presence Sensor", description: "Presence Sensor for this person's presence in the " + settings["vehicle${vehicleId}Name"] + " vehicle", submitOnChange: true, multiple: false, required: false
                    }
                }
                
                if (state.places) {
                    for (placeId in state.places) {
                        input name: "place${placeId}Person${state.lastPersonID}Sensor", type: "capability.presenceSensor", title: "${settings["place${placeId}Name"]} Presence Sensor", description: "Presence Sensor for this person's presence at " + settings["place${placeId}Name"], submitOnChange: true, multiple: false, required: false
                    }
                }

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
                    input name: "person${id}Life360", type: "device.Life360User", title: "Life360 Device", submitOnChange: true, multiple: false, required: false
                    input name: "person${id}SleepSensor", type: "device.WithingsSleepSensor", title: "Withings Sleep Sensor", submitOnChange: false, multiple: false, required: false
                paragraph getInterface("subHeader", " Presence Sensors for Person")
                    if (state.vehicles) {
                        for (vehicleId in state.vehicles) {
                            input name: "vehicle${vehicleId}Person${id}Sensor", type: "capability.presenceSensor", title: settings["vehicle${vehicleId}Name"] + " Presence Sensor", description: "Presence Sensor for this person's presence in the " + settings["vehicle${vehicleId}Name"] + " vehicle", submitOnChange: true, multiple: false, required: false
                        }
                    }
                
                    if (state.places) {
                        for (placeId in state.places) {
                            input name: "place${placeId}Person${id}Sensor", type: "capability.presenceSensor", title: "${settings["place${placeId}Name"]} Presence Sensor", description: "Presence Sensor for this person's presence at " + settings["place${placeId}Name"], submitOnChange: true, multiple: false, required: false
                        }
                    }
                }
                input name: "submitEditPerson", type: "button", title: "Done", width: 3
            }            
            else {     

                input name: "addPerson", type: "button", title: "Add Person", width: 3                
                if (state.people) input name: "editPerson", type: "button", title: "Edit Person", width: 3
                app.clearSetting("personToEdit")
                if (state.people) input name: "deletePerson", type: "button", title: "Delete Person", width: 3
                app.clearSetting("personToDelete")
            } 
        }
    }
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
    def life360Map = [address: null, atTime: null, isDriving: null]
    def sleepMap = [presence: null, presenceAtTime: null, score: null, quality: null, sleepDataAtTime: null, winner: null, weekWinCount: null, weekWinner: null, monthWinCount: null, monthWinner: null]
    def personMap = [current: currentMap, previous: previousMap, life360: life360Map, places: null, vehicles: null, sleep: sleepMap]
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
    }
    logDebug("Sleep display window ${inWindow ? "is valid" : "has passed"} with secsPassed = ${secsPassed}")
    return inWindow
}

def isInBed(String personId) {
    def isInBed = false
    if (state.people[personId]?.sleep.presence == "present") isInBed = true
    return isInBed
}

def hasSleepSensor(String personId) {
    def hasSensor = false
    if (settings["person${id}SleepSensor"]) hasSensor = true
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
    logDebug("Getting place of presence for person ${personId}")
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
    
    app.clearSetting("person${personId}Name")
    app.clearSetting("person${personId}Avatar")
    app.clearSetting("person${personId}AvatarCustom")
    app.updateSetting("person${personId}Life360",[type:"device",value:null]) 
    app.updateSetting("person${personId}SleepSensor",[type:"device",value:null]) 
    
    if (state.vehicles) {
        for (vehicleId in state.vehicles) { 
            app.updateSetting("vehicle${vehicleId}Person${personId}Sensor",[type:"capability",value:[]])
        }
    }
    
    if (state.places) {
        for (placeId in state.places) {
             app.updateSetting("place${placeId}Person${personId}Sensor",[type:"capability",value:[]])       
        }
    }
    
    if (state.trips) {
        state.trips.each { tripId, trip ->
            def newTripPeople = []
            for (tripPerson in settings["trip${tripID}People"]) {
                if (tripPerson != personNameToDelete) newTripPeople.add(tripPerson)
            }
            app.updateSetting("trip${tripID}People",[type:"enum",value:newTripPeople])
        }
    }
    
}

void resetAddEditState() {
    state.addingPerson = false
    state.editingPerson = false
    state.deletingPerson = false
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
            paragraph getInterface("header", " Manage Vehicles")
            if (state.vehicles) {  
                paragraph 
                for (id in state.vehicles) {
                    paragraph '<table width="100%" border="0" style="float:right;"><tr><td align="center">' + formatImagePreview(getVehicleIconById(id)) + '<font style="font-size:20px;font-weight: bold">' + settings["vehicle${id}Name"] + '</font></td></tr></table>', width: 3
                }
            }
            paragraph getInterface("line", "")
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
                }
                input name: "submitEditVehicle", type: "button", title: "Done", width: 3
               // input name: "cancelEditVehicle", type: "button", title: "Cancel", width: 3
            }            
            else {               
                input name: "addVehicle", type: "button", title: "Add Vehicle", width: 3                
                if (state.vehicles) input name: "editVehicle", type: "button", title: "Edit Vehicle", width: 3
                app.clearSetting("vehicleToEdit")
                if (state.vehicles) input name: "deleteVehicle", type: "button", title: "Delete Vehicle", width: 3
                app.clearSetting("vehicleToDelete")
            } 
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
    
    app.clearSetting("vehicle${vehicleId}Name")
    app.clearSetting("vehicle${vehicleId}Icon")
    app.clearSetting("vehicle${vehicleId}IconCustom")
    if (state.people) {
        state.people.each { personId, person ->
            app.updateSetting("vehicle${vehicleId}Person${personId}Sensor",[type:"capability",value:[]])
        }
    }
    
    if (state.trips) {
        state.trips.each { tripId, trip ->
            def newTripVehicles = []
            for (tripVehicle in settings["trip${tripID}Vehicles"]) {
                if (tripVehicle != vehicleNameToDelete) newTripVehicles.add(tripVehicle)
            }
            app.updateSetting("trip${tripID}Vehicles",[type:"enum",value:newTripVehicles])
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
    for (id in state.vehicles) {
        if (settings["vehicle${id}Name"] == name) return id
    }
    log.warn "No Vehicle Found With the Name: ${name}"
    return null
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
            paragraph getInterface("header", " Manage Places")
            if (state.places) {                
                for (id in state.places) {
                    paragraph '<table width=100% border=0 style="float:right;"><tr><td align=center>' + formatImagePreview(getPlaceIconById(id)) + '<font style="font-size:20px;font-weight: bold">' + settings["place${id}Name"] + '</font></td></tr></table>', width: 3
                }
            }
            paragraph getInterface("line", "")
            if (state.addingPlace) {
                paragraph getInterface("subHeader", "Add New Place")
                input name: "place${state.lastPlaceID}Name", type: "text", title: "Unique Name", submitOnChange: false, required: true
                paragraph getInterface("note", "Name Places in ${app.name} the the same as those in any associated Life360 account, in order for presence in ${app.name} to follow presence in Life360.")
                                
                input name: "place${state.lastPlaceID}Icon", type: "enum", options: getIconsEnum("Places"), title: "Place Icon", submitOnChange: true, multiple: false, required: true, width: 5
                def placeIcon = settings["place${state.lastPlaceID}Icon"]
                if (placeIcon != null) {
                    def iconPath = placeIcon == "Custom" ? settings["place${state.lastPlaceID}IconCustom"] : getPathOfStandardIcon(settings["place${state.lastPlaceID}Icon"], "Places")
                    paragraph formatImagePreview(iconPath), width: 2
                }
                if (settings["place${state.lastPlaceID}Icon"] == "Custom") {
                    input name: "place${state.lastPlaceID}IconCustom", type: "text", title: "URL to Custom Icon", submitOnChange: true, required: true
                }
                                
                input name: "place${state.lastPlaceID}Address", type: "text", title: "Address", submitOnChange: false, required: false, description: "*Address Required for Travel Advisor"
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
                    
                    input name: "place${placeId}Address", type: "text", title: "Address", submitOnChange: true, required: false, description: "*Address Required for Travel Advisor"
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
                }
                input name: "submitEditPlace", type: "button", title: "Done", width: 3
              //  input name: "cancelEditPlace", type: "button", title: "Cancel", width: 3
            }            
            else {               
                input name: "addPlace", type: "button", title: "Add Place", width: 3                
                if (state.places) input name: "editPlace", type: "button", title: "Edit Place", width: 3
                app.clearSetting("placeToEdit")
                if (state.places) input name: "deletePlace", type: "button", title: "Delete Place", width: 3
                app.clearSetting("placeToDelete")
            } 

        }
    }
}

def clearPlaceSettings(String placeId) {
    app.clearSetting("place${placeId}Name")
    app.clearSetting("place${placeId}Icon")
    app.clearSetting("place${placeId}IconCustom")
    app.clearSetting("place${placeId}Address")
    
    if (state.people) {
       state.people.each { personId, person ->
                app.updateSetting("place${placeId}Person${personId}Sensor",[type:"capability",value:[]])                
        }
    }

    app.updateSetting("place${placeId}GarageDoor",[type:"capability",value:[]])
    app.updateSetting("place${placeId}ContactSensor",[type:"capability",value:[]])
    app.updateSetting("place${placeId}Switch",[type:"capability",value:[]])
    
    if (state.trips) {
        state.trips.each { tripId, trip ->
            if (settings["trip${tripID}Origin"]) app.clearSetting("trip${tripID}Origin")
            if (settings["trip${tripID}Destination"]) app.clearSetting("trip${tripID}Destination")
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
        for (id in state.places) {
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
    if (!state.places) state.places = []
    state.places.add(id)
}

String getIdOfPlaceWithName(String name) {
    for (id in state.places) {
        if (settings["place${id}Name"] == name) return id
    }
    return null
}

String getNameOfPlaceWithId(String id) {
    return settings["place${id}Name"]
}


String getIdOfPlaceWithAddress(String address) {
    for (id in state.places) {
        if (settings["place${id}Address"] == address) return id
    }
    return null
}

def deletePlace(String nameToDelete) {
    def idToDelete = getIdOfPlaceWithName(nameToDelete)
    if (idToDelete && state.places) {       
        state.places.removeElement(idToDelete)
        state.images.places[idToDelete] = null
        clearPlaceSettings(idToDelete)
    }
}



def RestrictionsPage() {
    dynamicPage(name: "RestrictionsPage") {
        section {
            paragraph getInterface("header", " Manage Restrictions")
            paragraph "Do not check travel conditions when..."
            input name: "restrictedModes",type: "mode", title: "The Location Mode is", multiple: true, required: false, width: 6
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
    
    return isRestricted  
}

def AdvancedPage() {
    dynamicPage(name: "AdvancedPage") {
        section {
            paragraph getInterface("header", " Advanced Settings")
            
            paragraph "${app.name} caches the response from Google Directions and considers the cached response valid for the duration selected here. Increasing the validity time reduces the number of API calls. Decreasing the validity time increases responsiveness to traffic fluctuations."
            input name: "cacheValidityDuration", type: "number", title: "Duration of Directions Cache (Secs)", required: false, defaultValue: cacheValidityDurationDefault
            paragraph "${app.name} also caches the response from Google Directions for showing the route options available in the app. Set for as long as a configuration session may last."
            input name: "optionsCacheValidityDuration", type: "number", title: "Duration of Options Cache (Secs)", required: false, defaultValue: optionsCacheValidityDurationDefault
            input name: "logEnable", type: "bool", title: "Enable debug logging for 30 minutes", defaultValue: false, submitOnChange: true
        }
    }   
}

def TrackerPage() {
    dynamicPage(name: "TrackerPage") {
        section {
            paragraph getInterface("header", " Tracker Settings")
            input name: "textColor", type: "text", title: "Text color", required: false, defaultValue: textColorDefault
            input name: "circleBackgroundColor", type: "text", title: "Circle background color", required: false, defaultValue: circleBackgroundColorDefault
            input name: "timeFormat", type: "enum", title: "Time Format", options: ["12 Hour", "24 Hour"], required: false, defaultValue: timeFormatDefault
            input name: "trafficDelayThreshold", type: "number", title: "Consider traffic bad when traffic delays arrival by how many more minutes than usual?", required: false, defaultValue: trafficDelayThresholdDefault
            input name: "isPreferredRouteDisplayed", type: "bool", title: "Display recommended route even if recommend preferred route?", required: false, defaultValue: isPreferredRouteDisplayedDefault
            input name: "fetchInterval", type: "number", title: "Interval (mins) for Checking Travel Conditions", required: false, defaultValue: fetchIntervalDefault
            input name: "tripPreCheckMins", type: "number", title: "Number of Minutes Before Earliest Departure Time to Check Travel Conditions", required: false, defaultValue: tripPreCheckMinsDefault
            input name: "postArrivalDisplayMins", type: "number", title: "Number of Minutes to Display Trip After Arrival", required: false, defaultValue: postArrivalDisplayMinsDefault
            input name: "sleepMetricsDisplayMins", type: "number", title: "Number of Minutes to Display New Sleep Score", required: false, defaultValue: sleepMetricsDisplayMinsDefault
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
    subscribeTriggers()
    scheduleTimeTriggers()
    initializePresence()
    initializeSleep()
    initializeSVGImages()
    initializeTrackers()
}

def initializeDebugLogging() {
    if (logEnable) runIn(1800, disableDebugLogging)
    
}

def disableDebugLogging() {
    logDebug("Disabling Debug Logging")
 //   app.updateSetting("logEnable",[value:"false",type:"bool"])
}

def initializeSleep() {
    if (state.people) {
        state.people.each { personId, person ->
            def sleepDevice = settings["person${personId}SleepSensor"]
            if (sleepDevice) {
                if (!state.people[personId]?.sleep.presence) {
                    state.people[personId]?.sleep.presence = sleepDevice.currentValue("presence")
                    state.people[personId]?.sleep.presenceAtTime = new Date().getTime()
                }
                if (!state.people[personId]?.sleep.score) {
                    state.people[personId]?.sleep.score = sleepDevice.currentValue("sleepScore")
                  //  state.people[personId]?.sleep.sleepDataAtTime = new Date().getTime()
                }
                if (!state.people[personId]?.sleep.quality) {
                    state.people[personId]?.sleep.quality = sleepDevice.currentValue("sleepQuality")
                  //  state.people[personId]?.sleep.sleepDataAtTime = new Date().getTime()
                }
            }
        }
    }
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
    
    state.images = [:]
    if (state.people) {
        state.images.people = [:]
        state.people.each { personId, person ->
            def imageUrl = getPersonAvatar(personId)
            if (isSVG(imageUrl)) {
                state.images.people[personId] = sanitizeSvg(imageUrl.toURL().text)
            }
        }
    }
    
    if (state.places) {
        state.images.places = [:]
        for (placeId in state.places) {
            def imageUrl = getPlaceIconById(placeId)
            if (isSVG(imageUrl)) {
                state.images.places[placeId] = sanitizeSvg(imageUrl.toURL().text)
            }
             
        }
    }
    
    if (state.vehicles) {
        state.images.vehicles = [:]
        for (vehicleId in state.vehicles) {
            def imageUrl = getVehicleIconById(vehicleId)
            if (isSVG(imageUrl)) {
                state.images.vehicles[vehicleId] = sanitizeSvg(imageUrl.toURL().text)
            }
             
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
}

def subscribeTriggers() {
    subscribePeople()
    subscribeVehicles()
    subscribePlaces()
}

def subscribePeople() {
    if (state.people) {
        state.people.each { id, person ->
             if (settings["person${id}Life360"]) {
                def life360Address = settings["person${id}Life360"].currentValue("address1")
                def timeOfPresence = (state.people[id].life360.address != null && state.people[id].life360.address.equals(life360Address) && state.people[id].life360.atTime != null) ? state.people[id].life360.atTime : new Date().getTime()                
                state.people[id].life360.address = life360Address
                state.people[id].life360.atTime = timeOfPresence
                subscribe(settings["person${id}Life360"], "address", life360AddressHandler)                 
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

def sleepScoreHandler() {
    state.people?.each { personId, person ->
        if (isSleepDeviceForPerson(personId, evt.getDevice())) {
            state.people[personId]?.sleep.score = evt.value
            state.people[personId]?.sleep.sleepDataAtTime = evt.getDate().getTime()
            settings["person${personId}SleepSensor"].currentValue("sleepQuality")  // grab sleep quality on assumption that updated at the same time
            if (haveMultipleSleepSensors()) updateSleepCompetition()  
            updateTracker(personId)
            schedule(getSleepMetricsDisplayMinsSetting(), clearSleepDisplay, [data: [personId: personId], overwrite: false])
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

def updateSleepCompetition() {
    // check if all sleep sensors have been updated today before competing 
    def allScoresUpdated = true
    def winner = [score: 0, personList: []] // list for ties
    state.people?.each { personId, person ->
        if (hasSleepSensor(personId)) {
            def sleepDataAtTime = state.people[personId]?.sleep.sleepDataAtTime
            if (sleepDataAtTime) {
                def midnight = new Date().clearTime()
                def midnightUtc = today.getTime()
                if (sleepDataAtTime < midnightUtc) {    // sleep data not updated today
                    allScoresUpdated = false
                }
                else {
                    if (state.people[personId]?.sleep.score > winner.score) {
                        winner = [score: state.people[personId]?.sleep.score, personList: [personId]]
                    }
                    else if (state.people[personId]?.sleep.score == winner.score) {
                        winner.personList.add(personId)
                    }
                }
            }
            else allScoresUpdated = false    // no sleep data at all yet
        }
    }
    
    if (allScoresUpdated) {
        
        Calendar cal = Calendar.getInstance()
        cal.setTimeZone(location.timeZone)
        cal.setTime(new Date())
        def dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        def dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        def lastDayOfMonth = cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH)
        def isWeekStart = dayOfWeek == Calendar.MONDAY ? true : false
        def isWeekEnd = dayOfWeek == Calendar.SUNDAY ? true : false
        def isMonthStart = dayOfMonth == 1 ? true : false
        def isMonthEnd = dayOfMonth == lastDayOfMonth ? true : false
        if (isWeekStart) clearWeeklyWinCount()
        if (isMonthStart) clearMonthlyWinCount()
    
        state.people?.each { personId, person ->
            if (winner.personList.contains(personId)) {
                state.people[personId]?.sleep.winner = true
                state.people[personId]?.sleep.weekWinCount++ 
                state.people[personId]?.sleep.monthWinCount++ 
            }
            else state.people[personId]?.sleep.winner = false
            updateTracker(personId)
        }
        
        if (isWeekEnd) {
            def weekWinner = [winCount: 0, personList: []] // list for ties
            state.people?.each { personId, person ->
                if (state.people[personId]?.sleep.weekWinCount > weekWinner.winCount) {
                        weekWinner = [winCount: state.people[personId]?.sleep.weekWinCount, personList: [personId]]
                }
                else if (state.people[personId]?.sleep.weekWinCount == weekWinner.winCount) {
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
                if (state.people[personId]?.sleep.monthWinCount > monthWinner.winCount) {
                        monthWinner = [winCount: state.people[personId]?.sleep.monthWinCount, personList: [personId]]
                }
                else if (state.people[personId]?.sleep.monthWinCount == monthWinner.winCount) {
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
    state.people?.each { personId, person ->
        state.people[personId]?.sleep.weekWinCount = 0
        state.people[personId]?.sleep.weekWinner = false
    }
}

def clearMonthlyWinCount() {
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
           for (placeId in state.places) {
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
        for (placeId in state.places) {
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
    for (placeId in state.places) {
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
    for (placeId in state.places) {
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
    logDebug("Starting Pre-Check for Trip ${tripId}")
    if (areDepartureConditionsMet(tripId, true) && !hasTripStarted(tripId)) {
        logDebug("Starting Trip Pre-Check for trip ${tripId}")
        for (personName in settings["trip${tripId}People"]) {
            def personId = getIdOfPersonWithName(personName)
            state.people[personId]?.current.trip.id = tripId
            state.people[personId]?.current.trip.departureTime = null      
            state.people[personId]?.current.trip.recommendedRoute = null
            state.people[personId]?.current.trip.eta = null
            state.people[personId]?.current.trip.hasPushedLateNotice = false
            updateTripPreCheck([tripId: tripId])
        }
    }
}

def updateTripPreCheck(data) {
    def tripId = data.tripId
    logDebug("In Update Trip Pre-Check for trip ${tripId}") 
    if (areDepartureConditionsMet(tripId, true) && !hasTripStarted(tripId)) {
        // perform check throughout the predeparture and departure window, until the trip starts (i.e., a person departs). Once a person departs on the trip, stop updating from this function.
         logDebug("Updating Trip Pre-Check for trip ${tripId}")   
        updateTrackersForTripPeople(tripId)
        pushLateNotification(tripId)
        badTrafficNotification(tripId)
        runIn(getFetchIntervalSetting()*60, "updateTripPreCheck", [data: [tripId: tripId]])
    }
    else {
        logDebug("Either departure window ended or trip started. No more updates for now.")
    }
}

def isBadTraffciNotificationConfigured(String tripId) {
    if (settings["trip${tripId}BadTrafficPushDevices"]) return true
    else return false
}

def badTrafficNotification(String tripId) {
    if (isBadTraffciNotificationConfigured(tripId)) {
        def bestRoute = getBestRoute(tripId)
        def relativeTrafficDelay = bestRoute?.trafficDelay - state.trips[tripId]?.averageTrafficDelay

        if (relativeTrafficDelay > gettrafficDelayThresholdSetting()) {
            def trafficDelayMins = Math.round(relativeTrafficDelay / 60)
            def trafficDelayStr = (trafficDelayMins == 1) ? trafficDelayMins.toString + " min" : trafficDelayMins.toString + " mins"
            def etaDate = getETADate(bestRoute.duration)
            def eta = extractTimeFromDate(etaDate)
            settings["trip${tripId}BadTrafficPushDevices"].deviceNotification("Bad traffic on your trip from ${settings["trip${tripId}Origin"]} to ${settings["trip${tripId}Destination"]}. Allow ${trafficDelayStr} more than usual. Best route as of now is ${bestRoute.summary}, for ${eta} arrival.")
            
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
    logDebug("Starting trip ${tripId} for person ${personId}")
    state.people[personId]?.current.trip.id = tripId
    state.people[personId]?.current.trip.departureTime = new Date().getTime()
    
    def bestRoute = getBestRoute(tripId, true) // force update of route information 
    state.people[personId]?.current.trip.eta = getETADate(bestRoute.duration).getTime()
     
    performDepartureActionsForTrip(personId, tripId)
    
    def tripTimeOut = bestRoute.duration * 2
    runIn(tripTimeOut, "timeOutTrip", [data: [personId: personId, tripId: tripId, originalDuration: bestRoute.duration]])
}

def timeOutTrip() {
    def tripId = data.tripId
    def personId = data.personId
    def originalDuration = data.originalDuration
    if (isPersonOnTrip(personId, tripId)) {
        if (isInVehicle(personId) || isDriving(personId)) {
            logDebug("Trip Timeout Postponed: Person ${personId} has not yet arrived at the destination of trip ${tripId} but is still  driving. Trip abort postponed.")
            runIn(originalDuration, "timeOutTrip", [data: [personId: personId, tripId: tripId, originalDuration: originalDuration]])
        }
        else {
            logDebug("Trip Timeout: Person ${personId} did not arrive at the destination of trip ${tripId} and is not currently driving. Trip being aborted.")
            abortCurrentTripForPerson(personId)
            updateTracker(personId)
        }
    }
}

def endCurrentTripForPerson(String personId) {
    logDebug("Ending trip ${state.people[personId]?.current.trip.id} for person ${personId}")
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
    logDebug("Aborting trip ${state.people[personId]?.current.trip.id} for person ${personId}")
             
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
    // TO DO: determine if anything to do here?
}

def endDepartureWindowHandler(data) {
    def tripId = data.tripId
    for (personName in settings["trip${tripId}People"]) {
        def personId = getIdOfPersonWithName(personName)
        if (state.people[personId]?.current.trip.id == tripId && state.people[personId]?.current.trip.departureTime == null) {
         // person never left on the trip, so cancel trip
            state.people[personId]?.current.trip.id = null
            state.people[personId]?.current.trip.departureTime = null      
            state.people[personId]?.current.trip.recommendedRoute = null 
            state.people[personId]?.current.trip.eta = null 
            state.people[personId]?.current.trip.hasPushedLateNotice = false
            updateTracker(personId)
        }
    }
}

def performPreDepartureActionsForTrip(String tripId) {
    pushRouteRecommendation(tripId)
}

def performDepartureActionsForTrip(String personId, String tripId) {
    logDebug("Performing Departure Actions for trip ${tripId}")
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
                def secsLateThreshold = settings["trip${tripId}LateNotificationMins"]*60
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
            def isPreferred = isPreferredRoute(tripId, bestRoute.summary)
        
            def nextBestRoute = getSecondBestRoute(tripId)
            def fasterBy = Math.round((nextBestRoute.duration - bestRoute.duration)/60)

            if (isPreferred && fasterBy < 0) {
                settings["trip${tripId}DeparturePushDevices"].deviceNotification("Take ${bestRoute.summary} as the preferred route, for ${eta} arrival. But ${nextBestRoute.summary} is ${fasterBy*-1} mins faster.")
            }
            else {
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
        logDebug("PersonId ${personId} Arrived on tripID ${state.people[personId]?.previous.trip.id} ${secsSinceArrival} seconds ago.")
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
        logDebug("Ending current trip for person ${personId}")
        endCurrentTripForPerson(personId)        
    }
    
        
    // Did the place change abort the person's current trip by arrival at the origin?
    if (isPersonOnTrip(personId) && atOriginOfCurrentTrip(personId)) {
        logDebug("Aborting current trip for person ${personId} since arrived back at the origin")
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
	if (settings["trip${tripId}People"].contains(personName)) {
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
    if (inDepartureWindow(tripId, isTripPreCheckIncluded) && !isRestricted()) return true
    else return false
}

Boolean inDepartureWindow(String tripId, Boolean isTripPreCheckIncluded=false) {
    Boolean inWindow = true
    
    // Day of Week Travel Check
    if(!isTripDay(tripId)) {
        logDebug("Trip Day Check Failed.")
	    inWindow = false
    }
    
    // Time Window
    if (settings["trip${tripId}EarliestDepartureTime"] && settings["trip${tripId}LatestDepartureTime"]) {
        def tripPreCheckTime = getPreCheckTime(tripId)
        logDebug("Trip PreCheck time for trip ${tripId} is ${tripPreCheckTime}. isTripPreCheckIncluded value is ${isTripPreCheckIncluded}")
        def windowStart = isTripPreCheckIncluded ? tripPreCheckTime : toDateTime(settings["trip${tripId}EarliestDepartureTime"])
        def windowEnd = toDateTime(settings["trip${tripId}LatestDepartureTime"])
        logDebug("Checking if in departure window which starts at ${windowStart} and ends at ${windowEnd} for trip ${tripId}")
        if(!timeOfDayIsBetween(windowStart, windowEnd, new Date(), location.timeZone)) {
            logDebug("Time Check Failed.")
            inWindow = false
        }
        else logDebug("Time Check Passed.")
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
    logDebug("Trip days for tripId ${tripId} are ${settings["trip${tripId}Days"]}.")
    if (settings["trip${tripId}Days"] == null) {
        logDebug("No trip days specified.")
        isTripDay = true // if no trip days specified, assume trip day
    }
	if (settings["trip${tripId}Days"].contains(dayOfTheWeek)) {
        logDebug("Trip days include ${dayOfTheWeek}.")
	    isTripDay = true
	}    
    logDebug("isTripDay is ${isTripDay}")
    return isTripDay
}
     
boolean atDestinationOfTrip(String personId, String tripId) {
    def atDest = false
    if (state.people[personId]?.current?.place?.id == getDestinationIdOfTrip(tripId)) {
        atDest = true
    }
    logDebug("Person ${personId} is ${(atDest) ? "" : "not "}at the destination of trip ${tripId}.")
    return atDest
}   
    
boolean atDestinationOfCurrentTrip(personId) {
    def atDest = false
    if (state.people[personId]?.current?.place?.id && state.people[personId]?.current?.trip?.id) {
        def tripId = state.people[personId]?.current?.trip?.id
        if (state.people[personId]?.current?.place?.id == getIdOfPlaceWithName(settings["trip${tripId}Destination"])) {
            atDest = true
        }
    } 
    logDebug("Person ${personId} is ${(atDest) ? "" : "not "}at the destination of their current trip.")
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
    logDebug("Person ${personId} is ${(atDest) ? "" : "not "} at the origin of their current trip.")
    return atOrigin
}

def handleVehicleChange(String personId) {
    // first update state based on impact of change to trip status
    logDebug("Handling Vehicle Change for person ${personId}")

    // did vehicle change start a trip?
    state.trips.each { tripId, trip ->
         if (inTripVehicle(personId, tripId) && areDepartureConditionsMet(tripId) && !isPersonOnTrip(personId, tripId) && !atDestinationOfTrip(personId, tripId)) {
             // assume trip just started because (i) got in a vehicle specified for the trip (ii) while departure conditions met; (iii) trip was not already in progress; and (iv) person is not already at the destination of the trip
             startTripForPerson(personId, tripId)             
         }
    }
    // did vehicle change end a trip?
    def currentTripId = state.people[personId]?.current.trip.id
    if (isPersonOnTrip(personId) && !inTripVehicle(personId, currentTripId) && !mostPreferredArrivalTriggersConfigured(personId, currentTripId) && acceptableArrivalTriggersConfigured(personId, currentTripId)) {
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
             if (isPersonOnTrip(personId) && areDepartureConditionsMet(tripId) && !isPersonOnTrip(personId, tripId)) {
                 // assume trip just started because (i) person scheduled for the trip just started driving (ii) while departure conditions met; (iii) and trip was not already in progress
                 startTripForPerson(personId, tripId)             
             }
        }
    }
    else {   // person just stopped driving
    // did driving change end a trip?
        def currentTripId = state.people[personId]?.current.trip.id
        if (isPersonOnTrip(personId) && !mostPreferredArrivalTriggersConfigured(personId, currentTripId) && acceptableArrivalTriggersConfigured(personId, currentTripId)) {
            // Stopping of driving state while the trip was in progress COULD mean that the trip just ended. But it could also mean that the person stopped on the way to the destination, but has not yet arrived. So, only detect arrival based on driving state stopping if it is the most acceptable way possible given the current sensor configuration.                
            endCurrentTripForPerson(personId) 
        }
    }
    
    // then update Tracker device (which will indicate any tripId stored at state.people[personId]?.current.trip.id as well as store, for a period of time, any tripId stored at state.people[personId]?.previous.trip.id)
    updateTracker(personId)
}

def setPersonPlace(String personId) {
    // ** set person's current/previous presence after per place presence updated in state in response to presence event **
    def placesPresent = []
    def lastChanged = null
    logDebug("State of person per place is ${state.people[personId].places}")
    def placesList = []
    state.people[personId].places.each { placeId, presenceInfo ->
        placesList.add(placeId)
        logDebug("Presence Info for person ${personId} at placeId ${placeId} is ${presenceInfo.presence}.")
        if (presenceInfo.presence.equals("present")) {
            logDebug("Adding placeId ${placeId} to placesPresent")
            placesPresent.add(placeId)
        }
        if (lastChanged == null || state.people[personId].places[lastChanged].atTime == null) lastChanged = placeId
        else if (presenceInfo.atTime > state.people[personId].places[lastChanged].atTime) lastChanged = placeId
            
        logDebug("Checking for last changed presence event. Presence info for placeId ${placeId} last updated at ${presenceInfo.atTime}. LatestChanged event considered so far was for placeId ${lastChanged} at ${state.people[personId].places[lastChanged].atTime}")
    }
    logDebug("places list is ${placesList}")
    logDebug("placeIds of places where present: ${placesPresent}. placeId of lastChanged: ${lastChanged}")
    if (placesPresent.size() == 0) {
        // just left a place and not present at any other place that has a presence sensor. Set to life360 address if available, or else set to null to indicate presence is unknown
        
        // NOTE: setting to life360 presence means a person won't be able to leave a place until life360 detects departure. But trip can be started sooner with detection that present in vehicle or with garage door
        
        def life360Address = state.people[personId].life360?.address
        logDebug("No presence sensors of places present. Life360 address is ${life360Address}")
        if (life360Address && didChangePlaceByName(personId, life360Address)) {
            logDebug("changing presence of personId: ${personId} to life360 address of ${life360Address}")
            changePersonPlaceByName(personId, life360Address)
        }
        else if (life360Address) {
            logDebug("Life360 address still valid. Nothing to do.")
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
        logDebug("Person ${personId} Only present at 1 place")
        if (state.people[personId].places[lastChanged]?.presence.equals("present")) {
            // case (i)
            logDebug("Presence at that place is the last presence event to occur")
            if (lastChanged != placesPresent[0]) logDebug("Mismatch from places presence sensor logic. Needs debugging.")
            
            if (state.people[personId].life360?.address != getNameOfPlaceWithId(placesPresent[0]) && state.people[personId].life360?.address !=  getPlaceAddressById(placesPresent[0])) {
                log.warn "Mismatch in presence for ${getNameOfPersonWithId(personId)}. Life360 indicates presence at ${state.people[personId].life360?.address} but presence sensor indicates he or she is present at ${getNameOfPlaceWithId(placesPresent[0])}"
            }
            // prioritize presence sensor presence over any life360 state, since presence sensor capable of combining info from multiple sources
            if (didChangePlaceById(personId, placesPresent[0])) {
                logDebug("Place of presence has changed, so updating current place in person's state to placeId ${placesPresent[0]}")
                changePersonPlaceById(personId, placesPresent[0])
            }
        }
        else if (state.people[personId].places[lastChanged]?.presence.equals("not present")) {
            // case (ii)    
            log.warn "Check to see if presence sensor for placeId ${placesPresent[0]} is stuck."
            if (didChangePlaceById(personId, placesPresent[0])) {
                logDebug("Place of presence has changed, so updating current place in person's state to placeId ${placesPresent[0]}")
                changePersonPlaceById(personId, placesPresent[0])
             }
        }
    }
    else {
        // present at multiple places. Must resolve conflict between multiple presence sensors in the present state
            log.warn "Present at multiple places, including placeIds ${placesPresent}. Setting presence to the last place where presence changed to present."
            if (didChangePlaceById(personId, lastChanged)) {
                logDebug("Place of presence has changed, so updating current place in person's state to placeId ${lastChanged}")
                changePersonPlaceById(personId, lastChanged)
             }
    }
}

def didChangePlaceByName(String personId, String placeName) {

    def placeIdByName = getIdOfPlaceWithName(placeName)
    def placeIdByAddress = getIdOfPlaceWithAddress(placeName)    // if life360 being used, placeName could be just an address
    
    logDebug("In didChangePlaceByName() for person ${personId} with placeName ${placeName}. placeIdByName is ${placeIdByName}. placeIdByAddress is ${placeIdByAddress}. And current place id is ${state.people[personId]?.current.place.id}")
    // check if place presence actually changed
    if (state.people[personId]?.current.place.id) {
        if (state.people[personId]?.current.place.id == placeIdByName || state.people[personId]?.current.place.id == placeIdByAddress) {
            // location hasn't changed
            logDebug("Current Place Id hasn't changed")
            return false
        }
    }
    else if (state.people[personId]?.current.place.name && state.people[personId]?.current.place.name.equals(placeName)) {
        // location hasn't changed
        logDebug("Current place name hasn't changed")
        return false
    }
    else if (state.people[personId]?.current.place.name == null && placeName == null) {
        // still at unknown place; no update to be done
        logDebug("Current place name is still null")
        return false
    }    
    return true
}

def didChangePlaceById(String personId, String placeId) {
    // check if place presence actually changed
    if (state.people[personId]?.current.place.id && state.people[personId]?.current.place.id == placeId) {
        // location hasn't changed
        logDebug("didChangePlaceById returning false")
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
    logDebug("State of person per vehicle is ${state.people[personId].vehicles}")
    def vehiclesList = []
    state.people[personId].vehicles.each { vehicleId, presenceInfo ->
        vehiclesList.add(vehicleId)
        logDebug("Presence Info for person ${personId} at vehicleId ${vehicleId} is ${presenceInfo.presence}.")
        if (presenceInfo.presence.equals("present")) {
            logDebug("Adding vehicleId ${vehicleId} to vehiclesPresent")
            vehiclesPresent.add(vehicleId)
        }
        if (lastChanged == null || state.people[personId].vehicles[lastChanged].atTime == null) lastChanged = vehicleId
        else if (presenceInfo.atTime > state.people[personId].vehicles[lastChanged].atTime) lastChanged = vehicleId
            
        logDebug("Checking for last changed presence event. Presence info for vehicleId ${vehicleId} last updated at ${presenceInfo.atTime}. LatestChanged event considered so far was for vehicleeId ${lastChanged} at ${state.people[personId].vehicles[lastChanged].atTime}")
    }
    logDebug("vehicles list is ${vehiclesList}")
    logDebug("vehicleIds of vehicles where present: ${vehiclesPresent}. vehicleId of lastChanged: ${lastChanged}")
    if (vehiclesPresent.size() == 0) {
        // just left a vehicle and not present at any other vehicle that has a presence sensor. Set to current vehicle null to indicate not present anywhere, if not already null

        logDebug("No presence sensors of vehicles present.")
        if (didChangeVehicle(personId, null)) {
            logDebug("changing presence of personId: ${personId} to no vehicle")
            changePersonVehicle(personId, null)
        }
    }
    else if (vehiclesPresent.size() == 1) {
        // either (i) just arrived at a vehicle after not being present anywhere or (ii) just left a vehicle after multiple presence sensors present
        
        // check if last presence sensor event was arrival or departure to detect case (i) or case (ii), respectively
        logDebug("Person ${personId} Only present at 1 vehicle")
        if (state.people[personId].vehicles[lastChanged]?.presence.equals("present")) {
            // case (i)
            logDebug("Presence at that vehicle is the last presence event to occur")
            if (lastChanged != vehiclesPresent[0]) log.warn "Mismatch from vehicles presence sensor logic. Debug."
            
            if (didChangeVehicle(personId, vehiclesPresent[0])) {
                logDebug("Vehicle of presence has changed, so updating current vehicle in person's state to vehicleId ${vehiclesPresent[0]}")
                changePersonVehicle(personId, vehiclesPresent[0])
            }
        }
        else if (state.people[personId].vehicles[lastChanged]?.presence.equals("not present")) {
            // case (ii) 
            log.warn "Check to see if presence sensor for vehicleId ${vehiclesPresent[0]} is stuck."
            if (didChangeVehicle(personId, vehiclesPresent[0])) {
                logDebug("Vehicle of presence has changed, so updating current vehicle in person's state to vehicleId ${vehiclesPresent[0]}")
                changePersonVehicle(personId, vehiclesPresent[0])
            }
        }
    }
    else {
        // present at multiple vehicles. Must resolve conflict between multiple presence sensors in the present state
        log.warn "Present at multiple vehicles, including vehicleIds ${vehiclesPresent}. Setting presence to the last vehicle where presence changed to present."
        if (didChangeVehicle(personId, lastChanged)) {
                logDebug("Vehicle of presence has changed, so updating current vehicle in person's state to vehicleId ${lastChanged}")
                changePersonVehicle(personId, lastChanged)
            }
    }    
}

def didChangeVehicle(String personId, String vehicleId) {
    // check if vehicle presence actually changed
    if (state.people[personId]?.current.vehicle.id && state.people[personId]?.current.vehicle.id == vehicleId) {
        // location hasn't changed
        logDebug("Vehicle ID has not changed. Returning false from didChangeVehicle")
        return false
    }
    else if (state.people[personId]?.current.vehicle.id == null && vehicleId == null) {
        logDebug("Still not present at any vehicle. Returning false from didChangeVehicle")
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
    logDebug("Changed Person ${personId} current vehicleID to ${(state.people[personId].current.vehicle.id) ? state.people[personId].current.vehicle.id : 'null'} with arrival at ${(state.people[personId].current.vehicle.arrival) ? state.people[personId].current.vehicle.arrival : 'null'}")
    handleVehicleChange(personId)
}

def isLife360DeviceForPerson(String personId, device) {
     if (settings["person${personId}Life360"] && settings["person${personId}Life360"].getDeviceNetworkId() == device.getDeviceNetworkId()) return true
    return false
}

def life360AddressHandler(evt) {
    state.people?.each { personId, person ->
        if (isLife360DeviceForPerson(personId, evt.getDevice())) {
            state.people[personId]?.life360.address = evt.value
            state.people[personId]?.life360.atTime = evt.getDate().getTime()
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
    logDebug("In isPlaceDeviceForPerson with sensor for person: ${settings["place${placeId}Person${personId}Sensor"]} and device: ${device}")
     if (settings["place${placeId}Person${personId}Sensor"] && settings["place${placeId}Person${personId}Sensor"].getDeviceNetworkId() == device.getDeviceNetworkId()) {
         log.debug "returning true from isPlaceDeviceForPerson"
         return true
     }
    else return false
}

def placePresenceSensorHandler(evt) {
    logDebug("In place presence sensor handler for event: ${evt.value} with device ${evt.getDevice()}")
    for (placeId in state.places) {
        state.people?.each { personId, person ->
            if (isPlaceDeviceForPerson(personId, placeId, evt.getDevice())) {
                state.people[personId].places[placeId].atTime = evt.getDate().getTime()
                state.people[personId].places[placeId].presence = evt.value
                logDebug("Set state for personId ${personId} to have a presence value of '${evt.value}' at placeId ${placeId} as of ${evt.getDate().getTime()}.")
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
            paragraph getInterface("header", " Manage Trips")
            if (state.trips) {
                state.trips.each { tripId, trip ->
                    paragraph '<table align=left border=0 margin=0 width=100%><tr><td align=center style:"width=50%;">' + formatImagePreview(getOriginIcon(tripId)) + '</td><td align=center style:"width=50%;">' + formatImagePreview(getDestinationIcon(tripId)) + '</td></tr><tr><td align=center style:"width=100%;" colspan=2><font style="font-size:20px;font-weight: bold">' + getNameOfTripWithId(tripId) + '</font></td></tr></table>', width: 3
                }
            }
            paragraph getInterface("line", "")
            if (state.addingTrip) {
                
                tripInput(state.lastTripID.toString())
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
                    def tripId = getIdOfTripWithName(tripToEdit)
                    if (tripId != null) {
                        state.editedTripId = tripId    // save the ID and name of the vehicle being edited in state
                        state.editedTripName = tripToEdit
                    }
                    else {
                        // just edited the trip's origin or destination so that tripToEdit no longer holds the same trip name. Need to update that.
                        tripId = state.editedTripId
                        def newTripName = getNameOfTripWithId(tripId)
                        app.updateSetting("tripToEdit",[type:"enum",value:newTripName]) 
                        state.editedTripName = newTripName
                    }
                    tripInput(tripId)
                                 
                }
                input name: "submitEditTrip", type: "button", title: "Submit", width: 3
               // input name: "cancelEditTrip", type: "button", title: "Cancel", width: 3
            }            
            else {               
                input name: "addTrip", type: "button", title: "Add Trip", width: 3                
                if (state.trips) input name: "editTrip", type: "button", title: "Edit Trip", width: 3
                app.clearSetting("tripToEdit")
                if (state.trips) input name: "deleteTrip", type: "button", title: "Delete Trip", width: 3
                app.clearSetting("tripToDelete")
            } 
        }

    }
}

def tripInput(String tripId) {
    paragraph getInterface("subHeader", " Configure Trip Logistics")
                input name: "trip${tripId}Origin", type: "enum", title: "Origin of Trip", required: true, submitOnChange: true, options: getPlacesEnumList()
                input name: "trip${tripId}Destination", type: "enum", title: "Destination of Trip", required: true, submitOnChange: true, options: getPlacesEnumList()    
    
                input name: "trip${tripId}People", type: "enum", title: "Traveler(s)", required: true, submitOnChange: true, options: getPeopleEnumList(), multiple: true 
                input name: "trip${tripId}Vehicles", type: "enum", title: "Vehicle(s)", required: true, submitOnChange: true, options: getVehiclesEnumList(), multiple: true 
                input name: "trip${tripId}Days", type: "enum", title: "Day(s) of Week", required: true, multiple:true, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]  
                input name: "trip${tripId}EarliestDepartureTime", type: "time", title: "Earliest Departure Time", required: false, width: 4, submitOnChange: false
    
                input name: "trip${tripId}LatestDepartureTime", type: "time", title: "Latest Departure Time", required: false, width: 4
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
}

def clearTripSettings(String tripId) {
    app.clearSetting("trip${tripID}Origin")
    app.clearSetting("trip${tripID}Destination")
    app.clearSetting("trip${tripID}People")
    app.clearSetting("trip${tripID}Vehicles")
    app.clearSetting("trip${tripID}Days")
    app.clearSetting("trip${tripID}EarliestDepartureTime")
    app.clearSetting("trip${tripID}LatestDepartureTime")
    app.clearSetting("trip${tripID}TargetArrivalTime")                
}

def getTripEnumList() {
    def list = []
    if (state.trips) {
        state.trips.each { tripId, trip ->
            def tripName = getNameOfTripWithId(tripId)
            list.add(tripName)
        }
    }
    return list
}

def getIdOfTripWithName(name) {
    def id = null
    state.trips.each { tripId, trip ->
        def tripName = getNameOfTripWithId(tripId)
        if (tripName == name) id = tripId
    }
    logDebug("Returning id = ${id} for trip name ${name}")
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

String getOrigin(String id) {
    return settings["trip${id}Origin"]
}

String getDestination(String tripId) {
    return settings["trip${tripId}Destination"]
}


String isPreferredRouteSet(String tripId) {
    return (settings["trip${tripId}PreferredRoute"]) ? true : false
}

def deleteTrip(nameToDelete) {
    def idToDelete = getIdOfTripWithName(nameToDelete)
    if (idToDelete && state.trips) {       
        state.trips.remove(idToDelete)
        clearTripSettings(idToDelete)
    }
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

def getSleepMetricsDisplayMinsSetting() {
    return (sleepMetricsDisplayMins) ? sleepMetricsDisplayMins : sleepMetricsDisplayMinsDefault
}


def getTextColorSetting() {
    return (textColor) ? textColor : textColorDefault
}
                   
Integer gettrafficDelayThresholdSetting() {
    return (trafficDelayThreshold) ? trafficDelayThreshold : trafficDelayThresholdDefault             
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

def createTracker(personId, personName)
{
    def networkID = getTrackerId(personId)
    def child = addChildDevice("lnjustin", "Multi-Place Tracker", networkID, [label:"${personName} Multi-Place Tracker", isComponent:true, name:"${personName} Multi-Place Tracker"])
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
        if (sleepData?.quality == "Restless") sleepColor = "#ee6c5c"  // red
        else if (sleepData?.quality == "Average") sleepColor = "#f2b14d"       // orange
        else if (sleepData?.quality == "Restful") sleepColor = "#73d49f"   // green
        svg += '<svg width="20" height="38" z-index="1" x="5" y="-7" viewBox="0 0 20 20" overflow="visible" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">'
        svg += '<style>'
        svg += '.moon {fill:' + sleepColor + '}'
        svg += '</style>'
        svg += '<g class="moon">'
        svg += state.images.sleep["Moon"]
        svg += '</g>'
        svg += '<text x="14" y="9" alignment-baseline="middle" style="font:bold 11px Oswald,sans-serif;" text-anchor="middle" fill="' + getTextColorSetting() + '">' + sleepData?.score + '</text>'
        if (sleepData.winner) {
            svg += '<svg x="0.5" y="14" viewBox="0 0 20 20">'
            svg += state.images.sleep["Ribbon"]
            svg += '</svg>'
        }
        svg += '</svg>'
   }   
    logDebug("svg is ${groovy.xml.XmlUtil.escapeXml(svg)}")
    return svg
}

def fetchSleepTracker() {
    logDebug("Fetching Sleep Tracker")
    def personId = params.personId
    def svg = getSleepDataSvg(personId)
    render contentType: "image/svg+xml", data: svg, status: 200
}

def fetchTracker() {
    logDebug("Fetching Tracker")
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
    
    if (trackerType == 'svg') {
        svg += '<svg x="20" width="80" height="80" z-index="1">'
        svg += state.images.people[personId]
        svg += '</svg>'
        
        if (isWithinSleepDisplayWindow(personId)) {
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
    
    if (state.people[personId]?.current.trip.id != null) {
    // show current trip
           
        def tripId = state.people[personId]?.current.trip.id
        logDebug("Tracker for personId ${personId} with active tripID ${tripId}")
        def bestRoute = getBestRoute(tripId)
        def relativeTrafficDelay = bestRoute.trafficDelay - state.trips[tripId].averageTrafficDelay
           
        def isPreferred = isPreferredRoute(tripId, bestRoute.summary)
        def isPreferredRouteSet = isPreferredRouteSet(tripId)
           
        def routeAlert = false
        if (relativeTrafficDelay > gettrafficDelayThresholdSetting()) routeAlert = true
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
                    def secsLateThreshold = settings["trip${tripId}LateNotificationMins"]*60
                    if (secondsLate >= secsLateThreshold) lateAlert = true
                }
            }
            svg += '<text height="10" width="120" text-align="center" text-anchor="middle" x="60" y="' + (50+yOffset) + '" class="large" fill="' + ((lateAlert) ? " red" : "black") + '">' + extractTimeFromDate(etaDate) + '</text>'
        }
        else {
            // otherwise display expected duration of trip
            svg += '<text height="10" width="120" text-align="center" text-anchor="middle" x="100" y="' + (trackerType == 'svg' ? '45' : '-20') + '" class="small"  fill="' + ((routeAlert) ? " red" : "black") + '">' + formatTimeMins(bestRoute.duration) + '</text>'

               if (!isPreferredRouteSet || (isPreferredRouteSet && !isPreferred) || (isPreferredRouteSet && isPreferred && getIsPreferredRouteDisplayedSetting())) {
                   svg += '<text height="10" width="120" text-align="center" text-anchor="middle" x="60" y="' + (50+yOffset) + '" class="small" fill="' + ((routeAlert) ? " red" : "black") + '">' + bestRoute.summary + '</text>'

               }      
           }
    }
     else { 
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
                      def secsLateThreshold = settings["trip${tripId}LateNotificationMins"]*60
                      if (secondsLate >= secsLateThreshold) lateAlert = true
                  }
              }
             svg += '<text height="10" width="120" text-align="center" text-anchor="middle" x="60" y="' + (50+yOffset) + '" class="large" fill="' + ((lateAlert) ? " red" : "black") + '">' + extractTimeFromDate(arrivalDateTime) + '</text>'
         }
         else if (placeOfPresenceById == null && placeOfPresenceByName != null) {
             svg += '<text height="10" width="120" text-align="center" text-anchor="middle" x="60" y="' + (50+yOffset) + '" class="small" fill="black">' + placeOfPresenceByName + '</text>' 
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
    else if (vehicleIdPresentIn != null) presenceIcon = trackerType == 'svg' ? state.images.vehicles[vehicleIdPresentIn] : getVehicleIconById(vehicleIdPresentIn)
    else if (placeOfPresenceById != null) presenceIcon = trackerType == 'svg' ? state.images.places[placeOfPresenceById] : getPlaceIconById(placeOfPresenceById) 
    else presenceIcon = trackerType == 'svg' ? state.images.unknown["Light"] : getPathOfStandardIcon("Light", "Unknown")
    
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
    
    def trackerType = 'svg'    
    if(!isSVG(personAvatar) || !isSVG(presenceIcon) || (destinationIcon != null && !isSVG(destinationIcon))) trackerType = 'html'
        
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
        content += '<style>'
        content += 'body { margin: 0; }'
        content += '.tracker { width: 100%; padding-top: 100%; position:relative; display:block;'
        content +=            'background-repeat: no-repeat;'

        def isInSleepWindow = isWithinSleepDisplayWindow(personId)
        def sleepData = getSleepData(personId)
        def isWeekWinner = sleepData.weekWinner
        def isMonthWinner = sleepData.monthWinner
        def isWinner = isWeekSleepWinner || isMonthSleepWinner

        // DEBUG CODE
      //  isInSleepWindow = true
      //  isWinner = true
      //  isWeekWinner = true
      //  isMonthWinner = true
        
        def trophy = ""
        if (isMonthWinner) trophy = "Month Trophy"
        else if (isWeekWinner) trophy = "Week Trophy"
        
        if (state.people[personId]?.current.trip.id != null) {
            content +=            'background-position: bottom 24% right 7.5%, bottom 24% left 8%, bottom 0% right 50%, center'
            if (isInSleepWindow && isWinner) content += ', top 18% left 4%, top 25% right 8%;'
            else if (isInSleepWindow) content += ', top 18% left 4%;'
            else if (isWinner) content += ', top 25% right 8%;'
            else content +=        ';'
            content +=            'background-size: 21%, 21%, 100% auto, 65%'
            if (isInSleepWindow && isWinner) content += ',20%,20%;'
            else if (isInSleepWindow) content += ',20%;'
            else if (isWinner) content += ',20%;'
            else content +=        ';'
            content +=            'background-image: url("' + destinationIcon + '"),'
            content +=            'url("' + presenceIcon + '"),'
            content +=            'url("' + trackerUrl + '"),' 
            content +=            'url("' + personAvatar + '")' + (isInSleepWindow ? "" : ";")
            if (isInSleepWindow && isWinner) {
                content += ',url("' + getSleepTrackerEndpoint(personId) + '&version=' + state.refreshNum + '")'
                content += ', url("' + getPathOfStandardIcon(trophy,"Sleep") + '");'
            }
            else if (isInSleepWindow) {
                content += ',url("' + getSleepTrackerEndpoint(personId) + '&version=' + state.refreshNum + '");'
            }
            else if (isWinner) {
                content += ', url("' + getPathOfStandardIcon(trophy,"Sleep") + '");'  
            }
            else content +=        ';'
        }
        else {
            content +=            'background-position: bottom 24% right 7.5%, bottom 0% right 50%, center'
            if (isInSleepWindow && isWinner) content += ', top 18% left 4%, top 25% right 8%;'
            else if (isInSleepWindow) content += ', top 18% left 4%;'
            else if (isWinner) content += ', top 25% right 8%;'
            else content +=        ';'
            content +=            'background-size: 21%, 100% auto, 65%'
            if (isInSleepWindow && isWinner) content += ',20%,20%;'
            else if (isInSleepWindow) content += ',20%;'
            else if (isWinner) content += ',22%;'
            else content +=        ';'
            content +=            'background-image:'
            content +=            'url("' + presenceIcon + '"),'
            content +=            'url("' + trackerUrl + '"),'
            content +=            'url("' + personAvatar + '")' + (isInSleepWindow ? "" : ";")      
            if (isInSleepWindow && isWinner) {
                content += ',url("' + getSleepTrackerEndpoint(personId) + '&version='
                content += state.refreshNum + '"), url("' + getPathOfStandardIcon(trophy,"Sleep") + '");'
            }
            else if (isInSleepWindow) {
                content += ',url("' + getSleepTrackerEndpoint(personId) + '&version=' + state.refreshNum + '");'
            }
            else if (isWinner) {
                content += ', url("' + getPathOfStandardIcon(trophy,"Sleep") + '");'  
            }
            else content +=        ';'
        }
        content += '}'
        content += '</style>'
        content += '<div class="tracker">'
        content += '</div>'
    }
    logDebug("html is ${groovy.xml.XmlUtil.escapeXml(content)}")
    def tracker = getTracker(personId)
    if (tracker) {
        tracker.sendEvent(name: 'tracker', value: content, displayed: true, isStateChange: true)   
        def placeOfPresenceByName = getPlaceOfPresenceByName(personId)
        tracker.sendEvent(name: 'place', value: placeOfPresenceByName)  
        def vehicleOfPresenceByName = getNameOfVehiclePresentIn(personId)
        def vehicle = vehicleOfPresenceByName ? vehicleOfPresenceByName : "None"
        tracker.sendEvent(name: 'vehicle', value: vehicle) 
    }
}

def getInterface(type, txt="") {
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


def logDebug(msg) 
{
    if (logEnable)
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
    logDebug("Getting Routes.")
    if (!isCacheValid(tripId) || doForceUpdate) { // if cache is valid, that means state.trips[tripId].routes already holds valid routes data. If the cache is invalid, or if an update is forced,  fetch and populate state.trips[tripId].routes with new routes data
        logDebug("Either Cached routes are invalid, or forcing update. Fetching routes from Google.")
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
                state.trips[tripId].averageTrafficDelay = oldAverage + ((bestRouteTrafficDelay - oldAverage) / newSampleNum)
                state.trips[tripId].numSamplesForAverage = newSampleNum
            }
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
        logDebug("Unbiased routes: ${routeMap}")
        routeMap[preferredRouteIndex] -= (settings["trip${tripId}PreferredRouteBias"]*60)    // bias preferred route duration for ranking
        routeMap = routeMap.sort {it.value}                        // rank routes after biasing
        logDebug("Biased route map: ${routeMap}")
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
            logDebug("Route option ${i} = ${routeOptions[i]}")
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
        logDebug("Routes cache is within the validity time window.")
        if (areRoutesToBeBiased(tripId) && state.trips[tripId].areRoutesBiased) {
           isCacheValid = true
            logDebug("Cached routes are biased, and routes are to be biased, so return cache valid")
        }
        else if (!areRoutesToBeBiased(tripId) && state.trips[tripId].areRoutesBiased == false) {
            isCacheValid = true
            logDebug("Cached routes are not biased, and routes are not to be biased, so return cache valid")
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
    
    return getImagePath() + iconPath
}
            
@Field static standardPlacesIcons = [
	'Home': '/Places/home.svg',
	'Work': '/Places/work.svg',
    'School': '/Places/school.svg',
    'Church': '/Places/church.svg',
    'Gym': '/Places/gym.svg',
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
