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
* 12/25/2018 v2.0.0.a
* Update routine to disarm Location alarm state to unschedule other location alarm events if needed.
*
* 1/16/2019 2.0.1
* Add Monitor and action for alarm tamper and power event
*
* 1/30/2019 2.0.2
* Updated notification routine to allow for usage of multiple SMS numbers.
*
* 3/12/2019 2.0.2a
* Corrected bug for notifications in the new app.
*
* 3/15/2019 2.0.2b
* Changed flag for Push notifications to a different type 
*
* 3/22/2019 2.0.3
* Add Panic alert Child app to allow monitoring for Panic events from Keyfobs and Panel
*
* 6/14/2019 2.0.4
* Corrected issue with About page displaying properlly
* Updated menu options with ability to control switches on alarm mode change
*
* 01/22/2020 2.0.5
* Added ability to use PushOver notification 
*
* 02/23/2020 2.06
* Added flagging for Virtual button creation. This should prevent issues if you don't turn off the flag to create the virtual buttons.
*
* 08/22/2020 2.06a
* Updated Mode change command to use true value instead of commmand value.
*
*/

preferences
{
	page (name: "mainPage", title: "ADT Tools")
	page (name: "adtNotifier", title: "ADT Custom Notification")
	page (name: "adtModeChange", title: "Setup mode change settings")
	page (name: "adtModeAction", title: "Setup Action on Mode Change")    
	page (name: "adtAlertActions", title: "Work with ADT alarm alert actions")
   	page (name: "optionalSettings", title: "Optional Setup")
    page (name: "about", title: "About ADT Tools")
}

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


def initialize() {
    // nothing needed here, since the child apps will handle preferences/subscriptions
    // this just logs some messages for demo/information purposes
    pushover_init()
    log.debug "there are ${childApps.size()} child smartapps"
    childApps.each {child ->
        log.debug "child app: ${child.label}"
    }
    if (settings.createVirtButton) {
    	if (state.crtVrtButton) {
        log.debug "Virtual buttons already created, Will not create buttons"
        }
        else {
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
                state?.crtVrtButton = true
                log.debug "ADT Tools Alarm Buttons created"
                }
		}
      	else {
        state?.crtVrtButton = false
//    	deleteChildDevice("ADT Tools Disarmed")
//        deleteChildDevice("ADT Tools Armed Stay")
//        deleteChildDevice("ADT Tools Armed Away")
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
			href "adtNotifier", title: "Alarm Status Notifications", description: "Setup Custom notifications for alarm system status changes."
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
        input "alarmPowerState", "bool", title: "Power State Notification", description: "This switch will tell ADT Tools to notify you when the Smartthings Panel change power sources between battery and home power", defaultValue: false, required: true, multiple: false
   		input "alarmTamperState", "bool", title: "Tamper Activity Notification", description: "This switch will tell ADT Tools to notify you if any tamper activty is detected on the Smartthings Alarm Panel", defaultValue: false, required: true, multiple: false

	}
	section("Via a push notification and/or an SMS message"){
		input("recipients", "contact", title: "Send notifications to") {
        	paragraph "Multiple numbers can be entered as long as sperated by a (;)"
			input "phone", "phone", title: "Enter a phone number to get SMS", required: false
			paragraph "If outside the US please make sure to enter the proper country code."
   			input "sendPush", "bool", title: "Send Push notifications to everyone?", description: "This will tell ADT Tools to send out push notifications to all users of the location", defaultValue: false, required: true, multiple: false
//          input "sendPush", "enum", title: "Send Push notifications to everyone?", required: false, options: ["Yes", "No"]
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
        href "adtModeAction", title: "Mode Change action", description: "Enables various functions around Mode change integration."
	
    }
   section("Smartthings location alarm state setup. These must be configured to use the Any Sensory Child App."){
   		input "locAlarmSync", "bool", title: "Maintain synchronization between Smartthings ADT alarm panel and location cloud alarm state", description: "This switch will tell ADT Tools if it needs to kep the ADT Alarm and the Smarthings location alarm status in sync.", defaultValue: false, required: true, multiple: false
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

def adtModeAction()
{
	dynamicPage(name: "adtModeAction", title: "ADT Mode Action Selection", uninstall: false, install: false)
    {
	section("Select actions for when alarm enters Disarmed"){
        input "disarmedOn", "capability.switch", title: "What switch to turn on when disarmed?", required: false, multiple: false
        input "disarmedOff", "capability.switch", title: "What switch to turn off when disarmed?", required: false, multiple: false
	}
   section("Select action for when alarm enters Armed/Stay"){
   		input "armedStayOn", "capability.switch", title: "What switch to turn on when armed/stay?", required: false, multiple: false
		input "armedStayOff", "capability.switch", title: "What swtich to turn off when armed/stay?", required: false, multiple: false
	} 
   section("Select action for when alarm enters Armed/Away"){
   		input "armedAwayOn", "capability.switch", title: "What switch to turn on when armed/away?", required: false, multiple: false
		input "armedAwayOff", "capability.switch", title: "What switch to turn off when armed/away?", required: false, multiple: false
	} 
    section ("Return to ADT Tools Main page"){
            href "adtModeChange", title: "ADT Tools Main Menu", description: "Return to main ADT Tools Main Menu"            
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
            app(name: "adtPanicAction", appName: "ADT Panic Alert Action", namespace: "Mavrrick", title: "Panic Alert Action apps", multiple: true)
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

def about()
{
	dynamicPage(name: "about", title: "About ADT Tools", uninstall: false, install: false)
	{
		section()
		{
			paragraph image: "https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg", "ADT Tools 2"
		}
        section("Support locations")
		{
			href (name: "thingsAreSmart", style:"embedded", title: "Things That Are Smart Support Page", url: "http://thingsthataresmart.wiki/index.php?title=ADT_tools_2")
			href (name: "smtReleaseThd", style:"embedded", title: "Smartthings Community Support Thread", url: "https://community.smartthings.com/t/released-adt-tools-2-for-smartthings-adt-alarm-sytsems/124951")
		}
        section("Support the Project")
		{
			paragraph "ADT Tools is provided free for personal and non-commercial use.  I have worked on this app in my free time to fill the needs I have found for myself and others like you.  I will continue to make improvements where I can. If you would like you can donate to continue to help with development please use the link below."
			href (name: "donate", style:"embedded", title: "Consider making a \$5 or \$10 donation today.", image: "https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg", url: "https://www.paypal.me/mavrrick58")
		}
        section ("Return to ADT Tools Main page"){
            href "mainPage", title: "ADT Tools Main Menu", description: "Return to main ADT Tools Main Menu"            
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
    state?.isInstalled = true
    state.crtVrtButton = false
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
			deleteChildDevice(childDevice.deviceNetworkId)
		}
	}

def subscribeToEvents() {
    subscribe(myDisarmButton, "momentary.pushed", disarmHandler)
    subscribe(myArmStay, "momentary.pushed", armstayHandler)
    subscribe(myArmAway, "momentary.pushed", armawayHandler)
    subscribe(location, "securitySystemStatus", alarmModeHandler)
    if (settings.alarmPowerState) {       
        subscribe(panel, "powerSource", adtPowerHandler)
		}
    if (settings.alarmTamperState) {       
        subscribe(panel, "tamper", adtTamperHandler)
		}
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
        panel?.armStay(true)

        }
	}
    
def armawayHandler(evt) {
       	log.debug "Changeing alarm to Alarm/Away"
        def alarmState = panel.currentSecuritySystemStatus
        if (alarmState == "armedStay") {
        	log.debug "Current alarm mode: ${alarmState}. Alarm must be in Disarmed state before changeing state"
        }
        else {
      	panel?.armAway(true)}
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
					unschedule()
                    break
                default:
					log.debug "Ignoring unexpected alarmtype mode."
        			log.debug "Unexpected value for Alarm status"
                    break
                    }
        }
	modeAction(evt)
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
    	if(settings?.pushoverEnabled == true) {
    		    Map msgObj = [
       				title: app?.getLabel(), //Optional and can be what ever
       				html: false, //Optional see: https://pushover.net/api#html
    				message: msg, //Required (HTML markup requires html: true, parameter)
        			priority: 0,  //Optional
        			retry: 30, //Requried only when sending with High-Priority
        			expire: 10800, //Requried only when sending with High-Priority
        			sound: settings?.pushoverSound, //Optional
//        			url: "https://www.foreverbride.com/files/6414/7527/3346/test.png", //Optional
//        			url_title: "ADT Tools Mode Change Armed/Away" //Optional
					]
    buildPushMessage(settings?.pushoverDevices, msgObj, true) // This method is part of the required code block
		}
    if (settings.sendPush) {
        log.debug("Sending Push to everyone")
        sendPush(msg)
    }
    sendNotificationEvent(msg)	
    }
    break
    case "armedStay":
    	def msg = messageArmedStay
        if ( msg == null ) {
        	log.debug "Message not configured. Skipping notification"
            }
        else {
        log.debug "Case Armstay., '$msg'"
        log.debug "$evt.name:$evt.value, sendPush:$sendPush, '$msg'"
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
    	if(settings?.pushoverEnabled == true) {
    		    Map msgObj = [
       				title: app?.getLabel(), //Optional and can be what ever
       				html: false, //Optional see: https://pushover.net/api#html
    				message: msg, //Required (HTML markup requires html: true, parameter)
        			priority: 0,  //Optional
        			retry: 30, //Requried only when sending with High-Priority
        			expire: 10800, //Requried only when sending with High-Priority
        			sound: settings?.pushoverSound, //Optional
//        			url: "https://www.foreverbride.com/files/6414/7527/3346/test.png", //Optional
//        			url_title: "ADT Tools Mode Chage Armed/Stay" //Optional
					]
    buildPushMessage(settings?.pushoverDevices, msgObj, true) // This method is part of the required code block
		}
    if (settings.sendPush) {
        log.debug("Sending Push to everyone")
        sendPush(msg)
    }
    sendNotificationEvent(msg)	
    }
	break
    case "disarmed":
    	def msg = messageDisarmed
        if ( msg == null ) {
        	log.debug "Message not configured. Skipping notification"
            }
        else {
        log.debug "Case disarmed., '$msg'"
        log.debug "$evt.name:$evt.value, sendPush:$sendPush, '$msg'"
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
    	if(settings?.pushoverEnabled == true) {
    		    Map msgObj = [
       				title: app?.getLabel(), //Optional and can be what ever
       				html: false, //Optional see: https://pushover.net/api#html
    				message: msg, //Required (HTML markup requires html: true, parameter)
        			priority: 0,  //Optional
        			retry: 30, //Requried only when sending with High-Priority
        			expire: 10800, //Requried only when sending with High-Priority
        			sound: settings?.pushoverSound, //Optional
//        			url: "https://www.foreverbride.com/files/6414/7527/3346/test.png", //Optional
//        			url_title: "ADT Tools Mode Change Disarm" //Optional
					]
    buildPushMessage(settings?.pushoverDevices, msgObj, true) // This method is part of the required code block
		}
    if (settings.sendPush) {
        log.debug("Sending Push to everyone")
        sendPush(msg)
    }
    sendNotificationEvent(msg)	
    }
	break
    default:
		log.debug "Ignoring unexpected ADT alarm mode."
        log.debug "$evt.name:$evt.value, sendPush:$sendPush, '$msg'"
        break
}
}

def adtPowerHandler(evt) {
       	log.debug "ADT Smartthigs Alarm Panel has changed power sources to ${evt}. "
        
        switch (evt.value){
        case "mains":
       	def msg = "The alarm has changed to run on main power"
        log.debug "$evt.name:$evt.value, sendPush:$sendPush, '$msg'"
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
    	if(settings?.pushoverEnabled == true) {
    		    Map msgObj = [
       				title: app?.getLabel(), //Optional and can be what ever
       				html: false, //Optional see: https://pushover.net/api#html
    				message: msg, //Required (HTML markup requires html: true, parameter)
        			priority: 0,  //Optional
        			retry: 30, //Requried only when sending with High-Priority
        			expire: 10800, //Requried only when sending with High-Priority
        			sound: settings?.pushoverSound, //Optional
//        			url: "https://www.foreverbride.com/files/6414/7527/3346/test.png", //Optional
//        			url_title: "ADT Tools Mode Chage Armed/Stay" //Optional
					]
    buildPushMessage(settings?.pushoverDevices, msgObj, true) // This method is part of the required code block
		}
    if (settings.sendPush) {
        log.debug("Sending Push to everyone")
        sendPush(msg)
    }
    sendNotificationEvent(msg)	
		break
        case "battery":
       	def msg = "The alarm has changed to run on battery power"
        log.debug "$evt.name:$evt.value, sendPush:$sendPush, '$msg'"

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
    	if(settings?.pushoverEnabled == true) {
    		    Map msgObj = [
       				title: app?.getLabel(), //Optional and can be what ever
       				html: false, //Optional see: https://pushover.net/api#html
    				message: msg, //Required (HTML markup requires html: true, parameter)
        			priority: 0,  //Optional
        			retry: 30, //Requried only when sending with High-Priority
        			expire: 10800, //Requried only when sending with High-Priority
        			sound: settings?.pushoverSound, //Optional
//        			url: "https://www.foreverbride.com/files/6414/7527/3346/test.png", //Optional
//        			url_title: "ADT Tools Mode Chage Armed/Stay" //Optional
					]
    buildPushMessage(settings?.pushoverDevices, msgObj, true) // This method is part of the required code block
		}
    if (settings.sendPush) {
        log.debug("Sending Push to everyone")
        sendPush(msg)
    }
    sendNotificationEvent(msg)	
		break
        default:
		log.debug "Ignoring unexpected power state ${evt.value}."
        log.debug "$evt.name:$evt.value, sendPush:$sendPush, '$msg'"
        break
	   }
       }
       
def adtTamperHandler(evt) {
       	log.debug "ADT Smartthigs Alarm Panel has experiencend a tamper event ${evt}. "
        
        switch (evt.value){
        case "detected":
       	def msg = "The ADT Smartthings Alarm Panel has experienced a tamper event. Please check your device."
        log.debug "$evt.name:$evt.value, sendPush:$sendPush, '$msg'"

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
    	if(settings?.pushoverEnabled == true) {
    		    Map msgObj = [
       				title: app?.getLabel(), //Optional and can be what ever
       				html: false, //Optional see: https://pushover.net/api#html
    				message: msg, //Required (HTML markup requires html: true, parameter)
        			priority: 0,  //Optional
        			retry: 30, //Requried only when sending with High-Priority
        			expire: 10800, //Requried only when sending with High-Priority
        			sound: settings?.pushoverSound, //Optional
//        			url: "https://www.foreverbride.com/files/6414/7527/3346/test.png", //Optional
//        			url_title: "ADT Tools Mode Chage Armed/Stay" //Optional
					]
    buildPushMessage(settings?.pushoverDevices, msgObj, true) // This method is part of the required code block
		}
    if (settings.sendPush) {
        log.debug("Sending Push to everyone")
        sendPush(msg)
    }
    sendNotificationEvent(msg)	
		break
        case "clear":
       	def msg = "The tamper event on your ADT Smartthings Panel has cleared."
        log.debug "$evt.name:$evt.value, sendPush:$sendPush, '$msg'"

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
    	if(settings?.pushoverEnabled == true) {
    		    Map msgObj = [
       				title: app?.getLabel(), //Optional and can be what ever
       				html: false, //Optional see: https://pushover.net/api#html
    				message: msg, //Required (HTML markup requires html: true, parameter)
        			priority: 0,  //Optional
        			retry: 30, //Requried only when sending with High-Priority
        			expire: 10800, //Requried only when sending with High-Priority
        			sound: settings?.pushoverSound, //Optional
//        			url: "https://www.foreverbride.com/files/6414/7527/3346/test.png", //Optional
//        			url_title: "ADT Tools Mode Chage Armed/Stay" //Optional
					]
    buildPushMessage(settings?.pushoverDevices, msgObj, true) // This method is part of the required code block
		}
    if (settings.sendPush) {
        log.debug("Sending Push to everyone")
        sendPush(msg)
    }
    sendNotificationEvent(msg)	
		break
        default:
		log.debug "Ignoring unexpected tamper condition ${evt.value}."
        log.debug "$evt.name:$evt.value, sendPush:$sendPush, '$msg'"
        break
	   }
       }   
       
def modeAction(evt){

	switch (evt.value) {
    case "armedAway":
    	if (armedAwayOn){
        	armedAwayOn?.on()
            }
         if (armedAwayOff) {
         	armedAwayOff?.off()
            }
    break
    case "armedStay":
    	 if (armedStayOn){
        	armedStayOn?.on()
            }
         if (armedStayOff) {
         	armedStayOff?.off()
            }
    break
    case "disarmed":
    	if (disarmedOn){
        	disarmedOn?.on()
            }
         if (disarmedOff) {
         	disarmedOff?.off()
           	}
    break
    }
    }