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
package org.openhab.binding.vesync.internal.handlers;

import static org.openhab.binding.vesync.internal.VeSyncConstants.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.vesync.internal.VeSyncBridgeConfiguration;
import org.openhab.binding.vesync.internal.VeSyncDeviceConfiguration;
import org.openhab.binding.vesync.internal.api.VesyncV2ApiHelper;
import org.openhab.binding.vesync.internal.dto.requests.VesyncAuthenticatedRequest;
import org.openhab.binding.vesync.internal.dto.requests.VesyncRequestManagedDeviceBypassV2;
import org.openhab.binding.vesync.internal.dto.responses.VesyncManagedDevicesPage;
import org.openhab.binding.vesync.internal.exceptions.AuthenticationException;
import org.openhab.binding.vesync.internal.exceptions.DeviceUnknownException;
import org.openhab.core.cache.ExpiringCache;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.BridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link VeSyncBaseDeviceHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public abstract class VeSyncBaseDeviceHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(VeSyncBaseDeviceHandler.class);

    private static final String MARKER_INVALID_DEVICE_KEY = "---INVALID---";

    private static final int CACHE_TIMEOUT_SECOND = 5;

    private int activePollRate = -2; // -1 is used to deactivate the poll, so default to a different value

    private @Nullable ScheduledFuture<?> backgroundPollingScheduler;
    private final Object pollConfigLock = new Object();

    protected @Nullable Channel findChannelById(final String channelGroupId) {
        // return getThing().getChannels().stream().anyMatch(x -> x.getUID().getId().equals(channelGroupId));
        return getThing().getChannel(channelGroupId);
    }

    protected ExpiringCache<String> lastPollResultCache = new ExpiringCache<>(Duration.ofSeconds(CACHE_TIMEOUT_SECOND),
            VeSyncBaseDeviceHandler::expireCacheContents);

    private static @Nullable String expireCacheContents() {
        return null;
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        super.channelLinked(channelUID);

        scheduler.execute(this::pollForUpdate);
    }

    protected void setBackgroundPollInterval(final int seconds) {
        if (activePollRate == seconds)
            return;

        logger.debug("Reconfiguring devices background polling to {} seconds", seconds);

        synchronized (pollConfigLock) {
            final ScheduledFuture<?> job = backgroundPollingScheduler;

            // Cancel the current scan's and re-schedule as required
            if (job != null && !job.isCancelled()) {
                job.cancel(true);
                backgroundPollingScheduler = null;
            }
            if (seconds > 0) {
                logger.trace("Device data is polling every {} seconds", seconds);
                backgroundPollingScheduler = scheduler.scheduleWithFixedDelay(this::pollForUpdate, seconds, seconds,
                        TimeUnit.SECONDS);
            }
            activePollRate = seconds;
        }
    }

    @NotNull
    protected String deviceLookupKey = MARKER_INVALID_DEVICE_KEY;

    public void configurationUpdated(Thing thing) {
        logger.debug("DETECTED CONFIG UPDATE FOR : {}", thing);
        // Get the new addressing lookup data
        deviceLookupKey = getValidatedIdString();
        initialize();
    }

    public boolean requiresMetaDataFrequentUpdates() {
        return (MARKER_INVALID_DEVICE_KEY.equals(deviceLookupKey));
    }

    private @Nullable BridgeHandler getBridgeHandler() {
        Bridge bridgeRef = getBridge();
        if (bridgeRef == null)
            return null;
        else {
            return bridgeRef.getHandler();
        }
    }

    protected boolean isDeviceOnline() {
        BridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null && bridgeHandler instanceof VeSyncBridgeHandler) {
            VeSyncBridgeHandler vesyncBridgeHandler = (VeSyncBridgeHandler) bridgeHandler;
            VesyncManagedDevicesPage.Result.@Nullable VesyncManagedDeviceBase metadata = vesyncBridgeHandler.api
                    .getMacLookupMap().get(deviceLookupKey);

            if (metadata == null)
                return false;

            return ("online".equals(metadata.connectionStatus));
        }
        return false;
    }

    public void updateDeviceMetaData() {

        Map<String, String> newProps = null;

        BridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null && bridgeHandler instanceof VeSyncBridgeHandler) {
            VeSyncBridgeHandler vesyncBridgeHandler = (VeSyncBridgeHandler) bridgeHandler;
            VesyncManagedDevicesPage.Result.@Nullable VesyncManagedDeviceBase metadata = vesyncBridgeHandler.api
                    .getMacLookupMap().get(deviceLookupKey);

            if (metadata == null)
                return;

            newProps = getMetadataProperities(metadata);

            // Refresh the device -> protocol mapping
            deviceLookupKey = getValidatedIdString();

            if ("online".equals(metadata.connectionStatus)) {
                updateStatus(ThingStatus.ONLINE);
            } else if ("offline".equals(metadata.connectionStatus)) {
                updateStatus(ThingStatus.OFFLINE);
            }
        }

        if (newProps != null && !newProps.isEmpty()) {
            this.updateProperties(newProps);
            removeChannels();
            if (!isDeviceSupported()) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_REGISTERING_ERROR,
                        "Device Model or Type not supported by this thing");
            }
        }
    }

    /**
     * Override this in classes that extend this, to
     */
    protected void customiseChannels() {
    }

    protected String[] getChannelsToRemove() {
        return new String[] {};
    }

    private void removeChannels() {
        final String[] channelsToRemove = getChannelsToRemove();
        final List<Channel> channelsToBeRemoved = new ArrayList<>();
        for (String name : channelsToRemove) {
            Channel ch = getThing().getChannel(name);
            if (ch != null)
                channelsToBeRemoved.add(ch);
        }

        final ThingBuilder builder = editThing().withoutChannels(channelsToBeRemoved);
        updateThing(builder.build());
    }

    /**
     * Extract the common properties for all devices, from the given meta-data of a device.
     * 
     * @param metadata - the meta-data of a device
     * @return - Map of common props
     */
    public Map<String, String> getMetadataProperities(
            final VesyncManagedDevicesPage.Result.@Nullable VesyncManagedDeviceBase metadata) {
        if (metadata == null) {
            return Map.of();
        }
        final Map<String, String> newProps = new HashMap<>(4);
        newProps.put(DEVICE_PROP_DEVICE_MAC_ID, metadata.getMacId());
        newProps.put(DEVICE_PROP_DEVICE_NAME, metadata.getDeviceName());
        newProps.put(DEVICE_PROP_DEVICE_TYPE, metadata.getDeviceType());
        // newProps.put(DEVICE_PROP_DEVICE_UUID, metadata.getUuid().replace("-", ""));
        newProps.put(DEVICE_PROP_DEVICE_UUID, metadata.getUuid());
        return newProps;
    }

    public VeSyncBaseDeviceHandler(Thing thing) {
        super(thing);
    }

    protected @Nullable VeSyncClient veSyncClient;

    protected synchronized @Nullable VeSyncClient getVeSyncClient() {
        if (veSyncClient == null) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                return null;
            }
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof VeSyncClient) {
                veSyncClient = (VeSyncClient) handler;
            } else {
                return null;
            }
        }
        return veSyncClient;
    }

    protected void requestBridgeFreqScanMetadataIfReq() {
        if (requiresMetaDataFrequentUpdates()) {
            BridgeHandler bridgeHandler = getBridgeHandler();
            if (bridgeHandler != null && bridgeHandler instanceof VeSyncBridgeHandler) {
                VeSyncBridgeHandler vesyncBridgeHandler = (VeSyncBridgeHandler) bridgeHandler;
                vesyncBridgeHandler.checkIfIncreaseScanRateRequired();
            }
        }
    }

    @NotNull
    public String getValidatedIdString() {
        final VeSyncDeviceConfiguration config = getConfigAs(VeSyncDeviceConfiguration.class);

        BridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null && bridgeHandler instanceof VeSyncBridgeHandler) {
            VeSyncBridgeHandler vesyncBridgeHandler = (VeSyncBridgeHandler) bridgeHandler;

            final String configMac = config.macId;

            // Try to use the mac directly
            if (configMac != null) {

                logger.debug("Searching for device mac id : {}", configMac);
                VesyncManagedDevicesPage.Result.@Nullable VesyncManagedDeviceBase metadata = vesyncBridgeHandler.api
                        .getMacLookupMap().get(configMac.toLowerCase());

                if (metadata != null && metadata.macId != null)
                    return metadata.macId;
            }

            final String deviceName = config.deviceName;

            // Check if the device name can be matched to a single device
            if (deviceName != null) {
                final String[] matchedMacIds = vesyncBridgeHandler.api.getMacLookupMap().values().stream()
                        .filter(x -> deviceName.equals(x.deviceName)).map(x -> x.macId).toArray(String[]::new); // .collect();//.toArray(String[]::new)

                for (String val : matchedMacIds) {
                    logger.debug("Found MAC match on name with : {}", val);
                }

                if (matchedMacIds.length != 1) {
                    return MARKER_INVALID_DEVICE_KEY;
                }

                if (vesyncBridgeHandler.api.getMacLookupMap().get(matchedMacIds[0]) != null) {
                    return matchedMacIds[0];
                }
            }
        }

        return MARKER_INVALID_DEVICE_KEY;
    }

    @Override
    public void initialize() {

        // Sanity check basic setup
        final VeSyncBridgeHandler bridge = (VeSyncBridgeHandler) getBridgeHandler();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED, "Missing bridge for API link");
            return;
        } else {
            updateStatus(ThingStatus.UNKNOWN);
        }

        deviceLookupKey = getValidatedIdString();

        // Populate device props - this is required for polling, to cross-check the device model.
        updateDeviceMetaData();

        // If the base device class marks it as offline there is an issue that will prevent normal operation
        if (getThing().getStatus().equals(ThingStatus.OFFLINE)) {
            return;
        }
        // This will force the bridge to push the configuration parameters for polling to the handler
        bridge.updateThing(this);

        // Give the bridge time to build the datamaps of the devices
        scheduler.schedule(this::pollForUpdate, 10, TimeUnit.SECONDS);
    }

    public void pollForUpdate() {
        pollForDeviceData(lastPollResultCache);
    }

    protected void pollForDeviceData(final ExpiringCache<String> cachedResponse) {
        // Each device should implement this to get the latest data that is not part of the meta data.
    }

    /**
     * Send a BypassV2 command to the device. The body of the response is returned, a poll is done if the request
     * should have been dispatched.
     * 
     * @param method - the V2 bypass method
     * @param payload - The payload to send in within the V2 bypass command
     * @return - The body of the response, or EMPTY_STRING if the command could not be issued.
     */
    protected final String sendV2BypassControlCommand(final String method,
            final VesyncRequestManagedDeviceBypassV2.EmptyPayload payload) {
        return sendV2BypassControlCommand(method, payload, true);
    }

    /**
     * Send a BypassV2 command to the device. The body of the response is returned.
     * 
     * @param method - the V2 bypass method
     * @param payload - The payload to send in within the V2 bypass command
     * @param readbackDevice - if set to true after the command has been issued, whether a poll of the devices data
     *            should be run.
     * @return - The body of the response, or EMPTY_STRING if the command could not be issued.
     */
    protected final String sendV2BypassControlCommand(final String method,
            final VesyncRequestManagedDeviceBypassV2.EmptyPayload payload, final boolean readbackDevice) {
        final String result = sendV2BypassCommand(method, payload);
        if (!result.equals(EMPTY_STRING) && readbackDevice)
            performReadbackPoll();
        return result;
    }

    public final String sendV1Command(final String method, final String url, final VesyncAuthenticatedRequest request) {
        if (ThingStatus.OFFLINE.equals(this.thing.getStatus())) {
            logger.debug("Command blocked as device is offline");
            return EMPTY_STRING;
        }

        try {
            if (MARKER_INVALID_DEVICE_KEY.equals(deviceLookupKey)) {
                deviceLookupKey = getValidatedIdString();
            }
            VeSyncClient client = getVeSyncClient();
            if (client != null)
                return client.reqV2Authorized(url, deviceLookupKey, request);
            else {
                throw new DeviceUnknownException("Missing client");
            }
        } catch (AuthenticationException e) {
            logger.debug("Auth exception {}", e.getMessage());
            return EMPTY_STRING;
        } catch (final DeviceUnknownException e) {
            logger.debug("Device unknown exception {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_REGISTERING_ERROR,
                    "Check configuration details");
            // In case the name is updated server side - request the scan rate is increased
            requestBridgeFreqScanMetadataIfReq();
            return EMPTY_STRING;
        }
    }

    /**
     * Send a BypassV2 command to the device. The body of the response is returned.
     * 
     * @param method - the V2 bypass method
     * @param payload - The payload to send in within the V2 bypass command
     * @return - The body of the response, or EMPTY_STRING if the command could not be issued.
     */
    protected final String sendV2BypassCommand(final String method,
            final VesyncRequestManagedDeviceBypassV2.EmptyPayload payload) {
        if (ThingStatus.OFFLINE.equals(this.thing.getStatus())) {
            logger.debug("Command blocked as device is offline");
            return EMPTY_STRING;
        }

        /*
         * if (deviceLookupKey == null) {
         * logger.debug("No key for addressing data");
         * return EMPTY_STRING;
         * }
         */

        VesyncRequestManagedDeviceBypassV2 readReq = new VesyncRequestManagedDeviceBypassV2();
        readReq.payload.method = method;
        readReq.payload.data = payload;

        try {
            if (MARKER_INVALID_DEVICE_KEY.equals(deviceLookupKey)) {
                deviceLookupKey = getValidatedIdString();
            }
            VeSyncClient client = getVeSyncClient();
            if (client != null)
                return client.reqV2Authorized(VesyncV2ApiHelper.BYPASS_V2_URL, deviceLookupKey, readReq);
            else {
                throw new DeviceUnknownException("Missing client");
            }
        } catch (AuthenticationException e) {
            logger.debug("Auth exception {}", e.getMessage());
            return EMPTY_STRING;
        } catch (final DeviceUnknownException e) {
            logger.debug("Device unknown exception {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_REGISTERING_ERROR,
                    "Check configuration details");
            // In case the name is updated server side - request the scan rate is increased
            requestBridgeFreqScanMetadataIfReq();
            return EMPTY_STRING;
        }
    }

    // Given several changes may be done at the same time, or in close proximity, delay the read-back to catch
    // multiple read-back's, so a single update can handle them.
    public void performReadbackPoll() {
        final long requestSystemMillis = System.currentTimeMillis();
        latestReadBackMillis = requestSystemMillis;
        scheduler.schedule(() -> {
            // This is a historical poll, ignore it
            if (requestSystemMillis != latestReadBackMillis) {
                logger.trace("Poll read-back cancelled, another later one is scheduled to happen");
                return;
            }
            logger.trace("Read-back poll executing");
            // Read-backs should never use the cached data - but may provide it for poll's that coincide with
            // the caches alive duration.
            lastPollResultCache.invalidateValue();
            pollForUpdate();
        }, 1L, TimeUnit.SECONDS);
    }

    private volatile long latestReadBackMillis = 0;

    public void updateBridgeBasedPolls(VeSyncBridgeConfiguration config) {
    }

    // Sub-classes should override this to return true, if the meta-data supports the device data read.
    protected boolean isDeviceSupported() {
        return false;
    }
}
