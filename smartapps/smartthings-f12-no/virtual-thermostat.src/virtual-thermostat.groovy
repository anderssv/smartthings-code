/**
 *  Copyright 2015 SmartThings
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
 *  Virtual Thermostat
 *
 *  Author: Anders Sveen based on the SmartThings Virtual Thermostat
 */
definition(
    name: "Virtual Thermostat",
    namespace: "smartthings.f12.no",
    author: "Anders Sveen",
    description: "Control a space heater or window air conditioner in conjunction with any temperature sensor, like a SmartSense Multi.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
    section("Choose a temperature sensor... "){
        input "sensor", "capability.temperatureMeasurement", title: "Sensor"
    }
    section("Select the heater or air conditioner outlet(s)... "){
        input "outlets", "capability.switch", title: "Outlets", multiple: true
    }
    section("Set the desired temperature..."){
        input "setpoint", "decimal", title: "Set Temp"
    }
    section("When there's been movement from (optional, leave blank to not require motion)..."){
        input "motion", "capability.motionSensor", title: "Motion", required: false
    }
    section("Within this number of minutes..."){
        input "minutes", "number", title: "Minutes", required: false
    }
    section("But never go below (or above if A/C) this value with or without motion..."){
        input "emergencySetpoint", "decimal", title: "Emer Temp", required: false
    }
    section("Shut off heat when doors open (optional)"){
        input "doors", "capability.contactSensor", title: "Doors", multiple: true, required: false
    }
    section("Select 'heat' for a heater and 'cool' for an air conditioner..."){
        input "mode", "enum", title: "Heating or cooling?", options: ["heat","cool"]
    }
}

def subscribeAll() {
    subscribe(sensor, "temperature", temperatureHandler)
    if (motion) {
        subscribe(motion, "motion", motionHandler)
    }
    if (doors) {
        subscribe(doors, "contact", doorHandler)
    }
}

def installed()
{
    subscribeAll()
}

def updated()
{
    unsubscribe()
    subscribeAll()
}


def doorHandler(evt) {
    log.debug("Door state changed '$evt.value'")
    evaluateTemperatureRules(sensor.currentTemperature)
}

def isDoorsOpen() {
    def openDoors = doors.findAll { it.currentState("contact").value == "open" }
    return openDoors.size() > 0
}

def evaluateTemperatureRules(temp) {
    def isActive = hasBeenRecentMotion()
    if (isDoorsOpen()) {
        log.debug("Doors are open, so keeping everything off...")
        flipState("off")
    } else if (isActive || emergencySetpoint) {
        evaluate(temp, isActive ? setpoint : emergencySetpoint)
    }
    else {
        outlets.off()
    }
}

def temperatureHandler(evt)
{
    evaluateTemperatureRules(evt.doubleValue)
}

def motionHandler(evt)
{
    if (evt.value == "active") {
        def lastTemp = sensor.currentTemperature
        if (lastTemp != null) {
            evaluate(lastTemp, setpoint)
        }
    } else if (evt.value == "inactive") {
        def isActive = hasBeenRecentMotion()
        log.debug "INACTIVE($isActive)"
        if (isActive || emergencySetpoint) {
            def lastTemp = sensor.currentTemperature
            if (lastTemp != null) {
                evaluate(lastTemp, isActive ? setpoint : emergencySetpoint)
            }
        }
        else {
            outlets.off()
        }
    }
}

private evaluate(currentTemp, desiredTemp)
{
    log.debug "EVALUATE($currentTemp, $desiredTemp)"
    def threshold = 0.5
    if (mode == "cool") {
        // air conditioner
        if (currentTemp - desiredTemp >= threshold) {
            flipState("on")
        }
        else if (desiredTemp - currentTemp >= threshold) {
            flipState("off")
        }
    }
    else {
        // heater
        if (desiredTemp - currentTemp >= threshold) {
            flipState("on")
        }
        else if (currentTemp - desiredTemp >= threshold) {
            flipState("off")
        }
     }   
}

private flipState(desiredState)
{
    def wrongState = outlets.findAll { outlet -> outlet.currentValue("switch") != desiredState }

    log.debug "FLIPSTATE: Found ${wrongState.size()} outlets in wrong state (Target state: $desiredState) ..."
    wrongState.each { outlet -> 
        log.debug "Flipping '$outlet' ..."
        if (desiredState == "on") {
            log.debug "On"
            outlet.on()
        } else {
            log.debug "Off"
            outlet.off()
        }
    }
}

private hasBeenRecentMotion()
{
    def isActive = false
    if (motion && minutes) {
        def deltaMinutes = minutes as Long
        if (deltaMinutes) {
            def motionEvents = motion.eventsSince(new Date(now() - (60000 * deltaMinutes)))
            log.trace "Found ${motionEvents?.size() ?: 0} events in the last $deltaMinutes minutes"
            if (motionEvents.find { it.value == "active" }) {
                isActive = true
            }
        }
    }
    else {
        isActive = true
    }
    isActive
}