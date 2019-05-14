/**
 *  Heating Control Thermostat
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
metadata {
    definition(name: "Virtual Thermostat", namespace: "smartthings.f12.no", author: "Anders Sveen <anders@f12.no>") {
        capability "Thermostat Heating Setpoint"
        capability "Thermostat Mode"
        capability "Thermostat Operating State"


        // Heating Setpoing
        command "setHeatingSetpoint"
        // Thermostat Mode
        command "auto"
        command "cool"
        command "emergencyHeat"
        command "heat"
        command "off"
        command "setThermostatMode"
        // Thermostat Operating State
        // None actually

        // Internal commands related to logic, only used by the tiles
        command "temperatureUp"
        command "temperatureDown"
    }


    simulator {
        // TODO: define status and reply messages here
    }

    tiles {
        multiAttributeTile(name: "thermostatFull", type: "thermostat", width: 6, height: 4) {
            tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
                attributeState("default", label: '${currentValue}', unit: "C")
            }
            tileAttribute("device.heatingSetpoint", key: "VALUE_CONTROL") {
                attributeState("VALUE_UP", action: "temperatureUp")
                attributeState("VALUE_DOWN", action: "temperatureDown")
            }
            tileAttribute("device.thermostatOperatingState", key: "OPERATING_STATE") {
                attributeState("idle", backgroundColor: "#44b621")
                attributeState("heating", backgroundColor: "#ffa81e")
                attributeState("cooling", backgroundColor: "#269bd2")
            }
            tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE") {
                attributeState("off", label: '${name}')
                attributeState("heat", label: '${name}')
                attributeState("cool", label: '${name}')
                attributeState("auto", label: '${name}')
            }
            tileAttribute("device.heatingSetpoint", key: "HEATING_SETPOINT") {
                attributeState("default", label: '${currentValue}', unit: "C")
            }
        }
        main("thermostatFull")
    }

    preferences {
        section("Choose a temperature sensor... ") {
            input "sensor", "capability.temperatureMeasurement", title: "Sensor"
        }
        section("Select the heater(s)... ") {
            input "outlets", "capability.switch", title: "Outlets", multiple: true
        }
    }

}

def installed() {
    subscribe(sensor, "temperature", temperatureHandler)
    sendEvent(name: "thermostatOperatingState", value: "idle")
    sendEvent(name: "heatingSetpoint", value: 20)
}

def updated() {
    unsubscribe()
    subscribe(sensor, "temperature", temperatureHandler)
}

def temperatureHandler(temperatureEvent) {
    newTemperature = temperatureEvent.doubleValue
    sendEvent(name: "temperature", value: newTemperature)
    evaluateTemperature(newTemperature)
}

def evaluateTemperature() {
    desiredTemp = new Double(device.currentState("heatingSetpoint").value)
    currentTemp = new Double(device.currentState("termperature").value)

    def threshold = 0.5
    if (desiredTemp - currentTemp >= threshold) {
        outlets.on()
        sendEvent(name: "thermostatOperatingState", value: 'heating')
    } else if (currentTemp - desiredTemp >= threshold) {
        outlets.off()
        sendEvent(name: "thermostatOperatingState", value: 'idle')
    }
}

// Capability methods
def setHeatingSetpoint(newTemp) {
    log.debug("setHeatingSetpoint(newTemp)")
    sendEvent(name: "heatingSetpoint", value: newTemp)
    evaluateTemperature()
}

def parse(String description) {
    log.debug "Parsing '${description}'"
}

def auto() {
    log.debug("auto()")
}

def cool() {
    log.debug("cool()")
}

def emergencyHeat() {
    log.debug("emergencyHeat()")
}

def heat() {
    log.debug("heat()")
}

def off() {
    log.debug("off()")
}

def setCoolingSetpoint(setpoint) {
    log.debug("setCoolingSetpoint()" + setpoint)
}

def setSchedule(schedule) {
    log.debug("setSchedule()" + schedule)
}

def setThermostatFanMode(fanmode) {
    log.debug("setThermostatFanMode()" + fanmode)
}

def setThermostatMode(mode) {
    log.debug("setThermostatMode()" + mode)
}

// handle commands
def temperatureUp() {
    setHeatingSetpoint(new Double(device.currentState("heatingSetpoint").value) + 0.5)
}

def temperatureDown() {
    setHeatingSetpoint(new Double(device.currentState("heatingSetpoint").value) - 0.5)
}