package tree.type;

import tree.expr.ExprNode;

public class WACCCompBinOp extends WACCBinOp {
	
	@Override
	public boolean check(ExprNode lhs, ExprNode rhs) {
		if (lhs.getType() == WACCType.INT)
			return rhs.getType() == WACCType.INT;
		else if (lhs.getType() == WACCType.CHAR)
			return rhs.getType() == WACCType.CHAR;
		else
			return false;
	}

	@Override
	public WACCType getType() {
		return WACCType.BOOL;
	}

}