// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.bacnet;

import org.ampii.xd.application.Application;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.data.basetypes.ObjectIdentifierData;
import org.ampii.xd.database.Session;
import org.ampii.xd.marshallers.DataParser;
import org.ampii.xd.bindings.Binding;


import java.io.File;

/**
 * This is NOT a real "BACnet Manager" for external BACnet protocol data. It's just a stub for simulation or testing
 * purposes that just initializes itself from config file data. But at least it does create one "Device" to have
 * something at /.bacnet/.local/.this
 * <p>
 * Connections to live data under the /.bacnet tree can be done with a {@link Binding}.
 *
 * @author daverobin
 */
public class BACnetManager {

    public static void initializeSystemDeviceFromFile(File file) throws XDException {

        Data device = DataParser.parse(file);

        // fix up placeholder wildcards to match our system device instance

        device.setName(String.valueOf(Application.thisDeviceInstance)); // device node becomes ".bacnet/.local/<instance>"

        Data deviceObject = device.get("device,4194303"); // get the Object
        deviceObject.setName("device," + Application.thisDeviceInstance); // becomes ".bacnet/.local/<instance>/device,<instance>"

        Data objectIdentifier = deviceObject.get("object-identifier"); // get the Object_Identifier property
        objectIdentifier.setValue("device," + Application.thisDeviceInstance); // set value to "device,<instance>"

        Data objectName = deviceObject.get("object-name"); // get the Object_Name property and replace "...4194303..." with real instance
        objectName.setValue(objectName.stringValue().replace("4194303", String.valueOf(Application.thisDeviceInstance)));

        Data softwareVersion = deviceObject.get("application-software-version"); // get the Application_Software_Version property
        softwareVersion.setValue(Application.version);

        Data objectList = deviceObject.get("object-list"); // initialize in the Object_List property
        objectList.post(new ObjectIdentifierData("", "device," + Application.thisDeviceInstance));

        Session.atomicPost("BACnetManager.init", ".../.bacnet/.local", device);

    }

}
