import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Class ProcessClient
 * Each Process sends event or state to another process running in different machine or same machine based 
 * on configurations in XML file on request from Process Thread.
 * @author Raghav Babu
 * Date : 03/22/2016
 */
public class ProcessClient{

	ProcessState state;
	Event event;
	int toProcessId;
	String toIPAddress = null;
	int  toPort;

	public ProcessClient(ProcessState state, int toProcessId) {
		this.state = state;
		this.toProcessId = toProcessId;
		this.toIPAddress = ProcessIPPortXmlParser.processIDToIpMap.get(toProcessId);
		this.toPort = ProcessIPPortXmlParser.processIDToPortMap.get(toProcessId);
	}

	public ProcessClient(Event event, int toProcessId) {
		this.event = event;
		this.toProcessId = toProcessId;
		this.toIPAddress = ProcessIPPortXmlParser.processIDToIpMap.get(toProcessId);
		this.toPort = ProcessIPPortXmlParser.processIDToPortMap.get(toProcessId);
	}


	public boolean send() {

		try {

			Socket socket = null;

			try {

				try {
					socket = new Socket(toIPAddress, toPort);
				} catch (Exception e) {
					//System.out.println("Server in "+toIPAddress+ " not yet bound to "+toPort);
					//System.out.println("Start the Process server in "+toIPAddress+ " at port "+toPort);
					return false;
				}

				//write msg object
				OutputStream os = socket.getOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(os);

				if(state != null){
					System.out.println("Sending state to Process ---> "+toProcessId+"");
					System.out.println(state);
					System.out.println("Transfering process state to process : "+toProcessId);
					oos.writeObject(state);
					System.out.println("--------------------------------------------------------------------");
				}
				else{
					if(event.eventType == EventType.TRANSFER){
						System.out.println("Sending Event to Process ----> "+toProcessId);
						oos.writeObject(event);
						System.out.println(event);
						Process.totalAmount -= event.amt;
						System.out.println("Process current Total Amount : "+Process.totalAmount);
						System.out.println("--------------------------------------------------------------------");
					}
					else{
						oos.writeObject(event);
					}
				}


			}catch (Exception e){
				System.out.println("Exception while passing event object to  "+toIPAddress);
				e.printStackTrace();
			}
			socket.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
}
