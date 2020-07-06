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

  - input screen:
        - people (name, avatar, life360 sensor)
        - vehicles (name, icon, presence sensor per person)
        - places (name, icon, presence sensor per person, doors that open/close when arrive/depart, switches that turn on/off when arrive/depart) (assume named same as life360)
        - trip (name, origin-dest pair, person(s), vehicle(s), days/time usually depart (with +/- input), target arrival)
        - Restrictions (vacation mode selection, so can exclude notifications while on vacation)
        
   - tile per person (input for how many minutes before departure to display)
        - avatar left
        - when at certain place, display green place icon to right of avatar
        - when taking a trip, display origin place icon --> car (in green) --> destination place icon, with either (a) best route or (b) estimated ETA or (c) both, either all the time or alternate. Or "not left" if haven't left

    - parent app holds people, vehicles, garages, places, and restrictions, as well as tile management
    - child app holds trips
    - parent/container device for tiles, with children for tiles
        - parent device allows generic requests for directions between two locations

Actions

    - All the time, need to check place where user is, so can update tile. 
           - 
            - Departure window is just for checking traffic to an intened destination place. When departure window starts, check to make sure that the user is at the origin place. If not at the origin place, abort trip.
            - maybe have parent app subscribe to user location, so can update tile all the time. And have trip/child app subscribe to user location, so can indicate trip upon departure, etc.? Will need to coordinate between the two though in order to not overwrite one another. Maybe parent yields to child if in the middle of a trip?

    - At least once before departure window begins, and during departure window, (i) update tile with recommended route; (ii) check if need to leave before the departure window to arrive on time; (iii) send push notification or turn on/off switch if bad traffic (update tile with how many minutes in advance need to leave or countdown to departure); (iv) if destination is home, update tile to show that haven't left (either text "Not Left" or can show location with green coloring) (can show target arrival time before departure, using a target icon along with the target time); (v) send text if haven't left yet and going to miss the target arrival by X mins

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

    

    - failsafe: stop checking traffic if haven't arrived after certain time (so doesn't keep making API calls if error happens)
    - tap on dashboard tile brings up larger display with map, address details, etc.


    TO DO: Handle delete vehicle settings when delete person, and vice versa
        Handle if leave before departure window?
        Handle editing place, person, or vehicle name after already created trackers


    THINKING PAD
        - leaning towards not using a device except for dashboard tile image. Just maintain presence values in state, since the presence devices will exist outside of the app already for any automation purposes. Focus on this app generating a dashboard image from the collection of existing devices that track presence, rather than this app contributing anything towards automation. Except for the traffic aspect --> allow the app to control devices based on traffic, as opposed to including traffic info in the device?
        - may show both place icon and vehicle icon if at both at the same time?
        - maintain state of all sensors for a person? Would allow to resolve inconsistincies between sensors.
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
                    apiInput()
                }
                else {
                    href(name: "TripsPage", title: "Manage Trips", required: false, page: "TripsPage")
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
    input name: "api_key", type: "text", title: "Enter Google API key", required: true, submitOnChange: true
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
                input name: "person${state.lastPersonID}Name", type: "text", title: "Unique Name", submitOnChange: false, required: true
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
                    def id = getIdOfPersonWithName(personToEdit)
                    input name: "person${id}Name", type: "text", title: "Unique Name", submitOnChange: false, required: true
                    input name: "person${id}Avatar", type: "text", title: "URL to Avatar Image", submitOnChange: false, required: true
                    input name: "person${id}Life360", type: "device.Life360User", title: "Life360 Device", submitOnChange: false, multiple: false, required: false
                }
                input name: "submitEditPerson", type: "button", title: "Submit", width: 3
                input name: "cancelEditPerson", type: "button", title: "Cancel", width: 3
            }            
            else {               
                input name: "addPerson", type: "button", title: "Add Person", width: 3                
                input name: "editPerson", type: "button", title: "Edit Person", width: 3
                app.clearSetting("personToEdit")
                input name: "deletePerson", type: "button", title: "Delete Person", width: 3
                app.clearSetting("personToDelete")
            } 

        }
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

def addPerson(id) {
    if (!state.people) {
        state.people = [:]
        def currentPlaceMap = [name: null, id: null, arrival: null]
        def currentVehicleMap = [id: null, arrival: null]
        def previousPlaceMap = [name: null, id: null, arrival: null, departure: null]
        def previousVehicleMap = [id: null, arrival: null, departure: null]
        def currentMap = [place: currentPlaceMap, vehicle: currentVehicleMap]
        def previousMap = [place: previousPlaceMap, vehicle: previousVehicleMap]
        def personMap = [current: currentMap, previous: previousMap, isDriving: null]
        // so use like state.people.persondId.current.place.name

        state.people[id] = personMap
    }
}

def getIdOfPersonWithName(name) {
    state.people?.each { id, person ->
        if (settings["person${id}Name"] == name) return id
    }
    log.warn "No Person Found With the Name: ${name}"
    return null
}

def deletePerson(nameToDelete) {
    def idToDelete = getIdOfPersonWithName(nameToDelete)
    if (idToDelete && state.people) {       
        state.people.remove(idToDelete)
        app.clearSetting("person${idToDelete}Name")
        app.clearSetting("person${idToDelete}Avatar")
        app.updateSetting("person${idToDelete}Life360",[type:"capability",value:[]])
        deleteTracker(idToDelete)
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
         addPerson(state.lastPersonID)  
         state.addingPerson = false
         break
      case "cancelAddPerson":
         state.addingPerson = false
        state.lastPersonID --
         break
      case "editPerson":
         state.editingPerson = true
         break
      case "submitEditPerson":
         state.editingPerson = false
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
         addVehicle(state.lastVehicleID)  
         state.addingVehicle = false
         break
      case "cancelAddVehicle":
         state.addingVehicle = false
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
         addPlace(state.lastPlaceID)  
         state.addingPlace = false
         break
      case "cancelAddPlace":
         state.addingPlace = false
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
         addTrip(state.lastTripID)  
         state.addingTrip = false
         break
      case "cancelAddTrip":
         state.addingTrip = false
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
                    input name: "vehicle${vehicleId}Name", type: "text", title: "Unique Name", submitOnChange: false, required: true
                    input name: "vehicle${vehicleId}Icon", type: "text", title: "URL to Vehicle Icon", submitOnChange: false, required: true
                    if (state.people) {
                        state.people.each { personId, person ->
                            def personDisplay = "<table border=0 margin=0><tr>"
                            personDisplay+= "<td align=center><img border=0 style='max-width:100px' src='${settings["person${personId}Avatar"]}'>"
                            personDisplay += "<br><font style='font-size:20px;font-weight: bold'>${settings["person${personId}Name"]}</font></td>"
                            personDisplay += "</tr></table>"
                            paragraph personDisplay
                            input name: "vehicle${vehicleId}Person${personId}Sensor", type: "capability.presenceSensor", title: "Vehicle Presence Sensor for ${settings["person${personId}Name"]}", submitOnChange: false, multiple: false, required: false, width: 4
                        }    
                        
                    }
                }
                input name: "submitEditVehicle", type: "button", title: "Submit", width: 3
                input name: "cancelEditVehicle", type: "button", title: "Cancel", width: 3
            }            
            else {               
                input name: "addVehicle", type: "button", title: "Add Vehicle", width: 3                
                input name: "editVehicle", type: "button", title: "Edit Vehicle", width: 3
                app.clearSetting("vehicleToEdit")
                input name: "deleteVehicle", type: "button", title: "Delete Vehicle", width: 3
                app.clearSetting("vehicleToDelete")
            } 

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

def getVehicleIcon(name) {
    def id = getIdOfVehicleWithName(name)
    return settings["vehicle${id}Icon"]
}

def addVehicle(id) {
    if (!state.vehicles) state.vehicles = []
    state.vehicles.add(id)
}

def getIdOfVehicleWithName(name) {
    for (id in state.vehicles) {
        if (settings["vehicle${id}Name"] == name) return id
    }
    log.warn "No Vehicle Found With the Name: ${name}"
    return null
}

def deleteVehicle(nameToDelete) {
    def idToDelete = getIdOfVehicleWithName(nameToDelete)
    if (idToDelete && state.vehicles) {       
        state.vehicles.removeElement(idToDelete)
        app.clearSetting("vehicle${idToDelete}Name")
        app.clearSetting("vehicle${idToDelete}Icon")
        if (state.people) {
            state.people.each { personId, person ->
                app.updateSetting("vehicle${idToDelete}Person${personId}Sensor",[type:"capability",value:[]])
            }
        }
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
                paragraph "Select garage door(s), contact sensor(s), and/or switch(es) that change when someone is about to arrive or depart. When this place is the origin of a trip, the changing of any of these devices during a time window for departure on the trip will trigger a proactive check of travel conditions, to advise you of the best route to take even before actual departure."
                input name: "place${state.lastPlaceID}GarageDoor", type: "capability.garageDoorControl", title: "Garage Door(s)", submitOnChange: false, multiple: true, required: false, width: 4
                input name: "place${state.lastPlaceID}ContactSensor", type: "capability.contactSensor", title: "Contact Sensor(s)", submitOnChange: false, multiple: true, required: false, width: 4
                input name: "place${state.lastPlaceID}Switch", type: "capability.switch", title: "Switch(es)", submitOnChange: false, multiple: true, required: false, width: 4
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
                    input name: "place${placeId}Name", type: "text", title: "Unique Name", submitOnChange: false, required: true
                    input name: "place${placeId}Icon", type: "text", title: "URL to Place Icon", submitOnChange: false, required: true
                    input name: "place${placeId}Address", type: "text", title: "Address", submitOnChange: false, required: true
                    input name: "place${placeId}Hub", type: "bool", title: "Place of Hub Location?", submitOnChange: true, required: true, defaultValue: false
                    if (state.people) {
                        state.people.each { personId, person ->
                            def personDisplay = "<table border=0 margin=0><tr>"
                            personDisplay+= "<td align=center><img border=0 style='max-width:100px' src='${settings["person${personId}Avatar"]}'>"
                            personDisplay += "<br><font style='font-size:20px;font-weight: bold'>${settings["person${personId}Name"]}</font></td>"
                            personDisplay += "</tr></table>"
                            paragraph personDisplay
                            input name: "place${placeId}Person${personId}Sensor", type: "capability.presenceSensor", title: "Place Presence Sensor for ${settings["person${personId}Name"]}", submitOnChange: false, multiple: false, required: false, width: 4
                        }   
                        
                    }
                    paragraph "Select garage door(s), contact sensor(s), and/or switch(es) that change when someone is arriving or departing."
                    input name: "place${placeId}GarageDoor", type: "capability.garageDoorControl", title: "Garage Door(s)", submitOnChange: false, multiple: true, required: false, width: 4
                    input name: "place${placeId}ContactSensor", type: "capability.contactSensor", title: "Contact Sensor(s)", submitOnChange: false, multiple: true, required: false, width: 4
                    input name: "place${placeId}Switch", type: "capability.switch", title: "Switch(es)", submitOnChange: false, multiple: true, required: false, width: 4
                }
                input name: "submitEditPlace", type: "button", title: "Submit", width: 3
                input name: "cancelEditPlace", type: "button", title: "Cancel", width: 3
            }            
            else {               
                input name: "addPlace", type: "button", title: "Add Place", width: 3                
                input name: "editPlace", type: "button", title: "Edit Place", width: 3
                app.clearSetting("placeToEdit")
                input name: "deletePlace", type: "button", title: "Delete Place", width: 3
                app.clearSetting("placeToDelete")
            } 

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


def getPlacesDescription() {
    def places = getPlacesEnumList()
    def description = ""
    for (i=0; i < places.size(); i++) {
         description += places[i] 
        if (i != places.size()-1) description += ", "
    }
    return description
}

def getPlaceAddress(name) {
    def id = getIdOfPlaceWithName(name)
    return settings["place${id}Address"]
}
def getPlaceIcon(name) {
    def id = getIdOfPlaceWithName(name)
    return settings["place${id}Icon"]
}

def addPlace(id) {
    if (!state.places) state.places = []
    state.places.add(id)
}

def getIdOfPlaceWithName(name) {
    for (id in state.places) {
        if (settings["place${id}Name"] == name) return id
    }
    return null
}

def getNameOfPlaceWithId(id) {
    return settings["place${id}Name"]
}


def getIdOfPlaceWithAddress(address) {
    for (id in state.places) {
        if (settings["place${id}Address"] == address) return id
    }
    return null
}

def deletePlace(nameToDelete) {
    def idToDelete = getIdOfPlaceWithName(nameToDelete)
    if (idToDelete && state.places) {       
        state.places.removeElement(idToDelete)
        app.clearSetting("place${idToDelete}Name")
        app.clearSetting("place${idToDelete}Icon")
        if (state.people) {
            state.people.each { personId, person ->
                app.updateSetting("place${idToDelete}Person${personId}Sensor",[type:"capability",value:[]])
                app.updateSetting("place${idToDelete}GarageDoor",[type:"capability",value:[]])
                app.updateSetting("place${idToDelete}ContactSensor",[type:"capability",value:[]])
                app.updateSetting("place${idToDelete}Switch",[type:"capability",value:[]])
            }
        }
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
   childApps.each { child ->
    	child.initialize()
    }
    unsubscribe()
    subscribeTriggers()
    unschedule()
    scheduleTimeTriggers()
    initializeTrackers()
}

def scheduleTimeTriggers() {
    if (state.trips) {
        for (tripId in state.trips) {  
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

def earlyTripFetchHandler(data) {
    def tripId = data.tripId
}

def startDepartureWindowHandler(data) {
    def tripId = data.tripId
}


def endDepartureWindowHandler(data) {
    def tripId = data.tripId
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
                subscribe(settings["person${id}Life360"], "address1", life360AddressHandler)
                subscribe(settings["person${id}Life360"], "isDriving", life360DrivingHandler)
            }           
        }
    }
}


def subscribeVehicles() {
    if (state.vehicles && state.people) {  
         for (vehicleId in state.vehicles) {
            state.people.each { personId, person ->
                if (settings["vehicle${vehicleId}Person${personId}Sensor"]) subscribe(settings["vehicle${vehicleId}Person${personId}Sensor"], "presence", vehiclePresenceSensorHandler)
            }
         }
    }
}
             

def subscribePlaces() {
    if (state.places && state.people) {                
           for (placeId in state.places) {
               state.people.each { personId, person ->
                    if (settings["place${placeId}Person${personId}Sensor"]) subscribe(settings["place${placeId}Person${personId}Sensor"], "presence", placePresenceSensorHandler)
               }
           }
    }    
}

def updateTracker(personId) {
    def tracker = getTracker(personId)
    if (tracker) {
        // update from state
    }   
}

def setPersonPlaceByName(personId, placeName) {    
    state.people.personId?.previous.place = state.people.personId?.current.place
    state.people.personId?.previous.place.departure = new Date()
    state.people.personId?.current.place.arrival = new Date()
    def placeIdByName = getIdOfPlaceWithName(placeName)
    def placeIdByAddress = getIdOfPlaceWithAddress(placeName)    // if life360 being used, placeName could be just an address
    if (placeIdByName) {
        state.people.personId?.current.place.id = placeIdByName
        state.people.personId?.current.place.name = placeName
    }
    else if (placeIdByAddress) {
        state.people.personId?.current.place.id = placeIdByAddress
        state.people.personId?.current.place.name = getNameOfPlaceWithId(placeIdByAddress)
    }
    else {    // place name is address of an unknown place
        state.people.personId?.current.place.id = null
        state.people.personId?.current.place.name = placeName
    }
    updateTracker(personId)
}

def setPersonPlaceById(personId, placeId) {    
    state.people.personId?.previous.place = state.people.personId?.current.place
    state.people.personId?.previous.place.departure = new Date()
    state.people.personId?.current.place.arrival = new Date()
    state.people.personId?.current.place.id = placeId
    state.people.personId?.current.place.name = getNameOfPlaceWithId(placeIdByAddress)
    updateTracker(personId)
}

def setPersonVehicleById(personId, vehicleId) {    
    state.people.personId?.previous.vehicle = state.people.personId?.current.vehicle
    state.people.personId?.previous.vehicle.departure = new Date()
    state.people.personId?.current.vehicle.arrival = new Date()
    state.people.personId?.current.vehicle.id = vehicleId
    updateTracker(personId)
}

def isLife360DeviceForPerson(personId, device) {
     if (settings["person${personId}Life360"] && settings["person${personId}Life360"] == device) return true
    return false
}

def life360AddressHandler(evt) {
    state.people?.each { personId, person ->
        if (isLife360DeviceForPerson(personId, evt.getDevice())) {
            setPersonPlaceByName(personId, evt.value)            
        }
    }
}

def life360DrivingHandler(evt) {
    state.people?.each { personId, person ->
        if (isLife360DeviceForPerson(personId, evt.getDevice())) {
            state.people.personId?.isDriving = evt.value
            updateTracker(personId)
        }
    }    
}
                    
def isVehicleDeviceForPerson(personId, vehicleId, device) {
     if (settings["vehicle${vehicleId}Person${personId}Sensor"] && settings["vehicle${vehicleId}Person${personId}Sensor"] == device) return true
    return false
}

def vehiclePresenceSensorHandler(evt) {
    for (vehicleId in state.vehicles) {
        state.people?.each { personId, person ->
            if (isVehicleDeviceForPerson(personId, vehicleId, evt.getDevice())) {
                setPersonVehicleById(personId, vehicleId)
            }
        }
    }
}
                    
def isPlaceDeviceForPerson(personId, placeId, device) {
     if (settings["place${placeId}Person${personId}Sensor"] && settings["place${placeId}Person${personId}Sensor"] == device) return true
    return false
}

def placePresenceSensorHandler(evt) {
    for (placeId in state.places) {
        state.people?.each { personId, person ->
            if (isPlaceDeviceForPerson(personId, placeId, evt.getDevice())) {
                setPersonPlaceById(personId, placeId)
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
                for (id in state.trips) {
                    tripsDisplay += "" + ((i % 2 == 0) ? "<tr>" : "") + "<td align=center style:'width=50%'><img style='max-width:100px' border=0  src='${getPlaceIcon(settings["trip${id}Origin"])}'> --> <img style='max-width:100px' border=0 src='${getPlaceIcon(settings["trip${id}Destination"])}'><br><font style='font-size:20px;font-weight: bold'>${getNameOfTripWithId(id)}</font></td>" + ((i % 4 == 1) ? "</tr>" : "")
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
                input name: "cancelEditTrip", type: "button", title: "Cancel", width: 3
            }            
            else {               
                input name: "addTrip", type: "button", title: "Add Trip", width: 3                
                input name: "editTrip", type: "button", title: "Edit Trip", width: 3
                app.clearSetting("tripToEdit")
                input name: "deleteTrip", type: "button", title: "Delete Trip", width: 3
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

def getTripEnumList() {
    def list = []
    if (state.trips) {
        for (id in state.trips) {
            def tripName = getNameOfTripWithId(id)
            list.add(tripName)
        }
    }
    return list
}

def getIdOfTripWithName(name) {
    for (id in state.trips) {
        def tripName = getNameOfTripWithId(id)
        if (tripName == name) return id
    }
    log.warn "No Trip Found With the Name: ${name}"
    return null    
}

def addTrip(id) {
    if (!state.trips) state.trips = []
    state.trips.add(id)
}

def getNameOfTripWithId(id) {
    return settings["trip${id}Origin"] + " to " + settings["trip${id}Destination"]
}

def deleteTrip(nameToDelete) {
    def idToDelete = getIdOfTripWithName(nameToDelete)
    if (idToDelete && state.trips) {       
        state.trips.removeElement(idToDelete)
        app.clearSetting("trip${id}Origin")
        app.clearSetting("trip${id}Destination")
        app.clearSetting("trip${id}People")
        app.clearSetting("trip${id}Vehicles")
        app.clearSetting("trip${id}Days")
        app.clearSetting("trip${id}EarliestDepartureTime")
        app.clearSetting("trip${id}LatestDepartureTime")
        app.clearSetting("trip${id}TargetArrivalTime")
    }
}

def getEarlyFetchMinsSetting() {
    return (earlyFetchMins) ? earlyFetchMins : earlyFetchMinsDefault
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


def logDebug(msg) 
{
    if (logEnable)
    {
        log.debug(msg)
    }
}
