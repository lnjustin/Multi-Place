<p align="center">
  <img width="110px" style="display: block;margin-left: auto;margin-right: auto;margin-top:0px;" border="0" src="https://github.com/lnjustin/App-Images/raw/master/Multi-Place/logo.png">
  <br>
<img width="160px" style="display: block;margin-left: auto;margin-right: auto; margin-top: 3px;" border="0" src="https://github.com/lnjustin/App-Images/blob/master/Multi-Place/MP.png">
</p>

You don't stay in one place. The smarts of your Hubitat smart home shouldn't either. Powered by Google Directions, Multi-Place is a Hubitat App that extends your smart home's reach to your favorite places and the roads therebetween.
 
 <img width="250px" align="right" border="0" src="https://github.com/lnjustin/App-Images/raw/master/Multi-Place/Preview/presence.PNG">
<b>One presence tile. Multiple Places. Clean dashboard look.</b><br>
Multi-Place displays your presence across multiple places on a single dashboard tile, referred to in the app as a "Tracker", with a clean graphical look that is highly customizable. Multi-Place can even display your presence across vehicles, using either dedicated presence sensors or Life360's driving attribute. Moreover, Multi-Place integrates with Withings Sleep, to display your presence in bed. No matter where you are, Multi-Place centralizes the display of your presence into one clean graphic for your dashboard.

<b>Automate based on current traffic conditions.</b><br>
Before you typically leave for a certain destination, like work or school, Multi-Place checks traffic conditions for you. Set up Multi-Place to turn on a switch or notify you with a push notification if traffic is bad and  you need to leave X minutes earlier than usual. Or, configure Multi-Place to just discretely display that information on your dashboard only when necessary, so as to keep your dashboard clean and not interrupt you otherwise. Multi-Place stays aprised of traffic conditions shortly before and throughout the window of time that you typically depart for your destination, so you don't have to worry about missing a traffic update.

  <img width="250px" align="right" border="0" style="margin-top:5px" src="https://github.com/lnjustin/App-Images/raw/master/Multi-Place/Preview/route.PNG">
<b>Always take the best route.</b><br>
When Multi-Place checks traffic conditions, it also identifies the best route for you to take to your destination. Set up Multi-Place to display the best route on the dashboard you have beside your door, so that one glance informs you of which way to go to your destination. Or, configure Multi-Place to send you a push notification with the best route when you get in your vehicle or open the garage door during your typical departure time window, so as to have timely traffic information exactly when you need it. Better yet, tell Multi-Place the route you prefer to take typically, and Multi-Place will only display or push route information if the best route to take is different than your preferred route. That way, you're only bothered if you need to take a different route than you would have taken anyway.
<br><br>
<b>Automate based on departure and arrival for a typical trip.</b><br>
Multi-Place can trigger automations based on your departure from the origin of a typical trip and/or your arrival at the destination of that trip. For example, configure Multi-Place to turn the kitchen pendant light to green when you get in the car to leave work and head home, in order to notify your family members at home that you're headed home. Or, tell Multi-Place the time your teenager is supposed to be at school, and selectively get a push notification if he or she arrives X minutes later than the required time.

  <img width="250px" align="right" border="0" src="https://github.com/lnjustin/App-Images/raw/master/Multi-Place/Preview/arrival.PNG">
<b>Keep others up-to-date on your ETA.</b><br>
Once you depart on a trip, Multi-Place determines your estimated time of arrival (ETA) at the trip's destination, given current traffic conditions. Multi-Place displays your ETA on your dashboard, so others at home can know when you'll arrive. You can also make Multi-Place smart enough to know when you're typically supposed to be at a certain destination. That way, if you're going to be late, either because you haven't left yet or you're stuck in traffic, Multi-Place can automatically send a push notification to someone else to notify them that you are going to be at least X minutes late. No more getting stuck in a meeting at work and getting grief for neglecting to at least send a note that you're running late; Multi-Place can do it for you now, automatically.

<br>
<img width="250px" align="right" border="0" src="https://github.com/lnjustin/App-Images/raw/master/Multi-Place/Preview/sleepWin.PNG">
<b>Sleep. Compete. Repeat.</b><br>
Multi-Place's integration with Withings Sleep allows your family to compete with one another in terms of your sleep scores. Multi-Place awards a daily ribbon, a weekly trophy, and a monthly trophy to the person with the best sleep score. Get to sleep and show off your bling in the morning.

<hr><br>
<p>
<b>App Setup</b>
<br>
1. Create one or more people in Multi-Place by giving each person a name and a picture of your choosing. Tell Multi-Place which Life360 device and/or which Withings Sleep device is specific to each person.<br>
2. Create one or more vehicles in Multi-Place by giving each vehicle a name and a picture of your choosing. Tell Multi-Place which sensors in Hubitat indicate which person's presence in each vehicle.<br>
3. Create one or more places in Multi-Place by giving each place a name, an address, and a picture of your choosing. Tell Multi-Place which sensors in Hubitat indicate which person's presence in each place. If you don't have a sensor for each place, just name each place the same as you do in Life360 and Multi-Place will detect your presence at that place when Life360 does.<br>
4. Create one or more trips in Multi-Place by defining an origin and destination for the trip, as well as which people and vehicles are associated with that trip. Tell Multi-Place a window of time during which you typically depart on the trip, as well as a target arrival time. Select which automations to configure for the trip, such as by selecting which devices to push which notifications to.<br>
5. Set up restrictions on when Multi-Place detects trip departures and checks travel conditions. For example, you can configure Multi-Place to not do those things when your Hubitat hub is in vacation mode.<br>
6. Click Done on the App's main page, and Multi-Place does the rest.<br>
7. For each person, Multi-Place creates a "Tracker" device with the name of "[Person's Name] Multi-Place Tracker". This Tracker device has an attribute called "tracker" with the graphical display that you can add to your dashboard. Multi-Place also outputs this graphical tracker at a cloud endpoint in SVG format, for use in dashboards like Sharptools that require SVG.
</p>
<p>
<b>Dashboard "Tracker" Tile</b>
<br>
Multi-Place can output your graphical "Tracker" tile in a format that is supported by most dashboards. The Tracker tile is included as an attribute in a custom "Tracker" device, for quickly adding to dashboards that support HTML, like the native Hubitat dashboard. Any image format, including bitmap images, are supported under this approach. If your dashboard only supports images, not HTML, Multi-Place can instead output the "Tracker" as an SVG image, provided that you use only SVG images for your avatar and icons. This is the case for those using the Sharptools dashboard.
</p>
<p>
  <b>Custom Icons</b>
  <br>
  Custom place and vehicle icons should be trimmed to the edge of the content, leaving no transparent margin. SVG icons will likely work best to scale seamlessly and provide the most flexibility in terms of dashboard support. SVGs in this regard are imported from the custom URL link you define. Import happens when you click "Done" on the app's main page. So any external changes to the SVG icon at that link will need to be re-imported by clicking "Done" again on the app's main page. Please contribute to the repository of built-in custom icons for the app, by sending a pull request <a href="https://github.com/lnjustin/App-Images/tree/master/Multi-Place">here</a>.
  </p>
  <b>Custom Avatar</b>
  <br>
  Custom avatar images may be bitmap or SVG (see note above regarding dashboard compatibility though). Images should have square dimensions, e.g., 200 x 200.
  </p>
<b>Recommended Configuration: Life360 + Vehicle Presence Sensors</b>
<br>
Multi-Place proves useful in any configuration, but to get the most out of Multi-Place, use an app like Life360 in conjunction with presence sensors for your vehicle(s). Multi-Place exploits the circles you define in Life360 for your presence across different places. Just name the places the same as in Life360, tell Multi-Place the address for those places, and you're set. You could also use any app that tracks your location using GPS and is capable of sending a webhook that Hubitat can use to set a virtual presence sensor. The same idea can be used in conjunction with beacons to capture presence in your vehicle. For example, here are steps you can use to set up presence sensors for a vehicle:<br><br>
1. Purchange a beacon for your car. Recommendation: RadBeacon by Radius Networks ($30). If your vehicle has a USB port, the RadBeacon USB plugs right into it and works well.<br>
2. Install an app on your phone capable of detecting beacons and sending webhooks. The GeoFency app for iOS works well, for example.<br>
3. Configure Hubitat with a virtual presence sensor for your vehicle.<br>
4. Configure Rule Machine with two rules: (1) a rule that sets the virtual sensor to present upon a cloud end point trigger; and (2) a rule that sets the virtual sensor to absent upon a different cloud end point trigger. Example rule below.<br>
5. Configure the app on your phone to send a webhook to rule (1) upon detection of the beacon, and to send a webhook to rule (2) upon departure from the beacon. See picture from Geofency below.<br>
6. At least with the Geofency app, you can set a threshold for how long the beacon has to be detected to be considered present and a separate threshold for how long the beacon has to be not detected to be considered absent. Adjust these thresholds until you find reliable presence as well as timely presence events.<br>
<img width="250px" border="0" src="https://github.com/lnjustin/App-Images/raw/master/Multi-Place/Preview/RM.png">
<img width="250px"  border="0" src="https://github.com/lnjustin/App-Images/raw/master/Multi-Place/Preview/rule.png">

<br>
<b>Hubitat Package Manager Install Instructions</b><br>
1. Install Multi-Place Package via Hubitat Package Manager<br>
2. Follow the install instructions in the Multi-Place app<br>

<br>
<b>Hubitat Manual Install Instructions</b><br>
1. Install the Multi-Place Tracker driver<br>
2. Install the Multi-Place app<br>
3. Enable OAuth in the Multi-Place app<br>
4. Follow the install instructions in the Multi-Place app<br>
