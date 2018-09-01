package io.bbqresearch.dsc.service;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.util.HashMap;
import java.util.List;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class DscGattAttributes {
    private final static String TAG = DscGattAttributes.class.getSimpleName();
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String GENERIC_ACCESS_UUID = "00001800-0000-1000-8000-00805f9b34fb";
    public static String GENERIC_APPEARANCE_UUID = "00002a01-0000-1000-8000-00805f9b34fb";
    public static String GENERIC_SERVICE_CHANGED_UUID = "00002a05-0000-1000-8000-00805f9b34fb";
    public static String GENERIC_DEVICE_NAME_UUID = "00002a00-0000-1000-8000-00805f9b34fb";
    public static String GENERIC_ATTRIBUTE_UUID = "00001801-0000-1000-8000-00805f9b34fb";

    public static String DSC_SERVICE_UUID = "deadbeef-0011-1001-1100-00000fffddd0";
    public static String DSC_SETTINGS_UUID = "deadbeef-0011-1001-1100-00000fffddd1";
    public static String DSC_STATUS_UUID = "deadbeef-0011-1001-1100-00000fffddd2";
    public static String DSC_MSG_INBOUND_UUID = "deadbeef-0011-1001-1100-00000fffddda";
    public static String DSC_MSG_OUTBOUND = "deadbeef-0011-1001-1100-00000fffdddb";
    public static String DSC_DATETIME_UUID = "deadbeef-0011-1001-1100-00000fffdddc";

    private static HashMap<String, String> attributes = new HashMap();

    static {
        attributes.put(DSC_SERVICE_UUID, "DSC Service");
        attributes.put(DSC_SETTINGS_UUID, "DSC Settings Attribute");
        attributes.put(DSC_MSG_INBOUND_UUID, "DSC Inbound Message Attribute");
        attributes.put(DSC_DATETIME_UUID, "DSC DateTime Synchronize Attribute");
        attributes.put(GENERIC_ACCESS_UUID, "GAP Generic Access");
        attributes.put(GENERIC_APPEARANCE_UUID, "GAP Generic Appearance");
        attributes.put(GENERIC_SERVICE_CHANGED_UUID, "GAP Generic Service Changed");
        attributes.put(GENERIC_DEVICE_NAME_UUID, "GAP Device Name");
        attributes.put(GENERIC_ATTRIBUTE_UUID, "GAP Generic Attribute");
    }

    public static boolean checkAllReqAttributesAvail(List<String> uuids) {
        int req_count = 9;
        int count = 0;

        for (String uuid : uuids) {
            if (attributes.containsKey(uuid)) count += 1;
        }
        return count == req_count;
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
