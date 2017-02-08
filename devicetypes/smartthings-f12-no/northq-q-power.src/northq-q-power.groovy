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
 *
 *  Z-Wave alliance device page: http://products.z-wavealliance.org/products/69
 *  Device homepage: http://northq.com/qpower/
 *  Technical specification: https://doc.eedomus.com/files/northq_nq-92021_manuel_us.pdf
 *
 */
metadata {
    definition(name: "NorthQ Q-Power", namespace: "smartthings.f12.no", author: "Anders Sveen <anders@f12.no>") {
        capability "Energy Meter"
        capability "Configuration"
        capability "Sensor"
        capability "Battery"

        fingerprint mfr: "0096", prod: "0001", model: "0001"
    }

    // simulator metadata
    simulator {
        for (int i = 0; i <= 100; i += 10) {
            status "energy  ${i} kWh": new physicalgraph.zwave.Zwave().meterV1.meterReport(
                    scaledMeterValue: i, precision: 3, meterType: 0, scale: 0, size: 4).incomingMessage()
        }
    }

    // tile definitions
    tiles {
        valueTile("energy", "device.energy") {
            state "default", label: '${currentValue} kWh'
        }

        main(["energy"])
        details(["energy"])
    }
}

def parse(String description) {
    log.debug("Event received parsing: '${description}'")
    def result = null
    if (description == "updated") return
    def cmd = zwave.parse(description, [0x20: 1, 0x32: 1, 0x72: 2])
    if (cmd) {
        log.debug "$device.displayName: Command received: $cmd"
        result = zwaveEvent(cmd)
    }
    return result
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd) {
	def allCommands = [
    	zwave.meterV1.meterGet().format(),
        zwave.batteryV1.batteryGet().format()
    ]
    if (state.configurationCommands) { 
    	allCommands = (allCommands + state.configurationCommands) 
    }
	
    state.configurationCommands = null
    
	log.debug("Sent ${allCommands.size} commands in response to wake up")
	return [
    	createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false),
       	response(allCommands)
    ]
}

def zwaveEvent(physicalgraph.zwave.commands.meterv1.MeterReport cmd) {
    if (cmd.scale == 0) {
        createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kWh")
    } else if (cmd.scale == 1) {
    	log.error("Did not think the meter scale was going to be 1")
        createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kVAh")
    } else {
        log.error("Received meter report with scale ${cmd.scale} , don't know how to interpret that")
    }
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	log.debug("Battery level is: ${cmd.batteryLevel}")
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
	log.debug("Configuration changed. Parameter number: ${cmd.parameterNumber}")
    return [:]
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.debug "$device.displayName: Unhandled: $cmd"
    return [:]
}

def updated() {
	configure()
    
    return [:]
}

def configure() {
	log.debug("Preparing configuration. It will be sent next time the device wakes up and checks in...")
    
    state.configurationCommands = [
    	zwave.configurationV1.configurationSet(parameterNumber: 1, size: 4, scaledConfigurationValue: 1000 * 10).format(),    // The number of blinks pr. kwh
    	zwave.configurationV1.configurationSet(parameterNumber: 2, size: 1, scaledConfigurationValue: 1).format()             // The type of meter, mechanical/electric pulse
    ]

	return [:]
}