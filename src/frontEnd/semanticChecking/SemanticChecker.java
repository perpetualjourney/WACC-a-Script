package semanticChecking;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;

import symboltable.SymbolTable;
import tree.ProgNode;
import tree.WACCTree;
import tree.assignments.*;
import tree.expr.*;
import tree.func.*;
import tree.stat.*;
import tree.type.*;
import util.DebugHelper;
import WACCExceptions.*;
import antlr.WACCParserBaseVisitor;
import antlr.WACCParser.*;

// library used for debugging
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;

public class SemanticChecker extends WACCParserBaseVisitor<WACCTree> {

	public static final DebugHelper dbh = new DebugHelper();
	private ParseTree parseTree;
	private SymbolTable currentSymbolTable;
	private SymbolTable functionSymbolTable;
	private WACCTree progTree;

	public SemanticChecker(ParseTree t) {
		this.parseTree = t;
		this.currentSymbolTable = new SymbolTable();
		this.functionSymbolTable = new SymbolTable();
	}
	
	public SemanticChecker(ParseTree t, SymbolTable st) {
		this.parseTree = t;
		this.currentSymbolTable = st;
	}

	public void init() {
 		dbh.printV("Checking sematic integrity...");
		progTree = parseTree.accept(this);
		
		//Debugging: prints the WACC tree after semantic checking
		XStream xstream = new XStream(new JsonHierarchicalStreamDriver());
		xstream.setMode(XStream.ID_REFERENCES);
		xstream.alias("WACCTree", WACCTree.class);
		dbh.printD(xstream.toXML(progTree));
	}
	
	public WACCTree getProgTree() {
		return progTree;
	}

	@Override
	public WACCTree visitFunc(FuncContext ctx) {
		String funcName = ctx.ident().getText();
		assert (currentSymbolTable.containsCurrent(funcName));
		FuncDecNode func = (FuncDecNode) currentSymbolTable.get(funcName);

		// Create an inner scope Symbol Table for the function body.
		currentSymbolTable = new SymbolTable(currentSymbolTable, func.getType());
		functionSymbolTable = currentSymbolTable;

		// Add params to current SymbolTable
		ParamListNode paramList = func.getParams();
		registerParams(currentSymbolTable, paramList);

		// Create the functionBody node
		StatNode funcBody = (StatNode) visit(ctx.stat());
		boolean complete = funcBody.checkExpectation();

		// finalise the current symbolTable and restore the parent scope
		currentSymbolTable.finaliseScope(complete, funcName);
		currentSymbolTable = currentSymbolTable.getParent();
		functionSymbolTable = currentSymbolTable;

		// Add function body statement to the function node
		func.addFuncBody(funcBody);
		func.check(functionSymbolTable, currentSymbolTable, ctx);
		return func;
	}

	private void registerParams(SymbolTable st, ParamListNode paramList) {
		for (ParamNode param : paramList) {
			st.add(param.getIdent(), param);
		}
	}

	@Override
	public WACCTree visitReturn_stat(Return_statContext ctx) {
		ExprNode exprType = (ExprNode) visit(ctx.expr());
		ReturnStatNode rst = new ReturnStatNode(exprType);
		rst.check(functionSymbolTable, currentSymbolTable, ctx);

		return rst;
	}

	@Override
	public WACCTree visitSequential_stat(Sequential_statContext ctx) {
		StatNode lhs = (StatNode) visit(ctx.stat(0));
		StatNode rhs = (StatNode) visit(ctx.stat(1));
		
		SeqStatNode seqStat = new SeqStatNode(lhs, rhs);
		seqStat.check(functionSymbolTable, currentSymbolTable, ctx);

		return seqStat;
	}

	@Override
	public WACCTree visitPrint_stat(Print_statContext ctx) {
		ExprNode expr = (ExprNode) visit(ctx.expr());
		PrintStat ps = new PrintStat(expr);
		ps.check(functionSymbolTable, currentSymbolTable, ctx);

		return ps;
	}

	@Override
	public WACCTree visitPrintln_expr(Println_exprContext ctx) {
		ExprNode expr = (ExprNode) visit(ctx.expr());
		PrintLnStat ps = new PrintLnStat(expr);
		ps.check(functionSymbolTable, currentSymbolTable, ctx);

		return ps;
	}

	@Override
	public WACCTree visitRead_stat(Read_statContext ctx) {
		AssignLhsNode lhs = (AssignLhsNode) visit(ctx.assign_lhs());
		ReadStatNode rsn = new ReadStatNode(lhs);
		rsn.check(functionSymbolTable, currentSymbolTable, ctx);

		return rsn;
	}

	@Override
	public WACCTree visitFree_stat(Free_statContext ctx) {
		ExprNode expr = (ExprNode) visit(ctx.expr());
		FreeStat stat = new FreeStat(expr);
		stat.check(functionSymbolTable, currentSymbolTable, ctx);

		return stat;
	}

	@Override
	public WACCTree visitExit_stat(Exit_statContext ctx) {
		ExprNode exitVal = (ExprNode) visit(ctx.expr());
		ExitStat stat = new ExitStat(exitVal);
		stat.check(functionSymbolTable, currentSymbolTable, ctx);

		return stat;
	}

	@Override
	public WACCTree visitSkip_stat(Skip_statContext ctx) {
		SkipStatNode ssn = new SkipStatNode();

		return ssn;
	}

	/*
	 * This method will only be reached in the case of a variable assignment. If
	 * the variable is undeclared we want to throw an exception and stop checking
	 * further. (non-Javadoc)
	 * 
	 * @see
	 * antlr.WACCParserBaseVisitor#visitIdent(antlr.WACCParser.IdentContext)
	 */
	@Override
	public WACCTree visitIdent(IdentContext ctx) {
		String ident = ctx.IDENTITY().getText();
		if (currentSymbolTable.containsRecursive(ident)) {
			WACCTree var = currentSymbolTable.get(ident);
			return var;
		}
		
		throw new UndeclaredIdentifierException("The variable " + ident
				+ " was undefined", ctx);
	}

	@Override
	public WACCTree visitProg(ProgContext ctx) {
		// First we visit all functions and register a stub of its return type
		for (FuncContext fctx : ctx.func()) {
			registerFunction(fctx);
		}

		ArrayList<FuncDecNode> functions = new ArrayList<>();
		// We visit all the functions and create full nodes
		for (FuncContext fctx : ctx.func()) {
			FuncDecNode fdec = (FuncDecNode) visit(fctx);
			fdec.check(functionSymbolTable, currentSymbolTable, ctx);
			functions.add(fdec);
		}

		// Then we visit the statement
		StatNode progBody = (StatNode) visit(ctx.stat());

		// Finally, we return the program node
		return new ProgNode(functions, progBody);
	}

	@Override
	public WACCTree visitVariable_assigment(Variable_assigmentContext ctx) {
		AssignLhsNode lhs = (AssignLhsNode) visit(ctx.assign_lhs());
		Assignable rhs = (Assignable) visit(ctx.assign_rhs());
		AssignStatNode assignment = new AssignStatNode(lhs, rhs);
		assignment.check(functionSymbolTable, currentSymbolTable, ctx);

		return assignment;
	}

	/**
	 * registerFunction adds a stub of the function into the currentSymbolTable
	 * without recursing into the function body. This is to support nested
	 * function calls, where a function body might call a not yet defined
	 * function.
	 * 
	 * @param fctx
	 *            The FuncContext generated by the Parser
	 */
	private void registerFunction(FuncContext fctx) {
		String funcName = fctx.ident().getText();
		if (currentSymbolTable.containsRecursive(funcName)) {
			throw new NotUniqueIdentifierException("Function has already been defined", fctx);
		}
		WACCType returnType = WACCType.evalType(fctx.type());
		Param_listContext paramCtx = fctx.param_list();
		ParamListNode params = null;
		if (paramCtx != null) {
			params = (ParamListNode) visit(fctx.param_list());
		} else {
			params = new ParamListNode();
		}
		FuncDecNode func = new FuncDecNode(returnType, funcName, params);
		currentSymbolTable.add(funcName, func);
	}

	public WACCTree visitVariable_declaration(Variable_declarationContext ctx) {
		Assignable rhsTree = (Assignable) visit(ctx.assign_rhs());
		WACCType varType = WACCType.evalType(ctx.type());
		String ident = ctx.ident().getText();
		VarNode var = new VarNode(varType, ident);
		VarDecNode vdn = new VarDecNode(var, rhsTree);
		vdn.check(functionSymbolTable, currentSymbolTable, ctx);

		return vdn;
	}

	// Pair literals are null by default.
	@Override
	public WACCTree visitPair_liter(Pair_literContext ctx) {
		return new PairLiterNode();
	}

	/*
	 * Rule: `newpair`(expr, expr)
	 */
	@Override
	public WACCTree visitNewpair_assignment(Newpair_assignmentContext ctx) {
		ExprNode fst = (ExprNode) visit(ctx.expr(0));
		ExprNode snd = (ExprNode) visit(ctx.expr(1));
		NewPairNode pair = new NewPairNode(fst, snd);
		pair.check(functionSymbolTable, currentSymbolTable, ctx);
		return pair;
	}

	@Override
	public WACCTree visitFunc_call_assignment(Func_call_assignmentContext ctx) {
		String ident = ctx.ident().getText();
		FuncDecNode funcDef = (FuncDecNode) currentSymbolTable.get(ident);
		ArgListNode args;
		
		//Here we check that the call has arguments
		//if no arguments are present, a new empty arg_list will be made.
		if (ctx.arg_list() == null) {
			args = new ArgListNode();
		} else {
			args = (ArgListNode) visit(ctx.arg_list());
		}
		CallStatNode callStat = new CallStatNode(funcDef, args);
		callStat.check(functionSymbolTable, currentSymbolTable, ctx);
		return callStat;
	}

	@Override
	public WACCTree visitArg_list(Arg_listContext ctx) {
		int argListLength = ctx.expr().size();
		ArgListNode args = new ArgListNode();
		for (int i = 0; i < argListLength; i++) {
			args.add((ExprNode) visit(ctx.expr(i)));
		}
		args.check(functionSymbolTable, currentSymbolTable, ctx);
		return args;
	}

	/*
	 * Rule: 
	 * 		(FST | SND) ident 
	 */
	@Override
	public WACCTree visitPair_elem(Pair_elemContext ctx) {
		String fstOrSnd = ctx.getChild(0).getText();
		ExprNode expr = (ExprNode) visit(ctx.expr());
		PairElemNode pairElem = new PairElemNode(fstOrSnd, expr);
		pairElem.check(functionSymbolTable, currentSymbolTable, ctx);
		return pairElem;
	}

	@Override
	public WACCTree visitArray_liter(Array_literContext ctx) {
		ArrayList<ExprNode> elems = new ArrayList<ExprNode>();

		for (int i = 0; i < ctx.expr().size(); i++) {
			elems.add((ExprNode) visit(ctx.expr(i)));
		}

		ArrayLiterNode arrayLiter = new ArrayLiterNode(elems);
		arrayLiter.check(functionSymbolTable, currentSymbolTable, ctx);
		return arrayLiter;
	}

	@Override
	public WACCTree visitArray_elem(Array_elemContext ctx) {
		String ident = ctx.ident().getText();
		List<ExprContext> exprCtxs = ctx.expr();
		ArrayList<ExprNode> exprs = new ArrayList<ExprNode>();
		for (ExprContext ec : exprCtxs) {
			exprs.add((ExprNode) visit(ec));
		}
		
		VarNode var = (VarNode) currentSymbolTable.get(ident);
		ArrayElemNode arrayElem = new ArrayElemNode(exprs, var);
		return arrayElem;
	}

	@Override
	public WACCTree visitChar_liter(Char_literContext ctx) {
		CharLeaf charleaf = new CharLeaf(ctx.getText());
		charleaf.check(functionSymbolTable, currentSymbolTable, ctx);
		return charleaf;
	}

	@Override
	public WACCTree visitInt_liter(Int_literContext ctx) {
		String intValue = ctx.getText();
		IntLeaf intLeaf = new IntLeaf(intValue);
		intLeaf.check(functionSymbolTable, currentSymbolTable, ctx);
		return intLeaf;
	}

	@Override
	public WACCTree visitBool_liter(Bool_literContext ctx) {
		BoolLeaf boolLeaf = new BoolLeaf(ctx.getText());
		boolLeaf.check(functionSymbolTable, currentSymbolTable, ctx);
		return boolLeaf;
	}

	@Override
	public WACCTree visitStr_liter(Str_literContext ctx) {
		StringLeaf strLeaf = new StringLeaf(ctx.getText());
		strLeaf.check(functionSymbolTable, currentSymbolTable, ctx);
		return strLeaf;
	}

	@Override
	public WACCTree visitParam(ParamContext ctx) {
		WACCType paramType = WACCType.evalType(ctx.type());
		String ident = ctx.ident().getText();
		ParamNode paramNode = new ParamNode(paramType, ident);
		paramNode.check(functionSymbolTable, currentSymbolTable, ctx);
		return paramNode;
	}

	@Override
	public WACCTree visitParam_list(Param_listContext ctx) {
		ParamListNode params = new ParamListNode();
		for (ParamContext p : ctx.param()) {
			ParamNode pn = (ParamNode) visit(p);
			params.add(pn);
		}
		params.check(functionSymbolTable, currentSymbolTable, ctx);
		return params;
	}

	@Override
	public WACCTree visitWhile_stat(While_statContext ctx) {
		currentSymbolTable = new SymbolTable(currentSymbolTable);
		ExprNode loopCond = (ExprNode) visit(ctx.expr());
		StatNode loopBody = (StatNode) visit(ctx.stat());
		
		currentSymbolTable.finaliseScope();
		currentSymbolTable = currentSymbolTable.getParent();
		
		WhileStatNode whileStat = new WhileStatNode(loopCond, loopBody);
		whileStat.check(functionSymbolTable, currentSymbolTable, ctx);

		return whileStat;
	}

	@Override
	public WACCTree visitIf_stat(If_statContext ctx) {
		
		currentSymbolTable = new SymbolTable(currentSymbolTable);
		ExprNode ifCond = (ExprNode) visit(ctx.expr());
		StatNode thenStat = (StatNode) visit(ctx.stat(0));
		StatNode elseStat = (StatNode) visit(ctx.stat(1));
		
		currentSymbolTable.finaliseScope();
		currentSymbolTable = currentSymbolTable.getParent();
		
		IfStatNode ifStat = new IfStatNode(ifCond, thenStat, elseStat);
		ifStat.check(functionSymbolTable, currentSymbolTable, ctx);
		
		return ifStat;
	}

	@Override
	public WACCTree visitExpr(ExprContext ctx) {
		// if it's an atomic `( expr )` expression, we just call visit on the
		// inner expr
		if (ctx.OPEN_PARENTHESES() != null) {
			assert (ctx.children.size() == 3);
			return visit(ctx.expr(0));
		}

		switch (ctx.getChildCount()) {
		case 3: // Binary Expression of type `lhs OP rhs`
			ExprNode lhs = (ExprNode) visit(ctx.expr(0));
			ExprNode rhs = (ExprNode) visit(ctx.expr(1));
			WACCBinOp binaryOp = WACCBinOp.evalBinOp(ctx.getChild(1).getText());
			BinExprNode binExpr = new BinExprNode(lhs, binaryOp, rhs);
			binExpr.check(functionSymbolTable, currentSymbolTable, ctx);

			return binExpr;

		case 2: // Unary Expression of type `OP expr`
			ExprNode expr = (ExprNode) visit(ctx.expr(0));
			WACCUnOp unaryOp = WACCUnOp.evalUnOp(ctx.getChild(0).getText());
			UnExprNode unaryExpr = new UnExprNode(unaryOp, expr);
			unaryExpr.check(functionSymbolTable, currentSymbolTable, ctx);

			return unaryExpr;

		default: // in this case this is a single rule (i.e. int_liter, char_liter)
			return visit(ctx.getChild(0));
		}
	}

	@Override
	public WACCTree visitBlock_stat(Block_statContext ctx) {
		currentSymbolTable = new SymbolTable(currentSymbolTable);
		StatNode stat = (StatNode) visit(ctx.stat());
		currentSymbolTable.finaliseScope();
		currentSymbolTable = currentSymbolTable.getParent();
		return stat;
	}

	/**
	 * @return Returns true iff there were no semantic error in the compiler.
	 */
	public boolean terminate() {
		return WACCException.ERROR_LISTENER.finish();
	}

}
