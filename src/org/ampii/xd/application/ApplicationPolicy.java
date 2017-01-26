/*
  Copyright (c) 2016 Dave Robin   All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE.
*/
/*
  This software was written as a contribution to the efforts of ASHRAE SSPC
  135 (The BACnet Committee) for the development of the Web Services portions
  of ANSI/ASHRAE Standard 135 (BACnet). Users of this software are encouraged
  to purchase the BACnet standard at www.ashrae.org. See README.txt for a
  description of this software and its intended capabilities and uses.
*/
package org.ampii.xd.application;

import org.ampii.xd.application.hooks.PolicyHooks;
import org.ampii.xd.data.Base;
import org.ampii.xd.data.Data;
import org.ampii.xd.database.DataStore;

public class ApplicationPolicy implements Policy {

    // This is the Policy that is used by AbstractData if not overridden by a Binding for data item-based extensions. See also DefaultBindingPolicy.
    // This policy can also be overridden by PolicyHooks for path-based policy extensions.

    // See comment in PolicyHooks about methods to make application-wide or instance-specific extensions to this default policy.

    static ApplicationPolicy thePolicy = new ApplicationPolicy();

    private ApplicationPolicy() {} // private constructor to ensure only one static copy

    public static Policy getPolicy() { return thePolicy; }

    public boolean allowCreate(Data host, String name, String type, Base base) {
        Boolean result = PolicyHooks.allowCreate(host, name, type, base); // ask the applilcation hook if this can be created
        if (result != null) return result;
        return DataStore.getPolicy().allowCreate(host, name, type, base); // ask the datastore if it can be created
    }

    public boolean allowDelete(Data host, String name) {
        Boolean result = PolicyHooks.allowDelete(host,name); // ask the application hook if this can be deleted
        if (result != null) return result;
        return DataStore.getPolicy().allowDelete(host,name); // ask the datastore if it can be deleted
    }




}
