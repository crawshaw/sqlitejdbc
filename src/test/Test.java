package test;

public class Test
{
    public static final boolean STOP_ON_FAIL = true;

    private static final Case[] tests = new Case[] {
        new Test1(), new Test2(), new Test3(), new Test4(), new Test5(),
        new Test6(), new Test7(), new Test8(), new Test9()
    };

    public static void main(String[] args) {
        System.out.println("Running " + tests.length + " tests:");
        int ran = 0;
        int pass = 0;

        for (int i=0; i < tests.length; i++) {
            System.out.print(" test " + tests[i].name() + "... ");
            ran++;
            boolean res = false;

            try {
                res = tests[i].run();
            } catch (Exception e) {
                System.out.println("FAIL");
                System.out.println("  unexpected exception:");
                e.printStackTrace();
                if (STOP_ON_FAIL) break;
            }

            if (res) {
                System.out.println("pass");
                pass++;
            } else {
                System.out.println("FAIL");
                System.out.println("   error: " + tests[i].error());
                if (tests[i].ex() != null) tests[i].ex().printStackTrace();
                if (STOP_ON_FAIL) break;
            }
        }

        System.out.println("");
        System.out.println(
            "Ran "+ ran +" tests, "+ pass +" pass, "+ (ran - pass) + " fail.");
    }

    public static interface Case
    {
        /** Returns true if test successful, false if a predicatble
         *  error/exception occurs, or throws if unexpected. */
        public boolean run() throws Exception;

        /** Name of test case. */
        public String name();

        /** Description of error if run() returns false. */
        public String error();

        /** Expected Exception if run() returns false. */
        public Exception ex();
    }
}
