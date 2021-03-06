package top.spencercjh.chapter2.singleton.type1;

/**
 * @author SpencerCJH
 * @date 2019/10/17 19:41
 */
class SingletonOneInJava {
    private static SingletonOneInJava INSTANCE = new SingletonOneInJava();

    private SingletonOneInJava() {
    }

    public static SingletonOneInJava getInstance() {
        return INSTANCE;
    }
}
