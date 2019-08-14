package chapter6;

import chapter5.AbstractList;

import java.util.function.Function;

import static chapter5.AbstractList.sum;

/**
 * @author Spencer
 */
public interface AbstractLists {
    /**
     * 计算一个列表的平均值
     * @param list double list
     * @return AbstractOption<Double>
     */
    static AbstractOption<Double> average(AbstractList<Double> list) {
        return list.isEmpty() ?
                AbstractOption.none() :
                AbstractOption.some(sum(list, 0) / list.length());
    }

    /**
     * 计算一个列表的方差
     * @param list double list
     * @return AbstractOption<Double>
     */
    static AbstractOption<Double> variance(AbstractList<Double> list) {
        return average(list).flatMap((Double averageValue) -> average(list.map((Double x) -> Math.pow(x - averageValue, 2))));
    }

    /**
     * 计算一个列表的最大值
     * @param <A> value type
     * @return max value
     */
    static <A extends Comparable> Function<AbstractList<A>, AbstractOption<A>> max() {
        return (AbstractList<A> x) -> x.isEmpty() ?
                AbstractOption.none() :
                AbstractOption.some(x.foldLeft(x.head(), (A a) -> (A b) -> a.compareTo(b) > 0 ? a : b));
    }

    /**
     * 将一个AbstractList<AbstractOption<A>>转化为AbstractOption<AbstractList<A>>
     * @param list AbstractList<AbstractOption<A>>
     * @param <A>  value type
     * @return AbstractOption<AbstractList < A>>
     */
    static <A> AbstractOption<AbstractList<A>> sequence(AbstractList<AbstractOption<A>> list) {
        return traverse(list, x -> x);
    }

    /**
     * 将一个AbstractList<A>转化为AbstractOption<AbstractList<B>>，并对value使用A到AbstractOption<B>的function
     * @param list AbstractList<A>
     * @param f    function A to AbstractOption<B>
     * @param <A>  type A
     * @param <B>  return value type B
     * @return AbstractOption<AbstractList < B>>
     */
    static <A, B> AbstractOption<AbstractList<B>> traverse(AbstractList<A> list, Function<A, AbstractOption<B>> f) {
        return list.foldRight(AbstractOption.some(AbstractList.list()),
                (A x) -> (AbstractOption<AbstractList<B>> y) ->
                        AbstractOptions.map2(f.apply(x), y, (B a) -> (AbstractList<B> b) -> b.construct(a)));
    }
}
