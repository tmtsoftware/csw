package csw.services.event.utils;

import java.util.Collections;

/**
 * This method is copied from org.apache.sshd.common.file.util.BaseFileSystem#globToRegex(java.lang.String) which is
 * used by java.nio.file.FileSystem#getPathMatcher(java.lang.String). It had to be copied because of restricted access
 */
public class Utils {
    public static String globToRegex(String pattern) {
        StringBuilder sb = new StringBuilder(checkNotNull(pattern, "No pattern").length());
        int inGroup = 0;
        int inClass = 0;
        int firstIndexInClass = -1;
        char[] arr = pattern.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            switch (ch) {
                case '\\':
                    if (++i >= arr.length) {
                        sb.append('\\');
                    } else {
                        char next = arr[i];
                        switch (next) {
                            case ',':
                                // escape not needed
                                break;
                            case 'Q':
                            case 'E':
                                // extra escape needed
                                sb.append("\\\\");
                                break;
                            default:
                                sb.append('\\');
                                break;
                        }
                        sb.append(next);
                    }
                    break;
                case '*':
                    sb.append(inClass == 0 ? ".*" : "*");
                    break;
                case '?':
                    sb.append(inClass == 0 ? '.' : '?');
                    break;
                case '[':
                    inClass++;
                    firstIndexInClass = i + 1;
                    sb.append('[');
                    break;
                case ']':
                    inClass--;
                    sb.append(']');
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    if (inClass == 0 || (firstIndexInClass == i && ch == '^')) {
                        sb.append('\\');
                    }
                    sb.append(ch);
                    break;
                case '!':
                    sb.append(firstIndexInClass == i ? '^' : '!');
                    break;
                case '{':
                    inGroup++;
                    sb.append('(');
                    break;
                case '}':
                    inGroup--;
                    sb.append(')');
                    break;
                case ',':
                    sb.append(inGroup > 0 ? '|' : ',');
                    break;
                default:
                    sb.append(ch);
            }
        }

        return sb.toString();
    }

    private static <T> T checkNotNull(T t, String message) {
        checkTrue(t != null, message);
        return t;
    }

    private static void checkTrue(boolean flag, String message) {
        if (!flag) {
            throwIllegalArgumentException(message, Collections.emptyList());
        }
    }

    private static void throwIllegalArgumentException(String message, Object... args) {
        throw new IllegalArgumentException(String.format(message, args));
    }
}

