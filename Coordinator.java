/**
 * @file Coordinator.java
 * @author Marco Zavatta
 * @date 31/05/2012
 * @brief IOT homework Mote Runner, coordinator (source mote)
 */

package IOT.homework;
import com.ibm.saguaro.system.*;
import com.ibm.saguaro.logger.*;

public class Coordinator {


	private static int[] neighbours;
	private static Timer  tsend;
	private static byte[] frame;
	private static long   sendTime;
	static Radio radio = new Radio();


	static {

		Logger.appendString(csr.s2b("Coordinator: started"));
		Logger.flush(Mote.INFO);

		/* Set up the radio device:
		 * Set the PAN ID to 0x22 and the short address to the last two bytes of the extended address
		 * true as the second parameter indicates that the device is PAN coordinator
		 */
		radio = new Radio();
		radio.open(Radio.DID, null, 0, 0);
		radio.setPanId(0x22, true);
		radio.setShortAddr(0x000A);

		/* Launch fake topology discovery. */
		//neighbours = new int[1];
		//discovery();
	
		/* Prepare frame (beacon frame) with source addressing. */
		frame = new byte[11];
		frame[0] = Radio.FCF_DATA; // Frame control flags
		frame[1] = (Radio.FCA_DST_SADDR|Radio.FCA_SRC_SADDR); // Frame control address flags
		Util.set16le(frame, 3, 0x22); //DST pan, matching this node's one
		Util.set16le(frame, 7, 0x22); //SRC pan, matching this node's one
		        
		/* Start listening to radio channel 0. */
		radio.setRxHandler(new DevCallback(null){
			public int invoke (int flags, byte[] data, int len, int info, long time) {
			    return  Coordinator.onRxPDU(flags, data, len, info, time);
			}
		    });
		radio.startRx(Device.ASAP, 0, Time.currentTicks()+0x7FFFFFFF);


		Logger.appendString(csr.s2b("Reception started"));
		Logger.flush(Mote.INFO);

	
		/* Setup a timer callback for transmission. */
		tsend = new Timer();
		tsend.setCallback(new TimerEvent(null){
			public void invoke(byte param, long time){
			    Coordinator.packetSend(param, time);
			}
		    });
		/* Convert the desired delay to platform ticks. */
		sendTime = Time.toTickSpan(Time.MILLISECS, 6000);
		/* Start the timer. */
		tsend.setAlarmBySpan(sendTime);
     

	}
    

	/* Reception handler. */
        private static int onRxPDU (int flags, byte[] data, int len, int info, long time) {

		/* data null means expiry of reception period, thus re-enable reception for a very long time. */
		if (data == null) {
		    radio.startRx(Device.ASAP, 0, Time.currentTicks()+0x7FFFFFFF);
		    return 0; 
		}
	
		int i = 0;
		Logger.appendString(csr.s2b("Coordinator: packet received: "));
		Logger.appendString(csr.s2b("length:"));
		Logger.appendHexInt((int)len);
		Logger.appendString(csr.s2b(" data:"));
		while (i<len) {
			Logger.appendHexByte((byte)data[i]);
			Logger.appendString(csr.s2b("."));
			i++;
		}
		Logger.flush(Mote.INFO);

		/* Check if ACK. */
		if ((byte)data[2]==(byte)0xAC)
			Logger.appendString(csr.s2b("Coordinator: ACK received"));
		else
			Logger.appendString(csr.s2b("Coordinator: frame received but it is no ACK!"));
		Logger.flush(Mote.INFO);

		return 0;
	}
	

	/* Called on timer event, send packet. */
	public static void packetSend(byte param, long time) {

		Logger.appendString(csr.s2b("Coordinator: Sending a packet..."));
		Logger.flush(Mote.INFO);
		
		/* Final frame tunings. */
		Util.set16le(frame, 5, 0x000B); //destination address
		Util.set16le(frame, 9, radio.getShortAddr()); //source address
		frame[2] = (byte)0xDA; //data frame
		
		/* Fire. */
		radio.transmit(Device.ASAP|Radio.TXMODE_CCA, frame, 0, 11, 0);
		
	}


	/* Fake topology discovery. */
	private static void discovery() {
		neighbours[0]=0x000B;
	}
}
