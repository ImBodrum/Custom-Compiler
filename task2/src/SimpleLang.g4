grammar SimpleLang;

prog : dec+ EOF;

dec
    : typed_idfr LParen (typed_idfr (Comma typed_idfr)*)? RParen body
;


typed_idfr
    : type Idfr
;

type
    : IntType | BoolType | UnitType
;

body
    : LBrace ene+=exp (Semicolon ene+=exp)* RBrace Semicolon?
;



block
    : LBrace ene+=exp (Semicolon ene+=exp)* RBrace
;



exp
    : Idfr Assign exp                                       #AssignExpr
    | type Idfr Assign exp                                  #VarDeclInitExpr
    | LParen exp binop exp RParen                           #BinOpExpr
    | Idfr LParen (args+=exp (Comma args+=exp)*)? RParen    #InvokeExpr
    | block                                                 #BlockExpr
    | Repeat block Until exp                           #RepeatUntilExpr
    | If exp Then block Else block                          #IfExpr
    | While exp Do block                                    #WhileExpr
    | Print exp                                             #PrintExpr
    | Space                                                 #SpaceExpr
    | Idfr                                                  #IdExpr
    | IntLit                                                #IntExpr
    | BoolLit                                               #BoolExpr
    | Skip                                                  #SkipExpr
;

While : 'while' ;
Do : 'do' ;
Repeat : 'repeat' ;
Until : 'until' ;
Skip: 'skip';
binop
    : Eq              #EqBinop
    | Less            #LessBinop
    | LessEq          #LessEqBinop
    | Greater         #GreaterBinop
    | GreaterEq       #GreaterEqBinop
    | Plus            #PlusBinop
    | Minus           #MinusBinop
    | Times           #TimesBinop
    | And             #AndBinop
;

LParen : '(' ;
Comma : ',' ;
RParen : ')' ;
LBrace : '{' ;
Semicolon : ';' ;
RBrace : '}' ;

Eq : '==' ;
Less : '<' ;
LessEq : '<=' ;
Greater : '>' ;
GreaterEq : '>=' ;

Plus : '+' ;
Times : '*' ;
Minus : '-' ;
And : '&' ;

Assign : ':=' ;

Print : 'print' ;
Space : 'space' ;
NewLine : 'newline' ;
If : 'if' ;
Then : 'then' ;
Else : 'else' ;

IntType : 'int' ;
BoolType : 'bool' ;
UnitType : 'unit' ;

BoolLit : 'true' | 'false' ;
IntLit : '0' | ('-'? [1-9][0-9]*) ;
Idfr : [a-z][A-Za-z0-9_]* ;
WS : [ \n\r\t]+ -> skip ;