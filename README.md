<p align="center">
  <img width="110px" style="display: block;margin-left: auto;margin-right: auto;margin-top:0px;" border="0" src="https://github.com/lnjustin/App-Images/raw/master/Multi-Place/logo.png">
  <br>
<img width="160px" style="display: block;margin-left: auto;margin-right: auto; margin-top: 3px;" border="0" src="https://github.com/lnjustin/App-Images/blob/master/Multi-Place/MP.png">
</p>

You don't stay in one place. The smarts of your smart home shouldn't either. Powered by Google Directions, Multi-Place extends your smart home's reach to your favorite places and the roads therebetween.

<b>One presence tile. Multiple Places. Clean dashboard look.</b>
Multi-Place displays your presence across multiple places on a single dashboard tile, referred to in the app as a "Tracker", with a clean graphical look that is highly customizable. Multi-Place can even display your presence across vehicles, using either dedicated presence sensors or Life360's driving attribute. Moreover, Multi-Place integrates with Withings Sleep, to display your presence in bed. No matter where you are, Multi-Place centralizes the display of your presence into one clean graphic for your dashboard.

<b>Automate based on current traffic conditions.</b><br>
Before you typically leave for a certain destination, like work or school, Multi-Place checks traffic conditions for you. Set up Multi-Place to turn on a switch or notify you with a push notification if traffic is bad and  you need to leave X minutes earlier than usual. Or, configure Multi-Place to just discretely display that information on your dashboard only when necessary, so as to keep your dashboard clean and not interrupt you otherwise. Multi-Place stays aprised of traffic conditions shortly before and throughout the window of time that you typically depart for your destination, so you don't have to worry about missing a traffic update.

  <img width="250px" align="right" border="0" style="margin-top:5px" src="https://github.com/lnjustin/App-Images/raw/master/Multi-Place/Preview/routeInfo.PNG">
<b>Always take the best route.</b><br>
When Multi-Place checks traffic conditions, it also identifies the best route for you to take to your destination. Set up Multi-Place to display the best route on the dashboard you have beside your door, so that one glance informs you of which way to go to your destination. Or, configure Multi-Place to send you a push notification with the best route when you get in your vehicle or open the garage door during your typical departure time window, so as to have timely traffic information exactly when you need it. Better yet, tell Multi-Place the route you prefer to take typically, and Multi-Place will only display or push route information if the best route to take is different than your preferred route. That way, you're only bothered if you need to take a different route than you would have taken anyway.
<br><br>
<b>Automate based on departure and arrival for a typical trip.</b><br>
Multi-Place can trigger automations based on your departure from the origin of a typical trip and/or your arrival at the destination of that trip. For example, configure Multi-Place to turn the kitchen pendant light to green when you get in the car to leave work and head home, in order to notify your family members at home that you're headed home. Or, tell Multi-Place the time your teenager is supposed to be at school, and selectively get a push notification if he or she arrives X minutes later than the required time.

  <img width="250px" align="right" border="0" src="https://github.com/lnjustin/App-Images/raw/master/Multi-Place/Preview/eta.PNG">
<b>Keep others up-to-date on your ETA.</b><br>
Once you depart on a trip, Multi-Place determines your estimated time of arrival (ETA) at the trip's destination, given current traffic conditions. Multi-Place displays your ETA on your dashboard, so others at home can know when you'll arrive. You can also make Multi-Place smart enough to know when you're typically supposed to be at a certain destination. That way, if you're going to be late, either because you haven't left yet or you're stuck in traffic, Multi-Place can automatically send a push notification to someone else to notify them that you are going to be at least X minutes late. No more getting stuck in a meeting at work and getting grief for neglecting to at least send a note that you're running late; Multi-Place can do it for you now, automatically.

<br>
<img width="250px" align="right" border="0" src="https://github.com/lnjustin/App-Images/raw/master/Multi-Place/Preview/bed.PNG">
<b>Sleep. Compete. Repeat.</b>
Multi-Place's integration with Withings Sleep allows your family to compete with one another in terms of your sleep scores. Multi-Place awards a daily ribbon, a weekly trophy, and a monthly trophy to the person with the best sleep score. Get to sleep and show off your bling in the morning.

<hr><br>
<p>
<b>Dashboard Tile Output Format</b>
<br>
Multi-Place can output your graphical "Tracker" tile in a format that is supported by most dashboards. The Tracker tile is included as an attribute in a custom "Tracker" device, for quickly adding to dashboards that support HTML, like the native Hubitat dashboard. Any image format, including bitmap images, are supported under this approach. If your dashboard only supports images, not HTML, Multi-Place can instead output the "Tracker" as an SVG image, provided that you use only SVG images for your avatar and icons. This is the case for those using the Sharptools dashboard.
</p>
<br>
<b>Recommended Configuration</b>
<br>
Multi-Place proves useful in any configuration, but for the best performance and the most timely presence updates and traffic information, presence sensors for your vehicle(s) are quite helpful. Here are example steps to set that up:<br>
1. Purchange a beacon for your car. Recommendation: RadBeacon by Radius Networks ($30). If your vehicle has a USB port, the RadBeacon USB plugs right into it and works well.<br>
2. Install an app on your phone capable of detecting beacons and sending webhooks. The GeoFency app for iOS works well, for example.<br>
3. Configure Hubitat with a virtual presence sensor for your vehicle.<br>
4. Configure Rule Machine with two rules: (1) a rule that sets the virtual sensor to present upon a cloud end point trigger; and (2) a rule that sets the virtual sensor to absent upon a different cloud end point trigger. Example rule below.<br>
5. Configure the app on your phone to send a webhook to rule (1) upon detection of the beacon, and to send a webhook to rule (2) upon departure from the beacon.<br>
6. At least with the Geofency app, you can set a threshold for how long the beacon has to be detected to be considered present and a separate threshold for how long the beacon has to be not detected to be considered absent. Adjust these thresholds until you find reliable presence as well as timely presence events.<br>

<br>
<b>Hubitat Package Manager Install Instructions</b><br>
1. Install Multi-Place Package via Hubitat Package Manager<br>
2. Follow the install instructions in the Multi-Place app<br>

<br>
<b>Manual Install Instructions</b><br>
1. Install the Multi-Place Tracker driver<br>
2. Install the Multi-Place app<br>
3. Enable OAuth in the Multi-Place app<br>
4. Follow the install instructions in the Multi-Place app<br>
