//-----------------------------------------------------------------------------
// Copyright (C) 2014 Tormod Haugland and Inge Edward Haulsaunet
//
// This file is part of WS-Nu.
//
// WS-Nu is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// WS-Nu is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with WS-Nu. If not, see <http://www.gnu.org/licenses/>.
//-----------------------------------------------------------------------------

package org.ntnunotif.wsnu.base.internal;

import org.ntnunotif.wsnu.base.util.InternalMessage;

import java.io.OutputStream;
import java.util.Collection;

/**
 * Interface for a hub. Implementations of this interface should be able to receive net-messages, and local messages (from connected web services)
 * @author Tormod Haugland
 * Created by tormod on 3/3/14.
 */
public interface Hub {

    /**
     * Function to accept a message from the net.
     * @return Returns the message(s) that is going back
     */
    public InternalMessage acceptNetMessage(InternalMessage message, OutputStream streamToRequestor);

    /**
     * Function to accept a message from a local service, and forward it out into the internet.
     */
    public InternalMessage acceptLocalMessage(InternalMessage message);

    /**
     * Get the address this server is currently running on.
     * @return
     */
    public String getInetAdress();

    /**
     * Register's a service for usage with this hub object.
     * @param webServiceConnector
     */
    public void registerService(ServiceConnection webServiceConnector);

    /**
     * Removes a registered service for usage with this hub object.
     * @param webServiceConnector
     */
    public void removeService(ServiceConnection webServiceConnector);

    /**
     * Checks if the webServiceConnector is registered with the hub
     * @param webServiceConnector
     * @return
     */
    public boolean isServiceRegistered(ServiceConnection webServiceConnector);

    public Collection<ServiceConnection> getServices();
}
