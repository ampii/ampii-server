// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data;

import java.util.ArrayList;

/**
 * This is a typesafe List-of-Data that also provides extra info like $partial, $truncated, and $next.
 */
public class DataList extends ArrayList<Data> {
    public boolean partial;
    public boolean truncated;
    public String  next;
    public DataList() { super(0); } // '0' is because very often data lists have no members, so we will optimistically make it as small as possible
    public DataList(DataList other) { super(other); } // '0' is because very often data lists have no members, so we will optimistically make it as small as possible
    public DataList(boolean truncated, boolean partial, String  next) { super(0); this.partial=partial; this.truncated=truncated; this.next=next; }

    public boolean contains(String name) {
        return find(name) != null;
    }

    public Data    find(String name) {
        for (Data child : this) if (child.getName().equals(name)) return child;
        return null;
    }
}
