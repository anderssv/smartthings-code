/**
 *  Heating Control
 *
 *  Copyright 2017 Anders Sveen &lt;anders@f12.no&gt;
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
        name: "Heating Control",
        namespace: "smartthings.f12.no",
        author: "Anders Sveen <anders@f12.no>",
        description: "Manages heating for several rooms and several modes.",
        category: "Green Living",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "setupPage")
}

def setupPage() {
    if (!settings["numberOfRooms"]) {
        settings["numberOfRooms"] = 1
    }
    dynamicPage(name: "setupPage", title: "Set up heat control", install: true, uninstall: true) {
        section("General options and defaults", hideable: true) {
            input "numberOfRooms", "number", title: "Number of rooms", defaultValue: 1, submitOnChange: true
            paragraph "This temperature will be used across all rooms if no more specific setting is found."
            input "defaultMainTemp", "decimal", title: "Default thermostat temperature"
        }
        section("The separate rooms are listed below. If only sensor and switches are specified in a room, the defaults above will be used.")
        (1..settings["numberOfRooms"]).each { roomNumber ->
            def modeSections = modeCountForRoom(roomNumber)
            if (modeSections == null) {
                modeSections = 1
            }
            def roomTitle = "Unnamed room"
            def hidden = false
            if (settings["room${roomNumber}Name"]) {
                roomTitle = settings["room${roomNumber}Name"]
                hidden = true
            }
            section("Room: ${roomTitle}", hideable: true, hidden: hidden) {
                input "room${roomNumber}Name", "text", title: "Name", description: "Name for convenience"
                input "room${roomNumber}Sensor", "capability.temperatureMeasurement", title: "Temperature Sensor"
                input "room${roomNumber}Switches", "capability.switch", title: "Switches to manage", multiple: true
                input "room${roomNumber}MainTemp", "decimal", title: "Thermostat temperature", required: false, description: "The desired temperature for the room."

                (1..modeSections).each { modeNumber ->
                    paragraph " ===   Mode specific temperature   ==="
                    input "room${roomNumber}Mode${modeNumber}Modes", "mode", title: "Modes for alternative temperature", required: false, multiple: true
                    input "room${roomNumber}Mode${modeNumber}Temp", "decimal", title: "Alternative temperature", required: false, submitOnChange: true
                }
                input "room${roomNumber}Mode${modeSections + 1}Modes", "mode", title: "(+) Select modes here to add more sections", required: false, multiple: true, submitOnChange: true
            }
        }
    }
}

def modeCountForRoom(roomNumber) {
    return settings.count { key, value -> key.startsWith("room${roomNumber}Mode") && key.endsWith("Modes") && !value.empty }
}

def settingsToRooms() {
	def roomMap = [:]
    (1..settings["numberOfRooms"]).each { int roomNumber ->
    	def currentRoomMap = [:]
        def modesMap = [:]
        currentRoomMap["modes"] = modesMap
        
		settings
    		.findAll { key, value -> key.startsWith("room${roomNumber}") }
            .each { key, value ->
            	if (key.startsWith("room${roomNumber}Mode")) {
                	// TODO This will fail if modes > 9
                	int modeNumber = Integer.parseInt(key.replaceAll("room${roomNumber}Mode", "").take(1))
                	def attributeName = key.replaceAll("room${roomNumber}Mode${modeNumber}", "")
                    if (!modesMap.containsKey(modeNumber)) {
                    	modesMap[modeNumber] = [:]
                    }
                    modesMap[modeNumber][attributeName] = value
                } else {
                    def attributeName = key.replaceAll("room${roomNumber}", "")
                    currentRoomMap[attributeName] = value
                }
        	}
        roomMap[roomNumber] = currentRoomMap
    }
    
    return roomMap
}

def Double findDesiredTemperature(Map room) {
    Double desiredTemp = defaultMainTemp
    Double roomModeTemp = null

	room.modes.each { modeNumber, modeSettings ->
        modeSettings.Modes.each { oneMode ->
            if (oneMode.equals(location.currentMode.name)) {
                roomModeTemp = modeSettings.Temp
            }
        }
    }

    if (roomModeTemp) {
        log.debug("Selected temp based on mode (${location.currentMode.name}) for room '${room.Name}'")
        desiredTemp = roomModeTemp
    } else if (room.MainTemp) {
        log.debug("Selected temp based on default for room for room '${room.Name}'")
        desiredTemp = room.MainTemp
    } else {
        log.debug("Selected default value for any room for room '${room.Name}'")
    }

    return desiredTemp
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def initialize() {
	settingsToRooms().each { roomNumber, room ->
        subscribe(room.Sensor, "temperature", temperatureHandler)
        log.debug("Subscribed to sensor '${room.Sensor}'")
    }
}

def temperatureHandler(evt) {
	settingsToRooms()
    	.findAll { key, room -> room.Sensor.toString().equals(evt.getDevice().toString()) }
		.each { key, room ->
        	log.debug("Found sensor, handling...")
        	Double desiredTemp = findDesiredTemperature(room)
        	Double currentTemp = evt.doubleValue

        	log.debug("Desired temp is ${desiredTemp} in room ${room.Name} with current value ${currentTemp}")

        	Double threshold = 0.5
        	if (desiredTemp - currentTemp >= threshold) {
	            log.debug("Current temp (${currentTemp}) is lower than desired (${desiredTemp}) in room ${room.Name}. Switching on.")
	            flipState("on", room.Switches)
	        } else if (currentTemp - desiredTemp >= threshold) {
	            log.debug("Current temp (${currentTemp}) is higher than desired (${desiredTemp}) in room ${room.Name}. Switching off.")
	            flipState("off", room.Switches)
	        }
    	}
}

private flipState(desiredState, outlets) {
    List wrongState = outlets.findAll { outlet -> outlet.currentValue("switch") != desiredState }

    log.debug "FLIPSTATE: Found ${wrongState.size()} outlets in wrong state (Target state: $desiredState) ..."
    wrongState.each { outlet ->
        log.debug "Flipping '$outlet' ${desiredState} ..."
        if (desiredState == "on") {
            outlet.on()
        } else {
            outlet.off()
        }
    }
}