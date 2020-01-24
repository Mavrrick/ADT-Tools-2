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
* v1.0.0a 12-23-2018
* Added missed value for unmonitored sensor functionality.
*
* v1.0.0.b 12-26-2018
* Added light additional control for dimmer switches involved with Light on actions 2 and 4
* Corrected Light Action issue if Setlevel to 100% used when no dimmer switch avaliable. 
*
* v1.0.0.c 12-31-2018
* Updated Continous flash rotine to be functional enable breaking of flashing routine.
* Updated some text to correct spelling
*
* v1.0.1 1/16/19
* Added ability to have repeat notifications from event until it is cleared on the alarm
* Updated some text
*
* v1.0.1a 1/30/19
* Updated to new notification routine that will allow multiple SMS numbers entered
*
* v1.0.1b 3/2/19
* Added value for non dual branded sensor events to select from Location alarm state and ADT Panel Alarm state
* Updated Trigger routine to use new switch for source of alarm state.
*
* v1.0.2 3/10/19
* Added verbiage to explain use of multiple SMS numbers to be used for notifications
*
* v1.0.2a 3/12/2019
* Corrected bug with push notifications for new app.
*
* v1.0.3 6/1/2019
* Added ability to set a Timeout for a external alarm if configured.
*
* v1.0.4 1/20/20
* Added Pushover messaging integration to app
*
*/
import groovy.time.TimeCategory

//PushOver-Manager Input Generation Functions
private getPushoverSounds(){return (Map) atomicState?.pushoverManager?.sounds?:[:]}
private getPushoverDevices(){List opts=[];Map pmd=atomicState?.pushoverManager?:[:];pmd?.apps?.each{k,v->if(v&&v?.devices&&v?.appId){Map dm=[:];v?.devices?.sort{}?.each{i->dm["${i}_${v?.appId}"]=i};addInputGrp(opts,v?.appName,dm);}};return opts;}
private inputOptGrp(List groups,String title){def group=[values:[],order:groups?.size()];group?.title=title?:"";groups<<group;return groups;}
private addInputValues(List groups,String key,String value){def lg=groups[-1];lg["values"]<<[key:key,value:value,order:lg["values"]?.size()];return groups;}
private listToMap(List original){original.inject([:]){r,v->r[v]=v;return r;}}
private addInputGrp(List groups,String title,values){if(values instanceof List){values=listToMap(values)};values.inject(inputOptGrp(groups,title)){r,k,v->return addInputValues(r,k,v)};return groups;}
private addInputGrp(values){addInputGrp([],null,values)}
//PushOver-Manager Location Event Subscription Events, Polling, and Handlers
public pushover_init(){subscribe(location,"pushoverManager",pushover_handler);pushover_poll()}
public pushover_cleanup(){state?.remove("pushoverManager");unsubscribe("pushoverManager");}
public pushover_poll(){sendLocationEvent(name:"pushoverManagerCmd",value:"poll",data:[empty:true],isStateChange:true,descriptionText:"Sending Poll Event to Pushover-Manager")}
public pushover_msg(List devs,Map data){if(devs&&data){sendLocationEvent(name:"pushoverManagerMsg",value:"sendMsg",data:data,isStateChange:true,descriptionText:"Sending Message to Pushover Devices: ${devs}");}}
public pushover_handler(evt){Map pmd=atomicState?.pushoverManager?:[:];switch(evt?.value){case"refresh":def ed = evt?.jsonData;String id = ed?.appId;Map pA = pmd?.apps?.size() ? pmd?.apps : [:];if(id){pA[id]=pA?."${id}"instanceof Map?pA[id]:[:];pA[id]?.devices=ed?.devices?:[];pA[id]?.appName=ed?.appName;pA[id]?.appId=id;pmd?.apps = pA;};pmd?.sounds=ed?.sounds;break;case "reset":pmd=[:];break;};atomicState?.pushoverManager=pmd;}
//Builds Map Message object to send to Pushover Manager
private buildPushMessage(List devices,Map msgData,timeStamp=false){if(!devices||!msgData){return};Map data=[:];data?.appId=app?.getId();data.devices=devices;data?.msgData=msgData;if(timeStamp){data?.msgData?.timeStamp=new Date().getTime()};pushover_msg(devices,data);}

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
    state?.isInstalled = true
    initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
    initialize()
}

def initialize() {
    pushover_init()
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
        	label title: "Please name this security alert action.", required: true, defaultValue: "Security alert action"
        }
		section("Alert Action Trigger type")
		{
        	paragraph "Change the value below value only if you want to use non dual branded sensors and use your this alert action in a unmonitored condition."       	
			input "alertTrgType", "bool", title: "Are all sensors in this alert action ADT/Smarthings dual branded?", description: "Determines if using ADT dual branded sensors or generic sensors.", defaultValue: true, required: true, multiple: false
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
            input "adtcontact", "capability.contactSensor", title: "Look for ADT Activity on these contact sesors.", required: false, multiple: true
            input "adtmotion", "capability.motionSensor", title: "Look for ADT Activity on these motion sesors.", required: false, multiple: true
            }
            else {
            paragraph "This event is being configured as a generic sensor event and can use any sensor. This should not be used if you want to use ADT Monitoring or only use ADT sensors. Please select the correct sensors from the types below." 
       		paragraph "What Active alarm mode do you want to monitor for 1= Arm/Stay, 2=Armed/Away. All other numberical valudes wil be ignored."
        	input "alarmtype2", "number", title: "What type of alarm do you want to trigger from?", required: false, defaultValue: 1        
			input "alertStsSrc", "bool", title: "Did you configur location alarm status sync for SHM?", description: "This will determine if the alert action will use location alarm state or the ADT Panel Alarm state for actions.", defaultValue: true, required: true, multiple: false
			input "contact", "capability.contactSensor", title: "Use these sensors for Unmonitored security alerts.", required: false, multiple: true
            input "motion", "capability.motionSensor", title: "Look for activity on these motion sesors.", required: false, multiple: true
			input "panel", "capability.securitySystem", title: "Select ADT Panel for Alarm Status.", required: true
            }
		}
        section ("Return to ADT Tools Main page"){
            href "mainPage", title: "Return to the previous menu", description: "Return to the previous menu to complete setup."            
		}
	}
}

def adtActions()
{
	dynamicPage(name: "adtActions", title: "Alarm action", nextPage: "adtLightsOpt")
	{
		section()
		{
        	input "alarms", "capability.alarm", title: "Which Alarm(s) to trigger when ADT alarm goes off?", multiple: true, required: false
        	paragraph "Valid alarm types are 1= Siren, 2=Strobe, and 3=Both. All other numberical valudes wil be ignored."
        	input "alarmtype", "number", title: "What type of alarm do you want to trigger", required: false, range: "1..3", defaultValue: 3
			input "alarmTimeout", "bool", title: "Enable this option if you wish for the siren to automatticall turn off after a period of time.", description: "This switch will set a time to turn off the siren after a set time.", defaultValue: false, required: true, multiple: false
        	input "alarmTimeOValue", "number", title: "How Many minutes before the external alarm will be turned off", required: false, range: "1..15", defaultValue: 4
			paragraph "Valid Light actions are are 1 = None, 2 = Turn on lights, 3 = Flash Lights and 4 = Both. All other numberical valudes wil be ignored."
        	input "lightaction", "number", title: "What type of light action do you want to trigger?", required: true, range: "1..4", defaultValue: 1
        	paragraph "If you choose Light action 4 do not select the same lights in both values."
        	input "switches2", "capability.switch", title: "Turn these lights on if Light action is set to 2 or 4", multiple: true, required: false
        	input "switches", "capability.switch", title: "Flash these lights (optional) If Light action is set to 3 or 4", multiple: true, required: false
            
		}
        section ("Optional Light Configuration"){
            href "adtLightsOpt", title: "Change Light default values", description: "Change default values for flashing lights."            
		}
        section ("Return to ADT Tools Main page"){
            href "mainPage", title: "Return to the previous menu", description: "Return to the previous menu to complete setup."            
		}
	}
}

def adtLightsOpt()
{
	dynamicPage(name: "adtLightsOpt", title: "Optional Light Setup", nextPage: "adtCameraSetup")
	{
    	section("Light Activation options"){
      	input "switches3", "capability.switchLevel", title: "Adjust these lights to 100% when turned on as part of light action.", multiple: true, required: false
		input "lightRepeat", "bool", title: "Enable lights to continue flashing as long as arlarm is occuring.", description: "This switch will enable lights to continue to flash long as there is a active alarm.", defaultValue: false, required: true, multiple: false
		}		
        section("Flashing Options"){
		input "onFor", "number", title: "On for (default 5000)", required: false
		input "offFor", "number", title: "Off for (default 5000)", required: false
        input "numFlashes", "number", title: "This number of times (default 3)", required: false
		}
        section ("Return to ADT Tools Main page"){
            href "mainPage", title: "Return to the previous menu", description: "Return to the previous menu to complete setup."            
		}
	}
}

def adtCameraSetup()
{
	dynamicPage(name: "adtCameraSetup", title: "Camera Setup", nextPage: "notificationSetup")
	{
    	section("Camera setup (Optional)"){
        	input "recordCameras", "bool", title: "Enable Camera recording?", description: "This switch will enable cameras to record on alarm events.", defaultValue: false, required: true, multiple: false
			input "recordRepeat", "bool", title: "Enable Camare to trigger recording as long as arlarm is occuring?", description: "This switch will enable cameras generate new clips as long as there is a active alarm.", defaultValue: false, required: true, multiple: false
			input "cameras", "capability.videoCapture", multiple: true, required: false
        	input name: "clipLength", type: "number", title: "Clip Length", description: "Please enter the length of each recording.", required: true, range: "5..120", defaultValue: 120
        }
        section ("Return to ADT Tools Main page"){
            href "mainPage", title: "Return to the previous menu", description: "Return to the previous menu to complete setup."            
		}
	}
}

def notificationSetup()
{
	dynamicPage(name: "notificationSetup", title: "Notification setup", nextPage: "mainPage")
	{
        section("Via a push notification and/or an SMS message"){
        input "message", "text", title: "Send this message if activity is detected.", required: false
        }
        section("Via a push notification and/or an SMS message?"){
        	paragraph "Multiple numbers can be entered as long as sperated by a (;)"
			input("recipients", "contact", title: "Send notifications to?") {
			input "phone", "phone", title: "Enter a phone number to get SMS.", required: false
			paragraph "If outside the US please make sure to enter the proper country code."
   			input "sendPush", "bool", title: "Send Push notifications to everyone?", description: "This will tell ADT Tools to send out push notifications to all users of the location", defaultValue: false, required: true, multiple: false
			}	
		}
        section("Enable Pushover Support:") {
    input ("pushoverEnabled", "bool", title: "Use Pushover Integration", required: false, submitOnChange: true)
    if(settings?.pushoverEnabled == true) {
        if(state?.isInstalled) {
            if(!atomicState?.pushoverManager) {
                paragraph "If this is the first time enabling Pushover than leave this page and come back if the devices list is empty"
                pushover_init()
            } else {
                input "pushoverDevices", "enum", title: "Select Pushover Devices", description: "Tap to select", groupedOptions: getPushoverDevices(), multiple: true, required: false, submitOnChange: true
                if(settings?.pushoverDevices) {
                    input "pushoverSound", "enum", title: "Notification Sound (Optional)", description: "Tap to select", defaultValue: "pushover", required: false, multiple: false, submitOnChange: true, options: getPushoverSounds()
                }
            }
        } else { paragraph "New Install Detected!!!\n\n1. Press Done to Finish the Install.\n2. Goto the Automations Tab at the Bottom\n3. Tap on the SmartApps Tab above\n4. Select ${app?.getLabel()} and Resume configuration", state: "complete" }
    }
}
		section("Message repeat options") {
			input "notifyRepeat", "bool", title: "Enable this switch if you want to recieves messages until someone actively clears the alarm.", description: "Enable this switch if you want to recieves messages until someone actively clears the alarm.", defaultValue: false, required: true, multiple: false
			input "msgrepeat", "decimal", title: "Minutes", required: false
	}
	}
}

def devices = " "

def alarmHandler(evt) {
	if (evt.value == 'disarmed') {
    log.debug "Alarm switch to disarmed. Turing off siren."
    	alarms?.off() }
}

def alarmHandlerTO() {
    log.debug "Alarm Timeout reached. Turing off siren."
    	alarms?.off() 
}

def triggerHandler(evt) {
        if (settings.alertStsSrc) {
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
        else {
        def alarmState = panel.currentSecuritySystemStatus        
		if (alarmState == "armedStay" && alarmtype2 == 1) {
        log.debug "Current alarm mode: ${alarmState}."
		alarmAction()
        }
        else if (alarmState == "armedAway" && alarmtype2 == 2) {
        log.debug "Current alarm mode: ${alarmState}."
        alarmAction()
        }
        else
        log.debug "Current alarm mode: ${alarmState}. Ignoring event"
        }
    }
    
def alarmAction()    
	{
        switch (alarmtype.value)
        	{
            	case 1 :
                	log.debug "Alarm type ${alarmtype.value} detected. Turning on siren."
                    alarms?.siren()
                    	if (settings.alarmTimeout)
							{
							runIn((alarmTimeOValue * 60) , alarmHandlerTO)
							}
                    break
                case 2 :
                	log.debug "Alarm type ${alarmtype.value} detected. Turning on strobe."
                    alarms?.strobe()
                      	if (settings.alarmTimeout)
							{
							runIn((alarmTimeOValue * 60) , alarmHandlerTO)
							}
                    break
                case 3 :
                	log.debug "Alarm type ${alarmtype.value} detected. Turning on Siren and Strobe."
                    alarms?.both()
                       	if (settings.alarmTimeout)
							{
							runIn((alarmTimeOValue * 60) , alarmHandlerTO)
                            }
                    break
                default:
					log.debug "Ignoring unexpected alarmtype mode."
        			log.debug "Alarm type ${alarmtype.value} detected."
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
                    break
                case 3 :
                	log.debug "Light Action ${lightaction.value} detected. Flashing Selected lights"                    
                    flashLights()
                    break
                case 4 :
                	log.debug "Light Action ${lightaction.value} detected. Flash and turning on selected lights"
                    switches2?.on()
                    if (switches3) {
                    switches3?.setLevel(100)
                    }
                    flashLights()
                    break
                default:
					log.debug "Ignoring unexpected Light Action type."
        			log.debug "Light Action ${lightaction.value} detected"
                    break
			}
	if(settings?.pushoverEnabled == true) {
    		sendPushoverMessage()
		}
			sendnotification()
	if (settings.recordCameras)
	{
		cameraRecord()
	}
}


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
		def sequenceTime = ((numFlashes) * (onFor + offFor)-500)
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

def sendPushoverMessage() {
    Map msgObj = [
        title: app?.getLabel(), //Optional and can be what ever
        html: false, //Optional see: https://pushover.net/api#html
        message: settings?.message, //Required (HTML markup requires html: true, parameter)
        priority: 0,  //Optional
        retry: 30, //Requried only when sending with High-Priority
        expire: 10800, //Requried only when sending with High-Priority
        sound: settings?.pushoverSound, //Optional
//        url: "https://www.foreverbride.com/files/6414/7527/3346/test.png", //Optional
//        url_title: "ADT Tools Alert Action" //Optional
    ]
    /* buildPushMessage(List param1, Map param2, Boolean param3)
        Param1: List of pushover Device Names
        Param2: Map msgObj above
        Param3: Boolean add timeStamp
    */
    buildPushMessage(settings?.pushoverDevices, msgObj, true) // This method is part of the required code block
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
        	log.debug "Alarm Event is still occuring. Submitting another clip to record."
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
                      	if (settings.alarmTimeout)
							{
							runIn((alarmTimeOValue * 60) , alarmHandlerTO)
							}
                    break
                case 2 :
                	log.debug "Alarm type ${alarmtype.value} detected. Turning on strobe"
                    alarms?.strobe()
                     	if (settings.alarmTimeout)
							{
							runIn((alarmTimeOValue * 60) , alarmHandlerTO)
							}
                    break
                case 3 :
                	log.debug "Alarm type ${alarmtype.value} detected. Turning on Siren and Strobe"
                    alarms?.both()
                      	if (settings.alarmTimeout)
							{
							runIn((alarmTimeOValue * 60) , alarmHandlerTO)
							}
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
                    break
                case 3 :
                	log.debug "Light Action ${lightaction.value} detected. Flashing Selected lights"                    
                    flashLights()
                    break
                case 4 :
                	log.debug "Light Action ${lightaction.value} detected. Flash and turning on selected lights"
                    switches2?.on()
                    if (switches3) {
                    switches3?.setLevel(100)
                    }
                    flashLights()
                    break
                default:
					log.debug "Ignoring unexpected Light Action type."
        			log.debug "Light Action ${lightaction.value} detected"
                    break
			} 	
		if(settings?.pushoverEnabled == true) {
    		sendPushoverMessage()
			}
			sendnotification()
		if (settings.recordCameras)
			{
		cameraRecord()
			}
        }