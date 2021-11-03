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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.vesync.internal.api.VesyncV2ApiHelper;
import org.openhab.binding.vesync.internal.discovery.DeviceMetaDataUpdatedHandler;
import org.openhab.binding.vesync.internal.discovery.VeSyncDiscoveryService;
import org.openhab.binding.vesync.internal.dto.requests.VesyncAuthenticatedRequest;
import org.openhab.binding.vesync.internal.dto.responses.VesyncLoginResponse;
import org.openhab.binding.vesync.internal.dto.responses.VesyncManagedDevicesPage;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link VeSyncBridgeHandler} is responsible for handling the bridge things created to use the VeSync
 * API. This way, the user credentials may be entered only once.
 *
 * @author David Goodyear - Initial Contribution
 */
@NonNullByDefault
public class VeSyncBridgeHandler extends BaseBridgeHandler implements VeSyncClient {

    private static final int DEFAULT_DEVICE_SCAN_INTERVAL = 600;
    private static final int DEFAULT_DEVICE_SCAN_RECOVERY_INTERVAL = 60;
    private static final int DEFAULT_DEVICE_SCAN_DISABLED = -1;

    private final Logger logger = LoggerFactory.getLogger(VeSyncBridgeHandler.class);

    private @Nullable ScheduledFuture<?> backgroundDiscoveryPollingJob;

    protected final @NotNull VesyncV2ApiHelper api;

    public ThingUID getUID() {
        return thing.getUID();
    }

    public VeSyncBridgeHandler(Bridge bridge, VesyncV2ApiHelper api) {
        super(bridge);
        this.api = api;
    }

    private volatile int backgroundScanTime = -1;
    private Object scanConfigLock = new Object();

    protected void checkIfIncreaseScanRateRequired() {
        logger.trace("Checking if increased background scanning for new devices / base information is required");
        boolean frequentScanReq = false;
        for (Thing th : getThing().getThings()) {
            ThingHandler handler = th.getHandler();
            if (handler instanceof VeSyncBaseDeviceHandler) {
                if (((VeSyncBaseDeviceHandler) handler).requiresMetaDataFrequentUpdates()) {
                    frequentScanReq = true;
                    break;
                }
            }
        }

        if (!frequentScanReq
                && api.getMacLookupMap().values().stream().anyMatch(x -> "offline".equals(x.connectionStatus))) {
            frequentScanReq = true;
        }

        if (frequentScanReq) {
            setBackgroundScanInterval(DEFAULT_DEVICE_SCAN_RECOVERY_INTERVAL);
        } else {
            setBackgroundScanInterval(DEFAULT_DEVICE_SCAN_INTERVAL);
        }
    }

    protected void setBackgroundScanInterval(final int seconds) {
        synchronized (scanConfigLock) {
            ScheduledFuture<?> job = backgroundDiscoveryPollingJob;
            if (backgroundScanTime != seconds) {
                if (seconds > 0) {
                    logger.trace("Scheduling background scanning for new devices / base information every {} seconds",
                            seconds);
                } else {
                    logger.trace("Disabling background scanning for new devices / base information");
                }
                // Cancel the current scan's and re-schedule as required
                if (job != null && !job.isCancelled()) {
                    job.cancel(true);
                    backgroundDiscoveryPollingJob = null;
                }
                if (seconds > 0) {
                    backgroundDiscoveryPollingJob = scheduler.scheduleWithFixedDelay(() -> runDeviceScanSequence(),
                            seconds, seconds, TimeUnit.SECONDS);
                }
                backgroundScanTime = seconds;
            }
        }
    }

    public void registerMetaDataUpdatedHandler(DeviceMetaDataUpdatedHandler dmduh) {
        handlers.add(dmduh);
    }

    public void unregisterMetaDataUpdatedHandler(DeviceMetaDataUpdatedHandler dmduh) {
        handlers.remove(dmduh);
    }

    private CopyOnWriteArrayList<DeviceMetaDataUpdatedHandler> handlers = new CopyOnWriteArrayList<>();

    public void runDeviceScanSequence() {
        logger.trace("Scanning for new devices / base information now");
        api.discoverDevices();
        handlers.forEach(x -> x.HandleMetadataRetrieved(this));
        checkIfIncreaseScanRateRequired();

        this.updateThings();
    }

    public java.util.stream.Stream<VesyncManagedDevicesPage.Result.@NotNull VesyncManagedDeviceBase> getAirPurifiersMetadata() {
        return api.getMacLookupMap().values().stream()
                .filter(x -> VeSyncDeviceAirPurifierHandler.SUPPORTED_DEVICE_TYPES.contains(x.deviceType));
    }

    protected void updateThings() {
        final VeSyncBridgeConfiguration config = getConfigAs(VeSyncBridgeConfiguration.class);
        for (Thing th : getThing().getThings()) {
            ThingHandler handler = th.getHandler();
            if (handler instanceof VeSyncBaseDeviceHandler) {
                ((VeSyncBaseDeviceHandler) handler).updateDeviceMetaData();
                ((VeSyncBaseDeviceHandler) handler).updateBridgeBasedPolls(config);
            }
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(VeSyncDiscoveryService.class);
    }

    @Override
    public void initialize() {
        VeSyncBridgeConfiguration config = getConfigAs(VeSyncBridgeConfiguration.class);

        scheduler.submit(() -> {
            final String passwordMd5 = VesyncV2ApiHelper.calculateMd5(config.password);

            try {
                api.login(config.username, passwordMd5, "Europe/London");
                updateStatus(ThingStatus.ONLINE);
                api.UpdateBridgeData(this);
                runDeviceScanSequence();
            } catch (final AuthenticationException ae) {
                updateStatus(ThingStatus.OFFLINE);
                setBackgroundScanInterval(DEFAULT_DEVICE_SCAN_DISABLED);
            }
        });
    }

    @Override
    public void dispose() {
        setBackgroundScanInterval(DEFAULT_DEVICE_SCAN_DISABLED);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.warn("Handling command for VeSync bridge handler.");
    }

    public void handleNewUserSession(final VesyncLoginResponse.@Nullable VesyncUserSession userSessionData) {
        final Map<String, String> newProps = new HashMap<>();
        if (userSessionData != null) {
            newProps.put(DEVICE_PROP_BRIDGE_REG_TS, userSessionData.registerTime);
            newProps.put(DEVICE_PROP_BRIDGE_COUNTRY_CODE, userSessionData.countryCode);
            newProps.put(DEVICE_PROP_BRIDGE_ACCEPT_LANG, userSessionData.acceptLanguage);
        }
        this.updateProperties(newProps);
    }

    public String reqV2Authorized(final String url, final String macId, final VesyncAuthenticatedRequest requestData)
            throws AuthenticationException, DeviceUnknownException {
        return api.reqV2Authorized(url, macId, requestData);
    }
}
