import java.io.Serializable;

/**
 * Class ProcessState
 * Each Process updates its process state to send to other process wit lastest timestamp. 
 * @author Raghav Babu
 * Date : 04/2/2016
 */
public class ProcessState implements Comparable<ProcessState>,Serializable {

	private static final long serialVersionUID = 4121272507875364283L;
	int processId;
	int timeStamp;
	CriticalSectionStatus status;
	
	public ProcessState(int fromProcessId,
			int timeStamp, CriticalSectionStatus status) {
		this.processId = fromProcessId;
		this.timeStamp = timeStamp;
		this.status = status;
	}

	/*
	 * Ordering in queue based on timestamp. 
	 * If timestamps are equal, order based on processId.
	 */
	@Override
	public int compareTo(ProcessState o) {
		
		int val =  this.timeStamp - o.timeStamp;
		
		if(val == 0){
			return this.processId - o.processId;
		}else{
			return val;
		}
	}

	@Override
	public String toString() {
		return "ProcessState [fromProcessId=" + processId + 
				", status =" + status +
				", timeStamp=" + timeStamp + "]";
	}
	

	
}
