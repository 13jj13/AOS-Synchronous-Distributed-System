import com.sun.nio.sctp.*;
import java.net.InetSocketAddress;

// This object is a SCTPServer to accept multiple connections from different SCTPClients.
public class SCTPServer implements Runnable
{
	// Server node information
	// Client should connect to same port number that server opens
	Node serverInfo = null;

	// Synchronizer
	SCTPClientServer cs;

	// Number of rounds for a node to send/receive messages.
	int numOfRounds = 0;

	// Server channel
	SctpServerChannel sctpServerChannel;

	// Constructor - initialize synchronizer, server node information, and number of rounds
	public SCTPServer(SCTPClientServer cs, Node serverInfo, int numOfRounds) throws Exception {
		this.cs = cs;
		this.serverInfo = serverInfo;
		this.numOfRounds = numOfRounds;

	}

	/*
		Method: run()
		Description: Start server - Open server channel and bind server to address. Connect to clients.
		Parameters: None
		Returns: Nothing
	 */
	@Override
	public void run() {
		while(true) {
			try {
				// Get address from port number
				InetSocketAddress addr = new InetSocketAddress(serverInfo.listeningPort);
				// Open server channel
				sctpServerChannel = SctpServerChannel.open();
				// Bind server channel to address
				sctpServerChannel.bind(addr);

				System.out.println("SERVER: Server created at node " + serverInfo.nodeID);

				connectClients();
			} catch (Exception e) {

				e.printStackTrace();
			}
		}
	}

	/*
		Method: connectClients
		Description: Enter infinite loop accepting connections from clients.
		Parameters: None
		Returns: Nothing
	 */
	private void connectClients()
	{

		while(true) {
			try {

				// Wait for incoming connection from client - accept() blocks until connection made.
				SctpChannel sctpChannel = sctpServerChannel.accept();
				System.out.println("CHANNEL: Client connected to this server.");
				Thread.sleep(3000);

				// Create new thread for the new client.
				Thread thread = new Thread(new SCTPClientHandler(cs, sctpChannel, serverInfo, numOfRounds));
				thread.start();

			} catch (Exception e) {

				e.printStackTrace();
			}


		}

	}
}



