    script = { statement } ;
    statement = "{" , { statement } ,  "}"
        | var , "=" , expr
        | ( "give' | "take" | "warp" ) , "(" , IDENTIFIER , ")"
        | "text" , "(" , STRING , ")"
        | if_stmt ;
    var = IDENTIFIER
        | IDENTIFIER , "." , IDENTIFIER , "." "visible"
        | IDENTIFIER , "." , [ IDENTIFIER , "." ] , "state" ;
    if_stmt = "if" , "(" , expr , ")" , statement , [ "else" , statement ] ;
    expr = INT
        | var
        | ("-" | "~") , expr
        | "(" , expr , ")"
        | expr , BIN_OP , expr
        | ("has" | "take") , "(" , IDENTIFIER , ")" ;
        
    IDENTIFIER = [_$a-zA-Z][_$0-9a-zA-Z]*
    STRING = '[^\n]*'
    INT = 0 | [1-9][0-9]*
    BIN_OP = [+-&|^*/%<>] | [=!<>]=
    