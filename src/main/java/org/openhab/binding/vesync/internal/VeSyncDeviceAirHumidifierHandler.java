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
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
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
        updateState(DEVICE_CHANNEL_STOP_AT_TARGET,
                OnOffType.from(humidifierStatus.result.result.automatic_stop_reach_target));
        updateState(DEVICE_CHANNEL_HUMIDITY, new DecimalType(humidifierStatus.result.result.humidity));
        updateState(DEVICE_CHANNEL_MIST_LEVEL, new DecimalType(humidifierStatus.result.result.mist_level));
        updateState(DEVICE_CHANNEL_MIST_VIRTUAL_LEVEL,
                new DecimalType(humidifierStatus.result.result.mist_virtual_level));
        updateState(DEVICE_CHANNEL_HUMIDIFIER_MODE, new StringType(humidifierStatus.result.result.mode));
    }

    private Object pollLock = new Object();
}
