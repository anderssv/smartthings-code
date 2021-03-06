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
 * Based on: https://github.com/nilskaa/Smartthings/blob/master/devicetypes/Elko%20Dimmer
 *
 */

metadata {
    definition(name: "Elko ESH RS16 316GLED Dimmer RF PH", namespace: "smartthings.f12.no", author: "Anders Sveen", vid: "generic-dimmer") {
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"
        capability "Switch"
        capability "Switch Level"
        capability "Light"

        //Raw code from elko dimmer: 01 0104 0101 00 04 0000 0003 0006 0008 01 0003
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0006, 0008", outClusters: "0003"
    }

    tiles(scale: 2) {
        multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
            tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
                attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
                attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
                attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
            }
            tileAttribute("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action: "switch level.setLevel"
            }
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label: "", action: "refresh.refresh", icon: "st.secondary.refresh"
        }
        standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "configure", label: '', action: "configuration.configure", icon: "st.secondary.configure"
        }
        main "switch"
        details(["switch", "refresh", "configure"])
    }
}

// Parse incoming device messages to generate events
def parse(String description) {
    log.debug "description is $description"
    def event = zigbee.getEvent(description)
    if (event) {
        log.debug "event name= " + event.name + "  event value= " + event.value
        if (event.name == "level" && event.value == 0) {
        } else {
            sendEvent(event)
        }
    }
}

def off() {
    log.debug("on()")
    zigbee.off()
}

def on() {
    log.debug("on()")
    zigbee.on()
}

def setLevel(value, rate=0) {
    log.debug("setLevel()")
    zigbee.on() + zigbee.setLevel(value)
}

def refresh() {
    zigbee.onOffRefresh() + zigbee.levelRefresh()
}

def configure() {
    log.debug "binding to on/off cluster"
    [
            "zdo bind 0x${device.deviceNetworkId} 1 1 0x006 {${device.zigbeeId}} {}"
    ]

    log.debug "binding to level cluster"
    [
            "zdo bind 0x${device.deviceNetworkId} 1 1 0x008 {${device.zigbeeId}} {}"
    ]

    log.debug "set up reporting on: on/off and level"
    [
            zigbee.configureReporting(0x006, 0x0000, 0x10, 0, 3600, null) +
                    zigbee.configureReporting(0x008, 0x0000, 0x20, 0, 3600, 0x0001)
    ]
}
