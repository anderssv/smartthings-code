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
        section("Defaults") {
            input "numberOfRooms", "number", title: "Number of rooms", defaultValue: 1, submitOnChange: true
            paragraph "This temperature will be used across all rooms if no more specific setting is found."
            input "defaultMainTemp", "decimal", title: "Default thermostat temperature"
        }
        (1..settings["numberOfRooms"]).each { roomNumber ->
            def modeSections = settings.count { key, value -> key.startsWith("room${roomNumber}Mode") && key.endsWith("Modes") }
            if (modeSections == null || modeSections == 0) {
                modeSections = 1
            }
            section("Room ${roomNumber}") {
                input "room${roomNumber}Name", "text", title: "Name", description: "Name for convenience"
                input "room${roomNumber}Sensor", "capability.temperatureMeasurement", title: "Temperature Sensor"
                input "room${roomNumber}Switches", "capability.switch", title: "Switches to manage", multiple: true
                input "room${roomNumber}MainTemp", "decimal", title: "Thermostat temperature", required: false, description: "The desired temperature for the room."

                (1..modeSections + 1).each { modeNumber ->
                    paragraph "----------------------------------------\nSelect modes below to have a different temperature for those."
                    input "room${roomNumber}Mode${modeNumber}Modes", "mode", title: "Modes for alternative temperature", required: false, multiple: true, submitOnChange: true
                    input "room${roomNumber}Mode${modeNumber}Temp", "decimal", title: "Alternative temperature", required: false
                }
            }
        }
    }
}

def findDesiredTemperature(roomNumber) {
    def desiredTemp = defaultMainTemp

    def roomMainTemp = settings["room${roomNumber}MainTemp"]
    def roomModeTemp = null

    (1..1).each { modeNumber ->
        def modeSetting = settings["room${roomNumber}Mode${modeNumber}Modes"]
        if (modeSetting) {
            modeSetting.each { oneMode ->
                if (oneMode.equals(location.currentMode.name)) {
                    roomModeTemp = settings["room${roomNumber}Mode${modeNumber}Temp"]
                }
            }
        }
    }

    if (roomModeTemp) {
        log.debug("Selected temp based on mode (${location.currentMode.name}) for room number ${roomNumber}")
        desiredTemp = roomModeTemp
    } else if (roomMainTemp) {
        log.debug("Selected temp based on default for room for room number ${roomNumber}")
        desiredTemp = roomMainTemp
    } else {
        log.debug("Selected default value for any room for room number ${roomNumber}")
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
    def sensors = new ArrayList()

    (1..settings["numberOfRooms"]).each { roomNumber ->
        def currentSensor = settings["room${roomNumber}Sensor"]
        if (currentSensor) {
            sensors.add(currentSensor)
        }
    }

    sensors.each {
        subscribe(it, "temperature", temperatureHandler)
    }
    log.debug("Subscribed to ${sensors.size} sensor(s)")
}

def temperatureHandler(evt) {
    def allSensors = settings.findAll { it.key.endsWith("Sensor") }
    def registeredRoomSensors = allSensors.findAll { it.value.toString().equals(evt.getDevice().toString()) }

    registeredRoomSensors.each { currentSensor ->
        def currentRoom = currentSensor.key.replaceAll("Sensor", "").replaceAll("room", "")
        def currentRoomName = settings["room${currentRoom}Name"]
        def currentRoomSwitches = settings["room${currentRoom}Switches"]

        def desiredTemp = findDesiredTemperature(currentRoom)
        def currentTemp = evt.doubleValue

        log.debug("Desired temp is ${desiredTemp} in room ${currentRoomName} ${currentRoom} with current value ${currentTemp}")

        def threshold = 0.5
        if (desiredTemp - currentTemp >= threshold) {
            log.debug("Current temp (${currentTemp}) is lower than desired (${desiredTemp}) in room ${currentRoomName} (${currentRoom}). Switching on.")
            flipState("on", currentRoomSwitches)
        } else if (currentTemp - desiredTemp >= threshold) {
            log.debug("Current temp (${currentTemp}) is higher than desired (${desiredTemp}) in room ${currentRoomName} (${currentRoom}). Switching off.")
            flipState("off", currentRoomSwitches)
        }
    }
}

private flipState(desiredState, outlets) {
    def wrongState = outlets.findAll { outlet -> outlet.currentValue("switch") != desiredState }

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

