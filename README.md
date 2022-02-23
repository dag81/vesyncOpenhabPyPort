# VeSync Binding

It's current support is for the Air purifiers & Humidifer's branded as Levoit which utilise the VeSync app based on the V2 protocol.

### Verified Models

Air Filtering models supported are Core300S, Core400S.
Air Humidifier models supported are Dual200S, Classic 300S, 600S.

### Awaiting User Verification Models

Air Filtering models supported are Core200S and Core600S.
Air Humidifier Classic 200S

### Restrictions / TODO

1. Look at protocol mapping the V1 specification for the LV prefixed model.
2. Look at potentially other equipment supported by the VeSync API.
3. Add remaining write commands to complete Humidifier 600S support, if warm mode is controllable.
4. Adjust Filtering Core200S & Core600S support based on feedback, given initial conversion.
5. Adjust Humidifier 200S support based on feedback, given initial conversion.

### Credits

The binding code is based on a lot of work done by other developers:

- Contributors of (https://github.com/webdjoe/pyvesync) - Python interface for VeSync
- Rene Scherer, Holger Eisold - (https://www.openhab.org/addons/bindings/surepetcare) Sure Petcare Binding for OpenHab as a reference point for the starting blocks of this code

## Supported Things

This binding supports the follow thing types:

| Thing        | Thing Type | Discovery | Description                                                          |  
|--------------|------------|-----------|----------------------------------------------------------------------|
| Bridge       | Bridge     | Manual    | A single connection to the VeSync API                                |
| AirPurifier  | Thing      | Automatic | A Air Purifier supporting V2 e.g. Core200S/Core300S or Core400S unit |
| AirHumidifier| Thing      | Automatic | A Air Humidifier supporting V2 e.g. Classic300S or 600s               |

This binding was developed from the great work in the listed projects.

The only Air Filter unit it has been tested against is the Core400S unit, **I'm looking for others to confirm** my queries regarding **the Core200S and Core300S** units.
The ***Classic 300S Humidifier*** has been tested, and ***600S with current warm mode restrictions***.

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

| Channel                | Type                    | Description                                               | Model's Supported |  
|------------------------|-------------------------|-----------------------------------------------------------|-------------------|
| **enabled**            | Switch                  | Whether the hardware device is enabled (Switched on)      | 600S, 400S, 300S  |
| **childLock**         | Switch                  | Whether the child lock (display lock is enabled)          | 600S, 400S, 300S  |
| **display**            | Switch                  | Whether the display is enabled (display is shown)         | 600S, 400S, 300S  |
| **fanMode**           | String                  | The operation mode of the fan                             | 600S, 400S, 300S  |
| **manualFanSpeed**   | String                  | The speed of the fan when in manual mode                  | 600S, 400S, 300S  |
| **nightLightMode**   | String                  | The night lights mode                                     | 600S, 400S, 300S  |
| filterLifePercentage | Number:Dimensionless    | The remaining filter life as a percentage                 | 600S, 400S, 300S  |
| airQuality            | Number                  | The air quality as represented by the Core200S / Core300S | 600S, 400S, 300S  |
| airQualityPPM25      | Number:Dimensionless    | The air quality as represented by the Core400S            | 600S, 400S, 300S  |
| errorCode             | Number                  | The error code reported by the device                     | 600S, 400S, 300S  |
| timerExpiry           | DateTime                | The expected expiry time of the current timer             | 600S, 400S        |
| schedulesCount        | Number                  | The number schedules configured                           | 600S, 400S        |
| configDisplayForever | Switch                  | Config: Whether the display will disable when not active  | 600S, 400S, 300S  |
| configAutoMode       | String                  | Config: The mode of operation when auto is active         | 600S, 400S, 300S  |
| configAutoRoomSize  | Number                  | Config: The room size set when auto utilises the room size| 600S, 400S, 300S  |

### AirHumidifier Thing

| Channel                  | Type                    | Description                                                | Model's Supported          | 
|--------------------------|-------------------------|------------------------------------------------------------|----------------------------|
| **enabled**              | Switch                  | Whether the hardware device is enabled (Switched on)       | 200S, Dual200S, 300S, 600S |
| **display**              | Switch                  | Whether the display is enabled (display is shown)          | 200S, Dual200S, 300S, 600S |
| waterLacking             | Switch                  | Indicator whether the unit is lacking water                | 200S, Dual200S, 300S, 600S |
| humidityHigh             | Switch                  | Indicator for high humidity                                | 200S, Dual200S, 300S, 600S |
| waterTankLifted          | Switch                  | Indicator for whether the water tank is removed            | 200S, Dual200S, 300S, 600S |
| **stopAtTargetLevel**    | Switch                  | Whether the unit is set to stop when the target is reached | 200S, Dual200S, 300S, 600S |
| humidity                 | Number:Dimensionless    | Indicator for the currently measured humidity level        | 200S, Dual200S, 300S, 600S |
| **mistLevel**            | Number:Dimensionless    | The current mist level set (1-2) or (1-3)                  | 200S, Dual200S, 300S, 600S |
| **humidifierMode**       | String                  | The current mode of operation [auto,sleep]                 | 200S, Dual200S, 300S, 600S |
| **nightLightMode**       | String                  | The night light mode [on,dim,off]                          | 300S                       |
| **configTargetHumidity** | Number:Dimensionless    | Config: What the target humidity is set to reach           | 200S, Dual200S, 300S, 600S |

## Full Example

### Configuration (*.things)

#### Bridge configuration parameters

| Name                   | Type                    | Description                                           |
|------------------------|-------------------------|-------------------------------------------------------|
| username               | String                  | The username as used in the VeSync mobile application |
| password               | String                  | The password as used in the VeSync mobile application |
| airPurifierPollInterval| String                  | The poll interval for air filters / humidifers        |

* Note Air PPM Levels don't usually change quickly - 60s seems reasonable if OpenHab is controlling it and your don't want near instant feedback of physical interactions with the devices.

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

#### Air Purifiers Core 200S/300S/400S Models & Air Humidifier Classic300S/600S Models

```
Bridge vesync:bridge:vesyncServers [username="<USERNAME>", password="<PASSWORD>", airPurifierPollInterval=60] {
	AirPurifier loungeAirFilter [deviceName="<DEVICE NAME FROM APP>"]
	AirPurifier bedroomAirFilter [deviceName="<DEVICE NAME FROM APP>"]
	AirHumidifier loungeHumidifier [deviceName="<DEVICE NAME FROM APP>"]
}
```

### Configuration (*.items)

#### Air Purifier Core 400S / 600S Model

```
Switch               LoungeAPPower        	   "Lounge Air Purifier Power"                                  { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:enabled" }
Switch               LoungeAPDisplay      	   "Lounge Air Purifier Display"                                { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:display" }
Switch               LoungeAPControlsLock          "Lounge Air Purifier Controls Locked"                        { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:childLock" }
Number:Dimensionless LoungeAPFilterRemainingUse    "Lounge Air Purifier Filter Remaining [%.0f %%]"             { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:filterLifePercentage" }
String               LoungeAPMode                  "Lounge Air Purifier Mode [%s]"                              { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:fanMode" }
Number:Dimensionless LoungeAPManualFanSpeed        "Lounge Air Purifier Manual Fan Speed"                       { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:manualFanSpeed" }
Number:Dimensionless LoungeAPAirQuality		   "Lounge Air Purifier Air Quality [%.0f% PM2.5]"              { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:airQualityPPM25" }
Number               LoungeAPErrorCode     	   "Lounge Air Purifier Error Code"                             { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:errorCode" }
String               LoungeAPAutoMode		   "Lounge Air Purifier Auto Mode"                              { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:configAutoMode" }
Number               LoungeAPAutoRoomSize 	   "Lounge Air Purifier Auto Room Size [%.0f% sqft]"            { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:configAutoRoomSize" }
Number:Time          LoungeAPTimerLeft		   "Lounge Air Purifier Timer Left [%1$Tp]"                     { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:timerRemain" }	
DateTime             LoungeAPTimerExpire           "Lounge Air Purifier Timer Expiry [%1$tA %1$tI:%1$tM %1$Tp]" { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:timerExpiry" }
Number               LoungeAPSchedulesCount 	   "Lounge Air Purifier Schedules Count"                        { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:schedulesCount" }
```

#### Air Purifier Core 200S/300S Model

```
Switch               LoungeAPPower        	   "Lounge Air Purifier Power"                                  { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:enabled" }
Switch               LoungeAPDisplay      	   "Lounge Air Purifier Display"                                { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:display" }
String               LoungeAPNightLightMode        "Lounge Air Purifier Night Light Mode"                       { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:nightLightMode" }
Switch               LoungeAPControlsLock          "Lounge Air Purifier Controls Locked"                        { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:childLock" }
Number:Dimensionless LoungeAPFilterRemainingUse    "Lounge Air Purifier Filter Remaining [%.0f %%]"             { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:filterLifePercentage" }
String               LoungeAPMode                  "Lounge Air Purifier Mode [%s]"                              { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:fanMode" }
Number:Dimensionless LoungeAPManualFanSpeed        "Lounge Air Purifier Manual Fan Speed"                       { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:manualFanSpeed" }
Number:Dimensionless LoungeAPAirQuality		   "Lounge Air Purifier Air Quality [%.0f%]"                    { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:airQuality" }
Number               LoungeAPErrorCode     	   "Lounge Air Purifier Error Code"                             { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:errorCode" }
String               LoungeAPAutoMode		   "Lounge Air Purifier Auto Mode"                              { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:configAutoMode" }
Number               LoungeAPAutoRoomSize 	   "Lounge Air Purifier Auto Room Size [%.0f% sqft]"            { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:configAutoRoomSize" }
Number:Time          LoungeAPTimerLeft		   "Lounge Air Purifier Timer Left [%1$Tp]"                     { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:timerRemain" }	
DateTime             LoungeAPTimerExpire           "Lounge Air Purifier Timer Expiry [%1$tA %1$tI:%1$tM %1$Tp]" { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:timerExpiry" }
Number               LoungeAPSchedulesCount 	   "Lounge Air Purifier Schedules Count"                        { channel="vesync:AirPurifier:vesyncServers:loungeAirFilter:schedulesCount" }
```

#### Air Humidifier Classic 200S / Dual 200S Model

```
Switch               LoungeAHPower             "Lounge Air Humidifier Power"                                  { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:enabled" }
Switch               LoungeAHDisplay           "Lounge Air Humidifier Display"                                { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:display" }
String               LoungeAHMode              "Lounge Air Humidifier Mode"                                   { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:humidifierMode" }
Switch               LoungeAHWaterLacking      "Lounge Air Humidifier Water Lacking"                          { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:waterLacking" }
Switch               LoungeAHHighHumidity      "Lounge Air Humidifier High Humidity"                          { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:humidityHigh" }
Switch               LoungeAHWaterTankRemoved  "Lounge Air Humidifier Water Tank Removed"                     { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:waterTankLifted" }
Number:Dimensionless LoungeAHHumidity          "Lounge Air Humidifier Measured Humidity"                      { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:humidity" }
Switch               LoungeAHTargetStop        "Lounge Air Humidifier Stop at target"                         { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:stopAtTargetLevel" }
Number:Dimensionless LoungeAHTarget            "Lounge Air Humidifier Target Humidity"                        { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:configTargetHumidity" }
Number:Dimensionless LoungeAHMistLevel         "Lounge Air Humidifier Mist Level"                             { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:mistLevel" }
```

#### Air Humidifier Classic 300S Model

```
Switch               LoungeAHPower             "Lounge Air Humidifier Power"                                  { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:enabled" }
Switch               LoungeAHDisplay           "Lounge Air Humidifier Display"                                { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:display" }
String               LoungeAHNightLightMode    "Lounge Air Humidifier Night Light Mode"                       { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:nightLightMode }
String               LoungeAHMode              "Lounge Air Humidifier Mode"                                   { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:humidifierMode" }
Switch               LoungeAHWaterLacking      "Lounge Air Humidifier Water Lacking"                          { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:waterLacking" }
Switch               LoungeAHHighHumidity      "Lounge Air Humidifier High Humidity"                          { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:humidityHigh" }
Switch               LoungeAHWaterTankRemoved  "Lounge Air Humidifier Water Tank Removed"                     { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:waterTankLifted" }
Number:Dimensionless LoungeAHHumidity          "Lounge Air Humidifier Measured Humidity"                      { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:humidity" }
Switch               LoungeAHTargetStop        "Lounge Air Humidifier Stop at target"                         { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:stopAtTargetLevel" }
Number:Dimensionless LoungeAHTarget            "Lounge Air Humidifier Target Humidity"                        { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:configTargetHumidity" }
Number:Dimensionless LoungeAHMistLevel         "Lounge Air Humidifier Mist Level"                             { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:mistLevel" }
```

#### Air Humidifier 600S Model

```
Switch               LoungeAHPower             "Lounge Air Humidifier Power"                                  { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:enabled" }
Switch               LoungeAHDisplay           "Lounge Air Humidifier Display"                                { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:display" }
String               LoungeAHMode              "Lounge Air Humidifier Mode"                                   { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:humidifierMode" }
Switch               LoungeAHWaterLacking      "Lounge Air Humidifier Water Lacking"                          { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:waterLacking" }
Switch               LoungeAHHighHumidity      "Lounge Air Humidifier High Humidity"                          { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:humidityHigh" }
Switch               LoungeAHWaterTankRemoved  "Lounge Air Humidifier Water Tank Removed"                     { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:waterTankLifted" }
Number:Dimensionless LoungeAHHumidity          "Lounge Air Humidifier Measured Humidity"                      { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:humidity" }
Switch               LoungeAHTargetStop        "Lounge Air Humidifier Stop at target"                         { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:stopAtTargetLevel" }
Number:Dimensionless LoungeAHTarget            "Lounge Air Humidifier Target Humidity"                        { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:configTargetHumidity" }
Number:Dimensionless LoungeAHMistLevel         "Lounge Air Humidifier Mist Level"                             { channel="vesync:AirHumidifier:vesyncServers:loungeHumidifier:mistLevel" }
```

### Configuration (*.sitemap)

#### Air Purifier Core 400S / 600S Model

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

#### Air Purifier Core 200S/300S Model

```
Frame {
   Switch item=LoungeAPPower label="Power"
   Text   item=LoungeAPFilterRemainingUse label="Filter Remaining"
   Switch item=LoungeAPDisplay label="Display"
   Switch item=LoungeAPNightLightMode label="Night Light Mode" mappings=[on="On", dim="Dimmed", off="Off"] icon="settings"
   Text   item=LoungeAPAirQuality label="Air Quality [%.0f]"                
   Switch item=LoungeAPControlsLock label="Controls Locked"
   Text   item=LoungeAPTimerExpiry label="Timer Shutdown @" icon="clock"
   Switch item=LoungeAPMode label="Mode" mappings=[manual="Manual Fan Control", sleep="Sleeping"] icon="settings"
   Text   item=LoungeAPErrorCode label="Error Code [%.0f]"
   Switch item=LoungeAPManualFanSpeed label="Manual Fan Speed [%.0f]" mappings=[1="1", 2="2", 3="3"] icon="settings"                               
}
```

#### Air Humidifier Classic 200S / Dual 200S Model

```
Frame {
   Switch item=LoungeAHPower
   Switch item=LoungeAHDisplay
   Switch item=LoungeAHMode label="Mode" mappings=[auto="Auto", sleep="Sleeping"] icon="settings"
   Text   icon="none" item=LoungeAHWaterLacking
   Text   icon="none" item=LoungeAHHighHumidity
   Text   icon="none" item=LoungeAHWaterTankRemoved
   Text   icon="none" item=LoungeAHHumidity
   Switch item=LoungeAHTargetStop
   Slider item=LoungeAHTarget minValue=30 maxValue=80
   Slider item=LoungeAHMistLevel minValue=1 maxValue=3
}
```

#### Air Humidifier Classic 300S Model

```
Frame {
   Switch item=LoungeAHPower
   Switch item=LoungeAHDisplay
   Switch item=LoungeAHNightLightMode label="Night Light Mode" mappings=[on="On", dim="Dimmed", off="Off"] icon="settings"
   Switch item=LoungeAHMode label="Mode" mappings=[auto="Auto", sleep="Sleeping"] icon="settings"
   Text   icon="none" item=LoungeAHWaterLacking
   Text   icon="none" item=LoungeAHHighHumidity
   Text   icon="none" item=LoungeAHWaterTankRemoved
   Text   icon="none" item=LoungeAHHumidity
   Switch item=LoungeAHTargetStop
   Slider item=LoungeAHTarget minValue=30 maxValue=80
   Slider item=LoungeAHMistLevel minValue=1 maxValue=3
}
```

#### Air Humidifier 600S Model

```
Frame {
   Switch item=LoungeAHPower
   Switch item=LoungeAHDisplay
   Switch item=LoungeAHMode label="Mode" mappings=[auto="Auto", sleep="Sleeping"] icon="settings"
   Text   icon="none" item=LoungeAHWaterLacking
   Text   icon="none" item=LoungeAHHighHumidity
   Text   icon="none" item=LoungeAHWaterTankRemoved
   Text   icon="none" item=LoungeAHHumidity
   Switch item=LoungeAHTargetStop
   Slider item=LoungeAHTarget minValue=30 maxValue=80
   Slider item=LoungeAHMistLevel minValue=1 maxValue=3
}
```
