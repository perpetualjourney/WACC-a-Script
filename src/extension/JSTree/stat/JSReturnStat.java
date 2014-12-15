package JSTree.stat;

import JSTree.JSTree;

public class JSReturnStat implements JSStat {

	private JSTree expr;
	
	public JSReturnStat(JSTree expr) {
		this.expr = expr;
	}
	
	@Override
	public String toCode() {
		return "return " + expr.toCode();
	}

}