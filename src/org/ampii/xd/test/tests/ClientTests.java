// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestException;

/**
 * ??? Client Experiments
 *
 * @author drobin
 */
public class ClientTests {

    public static Test[] tests = {
            new Test("Client Experiments") {
                public void execute() throws TestException {
                    definition("" +
                            "<Composition name='BitcoinResponse'>" +
                            "   <Array name='error' memberType='String'/>" +
                            "   <Collection name='result' memberType='BitcoinQuote'/>" +
                            "</Composition>");
                    definition("" +
                            "<Composition name='BitcoinQuote'>" +
                            "   <Array name='a' memberType='Real'/>" +
                            "   <Array name='b' memberType='Real'/>" +
                            "   <Array name='c' memberType='Real'/>" +
                            "   <Array name='v' memberType='Real'/>" +
                            "   <Array name='p' memberType='Real'/>" +
                            "   <Array name='t' memberType='Unsigned'/>" +
                            "   <Array name='l' memberType='Real'/>" +
                            "   <Array name='h' memberType='Real'/>" +
                            "   <Real  name='o'/>" +
                            "</Composition>");
                    step("blah");
                    clientData("<Composition type='BitcoinResponse' partial='true'/>");
                    alt("json");
                    uri("https://api.kraken.com/0/public/Ticker?pair=XXBTZEUR");
                    get();
                    expectClientData("" +
                            "<Composition name='BitcoinResponse' type='BitcoinResponse'>" +
                            "   <Array name='error'/>" +
                            "   <Collection name='result'>" +
                            "       <Composition name='XXBTZEUR'>" +
                            "           <Array name='a'>" +
                            "              <Real><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Real>" +
                            "              <Real><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Real>" +
                            "              <Real><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Real>" +
                            "           </Array>" +
                            "           <Array name='b'>" +
                            "              <Real><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Real>" +
                            "              <Real><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Real>" +
                            "              <Real><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Real>" +
                            "           </Array>" +
                            "           <Array name='c'>" +
                            "              <Real><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Real>" +
                            "              <Real><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Real>" +
                            "           </Array>" +
                            "           <Array name='v'>" +
                            "              <Real><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Real>" +
                            "              <Real><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Real>" +
                            "           </Array>" +
                            "           <Array name='p'>" +
                            "              <Real><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Real>" +
                            "              <Real><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Real>" +
                            "           </Array>" +
                            "           <Array name='t'>" +
                            "              <Unsigned><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Unsigned>" +
                            "              <Unsigned><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Unsigned>" +
                            "           </Array>" +
                            "           <Array name='l'>" +
                            "              <Real><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Real>" +
                            "              <Real><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Real>" +
                            "           </Array>" +
                            "           <Array name='h'>" +
                            "              <Real><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Real>" +
                            "              <Real><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Real>" +
                            "           </Array>" +
                            "           <Real  name='o'><Extensions><Boolean name='..matchAny' value='true'/></Extensions></Real>" +
                            "       </Composition>" +
                            "   </Collection>" +
                            "</Composition>");
                }
            }
    };

}
