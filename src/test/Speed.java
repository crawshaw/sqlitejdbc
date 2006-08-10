package test;

import java.io.File;

public class Speed
{
    private static final String DBFILE = "build/test/speed.db";
    private static final int loops = 100;

    private static final Case[] tests = new Case[] {
        new Speed01(), new Speed02()
    };

    public static void main(String[] args) throws Exception {
        Test.main(args);

        Class.forName("org.sqlite.JDBC");
        System.out.println("");
        System.out.println("--- Speed Tests -------------------------");
        System.out.println("Memory: (transactions)"); run("", false);
        System.out.println("Memory:"); run("", true);
        System.out.println("File: (transactions)"); run(DBFILE, false);
        if (args.length > 0 && "full".equals(args[0])) {
            System.out.println("File:"); run(DBFILE, true);
        }
    }

    private static void run(String dbfile, boolean autoCom) throws Exception {
        for (int i=0; i < tests.length; i++) {
            long start = System.currentTimeMillis();
            for (int j=0; j < loops; j++) tests[i].run(dbfile, autoCom);
            String time = String.valueOf(
                (System.currentTimeMillis() - start) / loops);

            System.out.print(" Test ");
            if (i < 9) System.out.print(' ');
            System.out.print(i + 1);
            System.out.print("  " + tests[i].name() + "... ");
            for (int j=tests[i].name().length(); j < 30; j++)
                System.out.print(' ');

            for (int j=time.length(); j < 5; j++)
                System.out.print(' ');
            System.out.print(time);
            System.out.println("ms");
        }

        new File(dbfile).delete();
    }

    public static interface Case
    {
        public void run(String dbfile, boolean autoCommit) throws Exception;
        public String name();
    }
}
