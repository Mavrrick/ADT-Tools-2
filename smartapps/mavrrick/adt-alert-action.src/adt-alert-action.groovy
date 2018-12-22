/**
 *  ADT Alert Action
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
    name: "ADT Alert Action",
    namespace: "Mavrrick",
    author: "CRAIG KING",
    description: "Smartthing ADT tools for additional functions ",
    category: "Safety & Security",
    parent: "Mavrrick:ADT Tools 2",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

/* 
*
* Initial release v1.0.0
* Initial release of Combined Alert Action app.. This is intial release of child app
*
* v.1.0.0a
* Added missed value for unmonitored sensor functionality.
*
*/
import groovy.time.TimeCategory

preferences {
	page (name: "mainPage", title: "ADT Alert App ")
	page (name: "adtTrigger", title: "Alert Trigger Devices")
	page (name: "adtActions", title: "Alert Actions triggered by event")
    page (name: "adtLightsOpt", title: "Optional Light Flash Setup")
	page (name: "adtCameraSetup", title: "Camera setup")
    page (name: "notificationSetup", title: "Notification Setup")
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
	if (settings.alertTrgType) {       
        subscribe(location, "alarm", adtAlarmHandler)
        }
    else {
    	subscribe(location, "securitySystemStatus", alarmHandler)
        }
	if (contact) {
		subscribe(contact, "contact.open", triggerHandler)
	}
	if (motion) {
		subscribe(motion, "motion.active", triggerHandler)
	}
}

def mainPage()
{
	dynamicPage(name: "mainPage", title: "ADT Alert Action", uninstall: true, install: true)
	{
    	section(title: "Security Alert Action Name") {
        	label title: "Please name this security alert action", required: true, defaultValue: "Security alert action"
        }
		section("Alert Action Trigger type")
		{
        	paragraph "Change the value below value only if you want to use non dual branded sensors and use your this alert action in a unmonitored condition."       	
			input "alertTrgType", "bool", title: "Are all sensors in this alert action ADT/Smarthings dual branded", description: "Determines if using ADT dual branded sensors or generic sensors.", defaultValue: true, required: true, multiple: false
			href "adtTrigger", title: "Select triggers", description: "Select alert trigger devices"
		}

		section("Alert Action after alarm is triggered")
		{
			href "adtActions", title: "Alarm Event Actions", description: "Actions that will trigger from alarm event. "
           	href "adtCameraSetup", title: "Camera setup for Alert Action", description: "Camera setup for Alert Action"
		}
		section("Messeging options")
		{
			href "notificationSetup", title: "Notification setup", description: "Notification values for smartapp"
		}
	}
}

def adtTrigger()
{
	dynamicPage(name: "adtTrigger", title: "Alert Triggers to be used", nextPage: "adtActions")
	{
		section("Select your Alert Devices")
		{
			if (settings.alertTrgType) {
            paragraph "This event is being configured as a ADT Sensor event and should only use ADT sensors. Please select the correct sensors from the types below"       	
            input "adtcontact", "capability.contactSensor", title: "Look for ADT Activity on these contact sesors", required: false, multiple: true
            input "adtmotion", "capability.motionSensor", title: "Look for ADT Activity on these motion sesors", required: false, multiple: true
            }
            else {
            paragraph "This event is being configured as a generic sensor event and can use any sensor. This should not be used if you want to use ADT Monitoring or only use ADT sensors. Please select the correct sensors from the types below" 
       		paragraph "What Active alarm mode do you want to monitor for 1= Arm/Stay, 2=Armed/Away. All other numberical valudes wil be ignored"
        	input "alarmtype2", "number", title: "What type of alarm do you want to trigger", required: false, defaultValue: 1        
            input "contact", "capability.contactSensor", title: "Use these sensors for Unmonitored security alerts", required: false, multiple: true
            input "motion", "capability.motionSensor", title: "Look for ADT Activity on these motion sesors", required: false, multiple: true
			input "panel", "capability.securitySystem", title: "Select ADT Panel for Alarm Status", required: true
            }
		}
        section ("Return to ADT Tools Main page"){
            href "mainPage", title: "Return to the previous menu", description: "Return to the previous menu to complete setup"            
		}
	}
}

def adtActions()
{
	dynamicPage(name: "adtActions", title: "Alarm action", nextPage: "adtLightsOpt")
	{
		section()
		{
        	input "alarms", "capability.alarm", title: "Which Alarm(s) to trigger when ADT alarm goes off", multiple: true, required: false
        	paragraph "Valid alarm types are 1= Siren, 2=Strobe, and 3=Both. All other numberical valudes wil be ignored"
        	input "alarmtype", "number", title: "What type of alarm do you want to trigger", required: false, range: "1..3", defaultValue: 3
        	paragraph "Valid Light actions are are 1 = None, 2 = Turn on lights, 3 = Flash Lights and 4 = Both. All other numberical valudes wil be ignored"
        	input "lightaction", "number", title: "What type of light action do you want to trigger", required: true, range: "1..4", defaultValue: 1
        	paragraph "If you choose Light action 4 do not select the same lights in both values"
        	input "switches2", "capability.switch", title: "Turn these lights on if Light action is set to 2 or 4", multiple: true, required: false
        	input "switches", "capability.switch", title: "Flash these lights (optional) If Light action is set to 3 or 4", multiple: true, required: false
            
		}
        section ("Optional Light Configuration"){
            href "adtLightsOpt", title: "Change Light default values", description: "Change default values for flashing lights"            
		}
        section ("Return to ADT Tools Main page"){
            href "mainPage", title: "Return to the previous menu", description: "Return to the previous menu to complete setup"            
		}
	}
}

def adtLightsOpt()
{
	dynamicPage(name: "adtLightsOpt", title: "Optional Light Setup", nextPage: "adtCameraSetup")
	{
		section(){
		input "onFor", "number", title: "On for (default 5000)", required: false
		input "offFor", "number", title: "Off for (default 5000)", required: false
        input "numFlashes", "number", title: "This number of times (default 3)", required: false
		}
        section ("Return to ADT Tools Main page"){
            href "mainPage", title: "Return to the previous menu", description: "Return to the previous menu to complete setup"            
		}
	}
}

def adtCameraSetup()
{
	dynamicPage(name: "adtCameraSetup", title: "Camera Setup", nextPage: "notificationSetup")
	{
    	section("Camera setup (Optional)"){
        	input "recordCameras", "bool", title: "Enable Camera recording?", description: "This switch will enable cameras to record on alarm events.", defaultValue: false, required: true, multiple: false
			input "recordRepeat", "bool", title: "Enable Camare to trigger recording as long as arlarm is occuring?", description: "This switch will enable cameras generate new clips as long as thre is a active alarm.", defaultValue: false, required: true, multiple: false
			input "cameras", "capability.videoCapture", multiple: true, required: false
        	input name: "clipLength", type: "number", title: "Clip Length", description: "Please enter the length of each recording", required: true, range: "5..120", defaultValue: 120
        }
        section ("Return to ADT Tools Main page"){
            href "mainPage", title: "Return to the previous menu", description: "Return to the previous menu to complete setup"            
		}
	}
}

def notificationSetup()
{
	dynamicPage(name: "notificationSetup", title: "Notification setup", nextPage: "mainPage")
	{
        section("Via a push notification and/or an SMS message"){
        input "message", "text", title: "Send this message if activity is detected", required: false
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
	}
}

def devices = " "

def alarmHandler(evt) {
	if (evt.value == 'disarmed') {
    log.debug "Alarm switch to disarmed. Turing off siren."
    	alarms?.off() }
}

def triggerHandler(evt) {
/*        def alarmState = panel.currentSecuritySystemStatus  */
        def alarmState = location.currentState("alarmSystemStatus")?.value
		if (alarmState == "stay" && alarmtype2 == 1) {
        log.debug "Current alarm mode: ${alarmState}."
		alarmAction()
        }
        else if (alarmState == "away" && alarmtype2 == 2) {
        log.debug "Current alarm mode: ${alarmState}."
        alarmAction()
        }
        else
        log.debug "Current alarm mode: ${alarmState}. Ignoring event"
    }
    
def alarmAction()    
	{
        switch (alarmtype.value)
        	{
            	case 1 :
                	log.debug "Alarm type ${alarmtype.value} detected. Turning on siren"
                    alarms?.siren()
                    break
                case 2 :
                	log.debug "Alarm type ${alarmtype.value} detected. Turning on strobe"
                    alarms?.strobe()
                    break
                case 3 :
                	log.debug "Alarm type ${alarmtype.value} detected. Turning on Siren and Strobe"
                    alarms?.both()
                    break
                default:
					log.debug "Ignoring unexpected alarmtype mode."
        			log.debug "Alarm type ${alarmtype.value} detected"
                    break
                    }
         switch (lightaction.value)
        	{
            	case 1 :
                	log.debug "Light action ${lightaction.value} detected. No Light Action"                    
                    break
                case 2 :
                	log.debug "Light action ${lightaction.value} detected. Turning on selected lights"
                    switches2?.on()
                    switches2?.setLevel(100)
                    break
                case 3 :
                	log.debug "Light Action ${lightaction.value} detected. Flashing Selected lights"                    
                    flashLights()
                    break
                case 4 :
                	log.debug "Light Action ${lightaction.value} detected. Flash and turning on selected lights"
                    switches2?.on()
                    switches2?.setLevel(100)
                    flashLights()
                    break
                default:
					log.debug "Ignoring unexpected Light Action type."
        			log.debug "Light Action ${lightaction.value} detected"
                    break
			} 		
	sendnotification()
	if (settings.recordCameras)
	{
		cameraRecord()
	}
}


def continueFlashing()
{
	unschedule()
	if (state.alarmActive) {
		flashLights(10)
		schedule(util.cronExpression(now() + 10000), "continueFlashing")
	}
}

private flashLights() {
	def doFlash = true
	def onFor = onFor ?: 5000
	def offFor = offFor ?: 5000
	def numFlashes = numFlashes ?: 3

	log.debug "LAST ACTIVATED IS: ${state.lastActivated}"
	if (state.lastActivated) {
		def elapsed = now() - state.lastActivated
		def sequenceTime = (numFlashes + 1) * (onFor + offFor)
		doFlash = elapsed > sequenceTime
		log.debug "DO FLASH: $doFlash, ELAPSED: $elapsed, LAST ACTIVATED: ${state.lastActivated}"
	}

	if (doFlash) {
		log.debug "FLASHING $numFlashes times"
		state.lastActivated = now()
		log.debug "LAST ACTIVATED SET TO: ${state.lastActivated}"
		def initialActionOn = switches.collect{it.currentSwitch != "on"}
		def delay = 0L
		numFlashes.times {
			log.trace "Switch on after  $delay msec"
			switches.eachWithIndex {s, i ->
				if (initialActionOn[i]) {
					s.on(delay: delay)
				}
				else {
					s.off(delay:delay)
				}
			}
			delay += onFor
			log.trace "Switch off after $delay msec"
			switches.eachWithIndex {s, i ->
				if (initialActionOn[i]) {
					s.off(delay: delay)
				}
				else {
					s.on(delay:delay)
				}
			}
			delay += offFor
		}
	}
}

def sendnotification() {
def msg = message
        if ( msg == null ) {
        	log.debug "Message not configured. Skipping notification"
            }
        else {
        log.debug "Alarm Notification., '$msg'"
/*        log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'" */

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
}

def cameraRecord() {	
	log.debug "Refreshing cameras with ${clipLength} second capture"
    Date start = new Date()
    Date end = new Date()
    use( TimeCategory ) {
    	end = start + clipLength.seconds
 	} 
    log.debug "Capturing..."
    cameras.capture(start, start, end)
    	if (settings.recordRepeat)
	{
		runIn(clipLength, cameraRepeatChk)
	}
}

def cameraRepeatChk() {
		def alarmActive = panel.currentSecuritySystemStatus
    	log.debug "Current alarms is in ${alarmActive} state"
		if (alarmActive != "disarmed") 
        	{
        	log.debug "Alarm Event is still occuring. Submitting another clip to record"
        cameraRecord()   
        	}
		else {
        log.debug "Alarm has cleared and is no longer active recordings are stoping."
		}
        }
        
def adtAlarmHandler(evt) {

switch (evt.value)
	{
    case "CLEARED":
    log.debug "Notify got alarm clear event ${evt}"
    alarms?.off()
    	break 
    case "siren":
    log.debug "siren turned on"
    	break
    case "strobe":
    log.debug "Strobe is turned on"
    	break
    case "both":
    log.debug "Siren and Strobe turned on"
    	break
    case "off":
    log.debug "Siren and Strobe turned off"
    	break
    default:
	log.debug "Notify got alarm event ${evt}"
    log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"
        log.debug "The event id to be compared is ${evt.value}"     
		if (adtcontact && adtmotion) {
        log.debug "The event id to be compared is ${settings.adtcontact} and ${adtmotion}"
		def devices = settings.adtcontact + settings.adtmotion
        log.debug "These devices were found ${devices.id} are being reviewed."
    	devices.findAll { it.id == evt.value } .each { 
        log.debug "Found device: ID: ${it.id}, Label: ${it.label}, Name: ${it.name}, Security event found" 
        adtActionHandler()
        	}
        	}
        else if (adtcontact) {
        log.debug "The event id to be compared is ${settings.adtcontact}"
        def devices = settings.adtcontact
        log.debug "These devices were found ${devices.id} are being reviewed."
    	devices.findAll { it.id == evt.value } .each { 
        log.debug "Found device: ID: ${it.id}, Label: ${it.label}, Name: ${it.name}, Security event found"
        adtActionHandler()
        	}
        	}
        else if (adtmotion) {
        log.debug "The event id to be compared is ${adtmotion}"
        def devices = settings.adtmotion
        log.debug "These devices were found ${devices.id} are being reviewed."
    	devices.findAll { it.id == evt.value } .each { 
        log.debug "Found device: ID: ${it.id}, Label: ${it.label}, Name: ${it.name}, Security event found"
        adtActionHandler()
        	}
            }
		break
}
}        
        
def adtActionHandler() {        
        switch (alarmtype.value)
        	{
            	case 1 :
                	log.debug "Alarm type ${alarmtype.value} detected. Turning on siren"
                    alarms?.siren()
                    break
                case 2 :
                	log.debug "Alarm type ${alarmtype.value} detected. Turning on strobe"
                    alarms?.strobe()
                    break
                case 3 :
                	log.debug "Alarm type ${alarmtype.value} detected. Turning on Siren and Strobe"
                    alarms?.both()
                    break
                default:
					log.debug "Ignoring unexpected alarmtype mode."
        			log.debug "Alarm type ${alarmtype.value} detected"
                    break
                    }
         switch (lightaction.value)
        	{
            	case 1 :
                	log.debug "Light action ${lightaction.value} detected. No Light Action"                    
                    break
                case 2 :
                	log.debug "Light action ${lightaction.value} detected. Turning on selected lights"
                    switches2?.on()
                    switches2?.setLevel(100)
                    break
                case 3 :
                	log.debug "Light Action ${lightaction.value} detected. Flashing Selected lights"                    
                    flashLights()
                    break
                case 4 :
                	log.debug "Light Action ${lightaction.value} detected. Flash and turning on selected lights"
                    switches2?.on()
                    switches2?.setLevel(100)
                    flashLights()
                    break
                default:
					log.debug "Ignoring unexpected Light Action type."
        			log.debug "Light Action ${lightaction.value} detected"
                    break
			} 		
		if (settings.recordCameras)
			{
		cameraRecord()
			}
        }