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

	/*
	 * The characterisation between a DATA packet or and ACK packet is brought by SEQ field
	 * 0xDA meaning DATA
	 * 0xAC meaning ACK
	 */  

	/* Frame format:					     <-----------payload----------->
	 *              2     3        5         7   	  9         11             13
	 * Bytes |  2   |  1  |    2   |    2    |    2   |   2     |       2      |        2       | 15tot
	 * Field | CTRL | SEQ | DSTPAN | DSTADDR | SRCPAN | SRCADDR | finaldstaddr | initialsrcaddr |
	 */

	/*
	 * Hypothesis of a star topology.
	 * Therefore a Peer cannot have children.
	 * For every tx frame, talk with the Coordinator.
	 * The DSTADDR, SRCADDR are specific for each frame between two peers, while finaldstaddr and initialsrcaddr
	 * are needed to indicate the ends of the whole route. As a frame travels along a path, DSTADDR and SRCADDR change,
	 * while finaldstaddr and initialsrcaddr do not change.
	 * It is important, for the coordinator, to distinguish if the final destination is him or not.
	 * If the frame is for him, then ACK back. If the frame is not for him, route it based on finalsrcaddr.
	 * With this mechanism, one-hop frames (from peer to coordinator or vice-versa) can also be handled.
	 * Once a frame reaches its finaldstaddr, if it's a DATA, it is acked back using initialsrcaddr.
	 * If its a ACK, no reaction is taken.
	 * Short addresses are used:
	 * Peer A, initiator: 0x000A
	 * Coordinator: 0x000B
	 * Peer C: 0x000C
	 */

	/* 
	 * The only thing in which PeerA and PeerC differ is the first frame timer trigger. The communication protocol is the same.
	 */
		
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
		radio.setPanId(0x22, false);
		radio.setShortAddr(0x000C);

		/* Prepare frame (data frame) with source and destination short addressing. */
		/* Header length in this case is 11 bytes. Therefore max payload length 125-11=114 bytes. */
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
		Logger.appendString(csr.s2b("Peer C: frame received: "));
		Logger.appendString(csr.s2b("length:"));
		Logger.appendHexInt((int)len);
		Logger.appendString(csr.s2b(" data:"));
		while (i<len) {
			Logger.appendHexByte((byte)data[i]);
			Logger.appendString(csr.s2b("."));
			i++;
		}
		Logger.flush(Mote.INFO);

		
		/* If intended for me, proceed. */
		if (Util.get16le(data, 11)==radio.getShortAddr()) {

			/* Check if ACK. */
			if ((byte)data[2]==(byte)0xAC) {
				Logger.appendString(csr.s2b("Peer C: ACK received"));
				Logger.flush(Mote.INFO);
			}

			/* If data, start ACK chain going through coordinator. */
			else if ((byte)data[2]==(byte)0xDA) {
				
				Logger.appendString(csr.s2b("Peer C: DATA received, acking back"));
				Logger.flush(Mote.INFO);
				
				data[2]=(byte)0xAC; //transform frame into ACK				
				
				//swap initial source with final destination
				Util.set16le(data, 11, Util.get16le(data, 13));
				Util.set16le(data, 13, radio.getShortAddr());

				//set as next-hop destination the coordinator 
				Util.set16le(data, 5, 0x000B);
				//set me as source
				Util.set16le(data, 9, radio.getShortAddr());
				PeerC.packetSend(data, 15);			
			}
			return 0;
		}
		/* If not intended for me, discard. */

		Logger.appendString(csr.s2b("Peer C: frame NOT INTENDED FOR ME!"));
		Logger.flush(Mote.INFO);

		return 0;
	}
	
	/* Send frame method. */
	public static void packetSend(byte[] data, int len) {
		int i = 0;
		Logger.appendString(csr.s2b("Peer C: sending a frame:"));
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

}
