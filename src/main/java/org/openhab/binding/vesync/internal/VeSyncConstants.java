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
package org.openhab.binding.vesync.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link VeSyncConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class VeSyncConstants {

    public static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting()
            .disableHtmlEscaping().serializeNulls().create();

    private static final String BINDING_ID = "vesync";

    public static final long DEFAULT_REFRESH_INTERVAL_DISCOVERED_DEVICES = 3600;
    public static final long DEFAULT_POLL_INTERVAL_AIR_FILTERS_DEVICES = 10;

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_AIR_PURIFIER = new ThingTypeUID(BINDING_ID, "AirPurifier");
    public static final ThingTypeUID THING_TYPE_AIR_HUMIDIFIER = new ThingTypeUID(BINDING_ID, "AirHumidifier");

    // Thing configuration properties
    public static final String DEVICE_MAC_ID = "macAddress";

    public static final String EMPTY_STRING = "";

    // Base Device Channel Names
    public static final String DEVICE_CHANNEL_ENABLED = "enabled";
    public static final String DEVICE_CHANNEL_DISPLAY_ENABLED = "display";
    public static final String DEVICE_CHANNEL_CHILD_LOCK_ENABLED = "child-lock";
    public static final String DEVICE_CHANNEL_AIR_FILTER_LIFE_PERCENTAGE_REMAINING = "filter-life-percentage";
    public static final String DEVICE_CHANNEL_FAN_MODE_ENABLED = "fan-mode";
    public static final String DEVICE_CHANNEL_FAN_SPEED_ENABLED = "manual-fan-speed";
    public static final String DEVICE_CHANNEL_ERROR_CODE = "error-code";
    public static final String DEVICE_CHANNEL_AIRQUALITY_BASIC = "air-quality";
    public static final String DEVICE_CHANNEL_AIRQUALITY_PPM25 = "air-quality-ppm25";

    public static final String DEVICE_CHANNEL_AF_CONFIG_DISPLAY_FOREVER = "config-display-forever";
    public static final String DEVICE_CHANNEL_AF_CONFIG_AUTO_MODE_PREF = "config-auto-mode";

    public static final String DEVICE_CHANNEL_AF_AUTO_OFF_CALC_TIME = "timer-expiry";
    public static final String DEVICE_CHANNEL_AF_CONFIG_AUTO_ROOM_SIZE = "config-auto-room-size";
    public static final String DEVICE_CHANNEL_AF_SCHEDULES_COUNT = "schedules-count";
    public static final String DEVICE_CHANNEL_AF_NIGHT_LIGHT = "night-light-mode";

    // Humidity related channels
    public static final String DEVICE_CHANNEL_WATER_LACKS = "water-lacking";
    public static final String DEVICE_CHANNEL_HUMIDITY_HIGH = "humidity-high";
    public static final String DEVICE_CHANNEL_WATER_TANK_LIFTED = "water-tank-lifted";
    public static final String DEVICE_CHANNEL_STOP_AT_TARGET = "stop-at-target-level";
    public static final String DEVICE_CHANNEL_HUMIDITY = "humidity";
    public static final String DEVICE_CHANNEL_MIST_LEVEL = "mist-level";
    public static final String DEVICE_CHANNEL_HUMIDIFIER_MODE = "humidifier-mode";
    public static final String DEVICE_CHANNEL_WARM_ENABLED = "warm-enabled";
    public static final String DEVICE_CHANNEL_WARM_LEVEL = "warm-level";

    public static final String DEVICE_CHANNEL_CONFIG_TARGET_HUMIDITY = "config-target-humidity";

    // Property name constants
    public static final String DEVICE_PROP_DEVICE_NAME = "Device Name";
    public static final String DEVICE_PROP_DEVICE_TYPE = "Device Type";
    public static final String DEVICE_PROP_DEVICE_MAC_ID = "MAC Id";
    public static final String DEVICE_PROP_DEVICE_UUID = "UUID";

    // Property name for config constants
    public static final String DEVICE_PROP_CONFIG_DEVICE_NAME = "deviceName";
    public static final String DEVICE_PROP_CONFIG_DEVICE_MAC = "macId";

    // Bridge name constants
    public static final String DEVICE_PROP_BRIDGE_REG_TS = "Registration Time";
    public static final String DEVICE_PROP_BRIDGE_COUNTRY_CODE = "Country Code";
    public static final String DEVICE_PROP_BRIDGE_ACCEPT_LANG = "Accept Language";
}
