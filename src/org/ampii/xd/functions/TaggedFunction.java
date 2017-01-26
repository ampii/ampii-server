// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.functions;

import org.ampii.xd.common.StringSet;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.IntegerData;
import org.ampii.xd.data.basetypes.LinkData;
import org.ampii.xd.data.basetypes.ListData;
import org.ampii.xd.data.basetypes.StringData;

/**
 * An Implementation of the .../tagged() URI function, called from {@link Functions}
 *
 */
public class TaggedFunction extends Functions {

    static Data invoke(Data target, String argString) throws XDException {
        Data tagsArg     = new StringData("tags");
        Data depth       = new IntegerData("depth", -1);
        Data resultsList = new ListData("results");
        parseArgs(argString, tagsArg, depth);
        String[] tags = tagsArg.stringValue().split(";");
        int depthLimit = depth.intValue();
        DataList parents = new DataList();
        if (target.canHaveChildren()) for (Data child: target.getChildren())  {
            search(child, parents, depthLimit, resultsList, tags);
        }
        return resultsList;
    }

    // TODO: yikes: do this with a real indexed database of tags some day
    static void search(Data data, DataList parents, int depthLimit, Data resultsList, String[] tags) throws XDException {
        Data meta;
        StringSet foundTags = new StringSet();
        meta = data.findEffective(Meta.TAGS);
        if (meta != null) foundTags.add(meta.stringSetValue());
        meta = data.findEffective(Meta.VALUETAGS);
        if (meta != null) for (Data child: meta.getChildren()) foundTags.add(child.getName());
        int found = 0;
        for (String tag : tags) {
            if (foundTags.containsComponent(tag)) found++;
        }
        if (found == tags.length) {  // did we find them all?
            StringBuilder path = new StringBuilder();
            for (Data parent : parents) {
                path.append(parent.getName()+"/");
            }
            path.append(data.getName());
            resultsList.post(new LinkData("", path.toString()));
        }
        if (data.getCount()!=0 && (depthLimit == -1 || parents.size()<depthLimit-1)) {
            parents.add(data);
            for (Data child: data.getChildren())  {
                search(child, parents, depthLimit,  resultsList, tags);
            }
            parents.remove(parents.size() - 1);
        }
    }
}
