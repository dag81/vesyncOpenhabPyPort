# VeSync Binding

This binding is based on the excellent work from https://github.com/webdjoe/pyvesync.

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

_Describe what is needed to manually configure a thing, either through the UI or via a thing-file. This should be mainly about its mandatory and optional configuration parameters. A short example entry for a thing file can help!_

_Note that it is planned to generate some part of this based on the XML files within ```src/main/resources/OH-INF/thing``` of your binding._

## Channels

Channel names in **bold** are read/write, everything else is read-only

### AirPurifier Thing

| Channel                | Type     | Description                                               |
|------------------------|----------|-----------------------------------------------------------|
| **enabled**            | Switch   | Whether the hardware device is enabled (Switched on)      |
| **child-lock**         | Switch   | Whether the child lock (display lock is enabled)          |
| **display**            | Switch   | Whether the display is enabled (display is shown)         |
| **fan-mode**           | String   | The operation mode of the fan                             |
| **manual-fan-speed**   | String   | The operation mode of the fan                             |
| **night-light-mode**   | String   | The night lights mode                                     |
| filter-life-percentage | Number   | The remaining filter life as a percentage                 |
| air-quality            | Number   | The air quality as represented by the Core200S / Core300S |
| air-quality-ppm25      | Number   | The air quality as represented by the Core400S            |
| error-code             | Number   | The error code reported by the device                     |
| timer-remain           | Number   | The seconds left on the timer at the last poll            |
| timer-expiry           | DateTime | The expected expiry time of the current timer             |
| schedules-count        | Number   | The number schedules configured                           |

## Full Example

_Provide a full usage example based on textual configuration files (*.things, *.items, *.sitemap)._

## Any custom content here!

_Feel free to add additional sections for whatever you think should also be mentioned about your binding!_
