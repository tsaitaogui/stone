package stone;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
    public static String regexPat
        = "\\s*((//.*)|([0-9]+)|(\"(\\\\\"|\\\\\\\\|\\\\n|[^\"])*\")"
          + "|[A-Z_a-z][A-Z_a-z0-9]*|==|<=|>=|&&|\\|\\||\\p{Punct})?";
    private Pattern pattern = Pattern.compile(regexPat);
    private ArrayList<Token> queue = new ArrayList<Token>();
    private boolean hasMore;
    private LineNumberReader reader;

    public Lexer(Reader r) {
        hasMore = true;
        reader = new LineNumberReader(r);
    }

    public Token read() throws ParseException {
        if (fillQueue(0)) {
            return queue.remove(0);
        } else {
            return Token.EOF;
        }
    }

    public Token peek(int i) throws ParseException {
        if (fillQueue(i)) {
            return queue.get(i);
        } else {
            return Token.EOF; 
        }
    }

    private boolean fillQueue(int i) throws ParseException {
        while (i >= queue.size()) {
            if (hasMore) {
                readLine();
            } else {
                return false;
            }
        }
        return true;
    }

    protected void readLine() throws ParseException {
        final String line;
        try {
            line = reader.readLine();
        } catch (IOException e) {
            throw new ParseException(e);
        }
        if (line == null) {
            hasMore = false;
            return;
        } else {
            final int lineNo = reader.getLineNumber();
            Matcher matcher =
                    pattern.matcher(line)
                        .useTransparentBounds(true)
                        .useAnchoringBounds(false);
            int pos = 0;
            final int endPos = line.length();
            while (pos < endPos) {
                matcher.region(pos, endPos);
                if (matcher.lookingAt()) {
                    addToken(lineNo, matcher);
                    pos = matcher.end();
                } else {
                    throw new ParseException("bad token at line " + lineNo);
                }
            }
            queue.add(new IdentifierToken(lineNo, Token.EOL));
        }
    }

    protected void addToken(int lineNo, Matcher matcher) {
        final String m = matcher.group(1);
        if (m != null) {  // if not a space
            if (matcher.group(2) == null) { // if not a comment
                final Token token;
                if (matcher.group(3) != null) {
                    token = new NumToken(lineNo, Integer.parseInt(m));
                } else if (matcher.group(4) != null) {
                    token = new StrToken(lineNo, toStringLiteral(m));
                } else {
                    token = new IdentifierToken(lineNo, m);
                }
                queue.add(token);
            }
        }
    }

    protected String toStringLiteral(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1, len = s.length() - 1; i < len; i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < len) {
                char c2 = s.charAt(i + 1);
                if (c2 == '"' || c2 == '\\')
                    c = s.charAt(++i);
                else if (c2 == 'n') {
                    ++i;
                    c = '\n';
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    protected static class NumToken extends Token {
        private int value;

        protected NumToken(int line, int v) {
            super(line);
            value = v;
        }

        public boolean isNumber() { return true; }
        public String getText() { return Integer.toString(value); }
        public int getNumber() { return value; }
    }

    protected static class IdentifierToken extends Token {
        private String text; 

        protected IdentifierToken(int line, String id) {
            super(line);
            text = id;
        }

        public boolean isIdentifier() { return true; }
        public String getText() { return text; }
    }

    protected static class StrToken extends Token {
        private String literal;

        StrToken(int line, String str) {
            super(line);
            literal = str;
        }

        public boolean isString() { return true; }
        public String getText() { return literal; }
    }
}
