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
package org.openhab.binding.vesync.internal.api.model;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public abstract class VesyncBasedDevice {

    /*
     * @return array of strings valid for the API
     * 
     * @author David Goodyear
     */
    public abstract String[] buildApiDictionary();

    /*
     * @return array of strings valid for the configuration of the device
     * 
     * @author David Goodyear
     */
    public abstract String[] buildConfigDictionary();
}
