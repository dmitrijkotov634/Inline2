package com.wavecat.inline;

import java.util.LinkedList;
import java.util.List;


public class ArgumentTokenizer {
    private static final int NO_TOKEN_STATE = 0;
    private static final int NORMAL_TOKEN_STATE = 1;
    private static final int SINGLE_QUOTE_STATE = 2;
    private static final int DOUBLE_QUOTE_STATE = 3;

    public static List<String> tokenize(String arguments) {
        return tokenize(arguments, false);
    }

    public static List<String> tokenize(String arguments, boolean stringify) {

        LinkedList<String> argList = new LinkedList<>();
        StringBuilder currArg = new StringBuilder();
        boolean escaped = false;
        int state = NO_TOKEN_STATE;
        int len = arguments.length();

        for (int i = 0; i < len; i++) {
            char c = arguments.charAt(i);
            if (escaped) {
                escaped = false;
                currArg.append(c);
            } else {
                switch (state) {
                    case SINGLE_QUOTE_STATE:
                        if (c == '\'') {
                            state = NORMAL_TOKEN_STATE;
                        } else {
                            currArg.append(c);
                        }
                        break;
                    case DOUBLE_QUOTE_STATE:
                        if (c == '"') {
                            state = NORMAL_TOKEN_STATE;
                        } else if (c == '\\') {
                            i++;
                            char next = arguments.charAt(i);
                            if (next != '"' && next != '\\') {
                                currArg.append(c);
                            }
                            currArg.append(next);
                        } else {
                            currArg.append(c);
                        }
                        break;
                    case NO_TOKEN_STATE:
                    case NORMAL_TOKEN_STATE:
                        switch (c) {
                            case '\\':
                                escaped = true;
                                state = NORMAL_TOKEN_STATE;
                                break;
                            case '\'':
                                state = SINGLE_QUOTE_STATE;
                                break;
                            case '"':
                                state = DOUBLE_QUOTE_STATE;
                                break;
                            default:
                                if (!Character.isWhitespace(c)) {
                                    currArg.append(c);
                                    state = NORMAL_TOKEN_STATE;
                                } else if (state == NORMAL_TOKEN_STATE) {
                                    argList.add(currArg.toString());
                                    currArg = new StringBuilder();
                                    state = NO_TOKEN_STATE;
                                }
                        }
                        break;
                    default:
                        throw new IllegalStateException("ArgumentTokenizer state " + state + " is invalid!");
                }
            }
        }

        if (escaped) {
            currArg.append('\\');
            argList.add(currArg.toString());
        } else if (state != NO_TOKEN_STATE) {
            argList.add(currArg.toString());
        }

        if (stringify) {
            for (int i = 0; i < argList.size(); i++) {
                argList.set(i, "\"" + _escapeQuotesAndBackslashes(argList.get(i)) + "\"");
            }
        }

        return argList;
    }

    protected static String _escapeQuotesAndBackslashes(String s) {
        final StringBuilder buf = new StringBuilder(s);

        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if ((c == '\\') || (c == '"')) {
                buf.insert(i, '\\');
            } else if (c == '\n') {
                buf.deleteCharAt(i);
                buf.insert(i, "\\n");
            } else if (c == '\t') {
                buf.deleteCharAt(i);
                buf.insert(i, "\\t");
            } else if (c == '\r') {
                buf.deleteCharAt(i);
                buf.insert(i, "\\r");
            } else if (c == '\b') {
                buf.deleteCharAt(i);
                buf.insert(i, "\\b");
            } else if (c == '\f') {
                buf.deleteCharAt(i);
                buf.insert(i, "\\f");
            }
        }

        return buf.toString();
    }
}