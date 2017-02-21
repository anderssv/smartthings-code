# smartthings-code

This is my repo with code I write for the SmartThings platform. I might split this into separate repos at a later time.

# SmartApps

## Verisure integration

- [SmartApp](smartapps/smartthings-f12-no/verisure.src/verisure.groovy)
- [Alarm Device Handler](devicetypes/smartthings-f12-no/verisure-alarm.src/verisure-alarm.groovy)

This SmartApp polls the Verisure alarm at given intervals to update it's state. Default is every 60 seconds, and minimum
at 15 seconds.

You'll also need to install the custom device handler to use this smartapp.

NOTE: If the app fails on installation the error is probably in your username/password. Make sure they are correct
and try again.

## Heating control

- [SmartApp](smartapps/smartthings-f12-no/heating-control.src/heating-control.groovy)
- [Thermostat Device Handler](devicetypes/smartthings-f12-no/heating-control-thermostat.src/heating-control-thermostat.groovy)

This is my version of a virtual thermostat. It reacts to mode changes in the hub, and lets you specify temperatures
in multiple rooms for multiple modes. You can have as many rooms as you'd like. My example setup is something like this:

- Default temp: 22
    - Room 1:
        - Temp Sensor: Room1 Fibaro Multisensor
        - Switches: Plug Room1 Heater1, Plug Room1 Heater2
        - Modes: Away, Night - Temp: 19
        - Modes: Vacation - Temp: 17
    - Room 2:
        - Temp Sensor: Room2 Fibaro Door Sensor
        - Switches: Plug Room2 Heater1
        - Modes: Away - Temp: 18
        - Modes: Home, Night - Temp: 20
    - Room 3:
        - Temp Sensor: Room3 Fibaro Door Sensor
        - Switches: Plug Room3 Heater1

What's important to note here is that if you've added a room, but the hub is in a mode that's not specified the temp
will be maintained at the default temp.

WARNING: You can specify the same mode multiple times for each room. Which one is chosen is unknown. :)

To use you will also need to install the device handler which creates a virtual thermostat for each room.

# Device Handlers

## Fibaro Wall Plug ZW5

- [Fibaro Wall Plug ZW5 Device Handler](devicetypes/smartthings-f12-no/fibaro-wall-plug-zw5.src/fibaro-wall-plug-zw5.groovy)

This is the ZW5 version of the wall plug.

You should probably read [the thread in the community forums](https://community.smartthings.com/t/beta-fibaro-wall-plug-zw5/71527) before using this. :)

See the header of the source for links to device and specs.

## NorthQ Q-Power Power Meter

- [NorthQ Q-Power Device Handler]((devicetypes/smartthings-f12-no/northq-q-power.src/northq-q-power.groovy))

[The NorthQ Q-Power Power Meter](http://northq.com/qpower/) measures energy consumption. I have added a calulated power for each
period reported.