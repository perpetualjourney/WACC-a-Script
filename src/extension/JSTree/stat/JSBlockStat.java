package JSTree.stat;

public class JSBlockStat implements JSStat {

	private JSStat stat;
	
	public JSBlockStat(JSStat stat) {
		this.stat = stat;
	}
	
	@Override
	public String toCode() {
		return "(function() { \n"
				+ stat.toCode()
				+ "})();";
	}

}
