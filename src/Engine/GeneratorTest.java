package Engine;

public class GeneratorTest {
    private static final int[][] MOVES = new int[8][218];
    private static boolean testFailed;
    private static long
            numPositions,
            capturesCounter,
            enPassantCounter,
            castlesCounter,
            checkCounter,
            promotionCounter,
            doubleCheckCounter,
            checkmateCounter;

    public static void test(int depth) {
        testFailed = false;
        String[] FENS = new String[]{
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - ",
                "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1 ",
                "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
                "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
                "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
        };
        String[][][] results = new String[][][]{
                {
                        {"1", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"20", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"400", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"8902", "34", "0", "0", "0", "12", "0", "0", "0"},
                        {"197281", "1576", "0", "0", "0", "469", "0", "0", "8"},
                        {"4865609", "82719", "258", "0", "0", "27351", "6", "0", "347"},
                        {"119060324", "2812008", "5248", "0", "0", "809099", "329", "46", "10828"},
                        {"3195901860", "108329926", "319617", "883453", "0", "33103848", "18026", "1628", "435767"},
                        {"84998978956", "3523740106", "7187977", "23605205", "0", "968981593", "847039", "147215", "9852036"}
                },
                {
                        {"1", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"48", "8", "0", "2", "0", "0", "0", "0", "0"},
                        {"2039", "351", "1", "91", "0", "3", "0", "0", "0"},
                        {"97862", "17102", "45", "3162", "0", "993", "0", "0", "1"},
                        {"4085603", "757163", "1929", "128013", "15172", "25523", "42", "6", "43"},
                        {"193690690", "35043416", "73365", "4993637", "8392", "3309887", "19883", "2637", "30171"},
                        {"8031647685", "1558445089", "3577504", "184513607", "56627920", "92238050", "568417", "54948", "360003"}
                },
                {
                        {"1", "1", "0", "0", "0", "0", "0", "0", "0"},
                        {"14", "1", "0", "0", "0", "2", "0", "0", "0"},
                        {"191", "14", "0", "0", "0", "10", "0", "0", "0"},
                        {"2812", "209", "2", "0", "0", "267", "3", "0", "0"},
                        {"43238", "3348", "123", "0", "0", "1680", "106", "0", "17"},
                        {"674624", "52051", "1165", "0", "0", "52950", "1292", "3", "0"},
                        {"11030083", "940350", "33325", "0", "7552", "452473", "26067", "0", "2733"},
                        {"178633661", "14519036", "294874", "0", "140024", "12797406", "370630", "3612", "87"},
                        {"3009794393", "267586558", "8009239", "0", "6578076", "135626805", "7181487", "1630", "450410"}
                },
                {
                        {"1", "0", "0", "0", "0", "1", "0", "0", "0"},
                        {"6", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"264", "87", "0", "6", "48", "10", "0", "0", "0"},
                        {"9467", "1021", "4", "0", "120", "38", "0", "0", "22"},
                        {"422333", "131393", "0", "7795", "60032", "15492", "0", "0", "5"},
                        {"15833292", "2046173", "6512", "0", "329464", "200568", "0", "0", "50562"},
                        {"706045033", "210369132", "212", "10882006", "81102984", "26973664", "0", "0", "81076"}
                },
                {
                        {"1", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"44", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"1486", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"62379", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"2103487", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"89941194", "0", "0", "0", "0", "0", "0", "0", "0"}
                },
                {
                        {"1", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"46", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"2079", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"89890", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"3894594", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"164075551", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"6923051137", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"287188994746", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"11923589843526", "0", "0", "0", "0", "0", "0", "0", "0"},
                        {"490154852788714", "0", "0", "0", "0", "0", "0", "0", "0"}
                }
        };
        Position position = new Position();

        for (int j = 0; j < FENS.length; j++) {
            position.loadFEN(FENS[j]);
            System.out.println("NEW FEN: " + (j + 1));
            if (j == 1 || j == 4 || j == 5) {
                depth--;
            }

            for (int i = 1; i <= depth; i++) {
                if (i <= results[j].length) {
                    numPositions = 0;
                    clearCounters();
                    long startTime = System.nanoTime();
                    moveGenerationTest(position, i, 0);
                    long endTime = System.nanoTime();
                    printTest(i, results[j][i], j);
                    System.out.println("Time taken: " + ((endTime - startTime) / 1_000_000_000.0) + " seconds");
                    System.out.println("nodes taken: " + (numPositions / ((endTime - startTime) / 1_000_000_000.0)) + " per Second");
                }
            }

            if (j == 1 || j == 4 || j == 5) {
                depth++;
            }
        }
        if (testFailed) {
            System.out.println("\nTEST HAS FAILED!.❌");
        } else {
            System.out.println("\nTEST HAS PASSED!.✅");
        }
    }

    private static void clearCounters() {
        capturesCounter = 0;
        enPassantCounter = 0;
        castlesCounter = 0;
        checkCounter = 0;
        promotionCounter = 0;
        doubleCheckCounter = 0;
        checkmateCounter = 0;
    }

    private static void printTest(int depth, String[] results, int j) {
        System.out.println();
        System.out.println("depth: " + depth);
        System.out.print("number of positions: " + numPositions);
        if (String.valueOf(numPositions).equals(results[0])) {
            System.out.println(", success.✅");
        } else {
            testFailed = true;
            System.out.println(", failure.❌");
        }
        if (j < 4) {
            System.out.print("captures: " + capturesCounter);
            if (String.valueOf(capturesCounter).equals(results[1])) {
                System.out.println(", success.✅");
            } else {
                testFailed = true;
                System.out.println(", failure.❌");
            }
            System.out.print("enPassant: " + enPassantCounter);
            if (String.valueOf(enPassantCounter).equals(results[2])) {
                System.out.println(", success.✅");
            } else {
                testFailed = true;
                System.out.println(", failure.❌");
            }
            System.out.print("castles: " + castlesCounter);
            if (String.valueOf(castlesCounter).equals(results[3])) {
                System.out.println(", success.✅");
            } else {
                testFailed = true;
                System.out.println(", failure.❌");
            }
            System.out.print("promotions: " + promotionCounter);
            if (String.valueOf(promotionCounter).equals(results[4])) {
                System.out.println(", success.✅");
            } else {
                testFailed = true;
                System.out.println(", failure.❌");
            }
            System.out.print("checks: " + checkCounter);
            if (String.valueOf(checkCounter).equals(results[5])) {
                System.out.println(", success.✅");
            } else {
                testFailed = true;
                System.out.println(", failure.❌");
            }

            // System.out.print("discovery checks: " +    );
            //  System.out.println((String.valueOf(   ).equals(results[6])) ? ", success.✅" : ", failed.❌");

            if (j != 3) {
                System.out.print("double checks: " + doubleCheckCounter);
                if (String.valueOf(doubleCheckCounter).equals(results[7])) {
                    System.out.println(", success.✅");
                } else {
                    testFailed = true;
                    System.out.println(", failure.❌");
                }
            }
            System.out.print("checkmates: " + checkmateCounter);
            if (String.valueOf(checkmateCounter).equals(results[8])) {
                System.out.println(", success.✅");
            } else {
                testFailed = true;
                System.out.println(", failure.❌");
            }
            System.out.println();
        }
    }

    private static void moveGenerationTest(Position position, int depth,int ply) {
        int[] moves = position.generateMoves(MOVES[ply]);
        if (depth == 0) {
            numPositions++;
            int testMove = position.getCurrentMove();
            int captured = (testMove >> 16) & 0xF;
            int promotion = (testMove >> 20) & 0xF;
            int castling = (testMove >> 24) & 0x3;
            int enPassant = (testMove >> 30) & 0x1;
            if (captured != 0) {
                capturesCounter++;
            }
            if (position.isInCheck()) {
                checkCounter++;
            }

            if (position.isMate()) {
                checkmateCounter++;
            } else if (position.isInDoubleCheck()) {
                doubleCheckCounter++;
            }
            if (promotion != 0) {
                promotionCounter++;
            }
            if (castling != 0) {
                castlesCounter++;
            }
            if (enPassant != 0) {
                enPassantCounter++;
            }
            return;
        }
        int length = position.getLegalMovesAmount();
        for (int i = 0; i < length; i++) {
            position.move(moves[i]);
            moveGenerationTest(position, depth - 1, ply+1);
            position.undo(moves[i]);
        }
    }
}
