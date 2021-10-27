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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.vesync.internal.dto.requests.VesyncRequestManagedDeviceBypassV2;
import org.openhab.binding.vesync.internal.dto.responses.VesyncV2BypassPurifierStatus;
import org.openhab.core.library.items.DateTimeItem;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link VeSyncDeviceAirPurifierHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class VeSyncDeviceAirPurifierHandler extends VeSyncBaseDeviceHandler {
    // "Device Type" values
    public final static String DEV_TYPE_CORE_400S = "Core400S";
    public final static String DEV_TYPE_CORE_300S = "Core300S";
    public final static String DEV_TYPE_CORE_200S = "Core200S";
    public final static List<String> SUPPORTED_DEVICE_TYPES = Arrays.asList(DEV_TYPE_CORE_400S, DEV_TYPE_CORE_300S,
            DEV_TYPE_CORE_200S);

    private final static List<String> CORE_400S_FAN_MODES = Arrays.asList("auto", "manual", "sleep");
    private final static List<String> CORE_200S300S_FAN_MODES = Arrays.asList("manual", "sleep");
    private final static List<String> CORE_200S300S_NIGHT_LIGHT_MODES = Arrays.asList("on", "dim", "off");

    private final Logger logger = LoggerFactory.getLogger(VeSyncDeviceAirPurifierHandler.class);

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_AIR_PURIFIER);

    public VeSyncDeviceAirPurifierHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        super.initialize();
        customiseChannels();
    }

    @Override
    protected void customiseChannels() {
        List<Channel> channelsToBeRemoved = List.of();
        switch (getThing().getProperties().get(DEVICE_PROP_DEVICE_TYPE)) {
            case DEV_TYPE_CORE_400S:
                channelsToBeRemoved = this.findChannelById(DEVICE_CHANNEL_AF_NIGHT_LIGHT);
                break;
        }

        final ThingBuilder builder = editThing().withoutChannels(channelsToBeRemoved);
        updateThing(builder.build());
    }

    @Override
    public void updateBridgeBasedPolls(final VeSyncBridgeConfiguration config) {
        setBackgroundPollInterval(config.airPurifierPollInterval);
    }

    @Override
    public void dispose() {
        this.setBackgroundPollInterval(-1);
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
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
                    case DEVICE_CHANNEL_CHILD_LOCK_ENABLED:
                        sendV2BypassControlCommand("setChildLock",
                                new VesyncRequestManagedDeviceBypassV2.SetChildLock(command.equals(OnOffType.ON)));
                        break;
                }
            } else if (command instanceof StringType) {
                switch (channelUID.getId()) {
                    case DEVICE_CHANNEL_FAN_MODE_ENABLED:
                        switch (getThing().getProperties().get(DEVICE_PROP_DEVICE_TYPE)) {
                            case DEV_TYPE_CORE_400S:
                                if (!CORE_400S_FAN_MODES.contains(command.toString())) {
                                    logger.warn("Fan mode command for \"{}\" is not valid in the (Core400S) API",
                                            command.toString());
                                    return;
                                }
                                break;
                            case DEV_TYPE_CORE_200S:
                            case DEV_TYPE_CORE_300S:
                                if (!CORE_200S300S_FAN_MODES.contains(command.toString())) {
                                    logger.warn(
                                            "Fan mode command for \"{}\" is not valid in the (Core200S/Core300S) API",
                                            command.toString());
                                    return;
                                }
                                break;
                        }

                        sendV2BypassControlCommand("setPurifierMode",
                                new VesyncRequestManagedDeviceBypassV2.SetMode(command.toString()));
                        break;
                    case DEVICE_CHANNEL_AF_NIGHT_LIGHT:
                        switch (getThing().getProperties().get(DEVICE_PROP_DEVICE_TYPE)) {
                            case DEV_TYPE_CORE_400S:
                                logger.warn("Core400S API does not support night light");
                                return;
                            case DEV_TYPE_CORE_200S:
                            case DEV_TYPE_CORE_300S:
                                if (!CORE_200S300S_NIGHT_LIGHT_MODES.contains(command.toString())) {
                                    logger.warn(
                                            "Night light mode command for \"{}\" is not valid in the (Core200S/Core300S) API",
                                            command.toString());
                                    return;
                                }

                                sendV2BypassControlCommand("setNightLight",
                                        new VesyncRequestManagedDeviceBypassV2.SetNightLight(command.toString()));

                                break;
                        }
                        break;
                }
            } else if (command instanceof QuantityType) {
                switch (channelUID.getId()) {
                    case DEVICE_CHANNEL_FAN_SPEED_ENABLED:
                        // If the fan speed is being set enforce manual mode
                        logger.warn("Current fan mode is {}",
                                getThing().getChannel(DEVICE_CHANNEL_FAN_MODE_ENABLED).toString());
                        sendV2BypassControlCommand("setPurifierMode",
                                new VesyncRequestManagedDeviceBypassV2.SetMode("manual"), false);

                        int requestedLevel = ((QuantityType<?>) command).intValue();
                        if (requestedLevel < 1) {
                            logger.warn("Fan speed command less than 0 - adjusting to 0 as the valid API value");
                            requestedLevel = 1;
                        }

                        switch (getThing().getProperties().get(DEVICE_PROP_DEVICE_TYPE)) {
                            case DEV_TYPE_CORE_400S:
                                if (requestedLevel > 4) {
                                    logger.warn(
                                            "Fan speed command greater than 4 - adjusting to 4 as the valid (Core400S) API value");
                                    requestedLevel = 4;
                                }
                                break;
                            case DEV_TYPE_CORE_200S:
                            case DEV_TYPE_CORE_300S:
                                if (requestedLevel > 3) {
                                    logger.warn(
                                            "Fan speed command greater than 3 - adjusting to 3 as the valid (Core200S/Core300S) API value");
                                    requestedLevel = 3;
                                }
                                break;
                        }

                        sendV2BypassControlCommand("setLevel",
                                new VesyncRequestManagedDeviceBypassV2.SetLevelPayload(0, "wind", requestedLevel));
                        break;
                }
            } else if (command instanceof RefreshType) {
                logger.trace("COMMAND: Refresh Type {}", channelUID);
            } else {
                logger.trace("UNKNOWN COMMAND: {} {}", command.getClass().toString(), channelUID);
            }
        });
    }

    @Override
    protected void pollForDeviceData() {
        final String response = sendV2BypassCommand("getPurifierStatus",
                new VesyncRequestManagedDeviceBypassV2.EmptyPayload());

        if (response.equals(EMPTY_STRING))
            return;

        final VesyncV2BypassPurifierStatus purifierStatus = VeSyncConstants.GSON.fromJson(response,
                VesyncV2BypassPurifierStatus.class);

        if (purifierStatus == null)
            return;

        // Bail and update the status of the thing - it will be updated to online by the next search
        // that detects it is online.
        if (purifierStatus.isMsgDeviceOffline()) {
            updateStatus(ThingStatus.OFFLINE);
            return;
        } else if (purifierStatus.isMsgSuccess()) {
            updateStatus(ThingStatus.ONLINE);
        }

        updateState(DEVICE_CHANNEL_ENABLED, OnOffType.from(purifierStatus.result.result.enabled));
        updateState(DEVICE_CHANNEL_CHILD_LOCK_ENABLED, OnOffType.from(purifierStatus.result.result.childLock));
        updateState(DEVICE_CHANNEL_DISPLAY_ENABLED, OnOffType.from(purifierStatus.result.result.display));
        updateState(DEVICE_CHANNEL_AIR_FILTER_LIFE_PERCENTAGE_ENABLED,
                new DecimalType(purifierStatus.result.result.filterLife));
        updateState(DEVICE_CHANNEL_FAN_MODE_ENABLED, new StringType(purifierStatus.result.result.mode));
        updateState(DEVICE_CHANNEL_FAN_SPEED_ENABLED, new DecimalType(purifierStatus.result.result.level));
        updateState(DEVICE_CHANNEL_ERROR_CODE, new DecimalType(purifierStatus.result.result.deviceErrorCode));
        updateState(DEVICE_CHANNEL_AIRQUALITY_BASIC, new DecimalType(purifierStatus.result.result.airQuality));
        updateState(DEVICE_CHANNEL_AIRQUALITY_PPM25, new DecimalType(purifierStatus.result.result.airQualityValue));

        updateState(DEVICE_CHANNEL_AF_CONFIG_DISPLAY,
                OnOffType.from(purifierStatus.result.result.configuration.display));
        updateState(DEVICE_CHANNEL_AF_CONFIG_DISPLAY_FOREVER,
                OnOffType.from(purifierStatus.result.result.configuration.displayForever));

        updateState(DEVICE_CHANNEL_AF_CONFIG_AUTO_MODE_PREF,
                new StringType(purifierStatus.result.result.configuration.autoPreference.autoType));

        updateState(DEVICE_CHANNEL_AF_AUTO_OFF_SECONDS,
                new DecimalType(purifierStatus.result.result.extension.timerRemain));

        if (purifierStatus.result.result.extension.timerRemain > 0) {
            updateState(DEVICE_CHANNEL_AF_AUTO_OFF_CALC_TIME, new DateTimeType(LocalDateTime.now()
                    .plus(purifierStatus.result.result.extension.timerRemain, ChronoUnit.SECONDS).toString()));
        } else {
            updateState(DEVICE_CHANNEL_AF_AUTO_OFF_CALC_TIME, new DateTimeItem("nullEnforcements").getState());
        }
        updateState(DEVICE_CHANNEL_AF_CONFIG_AUTO_ROOM_SIZE,
                new DecimalType(purifierStatus.result.result.configuration.autoPreference.roomSize));

        updateState(DEVICE_CHANNEL_AF_SCHEDULES_COUNT,
                new DecimalType(purifierStatus.result.result.extension.scheduleCount));

        // Not applicable to 400S payload's
        if (purifierStatus.result.result.nightLight != null) {
            updateState(DEVICE_CHANNEL_AF_NIGHT_LIGHT, new DecimalType(purifierStatus.result.result.nightLight));
        }
    }
}
