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

import static org.openhab.binding.vesync.internal.VeSyncConstants.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.vesync.internal.dto.requests.VesyncRequestManagedDeviceBypassV2;
import org.openhab.binding.vesync.internal.dto.responses.VesyncV2BypassHumidifierStatus;
import org.openhab.core.cache.ExpiringCache;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link VeSyncDeviceAirHumidifierHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class VeSyncDeviceAirHumidifierHandler extends VeSyncBaseDeviceHandler {

    public final static int DEFAULT_AIR_PURIFIER_POLL_RATE = 120;
    // "Device Type" values
    public final static String DEV_TYPE_CLASSIC_300S = "Classic300S";

    public final static List<String> SUPPORTED_DEVICE_TYPES = Arrays.asList(DEV_TYPE_CLASSIC_300S);

    private final Logger logger = LoggerFactory.getLogger(VeSyncDeviceAirHumidifierHandler.class);

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_AIR_HUMIDIFIER);

    public VeSyncDeviceAirHumidifierHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        super.initialize();
        customiseChannels();
    }

    @Override
    public void updateBridgeBasedPolls(final VeSyncBridgeConfiguration config) {
        Integer pollRate = config.airPurifierPollInterval;
        if (pollRate == null)
            pollRate = Integer.valueOf(DEFAULT_AIR_PURIFIER_POLL_RATE);

        if (ThingStatus.OFFLINE.equals(getThing().getStatus())) {
            setBackgroundPollInterval(-1);
        } else {
            setBackgroundPollInterval(pollRate);
        }
    }

    @Override
    public void dispose() {
        this.setBackgroundPollInterval(-1);
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
    }

    @Override
    protected void pollForDeviceData(final ExpiringCache<String> cachedResponse) {

        String response;
        VesyncV2BypassHumidifierStatus humidifierStatus;
        synchronized (pollLock) {

            response = cachedResponse.getValue();
            boolean cachedDataUsed = response != null;
            if (response == null) {
                logger.trace("Requesting fresh response");
                response = sendV2BypassCommand("getHumidifierStatus",
                        new VesyncRequestManagedDeviceBypassV2.EmptyPayload());
            } else {
                logger.trace("Using cached response {}", response);
            }

            if (response.equals(EMPTY_STRING))
                return;

            humidifierStatus = VeSyncConstants.GSON.fromJson(response, VesyncV2BypassHumidifierStatus.class);

            if (humidifierStatus == null)
                return;

            if (!cachedDataUsed) {
                cachedResponse.putValue(response);
            }
        }

        // Bail and update the status of the thing - it will be updated to online by the next search
        // that detects it is online.
        if (humidifierStatus.isMsgDeviceOffline()) {
            updateStatus(ThingStatus.OFFLINE);
            return;
        } else if (humidifierStatus.isMsgSuccess()) {
            updateStatus(ThingStatus.ONLINE);
        }

        updateState(DEVICE_CHANNEL_ENABLED, OnOffType.from(humidifierStatus.result.result.enabled));
        updateState(DEVICE_CHANNEL_DISPLAY_ENABLED, OnOffType.from(humidifierStatus.result.result.display));
        updateState(DEVICE_CHANNEL_WATER_LACKS, OnOffType.from(humidifierStatus.result.result.water_lacks));
        updateState(DEVICE_CHANNEL_HUMIDITY_HIGH, OnOffType.from(humidifierStatus.result.result.humidityHigh));
        updateState(DEVICE_CHANNEL_WATER_TANK_LIFTED, OnOffType.from(humidifierStatus.result.result.water_tank_lifted));
        updateState(DEVICE_CHANNEL_STOP_AT_TARGET, OnOffType.from(humidifierStatus.result.result.automatic_stop_reach_target));
        /*
         * updateState(DEVICE_CHANNEL_CHILD_LOCK_ENABLED, OnOffType.from(purifierStatus.result.result.childLock));
         * updateState(DEVICE_CHANNEL_DISPLAY_ENABLED, OnOffType.from(purifierStatus.result.result.display));
         * updateState(DEVICE_CHANNEL_AIR_FILTER_LIFE_PERCENTAGE_ENABLED,
         * new DecimalType(purifierStatus.result.result.filterLife));
         * updateState(DEVICE_CHANNEL_FAN_MODE_ENABLED, new StringType(purifierStatus.result.result.mode));
         * updateState(DEVICE_CHANNEL_FAN_SPEED_ENABLED, new DecimalType(purifierStatus.result.result.level));
         * updateState(DEVICE_CHANNEL_ERROR_CODE, new DecimalType(purifierStatus.result.result.deviceErrorCode));
         * updateState(DEVICE_CHANNEL_AIRQUALITY_BASIC, new DecimalType(purifierStatus.result.result.airQuality));
         * updateState(DEVICE_CHANNEL_AIRQUALITY_PPM25, new DecimalType(purifierStatus.result.result.airQualityValue));
         * 
         * updateState(DEVICE_CHANNEL_AF_CONFIG_DISPLAY,
         * OnOffType.from(purifierStatus.result.result.configuration.display));
         * updateState(DEVICE_CHANNEL_AF_CONFIG_DISPLAY_FOREVER,
         * OnOffType.from(purifierStatus.result.result.configuration.displayForever));
         * 
         * updateState(DEVICE_CHANNEL_AF_CONFIG_AUTO_MODE_PREF,
         * new StringType(purifierStatus.result.result.configuration.autoPreference.autoType));
         * 
         * updateState(DEVICE_CHANNEL_AF_AUTO_OFF_SECONDS,
         * new DecimalType(purifierStatus.result.result.extension.timerRemain));
         * 
         * if (purifierStatus.result.result.extension.timerRemain > 0) {
         * updateState(DEVICE_CHANNEL_AF_AUTO_OFF_CALC_TIME, new DateTimeType(LocalDateTime.now()
         * .plus(purifierStatus.result.result.extension.timerRemain, ChronoUnit.SECONDS).toString()));
         * } else {
         * updateState(DEVICE_CHANNEL_AF_AUTO_OFF_CALC_TIME, new DateTimeItem("nullEnforcements").getState());
         * }
         * updateState(DEVICE_CHANNEL_AF_CONFIG_AUTO_ROOM_SIZE,
         * new DecimalType(purifierStatus.result.result.configuration.autoPreference.roomSize));
         * 
         * updateState(DEVICE_CHANNEL_AF_SCHEDULES_COUNT,
         * new DecimalType(purifierStatus.result.result.extension.scheduleCount));
         * 
         * // Not applicable to 400S payload's
         * if (purifierStatus.result.result.nightLight != null) {
         * updateState(DEVICE_CHANNEL_AF_NIGHT_LIGHT, new DecimalType(purifierStatus.result.result.nightLight));
         * }
         */
    }

    private Object pollLock = new Object();
}
