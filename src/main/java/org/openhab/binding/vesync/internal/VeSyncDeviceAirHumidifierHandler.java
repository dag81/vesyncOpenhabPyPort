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
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
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
        final String deviceType = getThing().getProperties().get(DEVICE_PROP_DEVICE_TYPE);
        if (deviceType == null)
            return;

        scheduler.submit(() -> {

            if (command instanceof OnOffType) {
                switch (channelUID.getId()) {
                    case DEVICE_CHANNEL_ENABLED:
                        sendV2BypassControlCommand("setSwitch", new VesyncRequestManagedDeviceBypassV2.SetSwitchPayload(
                                command.equals(OnOffType.ON), 0));
                        break;
                    case DEVICE_CHANNEL_AF_CONFIG_DISPLAY:
                        sendV2BypassControlCommand("setDisplay",
                                new VesyncRequestManagedDeviceBypassV2.SetState(command.equals(OnOffType.ON)));
                        break;
                    case DEVICE_CHANNEL_STOP_AT_TARGET:
                        sendV2BypassControlCommand("setAutomaticStop",
                                new VesyncRequestManagedDeviceBypassV2.EnabledPayload(command.equals(OnOffType.ON)));
                        break;
                }
            } else if (command instanceof QuantityType) {
                switch (channelUID.getId()) {
                    case DEVICE_CHANNEL_NIGHT_LIGHT_LEVEL:
                        int nightLightLevel = ((QuantityType<?>) command).intValue();
                        if (nightLightLevel < 0) {
                            logger.warn("Night light level less than 0 - adjusting to 0 as the valid API value");
                            nightLightLevel = 0;
                        } else if (nightLightLevel > 100) {
                            logger.warn("Night light level greater than 100 - adjusting to 100 as the valid API value");
                            nightLightLevel = 0;
                        }
                        sendV2BypassControlCommand("setNightLightBrightness",
                                new VesyncRequestManagedDeviceBypassV2.SetNightLightBrightness(nightLightLevel));
                        break;
                    case DEVICE_CHANNEL_HUMIDITY:
                        int targetHumidity = ((QuantityType<?>) command).intValue();
                        if (targetHumidity < 30) {
                            logger.warn("Target Humidity less than 30 - adjusting to 30 as the valid API value");
                            targetHumidity = 30;
                        } else if (targetHumidity > 80) {
                            logger.warn("Target Humidity greater than 80 - adjusting to 80 as the valid API value");
                            targetHumidity = 80;
                        }
                        sendV2BypassControlCommand("setTargetHumidity",
                                new VesyncRequestManagedDeviceBypassV2.SetTargetHumidity(targetHumidity));
                        break;
                }
            } else if (command instanceof RefreshType) {
                pollForUpdate();
            } else {
                logger.trace("UNKNOWN COMMAND: {} {}", command.getClass().toString(), channelUID);
            }
        });
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
        updateState(DEVICE_CHANNEL_NIGHT_LIGHT_LEVEL,
                new DecimalType(humidifierStatus.result.result.night_light_brightness));
    }

    private Object pollLock = new Object();
}
