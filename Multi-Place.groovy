/*

    - Multi-Place Presence Tracker with Travel Advisor

 * TO DO: 
  - specify timeframes for "displaying" different routes/instances as the dashboard output. So, display home-to-work between 7-9am and work-to-home between 5-6pm. Maybe do this with a single child device that has an attribute describing the origin-destination pair at issue? Or one child device per vehicle (since same vehicle can't be headed to multiple destinations at the same time)?
  - Specify name of person driving, as well as the presence sensor to detect whether in car
  - 
  - give options for what to display on tile, either route to take or ETA
  - parent device that is generic across child devices, for retrieving a route between two addresses (could be used by any app)
  - in parent app, input places with addresses and icon, so child apps can select from those designated places
  - in parent app, select people and their associated presence devices (e.g., life 360 devices) indicating where they are located
  - in parent app, associate people with trips, so can say who is going where when

  - tile is person specific. displays whereabouts of a person, as well as travel information associated with that person. Select what presence sensors indicate person's presence at various places. May or may not use Life 360 app

  - if presence sensor for person specified, display "not left yet" on tile if hasn't left
        
   - tile per person (input for how many minutes before departure to display)
        - avatar left
        - when at certain place, display green place icon to right of avatar
        - when taking a trip, display origin place icon --> car (in green) --> destination place icon, with either (a) best route or (b) estimated ETA or (c) both, either all the time or alternate. Or "not left" if haven't left


    - At least once before departure window begins, and during departure window, (i) update tile with recommended route; (ii) check if need to leave before the departure window to arrive on time; (iii) send push notification or turn on/off switch if bad traffic (update tile with how many minutes in advance need to leave or countdown to departure); (v) send text if haven't left yet and going to miss the target arrival by X mins

    - tile options
        - display duration of presence?
    - action options
        - push notification or text if need to leave before departure window?
        - push notification or text with recommended route upon predeparture or departure?
            - only send if recommended route is not preferred route?
        - turn on/off switch or push button upon departure?
        - turn of/off switch or push button upon arrival?
        - push notificaiton or text if going to miss target arrival by X mins?
    
    - predeparture window starts
        - check if need to leave before the departure window to arrive on time. If so, advance departure window temporarily.
        - start displaying trip on tracker tile
            - display origin, vehicle, destination
            - display recommended route
            - display whether or not have departed
            - if departed, display ETA
    - departure window starts (but haven't departed yet)
        - send push notification or turn on/off switch if bad traffic (update tile with how many minutes in advance need to leave or countdown to departure)
        - turn on/off switch or push button if enabled
        - send text if haven't left yet and going to miss target arrival by X mins
    - predeparture sensors triggered (e.g., garage door opened) during departure window
        - send notification with route recommendation
        - but don't start trip yet (or should i let the user select an option re whether to start the trip when garage door opened?)
    - actual departure
        - update tracker tile to show departed and to show ETA
        - switch from checking for best route from origin to destination, to checking for best route from current location to destination, and notify if route has changed (although this could be tricky)
        - continue checking if going to be late and send text if so
    - departure window ends
        - stop displaying trip on tracker tile if never departed
    - arrival
        - update tracker tile to show arrival
        - turn on/off switch or push button upon arrival
        - stop displaying trip on tracker tile after X mins of arrival

    - When depart, (i) update tile with ETA; (ii) send push notification with recommended route if enabled; (iii) turn on/off switch or push button if enabled; (iv) send text to designated device if going to be late
    - If life360 device, continue to update tile and send push notification as route or ETA changes
    - When arrive, (i) update tile; (ii) turn on/off switch or push button if enabled

    - Detect depart when haven't departed yet and either (a) vehicle sensor detects person in vehicle within the depature window (b) (or, if origin is home, when garage door opens within departure window), or (c) when presence sensor or life360 detects left origin place within the departure window
    - Detect arrive when (a) presence sensor or door/switch detects arrive; or (b) life 360 detects at destination. If none of those triggers are set up, then detect arrive when no longer in car (but disadvantage that won't work if make a stop on the way). If also don't have a car sensor set up, then detect arrive when have departed and when time is estimated ETA

Action Options
    - option to send notification upon departure (or in advance of departure?)
    - option to send text to designated device if going to be late (upon departure or in advance of departure or certain amount of time before target arrival?)
    - option to turn on/off switch when depart or arrive

    - option to display ETA to home if don't have trip set up?

    - use Life360 isDriving field if no vehicle sensors set but life360 is set


--> Have different options for format of tile. That way, people can share any modifications to it and easily incorporate into the app
    

    - failsafe: stop checking traffic if haven't arrived after certain time (so doesn't keep making API calls if error happens)
    - tap on dashboard tile brings up larger display with map, address details, etc.


    TO DO:
        - Handle delete vehicle settings when delete person, and vice versa
        - Handle if leave before departure window?
            - at the start of the departure window, do i check to see if the person has already left the origin or if the origin was the last place left and it was fairly recently, suggesting that they left early?


    THINKING PAD
        - may show both place icon and vehicle icon if at both at the same time?
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
@Field Integer earlyFetchMinsDefault = 30    // default number of minutes before earliest departure time to check travel conditions
@Field Integer postArrivalDisplayMinsDefault = 10 // default number of minutes to display trip after arrival
@Field Integer cacheValidityDurationDefault = 120
@Field Integer optionsCacheValidityDurationDefault = 900

mappings
{
    path("/tile/:personId") { action: [ GET: "buildTile"] }
}

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
     page name: "PeoplePage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "VehiclesPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "PlacesPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "TripsPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "RestrictionsPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "TravelAPIPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
     page name: "AdvancedPage", title: "", install: false, uninstall: false, nextPage: "mainPage" 
}

def mainPage() {
    dynamicPage(name: "mainPage") {
            section {
                href(name: "PeoplePage", title: "Manage People", description: getPeopleDescription(), required: false, page: "PeoplePage")
                href(name: "VehiclesPage", title: "Manage Vehicles", description: getVehiclesDescription(), required: false, page: "VehiclesPage")
                href(name: "PlacesPage", title: "Manage Places", description: getPlacesDescription(), required: false, page: "PlacesPage")
            }
            
  			section {
                paragraph getInterface("header", " Manage Travel Advisor")
                if (!api_key) {
                    paragraph "Google API Key needed for Travel Advisory Functionality."
                    apiInput()
                }
                else {
                    href(name: "TripsPage", title: "Manage Trips", description: getTripEnumList(), required: false, page: "TripsPage")
                    href(name: "RestrictionsPage", title: "Manage Travel Advisor Restrictions", required: false, page: "RestrictionsPage")
                    href(name: "TravelAPIPage", title: "Manage Travel API Access", required: false, page: "TravelAPIPage")
                    href(name: "AdvancedPage", title: "Manage Advanced Settings", required: false, page: "AdvancedPage")
                }
			}
            
            section
            {
               paragraph getInterface("line","")

                input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
            }
	}
}

def TravelAPIPage() {
    dynamicPage(name: "TravelAPIPage") {
        section {
            paragraph getInterface("header", " Manage Travel API Access")
            apiInput()
        }
    }
}

def apiInput() {
    input name: "api_key", type: "text", title: "Enter Google API key", required: false, submitOnChange: true
}

def PeoplePage() {
    dynamicPage(name: "PeoplePage") {
        section {
            
            paragraph getInterface("header", " Manage People")
            if (state.people) {
                def i = 0
                def peopleDisplay = "<table border=0>"
                state.people.each { id, person ->
                    peopleDisplay += "" + ((i % 4 == 0) ? "<tr>" : "") + "<td align=center><img border=0 style='max-width:100px' src='${settings["person${id}Avatar"]}'><br><font style='font-size:20px;font-weight: bold'>${settings["person${id}Name"]}</font></td>" + ((i % 4 == 3) ? "</tr>" : "")
                    i++
                }
                peopleDisplay += "</table>"
                paragraph peopleDisplay
                paragraph getInterface("line", "")
            }

            if (state.addingPerson) {
                input name: "person${state.lastPersonID}Name", type: "text", title: "Unique Name", submitOnChange: true, required: true
                input name: "person${state.lastPersonID}Avatar", type: "text", title: "URL to Avatar Image", submitOnChange: false, required: true
                input name: "person${state.lastPersonID}Life360", type: "device.Life360User", title: "Life360 Device", submitOnChange: false, multiple: false, required: false
                paragraph getInterface("note", "If the names of Places in ${app.name} are the same as those in Life360, this Person's presence in ${app.name} will follow his/her presence in Life360.")
                input name: "submitNewPerson", type: "button", title: "Submit", width: 3
                input name: "cancelAddPerson", type: "button", title: "Cancel", width: 3
            }
            else if (state.deletingPerson) {
                input name: "personToDelete", type: "enum", title: "Delete Person:", options: getPeopleEnumList(), multiple: false, submitOnChange: true
                if (personToDelete) input name: "submitDeletePerson", type: "button", title: "Confirm Delete", width: 3
                input name: "cancelDeletePerson", type: "button", title: "Cancel", width: 3
            }
            else if (state.editingPerson) {
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
                    input name: "person${id}Avatar", type: "text", title: "URL to Avatar Image", submitOnChange: true, required: true
                    input name: "person${id}Life360", type: "device.Life360User", title: "Life360 Device", submitOnChange: true, multiple: false, required: false
                }
                input name: "submitEditPerson", type: "button", title: "Done", width: 3
             //   input name: "cancelEditPerson", type: "button", title: "Cancel", width: 3
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
    def currentTripMap = [id: null, isPreDeparture: null, departureTime: null, recommendedRoute: null,] // map for details about active trip
    def previousTripMap = [id: null, departureTime: null, arrivalTime: null] // map for details about last trip
    def previousPlaceMap = [name: null, id: null, arrival: null, departure: null]
    def previousVehicleMap = [id: null, arrival: null, departure: null]
    def currentMap = [place: currentPlaceMap, vehicle: currentVehicleMap, trip: currentTripMap]
    def previousMap = [place: previousPlaceMap, vehicle: previousVehicleMap, trip: previousTripMap]
    def life360Map = [address: null, atTime: null, isDriving: null]
    def personMap = [current: currentMap, previous: previousMap, life360: life360Map, places: null, vehicles: null]
        // so use like state.people.persondId.current.place.name
    state.people[id] = personMap
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


String getNameOfPersonWithId(String id) {
    return settings["person${id}Name"]
}

Boolean isDriving(String personId) {
    if (settings["person${id}Life360"]) return settings["person${id}Life360"].isDriving
    else return null
}

def deletePerson(String nameToDelete) {
    def idToDelete = getIdOfPersonWithName(nameToDelete)
    if (idToDelete && state.people) {       
        state.people.remove(idToDelete)
        clearPersonSettings(idToDelete)
        deleteTracker(idToDelete)
    }
}

def clearPersonSettings(String personId) {
    app.clearSetting("person${personId}Name")
    app.clearSetting("person${personId}Avatar")
    app.updateSetting("person${personId}Life360",[type:"capability",value:[]]) 
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
                def i = 0
                def vehicleDisplay = "<table border=0>"
                for (id in state.vehicles) {
                    vehicleDisplay += "" + ((i % 4 == 0) ? "<tr>" : "") + "<td align=center><img border=0 style='max-width:100px' src='${settings["vehicle${id}Icon"]}'><br><font style='font-size:20px;font-weight: bold'>${settings["vehicle${id}Name"]}</font></td>" + ((i % 4 == 3) ? "</tr>" : "")
                    i++
                }
                vehicleDisplay += "</table>"
                paragraph vehicleDisplay
                paragraph getInterface("line", "")
            }

            if (state.addingVehicle) {
                input name: "vehicle${state.lastVehicleID}Name", type: "text", title: "Unique Name", submitOnChange: false, required: true
                input name: "vehicle${state.lastVehicleID}Icon", type: "text", title: "URL to Vehicle Icon", submitOnChange: false, required: true
                if (state.people) {
                    state.people.each { personId, person ->
                        def personDisplay = "<table border=0 margin=0><tr>"
                        personDisplay+= "<td align=center><img border=0 style='max-width:100px' src='${settings["person${personId}Avatar"]}'>"
                        personDisplay += "<br><font style='font-size:20px;font-weight: bold'>${settings["person${personId}Name"]}</font></td>"
                        personDisplay += "</tr></table>"
                        paragraph personDisplay
                        input name: "vehicle${state.lastVehicleID}Person${personId}Sensor", type: "capability.presenceSensor", title: "Vehicle Presence Sensor for ${settings["person${personId}Name"]}", submitOnChange: false, multiple: false, required: false, width: 4
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
                    input name: "vehicle${vehicleId}Icon", type: "text", title: "URL to Vehicle Icon", submitOnChange: true, required: true
                    if (state.people) {
                        state.people.each { personId, person ->
                            def personDisplay = "<table border=0 margin=0><tr>"
                            personDisplay+= "<td align=center><img border=0 style='max-width:100px' src='${settings["person${personId}Avatar"]}'>"
                            personDisplay += "<br><font style='font-size:20px;font-weight: bold'>${settings["person${personId}Name"]}</font></td>"
                            personDisplay += "</tr></table>"
                            paragraph personDisplay
                            input name: "vehicle${vehicleId}Person${personId}Sensor", type: "capability.presenceSensor", title: "Vehicle Presence Sensor for ${settings["person${personId}Name"]}", submitOnChange: true, multiple: false, required: false, width: 4
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
    app.clearSetting("vehicle${vehicleId}Name")
    app.clearSetting("vehicle${vehicleId}Icon")
    if (state.people) {
        state.people.each { personId, person ->
            app.updateSetting("vehicle${vehicleId}Person${personId}Sensor",[type:"capability",value:[]])
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
    return settings["vehicle${id}Icon"]
}

def addVehicle(String id) {
    if (!state.vehicles) state.vehicles = []
    state.vehicles.add(id)
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
        clearVehicleSettings(idToDelete)
    }
}

def PlacesPage() {
    dynamicPage(name: "PlacesPage") {
        section {
            paragraph getInterface("header", " Manage Places")
            if (state.places) {                
                def i = 0
                def placesDisplay = "<table border=0>"
                for (id in state.places) {
                    placesDisplay += "" + ((i % 4 == 0) ? "<tr>" : "") + "<td align=center><img border=0 style='max-width:100px' src='${settings["place${id}Icon"]}'><br><font style='font-size:20px;font-weight: bold'>${settings["place${id}Name"]}</font></td>" + ((i % 4 == 3) ? "</tr>" : "")
                    i++
                }
                placesDisplay += "</table>"
                paragraph placesDisplay
                paragraph getInterface("line", "")
            }

            if (state.addingPlace) {
                input name: "place${state.lastPlaceID}Name", type: "text", title: "Unique Name", submitOnChange: false, required: true
                paragraph getInterface("note", "Name Places in ${app.name} the the same as those in any associated Life360 account, in order for presence in ${app.name} to follow presence in Life360.")
                input name: "place${state.lastPlaceID}Icon", type: "text", title: "URL to Place Icon", submitOnChange: false, required: true
                input name: "place${state.lastPlaceID}Address", type: "text", title: "Address", submitOnChange: false, required: true
                input name: "place${state.lastPlaceID}Hub", type: "bool", title: "Place of Hub Location?", submitOnChange: true, required: true, defaultValue: false
                if (state.people) {
                        state.people.each { personId, person ->
                            def personDisplay = "<table border=0 margin=0><tr>"
                            personDisplay+= "<td align=center><img border=0 style='max-width:100px' src='${settings["person${personId}Avatar"]}'>"
                            personDisplay += "<br><font style='font-size:20px;font-weight: bold'>${settings["person${personId}Name"]}</font></td>"
                            personDisplay += "</tr></table>"
                            paragraph personDisplay
                            input name: "place${state.lastPlaceID}Person${personId}Sensor", type: "capability.presenceSensor", title: "Place Presence Sensor for ${settings["person${personId}Name"]}", submitOnChange: false, multiple: false, required: false, width: 4
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
                input name: "placeToDelete", type: "enum", title: "Delete Place:", options: getPlacesEnumList(), multiple: false, submitOnChange: true
                if (placeToDelete) input name: "submitDeletePlace", type: "button", title: "Confirm Delete", width: 3
                input name: "cancelDeletePlace", type: "button", title: "Cancel", width: 3
            }
            else if (state.editingPlace) {
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
                    input name: "place${placeId}Icon", type: "text", title: "URL to Place Icon", submitOnChange: true, required: true
                    input name: "place${placeId}Address", type: "text", title: "Address", submitOnChange: true, required: true
                    input name: "place${placeId}Hub", type: "bool", title: "Place of Hub Location?", submitOnChange: true, required: true, defaultValue: false
                    if (state.people) {
                        state.people.each { personId, person ->
                            def personDisplay = "<table border=0 margin=0><tr>"
                            personDisplay+= "<td align=center><img border=0 style='max-width:100px' src='${settings["person${personId}Avatar"]}'>"
                            personDisplay += "<br><font style='font-size:20px;font-weight: bold'>${settings["person${personId}Name"]}</font></td>"
                            personDisplay += "</tr></table>"
                            paragraph personDisplay
                            input name: "place${placeId}Person${personId}Sensor", type: "capability.presenceSensor", title: "Place Presence Sensor for ${settings["person${personId}Name"]}", submitOnChange: true, multiple: false, required: false, width: 4
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
    app.clearSetting("place${placeId}Address")
    app.clearSetting("place${placeId}Hub")
    
    if (state.people) {
       state.people.each { personId, person ->
                app.updateSetting("place${placeId}Person${personId}Sensor",[type:"capability",value:[]])                
        }
    }

    app.updateSetting("place${placeId}GarageDoor",[type:"capability",value:[]])
    app.updateSetting("place${placeId}ContactSensor",[type:"capability",value:[]])
    app.updateSetting("place${placeId}Switch",[type:"capability",value:[]])
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
            list.add(settings["place${id}Name"])
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
    return settings["place${id}Icon"]
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

Boolean isRestricted() {
    Boolean isRestricted = false     
    
    if (restrictedModes) {
        if(restrictedModes.contains(location.mode)) {
            if (logEnable) log.debug "Mode Check Failed."
            isRestricted = true
        }
    }
    
    return isRestricted  
}

def AdvancedPage() {
    dynamicPage(name: "AdvancedPage") {
        section {
            paragraph getInterface("header", " Advanced Settings")
            input name: "fetchInterval", type: "number", title: "Interval (mins) for Checking Travel Conditions", required: true, defaultValue: fetchIntervalDefault
            input name: "earlyFetchMins", type: "number", title: "Number of Minutes Before Earliest Departure Time to Check Travel Conditions", required: true, defaultValue: earlyFetchMinsDefault
            input name: "postArrivalDisplayMins", type: "number", title: "Number of Minutes to Display Trip After Arrival", required: true, defaultValue: postArrivalDisplayMinsDefault
            paragraph "Smart Travel caches the response from Google Directions and considers the cached response valid for the duration selected here. Increasing the validity time reduces the number of API calls. Decreasing the validity time increases responsiveness to traffic fluctuations."
            input name: "cacheValidityDuration", type: "number", title: "Duration of Directions Cache (Secs)", required: false, defaultValue: cacheValidityDurationDefault
            paragraph "${app.name} also caches the response from Google Directions for showing the route options available in the app. Set for as long as a configuration session may last."
            input name: "optionsCacheValidityDuration", type: "number", title: "Duration of Options Cache (Secs)", required: false, defaultValue: optionsCacheValidityDurationDefault
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
    subscribeTriggers()
    unschedule()
    scheduleTimeTriggers()
    initializePresence()
    initializeTrackers()
}

def initializePresence() {
    if (state.people) {
        state.people.each { personId, person ->
            setPersonPlace(personId) 
            setPersonVehicle(personId)
        }
    }
}

def scheduleTimeTriggers() {
    if (state.trips) {
        state.trips.each { tripId, trip ->
            def earliestDeparture = settings["trip${tripId}EarliestDepartureTime"]
            def latestDeparture = settings["trip${tripId}LatestDepartureTime"]
            if (earliestDeparture) {
                def earlyFetchTime = adjustTimeBySecs(earliestDeparture, getEarlyFetchMinsSetting()*60)
                schedule(earlyFetchTime, earlyTripFetchHandler, [data: [tripId: tripId]])
            }
            if (earliestDeparture) {
                schedule(earliestDeparture, startDepartureWindowHandler, [data: [tripId: tripId]])
            }
            if (latestDeparture) {
                schedule(latestDeparture, endDepartureWindowHandler, [data: [tripId: tripId]])
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
        }
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
         if (isPlaceTripOrigin(placeId, tripId) && areDepartureConditionsMet(tripId)) {
              for (personName in settings["trip${tripId}People"]) {
                   def isPreDeparture = false
                   def personId = getIdOfPersonWithName(personName)
                   if (!isPersonOnTrip(personId)) {  // person has not left already so as to constitute an actual predeparture event (as opposed to someone opening the garage door right after departure)
                       startTripPreDepartureForPerson(personId, tripId)
                       isPreDeparture = true
                   }
                   if (isPreDeparture) performPreDepartureActionsForTrip(tripId) // perform predeparture actions for trip only once, since predeparture actions are not specific to a certain person
              }            
         }
    }    
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

def earlyTripFetchHandler(data) {
    def tripId = data.tripId
}

def startDepartureWindowHandler(data) {
    def tripId = data.tripId
}


def endDepartureWindowHandler(data) {
    def tripId = data.tripId
}

def performPreDepartureActionsForTrip(String tripId) {
    
}

def performDepartureActionsForTrip(String personId, String tripId) {
    
}

def performArrivalActionsForTrip(String personId, String tripId) {
    
}

def updateTracker(String personId) {
    def tracker = getTracker(personId)
    if (tracker) {
        // update from state
        
        // option: show life360 address if person is not at any place (e.g., icon showing not at home, and address where at)
        
        // show any previous trip for X minutes according to getPostArrivalDisplayMinsSettings()*60
    }   
}

def handlePlaceChange(String personId) {
    // first update state based on impact of change to trip status
    
    // Did the place change start a trip?
    state.trips.each { tripId, trip ->
        if (isTripPerson(personId, tripId) && didDepartOrigin(personId, tripId) && areDepartureConditionsMet(tripId) && inTripVehicle(personId, tripId) && !isTripInProgress(personId, tripId)) {
            // trip just started because (i) person scheduled for trip (ii) left origin; (iii) while departure conditions met; (iv) within a vehicle specified for the trip; and (iv) trip was not already in progress
            startTripForPerson(personId, tripId)
        }
    }
    
    // Did the place change end the person's current trip by arrival at the destination?
    if (isPersonOnTrip(personId) && atDestinationOfCurrentTrip(personId)) {
        // trip just ended   
        logDebug("Ending current trip for person ${personId}")
        endCurrentTripForPerson(personId)        
    }
    
    // then update Tracker device (which will indicate any tripId stored at state.people[personId]?.current.trip.id as well as store, for a period of time, any tripId stored at state.people[personId]?.previous.trip.id)
    updateTracker(personId)
}

Boolean isPersonOnTrip(String personId) {
    return (state.people[personId]?.current.trip.id != null) ? true : false
}

Boolean isTripPerson(String personId, String tripId) {
    def personName = getNameOfPersonWithId(personId)
	if (settings["trip${tripId}People"].contains(personName)) {
	    return true
	}    
    else return false    
}

Boolean isTripInProgress(String personId, String tripId) {
    def inProgress = false
    if (state.people[personId]?.current.trip.id == tripId) inProgress = true
    return inProgress
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
                
                
Boolean areDepartureConditionsMet(String tripId, Boolean isPreDepartureIncluded=false) {
    if (inDepartureWindow(tripId, isPredepartureIncluded) && !isRestricted()) return true
    else return false
}

Boolean inDepartureWindow(String tripId, Boolean isPredepartureIncluded=false) {
    Boolean inWindow = true
    
    // Day of Week Travel Check
    if(!isTripDay()) {
        if (logEnable) log.debug "Trip Day Check Failed."
	    inWindow = false
    }
    
    // Time Window
    if (settings["trip${tripId}EarliestDepartureTime"] && settings["trip${tripId}LatestDepartureTime"]) {
        def preDepartureTime = adjustTimeBySecs(settings["trip${tripId}EarliestDepartureTime"], getEarlyFetchMinsSetting()*60)
        def windowStart = isPredepartureIncluded ? preDepartureTime : toDateTime(settings["trip${tripId}EarliestDepartureTime"])
        if(!timeOfDayIsBetween(windowStart, toDateTime(settings["trip${tripId}LatestDepartureTime"]), new Date(), location.timeZone)) {
            if (logEnable) log.debug "Time Check Failed."
            inWindow = false
        }
    }    
    return inWindow
}
                
                
def isTripDay(String tripId) {
    def dateFormat = new SimpleDateFormat("EEEE")
    def dayOfTheWeek = dateFormat.format(new Date())
    if (!settings["trip${tripId}Days"]) return true // if no trip days specified, assume trip day
	if (settings["trip${tripId}Days"].contains(dayOfTheWeek)) {
	    return true
	}    
    else return false
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

def handleVehicleChange(String personId) {
    // first update state based on impact of change to trip status
    
    // did vehicle change start a trip?
    state.trips.each { tripId, trip ->
         if (inTripVehicle(personId, tripId) && areDepartureConditionsMet(tripId) && !isTripInProgress(personId, tripId)) {
             // assume trip just started because (i) got in a vehicle specified for the trip (ii) while departure conditions met; (iii) and trip was not already in progress
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

def startTripPreDepartureForPerson(String personId, String tripId) {
    logDebug("Starting pre-departure for trip ${tripId} for person ${personId}")
    state.people[personId].current.trip.id = tripId
    state.people[personId].current.trip.isPreDeparture = true    
    state.people[personId]?.current.trip.departureTime = null
}

def startTripForPerson(String personId, String tripId) {
    logDebug("Starting trip ${tripId} for person ${personId}")
    state.people[personId]?.current.trip.id = tripId
    state.people[personId]?.current.trip.isPreDeparture = false
    state.people[personId]?.current.trip.departureTime = new Date().getTime()   
    
    performDepartureActionsForTrip(personId, tripId)
}

def endCurrentTripForPerson(String personId) {
    logDebug("Ending trip ${state.people[personId]?.current.trip.id} for person ${personId}")
    performArrivalActionsForTrip(personId, state.people[personId]?.current.trip.id) 
    
    state.people[personId]?.previous.trip.id = state.people[personId]?.current.trip.id
    state.people[personId]?.previous.trip.departureTime = state.people[personId]?.current.trip.departureTime
    state.people[personId]?.previous.trip.arrivalTime = new Date().getTime()            
    state.people[personId]?.current.trip.id = null
    state.people[personId]?.current.trip.isPreDeparture = null
    state.people[personId]?.current.trip.departureTime = null        
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
             if (isPersonOnTrip(personId) && areDepartureConditionsMet(tripId) && !isTripInProgress(personId, tripId)) {
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
            if (lastChanged != placesPresent[0]) log.warn "Mismatch from places presence sensor logic. Debug."
            
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
            // TO DO
        }
    }
    else {
        // present at multiple places. Must resolve conflict between multiple presence sensors in the present state
        // TO DO
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
        if (logEnable) "didChangePlaceById returning false"
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
    
    // TO DO: check if better way to set arrival and departure time, e.g., to be the exact same as set in places or life360 state variables
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
            logDebug("Adding vehicleId ${vehiclesId} to vehiclesPresent")
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
        }
    }
    else {
        // present at multiple vehicles. Must resolve conflict between multiple presence sensors in the present state
        
    }    
}

def didChangeVehicle(String personId, String vehicleId) {
    // check if vehicle presence actually changed
    if (state.people[personId]?.current.vehicle.id && state.people[personId]?.current.vehicle.id == vehicleId) {
        // location hasn't changed
        if (logEnable) "Vehicle ID has not changed. Returning false from didChangeVehicle"
        return false
    }
    else if (state.people[personId]?.current.vehicle.id == null && vehicleId == null) {
        if (logEnable) "Still not present at any vehicle. Returning false from didChangeVehicle"
        return false
    }
    else return true
}

def changePersonVehicle(String personId, String vehicleId) {    
    state.people[personId]?.previous.vehicle.id = state.people[personId]?.current.vehicle.id
    state.people[personId]?.previous.vehicle.arrival = state.people[personId]?.current.vehicle.arrival
    state.people[personId]?.previous.vehicle.departure = new Date().getTime()
    state.people[personId]?.current.vehicle.arrival = (vehicleId) ? new Date().getTime() : null
    state.people[personId]?.current.vehicle.id = vehicleId
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
    if (logEnable) log.debug "In isPlaceDeviceForPerson with sensor for person: ${settings["place${placeId}Person${personId}Sensor"]} and device: ${device}"
     if (settings["place${placeId}Person${personId}Sensor"] && settings["place${placeId}Person${personId}Sensor"].getDeviceNetworkId() == device.getDeviceNetworkId()) {
         log.debug "returning true from isPlaceDeviceForPerson"
         return true
     }
    else return false
}

def placePresenceSensorHandler(evt) {
    if (logEnable) log.debug "In place presence sensor handler for event: ${evt.value} with device ${evt.getDevice()}"
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
                def i = 0
                def tripsDisplay = "<table align=left border=0 margin=0>"
                state.trips.each { tripId, trip ->
                    tripsDisplay += "" + ((i % 2 == 0) ? "<tr>" : "") + "<td align=center style:'width=50%'><img style='max-width:100px' border=0  src='${getPlaceIcon(settings["trip${tripId}Origin"])}'><img style='max-width:100px' border=0 src='${getPlaceIcon(settings["trip${tripId}Destination"])}'><br><font style='font-size:20px;font-weight: bold'>${getNameOfTripWithId(tripId)}</font></td>" + ((i % 4 == 1) ? "</tr>" : "")
                    i++
                }
                tripsDisplay += "</table>"
                paragraph tripsDisplay
                paragraph getInterface("line", "")
            }

            if (state.addingTrip) {
                input name: "trip${state.lastTripID}Origin", type: "enum", title: "Origin of Trip", required: true, submitOnChange: true, options: getPlacesEnumList()
                input name: "trip${state.lastTripID}Destination", type: "enum", title: "Destination of Trip", required: true, submitOnChange: true, options: getPlacesEnumList()    
    
                input name: "trip${state.lastTripID}People", type: "enum", title: "Traveler(s)", required: true, submitOnChange: true, options: getPeopleEnumList(), multiple: true 
                input name: "trip${state.lastTripID}Vehicles", type: "enum", title: "Vehicle(s)", required: true, submitOnChange: true, options: getVehiclesEnumList(), multiple: true 
                input name: "trip${state.lastTripID}Days", type: "enum", title: "Day(s) of Week", required: true, multiple:true, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]  
                input name: "trip${state.lastTripID}EarliestDepartureTime", type: "time", title: "Earliest Departure Time", required: false, width: 4, submitOnChange: true
    
                input name: "trip${state.lastTripID}LatestDepartureTime", type: "time", title: "Latest Departure Time", required: false, width: 4
                input name: "trip${state.lastTripID}TargetArrivalTime", type: "time", title: "Target Arrival", required: false, width: 4
                
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
                    def id = getIdOfTripWithName(tripToEdit)
                    if (id != null) {
                        state.editedTripId = id    // save the ID and name of the vehicle being edited in state
                        state.editedTripName = tripToEdit
                    }
                    else {
                        // just edited the trip's origin or destination so that tripToEdit no longer holds the same trip name. Need to update that.
                        id = state.editedTripId
                        def newTripName = getNameOfTripWithId(id)
                        app.updateSetting("tripToEdit",[type:"enum",value:newTripName]) 
                        state.editedTripName = newTripName
                    }
                    
                    input name: "trip${id}Origin", type: "enum", title: "Origin of Trip", required: true, submitOnChange: true, options: getPlacesEnumList()
                    input name: "trip${id}Destination", type: "enum", title: "Destination of Trip", required: true, submitOnChange: true, options: getPlacesEnumList()    
    
                    input name: "trip${id}People", type: "enum", title: "Traveler(s)", required: true, submitOnChange: true, options: getPeopleEnumList(), multiple: true 
                    input name: "trip${id}Vehicles", type: "enum", title: "Vehicle(s)", required: true, submitOnChange: true, options: getVehiclesEnumList(), multiple: true 
                    input name: "trip${id}Days", type: "enum", title: "Day(s) of Week", required: true, multiple:true, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]  
                    input name: "trip${id}EarliestDepartureTime", type: "time", title: "Earliest Departure Time", required: false, width: 4, submitOnChange: true
    
                    input name: "trip${id}LatestDepartureTime", type: "time", title: "Latest Departure Time", required: false, width: 4
                    input name: "trip${id}TargetArrivalTime", type: "time", title: "Target Arrival", required: false, width: 4                    
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
       
        section 
        {
            
            href(name: "PreferredRoutePage", title: "Configure Preferred Route", required: false, page: "PreferredRoutePage")
            href(name: "ActionConfigPage", title: "Configure What To Do With Travel Information", required: false, page: "ActionConfigPage")
            paragraph getInterface("line", "")


        }
    }
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
    def tripMap = [routes: null, options: null]
    state.trips[id] = tripMap
}

def getNameOfTripWithId(String id) {
    return settings["trip${id}Origin"] + " to " + settings["trip${id}Destination"]
}

def deleteTrip(nameToDelete) {
    def idToDelete = getIdOfTripWithName(nameToDelete)
    if (idToDelete && state.trips) {       
        state.trips.remove(idToDelete)
        clearTripSettings(idToDelete)
    }
}

def PreferredRoutePage() {
    dynamicPage(name: "PreferredRoutePage") {      
        section("") {      
            paragraph getInterface("header", " Configure Preferred Route")
            def routeOptions = getRouteOptions()
            if (getRouteOptions() != null) {
                input name: "preferredRoute", type: "enum", title: "Preferred Route", required: false, options: getRouteOptions(), submitOnChange: true
                 paragraph "You can optionally bias route recommendations in favor of your preferred route. Preferred Route Bias is how many minutes faster an alternate route must be in order to be recommended over your preferred route."
                input name: "preferredRouteBias", type: "number", title: "Preferred Route Bias (mins)", required: false, width: 4
                if (preferredRoute) {          
                    def routeSteps = getPreferredRouteSteps()
                    if (routeSteps) {
                        paragraph getInterface("header", " Preferred Route Steps")
                        for (step in routeSteps) {
                            paragraph "${step.html_instructions}"
                        }
                    }
                }
            }
            else {
                log.error "Error: Unable to Retrieve Route Options."
                paragraph getInterface("error", "Error: Unable to Retrieve Route Options. Please check your Internet connection.")
            }

        }
    }
}

def ActionConfigPage() {
    dynamicPage(name: "ActionConfigPage") {
        section 
        {
            paragraph getInterface("header", " Configure What To Do With Fetched Travel Information")
            actionInput()
        }    
    }
}

def actionInput() {
  //  paragraph getInterface("subheader", "Push Notification")
    paragraph "Send a push notification to select notification devices indicating recommended route."
     input name: "isPushNotification", type: "bool", title: "Send Push Notification?", required: false, submitOnChange: true
    if (isPushNotification) {
         input name: "pushDevice", type: "capability.notification", title: "Push Notification Device(s)", required: false, multiple: true, submitOnChange: true    
         input name: "onlyPushIfNonPreferred", type: "bool", title: getInterface("subField", "But Only if Non-Preferred Route Recommended?"), required: false, submitOnChange: false
        paragraph getInterface("note", "Smart Travel avoids sending duplicate push notifications, so that push notifications only convey changed travel information.")
    }
    paragraph getInterface("line", "")
}

def getEarlyFetchMinsSetting() {
    return (earlyFetchMins) ? earlyFetchMins : earlyFetchMinsDefault
}

def getPostArrivalDisplayMinsSettings() {
    return (postArrivalDisplayMins) ? postArrivalDisplayMins : postArrivalDisplayMinsDefault
}

def getCacheValidityDurationSetting() {
    return (cacheValidityDuration) ? cacheValidityDuration : cacheValidityDurationDefault
}

def getOptionsCacheValidityDurationSetting() {    
     return (optionsCacheValidityDuration) ? optionsCacheValidityDuration : optionsCacheValidityDurationDefault 
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
            def networkID = "MultiPlaceTracker${personId}"
            def child = getChildDevice(networkID)
            if (!child) createTracker(personId, settings["person${personId}Name"])
            // TO DO: update tile
        }
    }
}

def createTracker(personId, personName)
{
    def networkID = "MultiPlaceTracker${personId}"
    def child = addChildDevice("lnjustin", "Multi-Place Tracker", networkID, [label:"${personName} Multi-Place Tracker", isComponent:true, name:"${personName} Multi-Place Tracker"])
}

def getTracker(personId) {
    def networkID = "MultiPlaceTracker${personId}"
    return getChildDevice(networkID)
}

def deleteTracker(personId)
{
    def networkID = "MultiPlaceTracker${personId}"
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


def getTileLocalUrl(tile) {
    instantiateToken()
    return getFullLocalApiServerUrl() + "/smartTravel/${tile}?access_token=${state.accessToken}"
}


def getTileCloudUrl(tile) {
    instantiateToken()
    getFullApiServerUrl() + "/smartTravel/${tile}?access_token=${state.accessToken}"
}

def updateTile(tile, routes, instance) {
    // Update Tile Device
    def networkID = "SmartTravelTile${tile}"
    def child = getChildDevice(networkID)
    if (child) {
        child.configureInstance(instance)
        child.configureRoutes(routes)
    }
}

def buildTile() {
    def tile = params.tile
    def networkID = "SmartTravelTile${tile}"
    def child = getChildDevice(networkID)
    def svg = null
    if (child) {
        svg = child.getTile()
    }
    else {
        log.warn "No Smart Travel Device Found for tile ${tile}. No tile built."
    }
    render contentType: "image/svg+xml", data: svg, status: 200
}


def getInterface(type, txt="") {
    switch(type) {
        case "line": 
            return "<hr style='background-color:#555555; height: 1px; border: 0;'></hr>"
            break
        case "header": 
            return "<div style='color:#ffffff;font-weight: bold;background-color:#555555;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${txt}</div>"
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
    }
} 

def adjustTimeBySecs(time, secs) {
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

def handleStartFetchEvent() {
    if (logEnable) log.trace "Handling start fetch event"
    if (isStartFetchAllowed()) {
        if (logEnable) log.trace "Fetch Allowed. Fetching..."
        if (fetchFrequency == periodicFreq) {
            initializeStateForFetchPeriod()
            state.inFetchingPeriod = true
            periodicFetch()
        }
        else fetch()
    }
}

def periodicFetch() {
    if (state.inFetchingPeriod) {
        fetch()
        runIn(fetchInterval*60, "periodicFetch")
    }
    else {
        if (logEnable) log.debug "Fetching Period Ended. No more fetching for now."   
    }
}

def handleStopFetchEvent() {
    if (isStopFetchAllowed()) {
        state.inFetchingPeriod = false
    }
}

def fetch() {
    if (logEnable) log.trace "Fetching Now."
    def response = getDirections()
    if (response) {
        state.routes = [:]
        def routes = response.routes
        if (preferredRoute && preferredRouteBias) routes = biasRoutes(routes)
        for (i=0; i<routes.size(); i++) {
            def route = routes[i]
            def summary = route.summary
            def preferred = (isPreferredRoute(summary)) ? true : false
            def duration = route.legs[0].duration_in_traffic?.value
            def durationStr = route.legs[0].duration_in_traffic?.text
            def distance = route.legs[0].distance.text
            def eta = getETA(duration)
            def requiredDeparture = getRequiredDeparture(duration)
            def departureCountDown = ""
            def departureCountDownStr = ""
            if (requiredDeparture) {
                departureCountDown = getSecondsBetween(new Date(), requiredDeparture)
                departureCountDownStr = formatTime(departureCountDown)
            }
            def lastUpdated = new Date()
            state.routes[i] = [summary: summary, duration: duration, durationStr: durationStr, eta: eta, departure: departure, preferred: preferred, departureCountDown: departureCountDown, departureCountDownStr: departureCountDownStr, lastUpdated: lastUpdated]
        }
        act()
    }
    else {
        log.warn "No response from Google Traffic API. Check connection."
    }
}

// ### ACT METHODS ###
def act() {
    if (isPushNotification) sendPush()
    if (isDashboardTile) parent.updateTile(tile, state.routes, app.name)    
}

def sendPush() {
    if (pushDevice && isPushAllowed()) {
        def routes = state.routes
              
        def bestRoute = routes[0].summary
        def bestRouteDuration = routes[0].duration
        def requiredDeparture = routes[0].departure
        def eta = routes[0].eta
        
        def nextBestRoute = routes[1].summary
        def nextBestRouteDuration = routes[1].duration
        def fasterBy = (nextBestRouteDuration - bestRouteDuration)/60

        pushDevice.deviceNotification("Take ${bestRoute}. ${eta} ETA. ${fasterBy} mins faster than ${nextBestRoute}.")
        state.lastPushedRoute = bestRoute
    }
    else {
        if (logEnable) log.info "Push Notification Restricted. Nothing Sent."
    }
}

def isPushAllowed() {
    def isAllowed = true
    
    def routes = state.routes
    def bestRoute = routes[0].summary   
    
    if (onlyPushIfNonPreferred) {
        if (preferredRoute && isPreferredRoute(bestRoute)) {
            isAllowed = false
            log.info "Preferred Route Best. No Push Notification Sent."      
        }                
    }
    
    if (state.lastPushedRoute == bestRoute) {
        // Only check for push notifications that would duplicate the recommended route, as ETA may change slightly
        isAllowed = false
        log.info "Push Notification recommending ${bestRoute} already sent. Duplicate Push Notification Not Sent."
    }
    
    return isAllowed
}


// ###  Route Info Methods  ###
def getRequiredDeparture(duration) {
    if (targetArrivalTime) {
        def target = toDateTime(targetArrivalTime)
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

def biasRoutes(unbiasedRoutes) {
    def preferredRouteIndex = -1
    def routeMap = [:]
    def biasedRoutes = [:]
    for (i=0; i<unbiasedRoutes.size(); i++) {
         def route = unbiasedRoutes[i]
         def summary = route.summary
        if (logEnable) log.debug "Route ${i} is ${summary}"
        if (isPreferredRoute(summary)) preferredRouteIndex = i
        routeMap[i] = route.legs[0].duration_in_traffic?.value
    }
    if (preferredRouteIndex <= 0) {
    // if preferred route is either not listed or is already the best route, no bias needed, so return unbiased routes
        return unbiasedRoutes
    }
    else if (preferredRouteIndex > 0) {
    // If preferred route is listed but is not the best route, bias ordering according to bias
        if (logEnable) log.debug "Unbiased routes: ${routeMap}"
        routeMap[preferredRouteIndex] -= (preferredRouteBias*60)    // bias preferred route duration for ranking
        routeMap = routeMap.sort {it.value}                        // rank routes after biasing
        if (logEnable) log.debug "Biased route map: ${routeMap}"
        def rank = 0
        routeMap.each { i, j ->
            biasedRoutes[rank] = unbiasedRoutes[i]                // reorder routes according to new rank
            rank++
        }
        return biasedRoutes
    }
}

def isPreferredRoute(route) {
    if (preferredRoute && route.contains(preferredRoute)) return true
    else return false
}

def getPreferredRouteSteps() {
    if (preferredRoute && state.optionsCache) {
        def routeOption = state.optionsCache.routes.find { it.summary.contains(preferredRoute) }
        return routeOption.legs[0].steps
    }
    return null
}

def getRouteOptions() {
    def response = getDirectionOptions()
    if(response) {
        def routes = response.routes
        def options = []
        for (i=0; i<routes.size(); i++) {
            options.add(routes[i].summary)
        }
        return options
    }
    return null
}



// ### API Methods ###

def getDirections() {
    if (isCacheValid()) {
        return state.cache
    }
    else {
        def subUrl = "directions/json?origin=${parent.getPlaceAddress(origin)}&destination=${parent.getPlaceAddress(destination)}&key=${getApiKey()}&alternatives=true&mode=driving&departure_time=now"   
        def response = httpGetExec(subUrl)
        if (response) {
            state.cache = response
            state.cacheTime = new Date()
        }
        return response
    }
}

def getDirectionOptions() {
    if (isOptionsCacheValid()) {
        return state.optionsCache
    }
    else {
        def subUrl = "directions/json?origin=${parent.getPlaceAddress(origin)}&destination=${parent.getPlaceAddress(destination)}&key=${getApiKey()}&alternatives=true&mode=driving"   // Don't need traffic info for route options, so exclude for lower billing rate
        log.debug subUrl
        def response = httpGetExec(subUrl)
        if (response) {
            state.optionsCache = response
            state.optionsCacheTime = new Date()
        }
        return response
    }
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
def isCacheValid() {
    if (state.cacheTime && (getSecondsBetween(toDateTime(state.cacheTime), new Date()) > getCacheValidityDuration())) {
        return true
    }
    else return false
}

def getCacheValidityDuration() {
    return parent.getCacheValidityDurationSetting() 
}

def getOptionsCacheValidityDuration() {
    return parent.getOptionsCacheValidityDurationSetting()
}

def isOptionsCacheValid() {

    if (state.optionsCacheTime) {
        def optionsCacheDuration = getSecondsBetween(state.optionsCacheTime, new Date())
        if (optionsCacheDuration <= getOptionsCacheValidityDuration()) {
            return true
        }
        else {
            return false      
        }
    }
    else {
        return false    
    }
}

// ### Utility Methods ###
def getSecondsBetween(startDate, endDate) {
    try {
        def difference = endDate.getTime() - startDate.getTime()
        return Math.round(difference/1000)
    } catch (ex) {
        log.error "getSecondsBetween Exception: ${ex}"
        return 1000
    }
}

def formatTime(duration) {
    def hours = (duration / 3600).intValue()
    def mins = ((duration % 3600) / 60).intValue()
    def secs = (duration % 60).intValue()
    return (hours > 0) ? String.format("%02d:%02d:%02d", hours, mins, secs) : String.format("%02d:%02d", mins, secs)
}
            
