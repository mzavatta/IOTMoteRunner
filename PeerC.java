/**
 * @file PeerC.java
 * @author Marco Zavatta
 * @date 31/05/2012
 * @brief IOT homework Mote Runner, peer C (destination mote)
 */

package IOT.homework;
import com.ibm.saguaro.system.*;
import com.ibm.saguaro.logger.*;

public class PeerC {

	private static int[] neighbours;
    	private static Timer  tsend;
    	private static byte[] frame;

    	static Radio radio = new Radio();

   	static {

	
		Logger.appendString(csr.s2b("Peer C: started"));
		Logger.flush(Mote.INFO);


		/* Set up the radio device:
		 * Set the PAN ID to 0x22 and the short address to the last two bytes of the extended address
		 * true as the second parameter indicates that the device is PAN coordinator
		 */
		radio = new Radio();
		radio.open(Radio.DID, null, 0, 0);
		radio.setPanId(0x22, true);
		radio.setShortAddr(0x000C);

	
		/* Launch fake topology discovery. */
		//neighbours = new int[1];
		//discovery();

	
		/* Prepare frame (beacon frame) with source addressing. */
		frame = new byte[11];
		frame[0] = Radio.FCF_DATA; // Frame control flags
		frame[1] = (Radio.FCA_DST_SADDR|Radio.FCA_SRC_SADDR); // Frame control address flags
		Util.set16le(frame, 3, 0x22); //DST pan, matching this node's one
		Util.set16le(frame, 7, 0x22); //SRC pan, matching this node's one

		        
		// Put radio into receive mode for a long time on channel 0
		radio.setRxHandler(new DevCallback(null){
			public int invoke (int flags, byte[] data, int len, int info, long time) {
			    return  PeerC.onRxPDU(flags, data, len, info, time);
			}
		    });
		radio.startRx(Device.ASAP, 0, Time.currentTicks()+0x7FFFFFFF);


		Logger.appendString(csr.s2b("Peer C: Reception started"));
		Logger.flush(Mote.INFO);

	
    
	}
	// On a received pdu turn on the appropriate LEDs based on sequence number
	private static int onRxPDU (int flags, byte[] data, int len, int info, long time) {

		/* data null means expiry of reception period, thus re-enable reception for a very long time. */
		if (data == null) {
		    radio.startRx(Device.ASAP, 0, Time.currentTicks()+0x7FFFFFFF);
		    return 0; 
		}

		int i = 0;
		Logger.appendString(csr.s2b("Peer C: packet received: "));
		Logger.appendString(csr.s2b("length:"));
		Logger.appendHexInt((int)len);
		Logger.appendString(csr.s2b(" data:"));
		while (i<len) {
			Logger.appendHexByte((byte)data[i]);
			Logger.appendString(csr.s2b("."));
			i++;
		}
		Logger.flush(Mote.INFO);


		/* If data packet, then change into acknowledge. */
		data[2] = (byte)0xAC;

		PeerC.packetSend(data, len);

		return 0;
	}
	
	/* Send packet method. */
	public static void packetSend(byte[] data, int len) {
		int i = 0;
		Logger.appendString(csr.s2b("Peer C: sending a packet:"));
		


		/* Swap source with destination. */
		Util.set16le(data, 5, Util.get16le(data, 9));
		Util.set16le(data, 9, radio.getShortAddr());

		Logger.appendString(csr.s2b("length:"));
		Logger.appendHexInt((int)len);
		Logger.appendString(csr.s2b(" data:"));
		while (i<len) {
			Logger.appendHexByte((byte)data[i]);
			Logger.appendString(csr.s2b("."));
			i++;
		}
		Logger.flush(Mote.INFO);

		/* Fire. */
		radio.transmit(Device.ASAP|Radio.TXMODE_CCA, data, 0, len, 0);
    	}

	/* Fake topology discovery. */
	private static void discovery() {
		neighbours[0]=0x000B;
	}
}
