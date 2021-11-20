/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.vesync.internal.dto.requests;

/**
 * The {@link VeSyncProtocolConstants} contains common Strings used by various elements of the protocol.
 *
 * @author David Goodyear - Initial contribution
 */
public interface VeSyncProtocolConstants {

    // Common Payloads
    String MODE_AUTO = "auto";
    String MODE_MANUAL = "manual";
    String MODE_SLEEP = "sleep";

    String MODE_ON = "on";
    String MODE_DIM = "dim";
    String MODE_OFF = "off";

    // Common Commands
    String DEVICE_SET_SWITCH = "setSwitch";
    String DEVICE_SET_DISPLAY = "setDisplay";

    // Humidifier Commands
    String DEVICE_SET_AUTOMATIC_STOP = "setAutomaticStop";
    String DEVICE_SET_HUMIDITY_MODE = "setHumidityMode";
    String DEVICE_SET_TARGET_HUMIDITY_MODE = "setTargetHumidity";
    String DEVICE_SET_VIRTUAL_LEVEL = "setVirtualLevel";
    String DEVICE_SET_NIGHT_LIGHT_BRIGHTNESS = "setNightLightBrightness";
    
    String DEVICE_LEVEL_TYPE_MIST = "mist";
}
