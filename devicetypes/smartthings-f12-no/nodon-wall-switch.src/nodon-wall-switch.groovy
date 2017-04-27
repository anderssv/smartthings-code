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
 * ===== NodOn Wall Switch =====
 *
 * This is just pulled from this thread and seems to work: https://community.smartthings.com/t/support-for-nodon-octan-z-wave-remote-dth-in-post-32/33896/32
 *
 * It was originally created for NodOn Octan (see comments below)
 *
 * ===== Nodon Octan =====
 *
 * 	Modified for NodOn Octan Remote (CRC-3-1-0x) by Richard_Woodward
 *
 * 	Octan supports 4 actions per button	Pushed
 * 										Double Tap
 * 										Held
 * 										Hold Released
 *
 * 	v1.01 Updated to support all 16 actions:
 *
 *  As Smartlighting can only support type "Pushed" or "Hold" events the extra 4 functions have been mapped to 
 *  fictional buttons 5 through 8.
 *
 *  Button 1 double Tap is mapped to   "Button 5 Pushed" in smartlighting
 *  Button 1 Hold Release is mapped to "Button 5 Hold"   in smarlighting
 *  .
 *  Button 4 double Tap is mapped to   "Button 8 Pushed" in Smartlighting
 *  Button 4 Hold Rlease is mapped to  "Button 8 Hold"   in Smartlighting
 *
 */
metadata {
    definition(name: "NodOn Wall Switch", namespace: "smartthings.f12.no", author: "Anders Sveen <anders@f12.no>") {
        capability "Actuator"
        capability "Button"
        capability "Holdable Button"
        capability "Configuration"
        capability "Sensor"
        capability "Battery"

        fingerprint deviceId: "0x0101", inClusters: "0x86,0x72,0x70,0x80,0x84,0x85"
    }

    simulator {
        status "button 1 pushed": "command: 2001, payload: 01"
        status "button 1 held": "command: 2001, payload: 15"
        status "button 2 pushed": "command: 2001, payload: 29"
        status "button 2 held": "command: 2001, payload: 3D"
        status "button 3 pushed": "command: 2001, payload: 51"
        status "button 3 held": "command: 2001, payload: 65"
        status "button 4 pushed": "command: 2001, payload: 79"
        status "button 4 held": "command: 2001, payload: 8D"
        status "wakeup": "command: 8407, payload: "
    }
    tiles {
        standardTile("button", "device.button", width: 2, height: 2) {
            state "default", label: "", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffffff"
            state "button 1 pushed", label: "pushed #1", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#79b821"
            state "button 2 pushed", label: "pushed #2", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#79b821"
            state "button 3 pushed", label: "pushed #3", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#79b821"
            state "button 4 pushed", label: "pushed #4", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#79b821"
            state "button 1 held", label: "held #1", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffa81e"
            state "button 2 held", label: "held #2", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffa81e"
            state "button 3 held", label: "held #3", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffa81e"
            state "button 4 held", label: "held #4", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffa81e"
        }
        valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
            state "battery", label: '${currentValue}% battery', unit: ""
        }
        main "button"
        details(["button", "battery"])
    }
}

def parse(String description) {
    def results = []
    if (description.startsWith("Err")) {
        results = createEvent(descriptionText: description, displayed: true)
    } else {
        def cmd = zwave.parse(description, [0x80: 1, 0x84: 1])
        //log.debug("cmd=$cmd")
        if (cmd) results += zwaveEvent(cmd)
        if (!results) results = [descriptionText: cmd, displayed: false]
    }
    log.debug("Parsed '$description' to $results")
    return results
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd) {
    def results = [createEvent(descriptionText: "$device.displayName woke up", isStateChange: false)]

    def prevBattery = device.currentState("battery")
    if (!prevBattery || (new Date().time - prevBattery.date.time) / 60000 >= 60 * 53) {
        results << response(zwave.batteryV1.batteryGet().format())
        log.debug("  read battery")
    }
    results += configurationCmds().collect { response(it) }
    results << response(zwave.wakeUpV1.wakeUpNoMoreInformation().format())
    return results
}

def buttonEvent(button, held) {
    button = button as Integer
    held = held as Integer
    //log.debug("button=$button Held=$held")
    if (held == 1) {
        def ebutton = button + 4
        createEvent(name: "button", value: "held", data: [buttonNumber: ebutton], descriptionText: "$device.displayName button $ebutton was pushed", isStateChange: true)
    } else if (held == 2) {
        createEvent(name: "button", value: "held", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was held", isStateChange: true)
    } else if (held == 3) {
        def ebutton = button + 4
        createEvent(name: "button", value: "pushed", data: [buttonNumber: ebutton], descriptionText: "$device.displayName button $ebutton was pushed", isStateChange: true)
    } else if (held == 0) {
        createEvent(name: "button", value: "pushed", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was pushed", isStateChange: true)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd) {
    Integer button = (cmd.sceneId / 10) as Integer
    Integer held = cmd.sceneId - (button * 10) as Integer
    buttonEvent(button, held)
}


def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
    def map = [name: "battery", unit: "%"]
    if (cmd.batteryLevel == 0xFF) {
        map.value = 1
        map.descriptionText = "${device.displayName} has a low battery"
    } else {
        map.value = cmd.batteryLevel
    }
    createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    [descriptionText: "$device.displayName: $cmd", linkText: device.displayName, displayed: false]
}

def configurationCmds() {
    delayBetween([
            zwave.configurationV1.configurationSet(parameterNumber: 250, scaledConfigurationValue: 1).format(),
            // zwave.associationV1.associationSet(groupingIdentifier: 2, nodeId: zwaveHubNodeId).format(),
            // zwave.associationV1.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format(),
            //  zwave.associationV1.associationSet(groupingIdentifier: 4, nodeId: zwaveHubNodeId).format(),
            // zwave.associationV1.associationSet(groupingIdentifier: 5, nodeId: zwaveHubNodeId).format(),
            zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1).format(),
            zwave.configurationV1.configurationSet(configurationValue: [3], parameterNumber: 8, size: 1).format(),
            zwave.associationV1.associationSet(groupingIdentifier: 1, nodeId: zwaveHubNodeId).format()
    ], 500)
}

def configure() {
    def cmd = configurationCmds()
    //log.debug("Sending configuration: $cmd")
    return cmd
}


def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    sendEvent(name: "numberOfButtons", value: 8)
    log.debug("Sent numberOfButtons 8")
}