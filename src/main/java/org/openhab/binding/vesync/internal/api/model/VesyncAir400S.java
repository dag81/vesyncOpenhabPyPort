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
public class VesyncAir400S extends VesyncPurifierBasedDevice {

    @Override
    public String[] buildApiDictionary() {
        return new String[0];
    }

    @Override
    public String[] buildConfigDictionary() {
        return new String[0];
    }

    @Override
    public String[] buildPurifierDictionary() {
        return new String[0];
    }

    @Override
    public int getMaxFanLevel() {
        return 4;
    }
}
