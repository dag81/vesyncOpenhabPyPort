# VeSync Binding

It's current support is for the CoreXXXS air purifiers branded as Levoit which utilise the VeSync app.

Models supported are Core200S, Core300S and Core400S.

### Restrictions / TODO

1. Add units to the air_quality_ppm25 channel
2. Confirm the night light for both reading and writing works against the Core200S and Core300S models 
3. Confirm the suspected behaviour that air_quality is relevant only to the Core200S and Core300S models, and maps to some sort of visual display on the devices. 
   * If this is confirmed dynamically remove the air_quality channel from the Core400S model
   * If this is confirmed dynamically remove the air_quality_ppm25 channel from the Core200S and Core300S models
4. Look at protocol mapping the V1 specification for the LV prefixed model. 
5. Look at adding support for the moisture unit also supported by the python lib. 
6. Look at potentially other equipment supported by the VeSync API.

### Credits

The binding code is based on a lot of work done by other developers:

- Contributors of (https://github.com/webdjoe/pyvesync) - Python interface for VeSync
- Rene Scherer, Holger Eisold - Sure Petcare Binding for OpenHab as a reference point for the starting blocks of this code 

## Supported Things

This binding supports the follow thing types:

| Thing       | Thing Type | Discovery | Description      |  
|-------------|------------|-----------|------------------|
| Bridge      | Bridge     | Manual    | A single connection to the VeSync API  |
| AirPurifier | Thing      | Automatic | A Air Purifier supporting V2 e.g. Core200S/Core300S or Core400S unit |

This binding was developed from the great work in the listed projects.

The only unit it has been tested against is the Core400S unit, **I'm looking for others to confirm** my queries regarding **the Core200S and Core300S** units.

## Discovery

Once the bridge is configured auto discovery will discover supported devices from the VeSync API.

## Binding Configuration

The binding consists of a Bridge (the API connection), and at the moment one Thing for each Air Purifier, which relates to the individual hardware devices supported. VeSync things can be configured either through the online configuration utility via discovery, or manually through a 'vesync.things' configuration file. The Bridge is not automatically discovered and must be added manually. That is because the VeSync API requires authentication credentials to communicate with the service.

After adding the Bridge and setting the config, it will go ONLINE. After a short while, the discovery process for the VeSync devices will start. When supported hardware is discovered it will appear in the Inbox.

## Thing Configuration

Details for the manual configuration are below, using the OpenHab3 UI hopefully it is self-explanatory.

## Channels

Channel names in **bold** are read/write, everything else is read-only

### AirPurifier Thing

| Channel                | Type                    | Description                                               |
|------------------------|-------------------------|-----------------------------------------------------------|
| **enabled**            | Switch                  | Whether the hardware device is enabled (Switched on)      |
| **child-lock**         | Switch                  | Whether the child lock (display lock is enabled)          |
| **display**            | Switch                  | Whether the display is enabled (display is shown)         |
| **fan-mode**           | String                  | The operation mode of the fan                             |
| **manual-fan-speed**   | String                  | The speed of the fan when in manual mode                  |
| **night-light-mode**   | String                  | The night lights mode                                     |
| filter-life-percentage | Number:Dimensionless    | The remaining filter life as a percentage                 |
| air-quality            | Number                  | The air quality as represented by the Core200S / Core300S |
| air-quality-ppm25      | Number:Dimensionless    | The air quality as represented by the Core400S            |
| error-code             | Number                  | The error code reported by the device                     |
| timer-remain           | Number                  | The seconds left on the timer at the last poll            |
| timer-expiry           | DateTime                | The expected expiry time of the current timer             |
| schedules-count        | Number                  | The number schedules configured                           |
| config-display         | Switch                  | Config: Whether the display is enabled                    |
| config-display-forever | Switch                  | Config: Whether the display will disable when not active  |
| config-auto-mode       | String                  | Config: The mode of operation when auto is active         |
| config-auto-room-size  | Number                  | Config: The room size set when auto utilises the room size|

## Full Example

### Configuration (*.things)

#### Bridge configuration parameters

| Name                   | Type                    | Description                                               |
|------------------------|-------------------------|-----------------------------------------------------------|
| username               | String                  | The username as used in the VeSync mobile application     |
| password               | String                  | The password as used in the VeSync mobile application     |

#### AirPurifier configuration parameters

It is recommended to use the device name, for locating devices. For this to work all the devices should have a unique
name in the VeSync mobile application.

The mac address from the VeSync mobile application may not align to the one the API
uses, therefore it's best left not configured or taken from auto-discovered information.

Device's will be found communicated with via the MAC Id first and if unsuccessful then by the deviceName.

| Name                   | Type                    | Description                                                         |
|------------------------|-------------------------|---------------------------------------------------------------------|
| deviceName             | String                  | The name given to the device under Settings -> Device Name          |
| macId                  | String                  | The mac for the device under Settings -> Device Info -> MAC Address |

#### Core 200S/300S/400S Model
```
Bridge vesync:bridge:vesyncServers [username="<USERNAME>", password="<PASSWORD>"] {
	AirPurifier loungeAirFilter [deviceName="<DEVICE NAME FROM APP>"]
	AirPurifier bedroomAirFilter [deviceName="<DEVICE NAME FROM APP>"]
}
```

### Configuration (*.items)

#### Core 400S Model

```
Switch               LoungeAPPower        	   "Lounge Air Purifier Power"                                  { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:enabled" }
Switch               LoungeAPDisplay      	   "Lounge Air Purifier Display"                                { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:config-display" }
Switch               LoungeAPControlsLock          "Lounge Air Purifier Controls Locked"                        { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:child-lock" }
Number:Dimensionless LoungeAPFilterRemainingUse    "Lounge Air Purifier Filter Remaining [%.0f %%]"             { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:filter-life-percentage" }
String               LoungeAPMode                  "Lounge Air Purifier Mode [%s]"                              { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:fan-mode" }
Number:Dimensionless LoungeAPManualFanSpeed        "Lounge Air Purifier Manual Fan Speed"                       { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:manual-fan-speed" }
Number:Dimensionless LoungeAPAirQuality		   "Lounge Air Purifier Air Quality [%.0f% PM2.5]"              { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:air-quality-ppm25" }
Number               LoungeAPErrorCode     	   "Lounge Air Purifier Error Code"                             { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:error-code" }
String               LoungeAPAutoMode		   "Lounge Air Purifier Auto Mode"                              { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:config-auto-mode" }
Number               LoungeAPAutoRoomSize 	   "Lounge Air Purifier Auto Room Size [%.0f% sqft]"            { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:config-auto-room-size" }
Number:Time          LoungeAPTimerLeft		   "Lounge Air Purifier Timer Left [%1$Tp]"                     { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:timer-remain" }	
DateTime             LoungeAPTimerExpire           "Lounge Air Purifier Timer Expiry [%1$tA %1$tI:%1$tM %1$Tp]" { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:timer-expiry" }
Number               LoungeAPSchedulesCount 	   "Lounge Air Purifier Schedules Count"                        { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:schedules-count" }
```

#### Core 200S/300S Model

```
Switch               LoungeAPPower        	   "Lounge Air Purifier Power"                                  { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:enabled" }
Switch               LoungeAPDisplay      	   "Lounge Air Purifier Display"                                { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:config-display" }
String               LoungeAPNightLightMode		   "Lounge Air Purifier Night Light Mode"                              { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:night-light-mode" }
Switch               LoungeAPControlsLock          "Lounge Air Purifier Controls Locked"                        { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:child-lock" }
Number:Dimensionless LoungeAPFilterRemainingUse    "Lounge Air Purifier Filter Remaining [%.0f %%]"             { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:filter-life-percentage" }
String               LoungeAPMode                  "Lounge Air Purifier Mode [%s]"                              { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:fan-mode" }
Number:Dimensionless LoungeAPManualFanSpeed        "Lounge Air Purifier Manual Fan Speed"                       { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:manual-fan-speed" }
Number:Dimensionless LoungeAPAirQuality		   "Lounge Air Purifier Air Quality [%.0f%]"              { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:air-quality" }
Number               LoungeAPErrorCode     	   "Lounge Air Purifier Error Code"                             { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:error-code" }
String               LoungeAPAutoMode		   "Lounge Air Purifier Auto Mode"                              { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:config-auto-mode" }
Number               LoungeAPAutoRoomSize 	   "Lounge Air Purifier Auto Room Size [%.0f% sqft]"            { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:config-auto-room-size" }
Number:Time          LoungeAPTimerLeft		   "Lounge Air Purifier Timer Left [%1$Tp]"                     { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:timer-remain" }	
DateTime             LoungeAPTimerExpire           "Lounge Air Purifier Timer Expiry [%1$tA %1$tI:%1$tM %1$Tp]" { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:timer-expiry" }
Number               LoungeAPSchedulesCount 	   "Lounge Air Purifier Schedules Count"                        { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:schedules-count" }
```

### Configuration (*.sitemap)

#### Core 400S Model
```
Frame {
   Switch item=LoungeAPPower label="Power"
   Text   item=LoungeAPFilterRemainingUse label="Filter Remaining"
   Switch item=LoungeAPDisplay label="Display"
   Text   item=LoungeAPAirQuality label="Air Quality [%.0f (PM2.5)]"                
   Switch item=LoungeAPControlsLock label="Controls Locked"
   Text   item=LoungeAPTimerExpiry label="Timer Shutdown @" icon="clock"
   Switch item=LoungeAPMode label="Mode" mappings=[auto="Auto", manual="Manual Fan Control", sleep="Sleeping"] icon="settings"
   Text   item=LoungeAPErrorCode label="Error Code [%.0f]"
   Switch item=LoungeAPManualFanSpeed label="Manual Fan Speed [%.0f]" mappings=[1="1", 2="2", 3="3", 4="4"] icon="settings"                               
}
```

#### Core 200S/300S Model

This is untested but based on data from pyvesync.

```
Frame {
   Switch item=LoungeAPPower label="Power"
   Text   item=LoungeAPFilterRemainingUse label="Filter Remaining"
   Switch item=LoungeAPDisplay label="Display"
   Switch item=LoungeAPNightLightMode label="Mode" mappings=[on="On", dim="Dimmed", off="Off"] icon="settings"
   Text   item=LoungeAPAirQuality label="Air Quality [%.0f]"                
   Switch item=LoungeAPControlsLock label="Controls Locked"
   Text   item=LoungeAPTimerExpiry label="Timer Shutdown @" icon="clock"
   Switch item=LoungeAPMode label="Mode" mappings=[manual="Manual Fan Control", sleep="Sleeping"] icon="settings"
   Text   item=LoungeAPErrorCode label="Error Code [%.0f]"
   Switch item=LoungeAPManualFanSpeed label="Manual Fan Speed [%.0f]" mappings=[1="1", 2="2", 3="3"] icon="settings"                               
}
