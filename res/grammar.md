    script = { cmp } ;
    expr = { "+" | "-" | "~" | "!" } , (
        "(" , cmp , ")"
        | "{" , { cmp } , "}"
        | INT
        | var , [ "=" , cmp ]
        | ( "give" | "has" | "take" | "warp" ) , "(" , IDENTIFIER , ")"
        | "text" , "(" , STRING , ")"
        | if_expr
        | while_expr
    ) ;
    var = IDENTIFIER
        | IDENTIFIER , "." , IDENTIFIER , "." "visible"
        | IDENTIFIER , "." , [ IDENTIFIER , "." ] , "state" ;
    if_expr = "if" , "(" , cmp , ")" , cmp , [ "else" , cmp ] ;
    while_expr = "while" , "(" , cmp , ")" , cmp ;
    
    cmp = sum , [ CMP_OP , sum ] ;
    sum = product , { SUM_OP , product } ;
    product = expr , { PROD_OP , expr } ;
        
    IDENTIFIER = [_$a-zA-Z][_$0-9a-zA-Z]*
        "_" and "state" are reserved and cannot be used as identifiers
    STRING = '[^\n]*'
    INT = 0 | [1-9][0-9]*
    CMP_OP = [<>] | [=!<>]=
    SUM_OP = [+-&|^]
    PROD_OP = [*/%]