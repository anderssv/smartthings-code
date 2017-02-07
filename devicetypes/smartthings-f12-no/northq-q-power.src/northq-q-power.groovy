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
        capability "Actuator"
        capability "Switch"
        capability "Power Meter"
        capability "Polling"
        capability "Refresh"
        capability "Configuration"
        capability "Sensor"

        command "reset"

        fingerprint mfr: "0096", prod: "0001", model: "0001"
    }

    // simulator metadata
    simulator {
        status "on": "command: 2003, payload: FF"
        status "off": "command: 2003, payload: 00"

        for (int i = 0; i <= 10000; i += 1000) {
            status "power  ${i} W": new physicalgraph.zwave.Zwave().meterV1.meterReport(
                    scaledMeterValue: i, precision: 3, meterType: 4, scale: 2, size: 4).incomingMessage()
        }
        for (int i = 0; i <= 100; i += 10) {
            status "energy  ${i} kWh": new physicalgraph.zwave.Zwave().meterV1.meterReport(
                    scaledMeterValue: i, precision: 3, meterType: 0, scale: 0, size: 4).incomingMessage()
        }

        // reply messages
        reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
        reply "200100,delay 100,2502": "command: 2503, payload: 00"

    }

    // tile definitions
    tiles {
        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
            state "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
        }
        valueTile("power", "device.power") {
            state "default", label: '${currentValue} W'
        }
        valueTile("energy", "device.energy") {
            state "default", label: '${currentValue} kWh'
        }
        standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat") {
            state "default", label: 'reset kWh', action: "reset"
        }
        standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat") {
            state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
        }

        main(["switch", "power", "energy"])
        details(["switch", "power", "energy", "refresh", "reset"])
    }
}

def updated() {
	response(configure())
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

def zwaveEvent(physicalgraph.zwave.commands.meterv1.MeterReport cmd) {
    if (cmd.scale == 0) {
        createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kWh")
    } else if (cmd.scale == 1) {
        createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kVAh")
    } else if (cmd.scale == 2) {
        createEvent(name: "power", value: Math.round(cmd.scaledMeterValue), unit: "W")
    }
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
    def evt = createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
    if (evt.isStateChange) {
        [evt, response(["delay 3000", zwave.meterV2.meterGet(scale: 2).format()])]
    } else {
        evt
    }
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital")
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
    def result = []

    def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
    log.debug "msr: $msr"
    updateDataValue("MSR", msr)

    // retypeBasedOnMSR()

    result << createEvent(descriptionText: "$device.displayName MSR: $msr", isStateChange: false)

    if (msr.startsWith("0086") && !state.aeonconfig) {  // Aeon Labs meter
        state.aeonconfig = 1
        result << response(delayBetween([
                zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 4).format(),   // report power in watts
                zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 300).format(), // every 5 min
                zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 8).format(),   // report energy in kWh
                zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: 300).format(), // every 5 min
                zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 0).format(),    // no third report
                //zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: 300).format(), // every 5 min
                zwave.meterV2.meterGet(scale: 0).format(),
                zwave.meterV2.meterGet(scale: 2).format(),
        ]))
    } else {
        result << response(delayBetween([
                zwave.meterV2.meterGet(scale: 0).format(),
                zwave.meterV2.meterGet(scale: 2).format(),
        ]))
    }

    result
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.debug "$device.displayName: Unhandled: $cmd"
    [:]
}

def on() {
    [
            zwave.basicV1.basicSet(value: 0xFF).format(),
            zwave.switchBinaryV1.switchBinaryGet().format(),
            "delay 3000",
            zwave.meterV2.meterGet(scale: 2).format()
    ]
}

def off() {
    [
            zwave.basicV1.basicSet(value: 0x00).format(),
            zwave.switchBinaryV1.switchBinaryGet().format(),
            "delay 3000",
            zwave.meterV2.meterGet(scale: 2).format()
    ]
}

def poll() {
    delayBetween([
            zwave.switchBinaryV1.switchBinaryGet().format(),
            zwave.meterV2.meterGet(scale: 0).format(),
            zwave.meterV2.meterGet(scale: 2).format()
    ])
}

def refresh() {
    delayBetween([
            zwave.switchBinaryV1.switchBinaryGet().format(),
            zwave.meterV2.meterGet(scale: 0).format(),
            zwave.meterV2.meterGet(scale: 2).format()
    ])
}

def configure() {
	log.debug("Configuring device...")
    zwave.configurationV1.configurationSet(parameterNumber: 1, size: 4, scaledConfigurationValue: 1000)    // The number of blinks pr. kwh
    zwave.configurationV1.configurationSet(parameterNumber: 2, size: 1, scaledConfigurationValue: 1)       // The type of meter, mechanical/electric pulse

    zwave.manufacturerSpecificV2.manufacturerSpecificGet().format()
}

def reset() {
    return [
            zwave.meterV2.meterReset().format(),
            zwave.meterV2.meterGet(scale: 0).format()
    ]
}