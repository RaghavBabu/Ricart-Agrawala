import java.io.Serializable;

/**
 * class Event
 * Event can be transfer or ping to another process.
 * @author Raghav Babu
 * Date:04/2/2016
 * 
 */
public class Event implements Serializable {

	private static final long serialVersionUID = 1170321869731823009L;
	int processId;
	EventType eventType;
	int amt;
	int timeStamp;
	
	public Event(EventType eventType, int processId, int amt,int timeStamp) {
		this.processId = processId;
		this.eventType = eventType;
		this.amt = amt;
		this.timeStamp = timeStamp;
	}

	public Event(EventType eventType, int processId) {
		this.processId = processId;
		this.eventType = eventType;
	}

	@Override
	public String toString() {
		
			return "Event [type=" + eventType
					+ ", amt=" + amt 
					+ ", TimeStamp=" + timeStamp 
					+ "]";
	}

}
