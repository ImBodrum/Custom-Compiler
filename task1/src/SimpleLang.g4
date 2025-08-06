grammar SimpleLang;

prog : dec+ EOF;
/*
dec
    : typed_idfr LParen (typed_idfr (Comma typed_idfr)*)? RParen body
;
*/
dec
    : typed_idfr LParen typed_idfrList? RParen body
;

typed_idfr
    : type Idfr
;
typed_idfrList
    : typed_idfr (Comma typed_idfr)*
;

type
    : IntType | BoolType | UnitType
;

body
    : LBrace stmt* RBrace
    ;
stmt
    : varDecl
    | exp (Semicolon)?
    ;
varDecl
    : type Idfr Assign exp Semicolon
    ;

block
    : LBrace ene+=exp (Semicolon ene+=exp)* RBrace
;


exp
    :Idfr Assign exp                                       #AssignExpr
    | LParen exp binop exp RParen                           #BinOpExpr
    | Idfr LParen (args+=exp (Comma args+=exp)*)? RParen    #InvokeExpr
    | block                                                 #BlockExpr
    | If exp Then block Else block                          #IfExpr
    | While exp Do block                                    #WhileExpr
    | Repeat block Until exp                                 #RepeatUntilExpr
    | Print exp                                             #PrintExpr
    | Space                                                 #SpaceExpr
    | Idfr                                                  #IdExpr
    | IntLit                                                #IntExpr
    | BoolLit                                               #BoolExpr
    | NewLine                                               #WhileExpr
    | Skip                                                #SkipExpr
;
While : 'while' ;
Do : 'do' ;
Skip: 'skip';
Repeat : 'repeat';
Until : 'until';

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
    | Div             #DivBinop
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
Greater : '>';
GreaterEq : '>=';

Plus : '+' ;
Times : '*' ;
Minus : '-' ;
And : '&' ;
Assign : ':=' ;
Div : '/';

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