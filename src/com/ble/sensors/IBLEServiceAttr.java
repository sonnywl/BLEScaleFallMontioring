
package com.ble.sensors;

import java.util.Set;
import java.util.UUID;

public interface IBLEServiceAttr {
    public UUID getServiceUUID();

    public String getServicUUIDName();

    public UUID getDataUUID();

    public UUID getConfigUUID();
    
    public UUID getPeriodUUID();

    public Set<UUID> getTaskUUIDs();
}
