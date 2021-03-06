package JSTree;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import JSTree.JSProg;
import JSTree.JSTree;
import tree.ProgNode;
import tree.WACCTree;
import tree.assignments.*;
import tree.assignments.PairElemNode.ORDER;
import tree.expr.*;
import tree.func.*;
import tree.stat.*;
import tree.type.WACCUnOp;
import visitor.WACCTreeVisitor;
import JSTree.assignable.*;
import JSTree.expr.*;
import JSTree.func.*;
import JSTree.stat.*;

public class WACCTreeToJsTree extends WACCTreeVisitor<JSTree> {

	private WACCTree progTree;
	private HashMap<String, String> funcDeps;
	private HashMap<String, JSFunc> symboltable;
	
	public WACCTreeToJsTree(WACCTree progTree) {
		this.progTree = progTree;
		this.symboltable = new HashMap<>();
	}
	
	public WACCTreeToJsTree(WACCTree progTree, HashMap<String, String> funcDeps) {
		this(progTree);
		this.funcDeps = funcDeps;
	}

	public static final JSTree EMPTY_NODE = new JSStat() {
		@Override
		public String toCode() {
			return "";
		}
	};
	
	@Override
	public JSTree visit(WACCTree node) {
		return node.accept(this);
	}

	public String init(String corePath) {
		JSProg finalTree = (JSProg) progTree.accept(this);
		finalTree.setCorePath(corePath);
		return finalTree.toCode();
	}

	@Override
	public JSTree visitAssignStatNode(AssignStatNode node) {
		JSTree lhs = visit(node.getLhs());
		JSTree rhs = visit(node.getRhs());
		if( lhs instanceof JSArrayElem 
			&& ((JSArrayElem) lhs).getType().equals("char") ) {
			String var = ((ArrayElemNode) node.getLhs()).getVar().getIdent();
			int index = ((JSInt) ((JSArrayElem) lhs).getLocations().get(0)).getVal();
			String c = ((JSChar) rhs).getText();
			return new JSChangeString(var, index, c);
		}
		return new JSAssignStat(lhs, rhs);
	}

	@Override
	public JSTree visitBlockStatNode(BlockStatNode node) {
		JSStat stat = (JSStat) visit(node.getStat());
		return new JSBlockStat(stat);
	}

	@Override
	public JSExitStat visitExitStat(ExitStat node) {
		JSExpr expr = (JSExpr) visit(node.getExpr());
		return new JSExitStat(expr);
	}

	@Override
	public JSTree visitFreeStat(FreeStat node) {
		return EMPTY_NODE;
	}

	@Override
	public JSPrintLn visitPrintLnStat(PrintLnStat node) {
		JSExpr expr = (JSExpr) visit(node.getExpr());
		return new JSPrintLn(expr);
	}

	@Override
	public JSPrint visitPrintStat(PrintStat node) {
		JSExpr expr = (JSExpr) visit(node.getExpr());
		return new JSPrint(expr);
	}

	@Override
	public JSReadStat visitReadStatNode(ReadStatNode node) {
		JSTree lhs = visit(node.getLhs());
		return new JSReadStat(lhs);
	}

	@Override
	public JSReturnStat visitReturnStatNode(ReturnStatNode node) {
		JSTree expr = visit(node.getExpr());
		return new JSReturnStat(expr);
	}

	@Override
	public JSSeqStat visitSeqStatNode(SeqStatNode node) {
		JSStat lhs = (JSStat) visit(node.getLhs());
		JSStat rhs = (JSStat) visit(node.getRhs());
		return new JSSeqStat(lhs, rhs);
	}

	@Override
	public JSTree visitSkipStatNode(SkipStatNode node) {
		return EMPTY_NODE;
	}

	@Override
	public JSVarDec visitVarDecNode(VarDecNode node) {
		JSExpr var = (JSExpr) visit(node.getVar());
		JSTree rhs = visit(node.getRhsTree());
		return new JSVarDec(var, rhs);
	}

	@Override
	public JSIfStat visitIfStatNode(IfStatNode node) {
		JSExpr condition = (JSExpr) visit(node.getIfCond());
		JSStat thenStat = (JSStat) visit(node.getThenStat());
		JSStat elseStat = (JSStat) visit(node.getElseStat());
		return new JSIfStat(condition, thenStat, elseStat);
	}

	@Override
	public JSWhileStat visitWhileStatNode(WhileStatNode node) {
		JSExpr condition = (JSExpr) visit(node.getLoopCond());
		JSStat loopBody = (JSStat) visit(node.getLoopBody());
		return new JSWhileStat(condition, loopBody);
	}
	


	@Override
	public JSTree visitProgNode(ProgNode node) {
		List<JSFunc> functions = new ArrayList<>();
		for(FuncDecNode f:node.getFunctions()) {
			JSFunc jsf = visitFuncDecNode(f);
			symboltable.put(f.getFuncName(), jsf);
			functions.add(jsf);
		}
		
		JSStat body = (JSStat) visit(node.getProgBody());
		
		return new JSProg(functions, body, funcDeps);
	}
	
	
	

	@Override
	public JSFunc visitFuncDecNode(FuncDecNode node) {
		JSParamList params = visitParamListNode(node.getParams());
		String funcName = node.getFuncName();
		JSStat body = (JSStat) visit(node.getFuncBody());
		
		return new JSFunc(funcName, params, body);
	}

	@Override
	public JSParamList visitParamListNode(ParamListNode node) {
		List<JSParam> params = new ArrayList<>();
		for (ParamNode n:node) {
			JSParam p = visitParamNode(n);
			params.add(p);
		}
		
		return new JSParamList(params);
	}

	@Override
	public JSParam visitParamNode(ParamNode node) {
		String ident = node.getIdent();
		return new JSParam(ident);
	}

	@Override
	public JSTree visitCallStatNode(CallStatNode node) {
		JSArgList args = visitArgListNode(node.getArgs());
		String functionName = node.getIdent();
		
		// This block strips down the dependency name from the path
		if (funcDeps.containsKey(functionName)
				&& !symboltable.containsKey(functionName)) {
			String filePath = funcDeps.get(functionName);
			String dep = new File(filePath).getName();
			dep = dep.substring(0, dep.lastIndexOf('.'));
			return new JSLibFuncCall(dep, functionName, args);
		} else if ( symboltable.containsKey(functionName)) {
			return new JSFuncCall(symboltable.get(functionName), args);
		}
		
		return new JSFuncCall(functionName, args, symboltable);
	}

	@Override
	public JSBinExpr visitBinExprNode(BinExprNode node) {
		JSExpr lhs = (JSExpr) visit(node.getLhs());
		String op = node.getOperator().toString();
		JSExpr rhs = (JSExpr) visit(node.getRhs());
		return new JSBinExpr(lhs, op, rhs);
				
	}

	@Override
	public JSChar visitCharLeaf(CharLeaf node) {
		String c = node.getText();
		return new JSChar(c);
	}

	@Override
	public JSBool visitBoolLeaf(BoolLeaf node) {
		boolean b = node.getValue();
		return new JSBool(b);
	}

	@Override
	public JSInt visitIntLeaf(IntLeaf node) {
		int i = Integer.parseInt(node.getValue());
		return new JSInt(i);
	}

	@Override
	public JSString visitStringLeaf(StringLeaf node) {
		String s = node.toString();
		return new JSString(s);
	}

	@Override
	public JSTree visitUnExprNode(UnExprNode node) {
		JSExpr expr = (JSExpr) visit(node.getExpr());
		WACCUnOp op = node.getOperator();
		return op.applyJS(expr);
	}

	@Override
	public JSVar visitVarNode(VarNode node) {
		String ident = node.getIdent();
		return new JSVar(ident);
	}
	
	@Override
	public JSArgList visitArgListNode(ArgListNode node) {
		List<JSExpr> args = new ArrayList<>();
		
		for (ExprNode n:node) {
			JSExpr e = (JSExpr) visit(n);
			args.add(e);
		}
		
		return new JSArgList(args);
	}

	@Override
	public JSTree visitPairLeaf(PairLeaf node) {
		String ident = node.getIdent();
		return new JSPair(ident);
	}

	@Override
	public JSNull visitPairLiterNode(PairLiterNode node) {
		// PairLiter nodes are always null
		return new JSNull();
	}


	@Override
	public JSNewPair visitNewPairNode(NewPairNode node) {
		JSExpr fst = (JSExpr) visit(node.getFst());
		JSExpr snd = (JSExpr) visit(node.getSnd());
		return new JSNewPair(fst, snd);
	}

	@Override
	public JSPairElem visitPairElemNode(PairElemNode node) {
		JSExpr expr = (JSExpr) visit(node.getExpr());
		ORDER order = node.getOrder();
		return new JSPairElem(expr, order);
	}

	@Override
	public JSTree visitArrayElemNode(ArrayElemNode node) {
		JSVar var = (JSVar) visit(node.getVar());
		ArrayList<ExprNode> locations = node.getLocations();
		ArrayList<JSExpr> jsLocations = new ArrayList<JSExpr>();
		for(ExprNode en : locations) {
			jsLocations.add((JSExpr) visit(en));
		}
		JSArrayElem arrayElem = new JSArrayElem(jsLocations, var, node.getType().toString());
		return arrayElem;
	}
	
	@Override
	public JSTree visitArrayLiterNode(ArrayLiterNode node) {
		ArrayList<JSExpr> elems = new ArrayList<JSExpr>();
		ArrayList<ExprNode> elemNodes = node.getElems();
		for (ExprNode expr : elemNodes) {
			elems.add((JSExpr) visit(expr));
		}
		return new JSArrayLiter(elems);
	}
	
}
