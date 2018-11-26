/**
 *  ADT Tools 2.0
 *
 *  Copyright 2018 CRAIG KING
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "ADT Tools 2",
    namespace: "Mavrrick",
    author: "CRAIG KING",
    description: "Smartthing ADT tools for additional functions ",
    category: "Safety & Security",
    iconUrl: "https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg",
    iconX2Url: "https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg",
    iconX3Url: "https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg",
    singleInstance: true)

/* 
* Initial release v2.0.0
*
*/

preferences
{
	page (name: "mainPage", title: "ADT Tools")
	page (name: "adtNotifier", title: "ADT Custom Notification")
	page (name: "adtModeChange", title: "Setup mode change settings")
	page (name: "adtAlertActions", title: "Work with ADT alarm alert actions")
   	page (name: "optionalSettings", title: "Optional Setup")
}



/* preferences {
    // The parent app preferences are pretty simple: just use the app input for the child app.
    page(name: "mainPage", title: "Tools", install: true, uninstall: true,submitOnChange: true) {
        section ("ADT Integration Apps"){
            app(name: "adtNotifier", appName: "ADT Notifier", namespace: "Mavrrick", title: "Create custom notification when alarm changes state", multiple: true)
            app(name: "adtModeChange", appName: "ADT Mode Change", namespace: "Mavrrick", title: "Allows changing alarm mode from smartapps", multiple: true)            
            }
        section ("Alarm Event Action Apps"){
            app(name: "adtContactAlarm", appName: "ADT Door or Window Alert", namespace: "Mavrrick", title: "Create new Door or Window triggered event action", multiple: true)
            app(name: "adtMotionAlarm", appName: "ADT Motion Alert", namespace: "Mavrrick", title: "Create new Motion triggered event action", multiple: true)
            app(name: "adtSmokeAlarm", appName: "ADT Smoke Alert", namespace: "Mavrrick", title: "Create new Smoke triggered event action", multiple: true)
            app(name: "adtWaterAlarm", appName: "ADT Water Alert", namespace: "Mavrrick", title: "Create new Water Leak triggered event action", multiple: true)
            app(name: "adtAnySensor", appName: "ADT Alert Any Sensor", namespace: "Mavrrick", title: "Allows Unmonitored Alarm action based on ADT status", multiple: true)            
		}
    }
}
*/

def initialize() {
    // nothing needed here, since the child apps will handle preferences/subscriptions
    // this just logs some messages for demo/information purposes
    log.debug "there are ${childApps.size()} child smartapps"
    childApps.each {child ->
        log.debug "child app: ${child.label}"
    }
    if (settings.createVirtButton) {
    				log.debug "initialize: Creating virtual button devices ADT Mode Change"
				addChildDevice("Mavrrick", "ADT Tools Button", "ADT Tools Disarmed", location.hubs[0].id, [
					"name": "ADT Tools Disarmed",
					"label": "ADT Tools Disarmed",
					"completedSetup": true, 					
				])
                				addChildDevice("Mavrrick", "ADT Tools Button", "ADT Tools Armed Stay", location.hubs[0].id, [
					"name": "ADT Tools Armed Stay",
					"label": "ADT Tools Armed Stay",
					"completedSetup": true, 					
				])
                				addChildDevice("Mavrrick", "ADT Tools Button", "ADT Tools Armed Away", location.hubs[0].id, [
					"name": "ADT Tools Armed Away",
					"label": "ADT Tools Armed Away",
					"completedSetup": true, 					
				])
                log.debug "ADT Tools Alarm Buttons created"
                }
	}


/*
	mainPage

	UI Page: Main menu for the app.
*/
def mainPage()
{
	dynamicPage(name: "mainPage", title: "ADT Tools Main Menu", uninstall: true, install: true)
	{
		section("ADT Integration Smartapps")
		{
			href "adtNotifier", title: "Alarm Mode Change Notifications", description: "Setup Custom notifications for alarm state change."
            href "adtModeChange", title: "ADT Smartthings Alarm Mode change integration", description: "Enables various functions around Mode change integration."
		}

		section("Alert Apps")
		{
			href "adtAlertActions", title: "Integration Alert Actions", description: "Setup Integration alert actions."
		}
		section("ADT Tools basic setup")
		{
			href "optionalSettings", title: "Optional setup steps", description: "Setup ADT Automation standard buttons."
			href "about", title: "About ADT Tools ", description: "Support the project...  Consider making a small contribution today!"
		}
	}
}

def adtNotifier()
{
	dynamicPage(name: "adtNotifier", title: "ADT Custon Notifications", uninstall: false, install: false)
	{
	section("Set Message for each state"){
		input "messageDisarmed", "text", title: "Send this message if alarm changes to Disarmed", required: false
        input "messageArmedAway", "text", title: "Send this message if alarm changes to Armed/Away", required: false
        input "messageArmedStay", "text", title: "Send this message if alarm changes to Armed/Stay", required: false
	}
	section("Via a push notification and/or an SMS message"){
		input("recipients", "contact", title: "Send notifications to") {
			input "phone", "phone", title: "Enter a phone number to get SMS", required: false
			paragraph "If outside the US please make sure to enter the proper country code"
			input "pushAndPhone", "enum", title: "Notify me via Push Notification", required: false, options: ["Yes", "No"]
		}
	}
	section("Minimum time between messages (optional, defaults to every message)") {
		input "frequency", "decimal", title: "Minutes", required: false
	}
    section ("Return to ADT Tools Main page"){
        href "mainPage", title: "ADT Tools Main Menu", description: "Return to main ADT Tools Main Menu"            
    }
	}
}

def adtModeChange()
{
	dynamicPage(name: "adtModeChange", title: "ADT Mode Change Integration", uninstall: false, install: false)
    {
	section("Select what button you want for each mode..."){
        input "myDisarmButton", "capability.momentary", title: "What Button will disarm the alarm?", required: false, multiple: false
        input "myArmStay", "capability.momentary", title: "What button will put the alarm in Armed/Stay?", required: false, multiple: false
        input "myArmAway", "capability.momentary", title: "What button will put the alarm in Armed/Away?", required: false, multiple: false
	}
   section("Smartthings location alarm state setup. These must be configured to use the Any Sensory Child App."){
   		input "locAlarmSync", "bool", title: "Maintain synchronization between Smartthings ADT alarm panel and location clound alarm state", description: "This switch will tell ADT Tools if it needs to kep the ADT Alarm and the Smarthings location alarm status in sync.", defaultValue: false, required: true, multiple: false
		input "delay", "number", range: "1..120", title: "Please specify your Alarm Delay", required: true, defaultValue: 0
	}
    
	section("Select your ADT Smart Panel..."){
		input "panel", "capability.securitySystem", title: "Select ADT Panel for Alarm Status", required: true
	}
    section ("Return to ADT Tools Main page"){
            href "mainPage", title: "ADT Tools Main Menu", description: "Return to main ADT Tools Main Menu"            
		}
    }
}

def adtAlertActions()
{
	dynamicPage(name: "adtAlertActions", title: "ADT Alert Actions ", uninstall: false, install: false)
    {
        section ("Alarm Event Action Apps"){
            app(name: "adtAlertAction", appName: "ADT Alert Action", namespace: "Mavrrick", title: "Security Alert Action apps", multiple: true)
            app(name: "adtHomeAction", appName: "ADT Home-Life Alert Action", namespace: "Mavrrick", title: "Home/Life Alert Action apps", multiple: true)
		}
        section ("Return to ADT Tools Main page"){
            href "mainPage", title: "ADT Tools Main Menu", description: "Return to main ADT Tools Main Menu"            
		}
    }
}

def optionalSettings()
{
	dynamicPage(name: "optionalSettings", title: "Optional settings", uninstall: false, install: false)
	{
        section ("Virtual Button Setup"){
	   		input "createVirtButton", "bool", title: "Would you like ADT Tools to create your virtual buttons for Mode change functianlity", description: "ADT Tools will attempt to create virtual devices for the mode change functinality", defaultValue: false, required: true, multiple: false
		}
        section ("Return to ADT Tools Main page"){
            href "mainPage", title: "ADT Tools Main Menu", description: "Return to main ADT Tools Main Menu"            
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
    initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
    initialize()
}

def uninstalled() {
    // external cleanup. No need to unsubscribe or remove scheduled jobs
    	// 1.4 Remove dead virtual devices
	getChildDevices()?.each
	{childDevice ->
//		if (settings.virtualCameraTiles.find{"ArloPilot_${it}" == childDevice.deviceNetworkId})
//		{
//			logTrace "initialize: Found active device: ${childDevice.label}."
//		}
//		else
//		{
//			logTrace "initialize: Removing unused device: ${childDevice.label}."
			deleteChildDevice(childDevice.deviceNetworkId)
		}
	}

def subscribeToEvents() {
    subscribe(myDisarmButton, "momentary.pushed", disarmHandler)
    subscribe(myArmStay, "momentary.pushed", armstayHandler)
    subscribe(myArmAway, "momentary.pushed", armawayHandler)
    subscribe(location, "securitySystemStatus", alarmModeHandler)
}

def msg = "" 

def disarmHandler(evt) {
      log.debug "Disarming alarm"
      panel?.disarm()
	}

def armstayHandler(evt) {
       log.debug "Changeing alarm to Alarm/Stay"
       def alarmState = panel.currentSecuritySystemStatus
        if (alarmState == "armedAway") {
        	log.debug "Current alarm mode: ${alarmState}. Alarm must be in Disarmed state before changeing state"
        }
        else {       
        panel?.armStay(armedStay)

        }
	}
    
def armawayHandler(evt) {
       	log.debug "Changeing alarm to Alarm/Away"
        def alarmState = panel.currentSecuritySystemStatus
        if (alarmState == "armedStay") {
        	log.debug "Current alarm mode: ${alarmState}. Alarm must be in Disarmed state before changeing state"
        }
        else {
      	panel?.armAway(armedAway)}
	   }

def alarmModeHandler(evt) {
	log.debug "Notify got evt ${evt}"
	if (frequency) {
		def lastTime = state[evt.deviceId]
		if (lastTime == null || now() - lastTime >= frequency * 60000) {
			sendMessage(evt)
		}
	}
	else {
		sendMessage(evt)
	}
	    if (settings.locAlarmSync) 
		{
	switch (evt.value)
        	{
            	case "armedAway":
        			runIn(delay, armawaySHMHandler)
                    break
                case "armedStay":
                	log.debug "Attempting change of Hub alarm Mode"
                    runIn(delay, armstaySHMHandler)
                    break
                case "disarmed" :
                    sendLocationEvent(name: "alarmSystemStatus", value: "off")
                    break
                default:
					log.debug "Ignoring unexpected alarmtype mode."
        			log.debug "Unexpected value for Alarm status"
                    break
                    }
        }
}

def armstaySHMHandler() {
       	log.debug "Changeing HUB alarm state to Armed/Stay"
        sendLocationEvent(name: "alarmSystemStatus", value: "stay")
	   }
       
def armawaySHMHandler() {
       	log.debug "Changeing HUB alarm state to Alarm/Away"
        sendLocationEvent(name: "alarmSystemStatus", value: "away")
	   } 

private sendMessage(evt) {

switch (evt.value)
    {
    case "armedAway":
    	def msg = messageArmedAway
           if ( msg == null ) {
        	log.debug "Message not configured. Skipping notification"
            }
        else {
        log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"

	Map options = [:]	

	if (location.contactBookEnabled) {
		sendNotificationToContacts(msg, recipients, options)
	} else {
		if (phone) {
			options.phone = phone
			if (pushAndPhone != 'No') {
				log.debug 'Sending push and SMS'
				options.method = 'both'
			} else {
				log.debug 'Sending SMS'
				options.method = 'phone'
			}
		} else if (pushAndPhone != 'No') {
			log.debug 'Sending push'
			options.method = 'push'
		} else {
			log.debug 'Sending nothing'
			options.method = 'none'
		}
		sendNotification(msg, options)
	}
	if (frequency) {
		state[evt.deviceId] = now()
	}
}
        break
    case "armedStay":
    	def msg = messageArmedStay
        if ( msg == null ) {
        	log.debug "Message not configured. Skipping notification"
            }
        else {
        log.debug "Case Armstay., '$msg'"
        log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"

	Map options = [:]	

	if (location.contactBookEnabled) {
		sendNotificationToContacts(msg, recipients, options)
	} else {
		if (phone) {
			options.phone = phone
			if (pushAndPhone != 'No') {
				log.debug 'Sending push and SMS'
				options.method = 'both'
			} else {
				log.debug 'Sending SMS'
				options.method = 'phone'
			}
		} else if (pushAndPhone != 'No') {
			log.debug 'Sending push'
			options.method = 'push'
		} else {
			log.debug 'Sending nothing'
			options.method = 'none'
		}
		sendNotification(msg, options)
	}
	if (frequency) {
		state[evt.deviceId] = now()
	}
}
        break
    case "disarmed":
    	def msg = messageDisarmed
        if ( msg == null ) {
        	log.debug "Message not configured. Skipping notification"
            }
        else {
        log.debug "Case disarmed., '$msg'"
        log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"

	Map options = [:]	

	if (location.contactBookEnabled) {
		sendNotificationToContacts(msg, recipients, options)
	} else {
		if (phone) {
			options.phone = phone
			if (pushAndPhone != 'No') {
				log.debug 'Sending push and SMS'
				options.method = 'both'
			} else {
				log.debug 'Sending SMS'
				options.method = 'phone'
			}
		} else if (pushAndPhone != 'No') {
			log.debug 'Sending push'
			options.method = 'push'
		} else {
			log.debug 'Sending nothing'
			options.method = 'none'
		}
		sendNotification(msg, options)
	}
	if (frequency) {
		state[evt.deviceId] = now()
	}
}
        break
    default:
		log.debug "Ignoring unexpected ADT alarm mode."
        log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"
        break
}

}