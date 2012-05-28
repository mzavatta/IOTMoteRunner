/**
 * @file PeerB.java
 * @author Marco Zavatta
 * @date 31/05/2012
 * @brief IOT homework Mote Runner, peer B (half-way mote)
 */


package IOT.homework;
import com.ibm.saguaro.system.*;
import com.ibm.saguaro.logger.*;

public class PeerB {

	private static int[] neighbours;
	private static Timer  tsend;
	private static byte[] frame;
	private static long   xmitDelay;

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
		neighbours = new int[1];
		discovery();

		/* Prepare frame (beacon frame) with source addressing. */
		frame = new byte[7];
		frame[0] = Radio.FCF_BEACON; // Frame control flags
		frame[1] = Radio.FCA_SRC_SADDR; // Frame control address flags
		Util.set16le(frame, 3, 0x22); //DST pan, matching this node's one

		        
		/* Start listening to radio channel 0. */
		radio.setRxHandler(new DevCallback(null){
			public int invoke (int flags, byte[] data, int len, int info, long time) {
			    return  PeerB.onRxPDU(flags, data, len, info, time);
			}
		    });
		radio.startRx(Device.ASAP, 0, Time.currentTicks()+0x7FFFFFFF);


		Logger.appendString(csr.s2b("Reception started"));
		Logger.flush(Mote.INFO);

	
		// Setup a periodic timer callback for transmissions
		tsend = new Timer();
		tsend.setCallback(new TimerEvent(null){
			public void invoke(byte param, long time){
			    PeerB.periodicSend(param, time);
			}
		    });
		// Convert the periodic delay from ms to platform ticks
		xmitDelay = Time.toTickSpan(Time.MILLISECS, 2500);
		// Start the timer
		tsend.setAlarmBySpan(xmitDelay);
     

	}


	/* Reception handler. */
	private static int onRxPDU (int flags, byte[] data, int len, int info, long time) {

		if (data == null) { // marks end of reception period
		    // re-enable reception for a very long time
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


		return 0;
	}
	
	// Called on a timer alarm
	public static void packetSend(byte param, long time) {
		Logger.appendString(csr.s2b("Sending a packet..."));
		Logger.flush(Mote.INFO);

		Util.set16le(frame, 5, 0x000B); //seems like destination address
		// set sequence number
		frame[2] = (byte)ledColor;
		// send the message
		//public void transmit(uint mode, byte[] pdu, uint beg, uint len, long time)
		radio.transmit(Device.ASAP|Radio.TXMODE_CCA, frame, 0, 7, 0);
		// Setup a new alarm
		tsend.setAlarmBySpan(xmitDelay);
	}

	// Fake topology discovery
	private static void discovery() {
		neighbours[0]=0x000A;
		neighbours[1]=0x000C;
	}
}
