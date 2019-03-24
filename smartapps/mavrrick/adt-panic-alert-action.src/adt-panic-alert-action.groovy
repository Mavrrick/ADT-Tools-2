/**
 *  ADT Panic Alert
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
    name: "ADT Panic Alert Action",
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
*  
*
*/
import groovy.time.TimeCategory

preferences {
	page (name: "mainPage", title: "Panic alert Action")
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
        subscribe(location, "alarm", adtAlarmHandler)
}

def mainPage()
{
	dynamicPage(name: "mainPage", title: "ADT Alert Action", uninstall: true, install: true)
	{
    	section(title: "Panic Alert Action") {
        	label title: "Please name this panic alert action", required: true, defaultValue: "Panic alert action"
        }
		section("Monitored panic alerts")
		{
        	paragraph "Please enable the type of panic alerts you wat to monitor below."       	
			input "alertEmergency", "bool", title: "Turn on monitoring for Alarm Panel Emergency Alerts", description: "Monitors alarm panel for Emergency Panic Alerts.", defaultValue: true, required: true, multiple: false
			input "alertFire", "bool", title: "Turn on monitoring for Alarm Panel Fire Alerts", description: "Monitors alarm panel for Fire Panic Alerts.", defaultValue: true, required: true, multiple: false
			input "alertPanic", "bool", title: "Turn on monitoring for Alarm Panel Panic Alerts", description: "Monitors alarm panel for Panic Alerts.", defaultValue: true, required: true, multiple: false
			input "adtpanic", "capability.panicAlarm", title: "Look for ADT Activity on these Keyfobs", required: false, multiple: true
			input "panel", "capability.securitySystem", title: "Select ADT Panel for Alarm Status verification.", required: true
//			href "adtTrigger", title: "Select triggers", description: "Select alert trigger devices"
		}

		section("Alert Action after alarm is triggered")
		{
			href "adtActions", title: "Alarm Event Actions", description: "Actions that will trigger from Panic Alert event. "
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
            input "adtsmoke", "capability.smokeDetector", title: "Look for ADT Activity on these Smoke detectors", required: false, multiple: true
            input "adtmonoxide", "capability.carbonMonoxideDetector", title: "Look for ADT Activity on these Carbon Monoxide Detector sensors", required: false, multiple: true
			input "adtwater", "capability.waterSensor", title: "Look for ADT Activity on these water sensors", required: false, multiple: true
			input "adtpanic", "capability.panicAlarm", title: "Look for ADT Activity on these Keyfobs", required: false, multiple: true
			}
            else {
            paragraph "This event is being configured as a generic sensor event and can use any sensor. This should not be used if you want to use ADT Monitoring or only use ADT sensors. Please select the correct sensors from the types below" 
            input "smoke", "capability.smokeDetector", title: "Use these sensors for Unmonitored smoke alarms", required: false, multiple: true
            input "monoxide", "capability.carbonMonoxideDetector", title: "Use these sensors to detect Carbon Monoxide alerts", required: false, multiple: true
            input "water", "capability.waterSensor", title: "Look for water leak activity on these water sensors", required: false, multiple: true
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
    	section("Light Activation options"){
      	input "switches3", "capability.switchLevel", title: "Adjust these lights to 100% when turned on as part of light action", multiple: true, required: false
        input "hues", "capability.colorControl", title: "Do you have any color changing bulbs that will turn on with the event", multiple:true, required: false
		input name: "color1", type: "enum", title: "Color to use during event", options: ["Red","Blue","Yellow"], multiple:false, required: false
        input name: "colorReturn", type: "enum", title: "Color to return to after event", options: ["White",["Soft White":"Soft White - 2700K"],["Cool White":"Cool White - 4100K"],["Daylight":"Daylight - 5500K"]], required: false
}
		section("Flashing Option"){
        input "hues2", "capability.colorControl", title: "Do you have any color changing bulbs that will flash with the event", multiple:true, required: false
		input name: "colorflash", type: "enum", title: "Color to use during event", options: ["Red","Blue","Yellow"], multiple:false, required: false
        input name: "colorReturn2", type: "enum", title: "Color to return to after event", options: ["White",["Soft White":"Soft White - 2700K"],["Cool White":"Cool White - 4100K"],["Daylight":"Daylight - 5500K"]], required: false
		input "lightRepeat", "bool", title: "Enable lights to continue flashing as long as event is occuring.", description: "This switch will enable lights to continue to flash long as there is a active alarm.", defaultValue: false, required: true, multiple: false
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
	dynamicPage(name: "notificationSetup", title: "Notification setup", uninstall: true, install: true)
	{
        section("Via a push notification and/or an SMS message"){
        input "message", "text", title: "Send this message if activity is detected", required: false
        }
        section("Via a push notification and/or an SMS message"){
        	paragraph "Multiple numbers can be entered as long as sperated by a (;)"
			input("recipients", "contact", title: "Send notifications to") {
			input "phone", "phone", title: "Enter a phone number to get SMS", required: false
		paragraph "If outside the US please make sure to enter the proper country code"
   			input "sendPush", "bool", title: "Send Push notifications to everyone?", description: "This will tell ADT Tools to send out push notifications to all users of the location", defaultValue: false, required: true, multiple: false
		}
	}
		section("Message repeat options") {
			input "notifyRepeat", "bool", title: "Enable this switch if you want to recieves messages until someone actively clears the alarm.", description: "Enable this switch if you want to recieves messages until someone actively clears the alarm.", defaultValue: false, required: true, multiple: false
			input "msgrepeat", "decimal", title: "Minutes", required: false
	}
	}
}

def devices = " "
    

def continueFlashing()
        {
		def alarmActive = panel.currentSecuritySystemStatus
    	log.debug "Current alarms is in ${alarmActive} state"
		if (alarmActive != "disarmed") 
        	{
        	log.debug "Alarm Event is still occuring. Submitting flashing for another cycle."
        flashLights()   
        	}
		else {
        log.debug "Alarm has cleared and is flashing should be stopped."
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
        	if (settings.lightRepeat)
	{
    	def lightCycleTime = ((numFlashes * (onFor + offFor)/1000))
        log.trace "Checking Alarm Status after $lightCycleTime sec"
		runIn(lightCycleTime , continueFlashing)
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
   if (phone) { // check that the user did select a phone number
        if ( phone.indexOf(";") > 0){
            def phones = phone.split(";")
            for ( def i = 0; i < phones.size(); i++) {
                log.debug("Sending SMS ${i+1} to ${phones[i]}")
                sendSmsMessage(phones[i], msg)
            }
        } else {
            log.debug("Sending SMS to ${phone}")
            sendSmsMessage(phone, msg)
        }
    }
    if (settings.sendPush) {
        log.debug("Sending Push to everyone")
        sendPush(msg)
    }
    sendNotificationEvent(msg)	
        if (settings.notifyRepeat)
	{
		runIn((msgrepeat * 60) , notifyRepeatChk)
	}
}
}

def notifyRepeatChk() {
		def alarmActive = panel.currentSecuritySystemStatus
    	log.debug "Current alarms is in ${alarmActive} state"
		if (alarmActive != "disarmed") 
        	{
        	log.debug "Alarm Event is still occuring. Submitting another notification"
        sendnotification()   
        	}
		else {
        log.debug "Alarm has cleared and is no more notifications are needed."
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
    sethuecolorreturn ()
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
    case "EMERGENCY":
    log.debug "Panel Emergency panic detected"
    	if (alertEmergency) {
        	log.debug "Emergency panic monitoring is enabled. Triggering event actions"
        	adtActionHandler()
    		}
    	break
      case "FIRE":
    log.debug "Panel Fire panic detected"
        if (alertFire) {
        	log.debug "Fire panic monitoring is enabled. Triggering event actions"
        	adtActionHandler()
    		}
    	break
      case "PANIC":
    log.debug "Panel personal Panic alarm detected"
       if (alertPanic) {
        	log.debug "Fire panic monitoring is enabled. Triggering event actions"
        	adtActionHandler()
    		}
    	break      
    default:
//	log.debug "Notify got alarm event ${evt}"
    log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"
        log.debug "The event id to be compared is ${evt.value}"     
		if (adtpanic) {
        log.debug "The event id to be compared with ${settings.adtpanic}"
		def devices = settings.adtpanic
        log.debug "These devices were found ${devices.id} are being reviewed."
    	devices.findAll { it.id == evt.value } .each { 
        log.debug "Found device: ID: ${it.id}, Label: ${it.label}, Name: ${it.name}, Panic Keyfob saftey event found" 
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
                    if (switches3) {
                    switches3?.setLevel(100)
                    }
                    if (hues) {
                    	sethuecoloron()
                    }
                    break
                case 3 :
                	log.debug "Light Action ${lightaction.value} detected. Flashing Selected lights"                    
                    if (hues2) {
                    	sethuecolorflash()
                    }
					flashLights()
                    break
                case 4 :
                	log.debug "Light Action ${lightaction.value} detected. Flash and turning on selected lights"
                    switches2?.on()
                    if (switches3) {
                    switches3?.setLevel(100)
                    }
                    if (hues) {
                    	sethuecoloron()
                    }
                    if (hues2) {
                    	sethuecolorflash()
                    }
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
        
// [hue: 17, saturation: 100, level: 100]
def sethuecoloron (){
	switch (color1) {
    	case "Red" :
        	log.debug "Color selection ${color1.value} detected. Change light color" 
            hues?.setColor([hue: 0, saturation: 100, level: 100])
            break
    	case "Blue" :
        	log.debug "Color selection ${color1.value} detected. Change light color" 
            hues?.setColor([hue: 65, saturation: 100, level: 100])
            break
        case "Yellow" :
        	log.debug "Color selection ${color1.value} detected. Change light color" 
            hues?.setColor([hue: 17, saturation: 100, level: 100])
            break
         default:
         log.debug "Ignoring unexpected Color Value."
         log.debug "Light Action ${color1.value} detected"
            break
	}
}

def sethuecolorflash (){
	switch (colorflash) {
    	case "Red" :
        	log.debug "Color selection ${colorflash.value} detected. Change light color" 
            hues2?.setColor([hue: 0, saturation: 100, level: 100])
            break
    	case "Blue" :
        	log.debug "Color selection ${colorflash.value} detected. Change light color" 
            hues2?.setColor([hue: 65, saturation: 100, level: 100])
            break
        case "Yellow" :
        	log.debug "Color selection ${colorflash.value} detected. Change light color" 
            hues2?.setColor([hue: 17, saturation: 100, level: 100])
            break
         default:
         log.debug "Ignoring unexpected Color Value."
         log.debug "Light Action ${colorflash.value} detected"
            break
	}
}

def sethuecolorreturn (){
	if (hues) {
	switch (colorReturn) {
    	case "White" :
        	log.debug "Color selection ${colorReturn.value} detected. Change light color" 
            hues?.setColor([hue: 0, saturation: 0, level: 100])
            break       
       	case "Soft White" :
        	log.debug "Color selection ${colorReturn.value} detected. Change light color" 
            hues?.setColorTemperature(2700)
            break    	
        case "Cool White" :
        	log.debug "Color selection ${colorReturn.value} detected. Change light color" 
            hues?.setColorTemperature(4100)
            break
		case "Daylight" :
        	log.debug "Color selection ${colorReturn.value} detected. Change light color" 
            hues?.setColorTemperature(5500)
            break
         default:
         log.debug "Ignoring unexpected Color Value."
         log.debug "Light Action ${colorReturn.value} detected"
            break
	}
    }
    if (hues2) {
    	switch (colorReturn2) {
    	case "White" :
        	log.debug "Color selection ${colorReturn2.value} detected. Change light color" 
            hues2?.setColor([hue: 0, saturation: 0, level: 100])
            break
    	case "Soft White" :
        	log.debug "Color selection ${colorReturn2.value} detected. Change light color" 
            hues?.setColorTemperature(2700)
            break
        case "Cool White" :
        	log.debug "Color selection ${colorReturn2.value} detected. Change light color" 
            hues?.setColorTemperature(4100)
            break
        case "Daylight" :
        	log.debug "Color selection ${colorReturn2.value} detected. Change light color" 
            hues?.setColorTemperature(5500)
            break
         default:
         log.debug "Ignoring unexpected Color Value."
         log.debug "Light Action ${colorReturn2.value} detected"
            break
	}
    }
}