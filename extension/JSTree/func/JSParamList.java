package JSTree.func;

import java.util.List;

import JSTree.JSTree;

public class JSParamList implements JSTree {
	
	private List<JSParam> params;
	
	public JSParamList(List<JSParam> params) {
		this.params = params;
	}
	
	@Override
	public String toCode() {
		String result = "(";
		for (JSParam p:params) {
			result += p.toCode() + ", ";
		}
		
		return result.substring(0, result.length() -2) + ")";
	}

}
