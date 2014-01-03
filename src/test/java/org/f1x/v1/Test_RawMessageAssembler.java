/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.f1x.v1;

import org.f1x.SessionIDBean;
import org.f1x.api.FixVersion;
import org.f1x.api.message.MessageBuilder;
import org.f1x.api.message.fields.*;
import org.f1x.api.session.SessionID;
import org.f1x.io.OutputChannel;
import org.f1x.util.ByteArrayReference;
import org.f1x.util.StoredTimeSource;
import org.f1x.util.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class Test_RawMessageAssembler {


    @Test
    public void testLogon () throws Exception {
        String expected = "8=FIX.4.4\u00019=73\u000135=A\u000134=1\u000149=CLIENT\u000152=20131125-19:09:38.746\u000156=SERVER\u000198=0\u0001108=30\u0001141=Y\u000110=209\u0001";


        MessageBuilder mb = new ByteBufferMessageBuilder(256, 2);
        mb.setMessageType("A");
        mb.add(98, 0);
        mb.add(108, 30);
        mb.add(141, true);

        String actual = format(mb, new SessionIDBean("CLIENT", "SERVER"), 1, "20131125-19:09:38.746");
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNewOrderSingle () throws Exception {
        String expected = "8=FIX.4.4\u00019=141\u000135=D\u000134=9\u000149=DEMO2Kweoj_DEMOFIX\u000152=20121009-13:44:49.421\u000156=DUKASCOPYFIX\u000111=506\u000121=1\u000138=1\u000140=1\u000154=1\u000155=EUR/USD\u000159=1\u000160=20121009-13:44:49.421\u000110=105\u0001";

        MessageBuilder mb = new ByteBufferMessageBuilder(256, 2);
        mb.setMessageType(MsgType.ORDER_SINGLE);
        mb.add(FixTags.ClOrdID, 506);
        mb.add(FixTags.HandlInst, HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE);
        mb.add(FixTags.OrderQty, 1);
        mb.add(FixTags.OrdType, OrdType.MARKET);
        mb.add(FixTags.Side, Side.BUY);
        mb.add(FixTags.Symbol, "EUR/USD");
        mb.add(FixTags.TimeInForce, TimeInForce.GOOD_TILL_CANCEL);
        mb.addUTCTimestamp(FixTags.TransactTime, TestUtils.parseUTCTimestamp("20121009-13:44:49.421"));

        String actual = format(mb, new SessionIDBean("DEMO2Kweoj_DEMOFIX", "DUKASCOPYFIX"), 9, "20121009-13:44:49.421");
        Assert.assertEquals(expected, actual);
    }



    private static final long sendingTime = System.currentTimeMillis();

    // FIX8.ORG test
    //    msg.getHeader().setField( FIX::BeginString( "FIX.4.4" ) );
    //    msg.getHeader().setField( FIX::MsgType( FIX::MsgType_NewOrderSingle ) );
    //    msg.getHeader().setField( FIX::MsgSeqNum(78));
    //    msg.getHeader().setField(FIX::SenderCompID("A12345B"));
    //    msg.getHeader().setField(FIX::SenderSubID("2DEFGH4"));
    //    msg.getHeader().setField(FIX::SendingTime(FIX::UtcTimeStamp()));
    //    msg.getHeader().setField(FIX::TargetCompID("COMPARO"));
    //    msg.getHeader().setField(FIX::TargetSubID("G"));
    //    msg.getHeader().setField(FIX::SenderLocationID("AU,SY"));
    //
    //    msg.setField( FIX::Account( "01234567") );
    //    msg.setField( FIX::ClOrdID( "4" ) );
    //    msg.setField( FIX::OrderQty( 50 ) );
    //    msg.setField( FIX::OrdType( FIX::OrdType_LIMIT) );
    //    msg.setField( FIX::Price( 400.5) );
    //    msg.setField( FIX::HandlInst( '1' ) );
    //    msg.setField( FIX::Symbol( "OC") );
    //    msg.setField( FIX::Text( "NIGEL") );
    //    msg.setField( FIX::Side( FIX::Side_BUY ) );
    //    msg.setField( FIX::SecurityDesc( "AOZ3 C02000") );
    //    msg.setField( FIX::TimeInForce( FIX::TimeInForce_DAY ) );
    //    msg.setField( FIX::TransactTime() );
    //    msg.setField( FIX::SecurityType( FIX::SecurityType_OPTION ) );

    /** Similar to FIX8 encoding test */
    @Test
    public void testNewOrderSingleEncodingPerformance () throws Exception {

        final MessageBuilder mb = new ByteBufferMessageBuilder(256, 2);
        final RawMessageAssembler asm = new RawMessageAssembler(FixVersion.FIX44, 256, new StoredTimeSource("20121009-13:44:49.421"));
        final SessionID sessionID = new SessionIDBean("A12345B", "2DEFGH4", "COMPARO", "G");
        NullOutputChannel nul = new NullOutputChannel();


        final int WARMUP = 20000, N = 500000;
        for (int i =0; i < WARMUP; i++) {
            encode(mb, asm, sessionID, nul);
        }

        long start = System.nanoTime();
        for (int i =0; i < N; i++) {
            encode(mb, asm, sessionID, nul);
        }
        long end = System.nanoTime();
        System.out.println("Time " + (end-start)/N + " ns. per encoding, dummy result: " + nul.toString());
    }

    private void encode(MessageBuilder mb, RawMessageAssembler asm, SessionID sessionID, NullOutputChannel nul) throws IOException {
        mb.clear();
        mb.setMessageType("01234567");
        mb.add(FixTags.Account, "AU,SY");
        mb.add(FixTags.SenderLocationID, "AU,SY");
        mb.add(FixTags.ClOrdID, 4);
        mb.add(FixTags.HandlInst, HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE);
        mb.add(FixTags.OrderQty, 50);
        mb.add(FixTags.OrdType, OrdType.LIMIT);
        mb.add(FixTags.Price, 400.5);
        mb.add(FixTags.Side, Side.BUY);
        mb.add(FixTags.Symbol, "OC");
        mb.add(FixTags.SecurityDesc, "AOZ3 C02000");
        mb.add(FixTags.SecurityType, SecurityType.OPTION);
        mb.add(FixTags.Text, "NIGEL");
        mb.add(FixTags.TimeInForce, TimeInForce.DAY);
        mb.addUTCTimestamp(FixTags.SendingTime, sendingTime);
        mb.addUTCTimestamp(FixTags.TransactTime, System.currentTimeMillis());
        asm.send(sessionID, 78, mb, nul);
    }


    private static String format (MessageBuilder mb, SessionID sessionID, int msgSeqNum, String time) throws IOException {
        RawMessageAssembler asm = new RawMessageAssembler(FixVersion.FIX44, 256, new StoredTimeSource(time));
        TextOutputChannel text = new TextOutputChannel();
        asm.send(sessionID, msgSeqNum, mb, text);
        return text.toString();
    }

    private static class TextOutputChannel implements OutputChannel{
        private final StringBuilder sb = new StringBuilder();
        private final ByteArrayReference ref = new ByteArrayReference();

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            ref.set(buffer, offset, length);
            sb.append (ref);
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

    private static class NullOutputChannel implements OutputChannel{
        private int something;

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            this.something += length + offset + buffer.length; // just to trick optimizer
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public String toString() {
            return "Something:" + something;
        }
    }
}
