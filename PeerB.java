/**
 * @file PeerB.java
 * @author Marco Zavatta
 * @date 31/05/2012
 * @brief IOT homework Mote Runner, peer B (half-way mote)
 */


// THE PROBLEM IS THAT IN THIS WAY IT WILL NEVER SEE IF A PACKET IS ACTUALLY FOR HIM OR NOT
// IT CAN ONLY ACT AS A BRIDGE

package IOT.homework;
import com.ibm.saguaro.system.*;
import com.ibm.saguaro.logger.*;

public class PeerB {

	private static int[] neighbours;
	private static Timer  tsend;
	private static byte[] frame;

	static Radio radio = new Radio();


	static {

		Logger.appendString(csr.s2b("PeerB: started"));
		Logger.flush(Mote.INFO);


		/* Set up the radio device:
		 * Set the PAN ID to 0x22 and the short address to the last two bytes of the extended address
		 * true as the second parameter indicates that the device is PAN coordinator
		 */
		radio = new Radio();
		radio.open(Radio.DID, null, 0, 0);
		radio.setPanId(0x22, true);
		radio.setShortAddr(0x000B);


		/* Launch fake topology discovery. */
		neighbours = new int[2];
		discovery();

		/* Prepare frame (beacon frame) with source addressing. */
		frame = new byte[11];
		frame[0] = Radio.FCF_BEACON; // Frame control flags
		frame[1] = (Radio.FCA_SRC_SADDR|Radio.FCA_SRC_SADDR); // Frame control address flags
		Util.set16le(frame, 3, 0x22); //DST pan, matching this node's one
		Util.set16le(frame, 7, 0x22); //SRC pan, matching this node's one

		        
		/* Start listening to radio channel 0. */
		radio.setRxHandler(new DevCallback(null){
			public int invoke (int flags, byte[] data, int len, int info, long time) {
			    return  PeerB.onRxPDU(flags, data, len, info, time);
			}
		    });
		radio.startRx(Device.ASAP, 0, Time.currentTicks()+0x7FFFFFFF);


		Logger.appendString(csr.s2b("Reception started"));
		Logger.flush(Mote.INFO);
     

	}


	/* Reception handler. */
	private static int onRxPDU (int flags, byte[] data, int len, int info, long time) {

		/* data null means expiry of reception period, thus re-enable reception for a very long time. */
		if (data == null) {
		    radio.startRx(Device.ASAP, 0, Time.currentTicks()+0x7FFFFFFF);
		    return 0; 
		}

		int i = 0;
		Logger.appendString(csr.s2b("PeerB: packet received: "));
		Logger.appendString(csr.s2b("length:"));
		Logger.appendHexInt((int)len);
		Logger.appendString(csr.s2b(" data:"));
		while (i<len) {
			Logger.appendHexByte((byte)data[i]);
			Logger.appendString(csr.s2b("."));
			i++;
		}
		Logger.flush(Mote.INFO);

		
		/* Chech whether it is intended for a neighbourg. */
		// (I've no way to chech that!)

		/* If a data packet, forward to other neighbourg. */
		/* If an ACK packet, forward the other neighbourg. */
		/* Therefore, just forward, swap source as destination. */
		if (Util.get16le(data, 9)== 0x000A) Util.set16le(data, 5, 0x000C);
		if (Util.get16le(data, 9)== 0x000C) Util.set16le(data, 5, 0x000A);
		PeerB.packetSend(data, len);
		
		return 0;
	}
	
	/* Send packet method. */
	public static void packetSend(byte[] data, int len) {

		Logger.appendString(csr.s2b("Sending a packet..."));
		Logger.flush(Mote.INFO);

		/* Pad the source address. */
		Util.set16le(data, 9, radio.getShortAddr());

		/* Fire. */
		radio.transmit(Device.ASAP|Radio.TXMODE_CCA, frame, 0, len, 0);

	}

	/* Fake topology discovery. */
	private static void discovery() {
		neighbours[0]=0x000A;
		neighbours[1]=0x000C;
	}
}
