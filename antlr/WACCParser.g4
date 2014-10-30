parser grammar WACCParser;

options {
  tokenVocab=WACCLexer;
}

func : type ident OPEN_PARANTHESES 
	  (param_list)? CLOSE PARENTHESES IS stat END ;

stat : SKIP
| type ident EQUALS assign_rhs
| assign_lhs EQUALS assign_rhs
| READ assign_lhs
| FREE expr
| RETURN expr
| EXIT expr
| PRINT expr
| PRINTLN expr
| IF expr THEN stat ELSE stat ENDIF
| WHILE expr DO stat DONE
| BEGIN stat END
| stat SEMI_COLON stat
;

assign_lhs : ident
| array_elem
| pair_elem
;

assign_rhs : expr
| array_liter
| NEWPAIR OPEN_PARENTHESES expr COMMA expr CLOSE_PARENTHESES
| pair_elem
| CALL ident OPEN_PARENTHESES (arg_list)? CLOSE_PARENTHESES
;

arg_list : expr (COMMA (expr))* ;

pair_elem: FST expr
| SND expr
;

binaryOper : PLUS | MINUS ;

param_list: param
| param COMMA param_list
;

param: type ident;

type: base_type
| array_type
| pair_type
;

base_type: INT
| BOOL
| CHAR
| STRING
;

array_type: type OPEN_SQUARE_BRACKET CLOSE_SQUARE_BRACKET ;

pair_type: PAIR OPEN_PARENTHESES pair_elem_type COLON pair_elem_type CLOSE_PARENTHESES ;

expr : int_liter
| bool_liter
| char_liter
| str_liter
| pair_liter
| ident
| array_elem
| unary_oper expr
| expr binary_oper expr
| OPEN_PARENTHESES expr CLOSE PARENTHESES
;

array_elem : ident OPEN_SQUARE_BRACKET expr CLOSE_SQUARE BRACKET ;

int-liter : int_sign INTEGER ;

int_sign : PLUS
| MINUS
;

bool_liter : TRUE
| FALSE
;

char_liter : APOSTROPHE character APOSTROPHE ;

str_liter : DOUBLE_APOSTROPHE (character)* DOUBLE_APOSTROPHE ;

pair_liter : NULL ;

comment : HASH_TAG (character ^EOL)* EOL ;

// EOF indicates that the program must consume to the end of the input.
prog: (expr)*  EOF ;