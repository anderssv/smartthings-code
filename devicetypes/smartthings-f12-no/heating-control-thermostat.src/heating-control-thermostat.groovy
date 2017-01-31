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
	definition (name: "Heating Control Thermostat", namespace: "smartthings.f12.no", author: "Anders Sveen <anders@f12.no>") {
		capability "Thermostat"

		command "temperatureUp"
		command "temperatureDown"
	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		multiAttributeTile(name:"thermostatFull", type:"thermostat", width:6, height:4) {
    		tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
        		attributeState("default", label:'${currentValue}', unit:"C")
    		}
    		tileAttribute("device.temperature", key: "VALUE_CONTROL") {
        		attributeState("VALUE_UP", action: "temperatureUp")
        		attributeState("VALUE_DOWN", action: "temperatureDown")
    		}
    		tileAttribute("device.thermostatOperatingState", key: "OPERATING_STATE") {
        		attributeState("idle", backgroundColor:"#44b621")
        		attributeState("heating", backgroundColor:"#ffa81e")
				attributeState("cooling", backgroundColor:"#269bd2")            
    		}
    		tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE") {
        		attributeState("off", label:'${name}')
        		attributeState("heat", label:'${name}')
        		attributeState("cool", label:'${name}')
        		attributeState("auto", label:'${name}')
    		}
    		tileAttribute("device.heatingSetpoint", key: "HEATING_SETPOINT") {
        		attributeState("default", label:'${currentValue}', unit:"C")
    		}
		}
        valueTile("setpointTile", "device.heatingSetpoint", width: 2, height: 2) {
        	state "heatingSetpoint", label: '${currentValue}'
        }
		main("thermostatFull")
	}
}

def parse(String description) {
	log.debug "Parsing '${description}'"
}

def setInitialInformation(temperature, setpoint) {
    sendEvent(name: "heatingSetpoint", value: setpoint)
    sendEvent(name: "thermostatMode", value: "heat")
    updateMode("heating")
	updateTemperature(temperature)
}

def updateMode(mode, setpoint) {
	log.debug("Updated mode with ${mode}")
    sendEvent(name: "thermostatOperatingState", value: mode)
    sendEvent(name: "heatingSetpoint", value: setpoint)
}

def updateTemperature(newTemperature) {
    log.debug("Set temperature: ${newTemperature}")
    sendEvent(name: "temperature", value: newTemperature)
}

// handle commands
def temperatureUp() {
	log.debug "Executing 'temperatureUp'"
	// TODO: handle 'temperatureUp' command
}

def temperatureDown() {
	log.debug "Executing 'temperatureDown'"
	// TODO: handle 'temperatureDown' command
}