package codeing;

public class Add {
    public static int countChar(String str, char ch) {
        if (str == null) {
            return 0;
        }
        int cnt = 0;
        for (char c : str.toCharArray()) {
            if (c == ch) cnt++;
        }
        return cnt;
    }
    public String reverse(String input) {
        if (input == null) {
            return "";
        }
        return new StringBuilder(input).reverse().toString();
    }
    public char charAt(String str, int index) {
        if (str == null || index <= 0 || index > str.length()) {
            throw new IllegalArgumentException("index out of range");
        }
        return str.charAt(index - 1);
    }
    private int maxLength(String[] arr) {
        if (arr == null || arr.length == 0) return 0;
        int max = 0;
        for (String s : arr) {
            if (s != null && s.length() > max) max = s.length();
        }
        return max;
    }
}
