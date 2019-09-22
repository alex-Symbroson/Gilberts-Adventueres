/*
 * TODO This is missing a JFlex license, maybe? Probably not. I hope.
 */
package script;

import script.Token.TokenType;

%%

%public
%class ScriptLexer
%public
%final
%type Token
%unicode
%char

%{

    StringBuilder string = new StringBuilder();

    private Token token(TokenType type)
    {
        return new Token(type, yychar);
    }
    
    private Token text_token(TokenType type)
    {
        return new Token(type, yychar, yytext());
    }
    
    private Token token(TokenType type, Object value)
    {
        return new Token(type, yychar, value);
    }
%}

/* main character classes */
LineTerminator = \r|\n|\r\n

Whitespace = {LineTerminator} | [ \t\f]+

/* identifiers */

Identifier = [:jletter:][:jletterdigit:]*

/* int literals */

IntLiteral = 0 | [1-9][0-9]*

%state STRING

%%

<YYINITIAL> {

  /* keywords */
  
  "if"      { return token(TokenType.IF); }
  "else"    { return token(TokenType.ELSE); }

  /* builtins */

  "visible" |
  "state"       { return text_token(TokenType.BUILTIN_VAR); }
  "give" |
  "has" |
  "take" |
  "text" |
  "warp"        { return text_token(TokenType.BUILTIN_FUNC); }

  /* literals */
  
  {IntLiteral}
            { return token(TokenType.INT, Integer.parseInt(yytext())); }
  \'        { string.setLength(0); yybegin(STRING); }

  /* separators */
  
  "."           { return token(TokenType.PERIOD); }
  "="           { return token(TokenType.ASSIGN); }
  "("           { return token(TokenType.LPAREN); }
  ")"           { return token(TokenType.RPAREN); }
  "{"           { return token(TokenType.LBRACE); }
  "}"           { return token(TokenType.RBRACE); }

  /* operators */
  "+" | "-" | "*" | "/" | "%" |
  "~" |
  "|" | "&" | "^" |
  "==" | "!=" | "<" | "<=" | ">" | ">="
                { return text_token(TokenType.OP); }

  {Identifier}  { return text_token(TokenType.IDENTIFIER); }
  
  {Whitespace}  { }
}

<STRING> {
  \'            { yybegin(YYINITIAL); return token(TokenType.STRING, string.toString()); }
  [^\'\\]+      { string.append( yytext() ); }
  \\t           { string.append('\t'); }
  \\n           { string.append('\n'); }
  \\r           { string.append('\r'); }
  \\\'          { string.append('\''); }
  \\            { string.append('\\'); }
}


/* error fallback */
[^]                              {  }
<<EOF>>                          { return Token.EOF; }