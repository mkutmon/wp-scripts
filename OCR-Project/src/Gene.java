
public class Gene {

	private String label;
	private String id;
	private String ds;
	private String comment;
	
	public Gene(String label, String id, String ds, String comment) {
		super();
		this.label = label;
		this.id = id;
		this.ds = ds;
		this.comment = comment;
	}
	
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getDs() {
		return ds;
	}
	public void setDs(String ds) {
		this.ds = ds;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
}
