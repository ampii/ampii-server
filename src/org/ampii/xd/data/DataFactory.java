// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data;

import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.basetypes.*;

/**
 * We need a factory to make a specific runtime class given a base type identifier.
 *
 * @author drobin
 */
public class DataFactory {

    public static Data make(Base base, String name) {
        try { return make(base,name,new Object[0]); }
        catch (XDException e) { throw new XDError("DataFactory.make(base,name) failed!"); }
    }

    public static Data make(Base base, String name, Object... initializers) throws XDException {
        switch (base) {
            case NULL:                    return new NullData(name,initializers);
            case BOOLEAN:                 return new BooleanData(name,initializers);
            case UNSIGNED:                return new UnsignedData(name,initializers);
            case INTEGER:                 return new IntegerData(name,initializers);
            case REAL:                    return new RealData(name,initializers);
            case DOUBLE:                  return new DoubleData(name,initializers);
            case OCTETSTRING:             return new OctetStringData(name,initializers);
            case STRING:                  return new StringData(name,initializers);
            case BITSTRING:               return new BitStringData(name,initializers);
            case ENUMERATED:              return new EnumeratedData(name,initializers);
            case DATE:                    return new DateData(name,initializers);
            case DATEPATTERN:             return new DatePatternData(name,initializers);
            case DATETIME:                return new DateTimeData(name,initializers);
            case DATETIMEPATTERN:         return new DateTimePatternData(name,initializers);
            case TIME:                    return new TimeData(name,initializers);
            case TIMEPATTERN:             return new TimePatternData(name,initializers);
            case OBJECTIDENTIFIER:        return new ObjectIdentifierData(name,initializers);
            case OBJECTIDENTIFIERPATTERN: return new ObjectIdentifierPatternData(name,initializers);
            case WEEKNDAY:                return new WeekNDayData(name,initializers);
            case SEQUENCE:                return new SequenceData(name,initializers);
            case ARRAY:                   return new ArrayData(name,initializers);
            case LIST:                    return new ListData(name,initializers);
            case SEQUENCEOF:              return new SequenceOfData(name,initializers);
            case CHOICE:                  return new ChoiceData(name,initializers);
            case OBJECT:                  return new ObjectData(name,initializers);
            case BIT:                     return new BitData(name,initializers);
            case LINK:                    return new LinkData(name,initializers);
            case ANY:                     return new AnyData(name,initializers);
            case STRINGSET:               return new StringSetData(name,initializers);
            case COMPOSITION:             return new CompositionData(name,initializers);
            case COLLECTION:              return new CollectionData(name,initializers);
            case UNKNOWN:                 return new UnknownData(name,initializers);
            case RAW:                     return new RawData(name,initializers);
            case POLY:                    return new PolyData(name,initializers);
            default:                      throw new XDError("DataFactory doesn't know how to create a "+base);
        }
    }

}
