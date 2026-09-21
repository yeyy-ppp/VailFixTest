package codeing;

public class Fix {
    public int add(int a, int b) {
        return a + b;
    }
    public char charAt(String str, int index) { return str.charAt(index); }
    public int divide(int dividend, int divisor) { if (divisor == 0) { throw new ArithmeticException("Divisor must not be zero"); }return dividend / divisor; }
}
