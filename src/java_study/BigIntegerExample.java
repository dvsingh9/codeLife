package java_study;

import java.math.BigInteger;

/**
 * @author SpencerCJH
 * @date 2019/4/29 22:07
 */
public class BigIntegerExample {
    public static void main(String[] args) {
        System.out.println(new BigInteger(String.valueOf(66)).modPow(new BigInteger(String.valueOf(77)),
                new BigInteger(String.valueOf(119))));
        System.out.println(new BigInteger(String.valueOf(773)).modPow(new BigInteger("1217"),
                new BigInteger("2773")));
        System.out.println(new BigInteger("128").modPow(new BigInteger("57"),
                new BigInteger("2773")));
    }

    /**
     * a**i mod b
     *
     * @param a=66
     * @param b=119
     * @param i=77
     * @return ans=19
     */
    @Deprecated
    private static int fun(int a, int b, int i) {
        int sum = 1;
        while (i != 0) {
            int temp = a * a % b;
            sum *= (i % 2 == 0 ? 1 : a) % b;
            a = temp;
            i /= 2;
        }
        return sum % b;
    }
}
