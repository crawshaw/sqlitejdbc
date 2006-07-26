package test;

public class Test
{
    public static final boolean STOP_ON_FAIL = true;

    private static final Case[] tests = new Case[] {
        new Test01(), new Test02(), new Test03(), new Test04(), new Test05(),
        new Test06(), new Test07(), new Test08(), new Test09(), new Test10(),
        new Test11(), new Test12()
    };

    public static void main(String[] args) {
        System.out.println("Running " + tests.length + " tests:");
        int ran = 0;
        int pass = 0;

        for (int i=0; i < tests.length; i++) {
            System.out.print(" Test ");
            if (i < 9) System.out.print(' ');
            System.out.print(i + 1);
            System.out.print("  " + tests[i].name() + "... ");
            for (int j=tests[i].name().length(); j < 30; j++)
                System.out.print(' ');

            ran++;
            boolean res = false;
            long startAt = System.currentTimeMillis();

            try {
                res = tests[i].run();
            } catch (Exception e) {
                System.out.println("FAIL");
                System.out.println("  unexpected exception:");
                e.printStackTrace();
                if (STOP_ON_FAIL) break;
            }

            if (res) {
                String time = String.valueOf(
                    System.currentTimeMillis() - startAt);

                System.out.print("pass ");
                for (int j=time.length(); j < 5; j++)
                    System.out.print(' ');
                System.out.print(time);
                System.out.println("ms");
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
