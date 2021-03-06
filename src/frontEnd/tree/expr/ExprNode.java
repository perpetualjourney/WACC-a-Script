package tree.expr;

import tree.assignments.Assignable;

public abstract class ExprNode extends Assignable  {
	
	/*
	 * The implementation of this class is left empty and abstract, 
	 * and will be used as a super class for concrete expr classes.
	 */
	public abstract int weight();

}
