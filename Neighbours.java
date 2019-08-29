import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.Random;

import static java.lang.Math.random;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;
import static java.lang.System.*;

/*
 *  Program to simulate segregation.
 *  See : http://nifty.stanford.edu/2014/mccown-schelling-model-segregation/
 *
 * NOTE:
 * - JavaFX first calls method init() and then method start() far below.
 * - To test uncomment call to test() first in init() method!
 *
 */
// Extends Application because of JavaFX (just accept for now)
public class Neighbours extends Application {
    final Random rand = new Random();

    // Enumeration type for the Actors
    enum Actor {
        BLUE, RED, NONE   // NONE used for empty locations
    }

    // Enumeration type for the state of an Actor
    enum State {
        UNSATISFIED,
        SATISFIED,
        NA     // Not applicable (NA), used for NONEs
    }

    // Below is the *only* accepted instance variable (i.e. variables outside any method)
    // This variable may *only* be used in methods init() and updateWorld()
    Actor[][] world;              // The world is a square matrix of Actors

    // This is the method called by the timer to update the world
    // (i.e move unsatisfied) approx each 1/60 sec.
    void updateWorld() {
        // % of surrounding neighbours that are like me
        final double threshold = 0.7;                   // procent grannar som krävs för att bli nöjd
        // TODO
        world = nextState(world, threshold);            // här körs vårt program för att uppdatera världen

    }

    // This method initializes the world variable with a random distribution of Actors
    // Method automatically called by JavaFX runtime (before graphics appear)
    // Don't care about "@Override" and "public" (just accept for now)
    @Override
    public void init() {
        //test();    // <---------------- Uncomment to TEST!

        // %-distribution of RED, BLUE and NONE
        double[] dist = {0.25, 0.25, 0.50};
        // Number of locations (places) in world (square)
        int nLocations = 90000;


        // TODO
        Actor[] startBoard = getCells(nLocations, dist);                            // skapar en array med ett antal celler samt distruberar det från enumnen
        shuffle(startBoard);                                                       // shufflar vår array ovan
        world = toMatrix(startBoard);                                              //gör en matrix av arrayen vi skapat två rader ovan

        // Should be last
        fixScreenSize(nLocations);                                                // java FX bestämmer planens storlek osv
    }

    // ------- Methods ------------------

    // TODO write the methods here, implement/test bottom up

    // Bygger upp vår bana
    Actor[] getCells(int nCells, double[] dist) {                                // perc RED, BLUE, NONE.
        Actor[] cells = new Actor[nCells];                                       // skapar en ny array med n antal celler
        int nRed = (int) round(dist[0] * nCells);                                 // hämtar antalet röda
        int nBlue = (int) round(dist[1] * nCells);                                // -- blå
        int nNone = (int) round(dist[2] * nCells);                                // -- none
        int i = 0;                                                               // sätter i som 0 för att använda i whilelooparna
        while (nRed > i) {
            cells[i] = Actor.RED;                                                 // kommer sätta in antalet röda i arrayn vilket i uppgiften är 25 st
            i++;
        }
        while (nBlue + nRed > i) {
            cells[i] = Actor.BLUE;                                              // samma fast blå
            i++;
        }
        while (nNone + nBlue + nRed > i) {
            cells[i] = Actor.NONE;                                            // samma fast none
            i++;
        }
        return cells;                                                           // ger oss en array som vi ska använda ovan
    }

    //shufflar vår bana
    void shuffle(Actor[] arr) {
        for (int i = arr.length - 1; i > 0; i--) {                                // börjar från slutet av arrayen och byter plats med någon längre fram i arrayn
            int j = rndNumb(i);
            Actor tmp = arr[j];                                                  // använder en temp där vi sätter in värdet på randomplatsen
            arr[j] = arr[i];                                                      // sätter i värdet på randomplatsen
            arr[i] = tmp;                                                         // sätter temp värdet på i pltsen
        }                                                                         // AKA nu har de bytt plats!!!
    }

    // gör vår array till en matrix
    Actor[][] toMatrix(Actor[] arr) {                                            // Gör vår arrray till en matrix
        int size = (int) round(sqrt(arr.length));                                 // då matrixen är en kvadrat kan man anta att sidorna är lika långa
        Actor[][] matrix = new Actor[size][size];                                 // skapar alltså en ny matrix med storleken på arrayen
        for (int i = 0; i < arr.length; i++) {
            matrix[i / size][i % size] = arr[i];                                     // sätter in värden från arrayen till vår nya matrix
        }
        return matrix;
    }

    // ska göra vår nästa bana när mapen uppdateras
    Actor[][] nextState(Actor[][] world, double threshold) {
        State[][] worldState = getSatisfaction(world, threshold);                           // kollar om pricken är nöjd
        Actor[] worldArr = matrix2Array(world);                                              // gör matrixen till en array
        State[] stateArr = state2Array(worldState);                                          // -"-
        getNewPositions(worldArr, stateArr);                                                   // ger oss nya positioner (shufflar alla missnöjda prickar)
        Actor[][] newWorld = toMatrix(worldArr);                                              // gör tillbaka arrayen till en matrix
        return newWorld;
    }

    // säger vilka i matrixen som är nöjda i sin position
    State[][] getSatisfaction(Actor[][] world, double threshold) {
        int size = world.length;                                                                 // pga kvadrat så är world och world[] lika långa
        State[][] tempWorld = new State[size][size];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {                                               // kollar för varje cell i matrixen
                double neighbours = getNeighbours(world, row, col);
                if (world[row][col] == Actor.NONE) {
                    tempWorld[row][col] = State.NA;
                } else if (neighbours >= threshold) {
                    tempWorld[row][col] = State.SATISFIED;
                } else {
                    tempWorld[row][col] = State.UNSATISFIED;
                }
            }
        }
        return tempWorld;
    }

    // kollar om grannarna är samma eller olika returnerar kvoten!
    double getNeighbours(Actor[][] world, int row, int col) {
        int good = 0;
        int bad = 0;
        Actor antiColor = null;
        Actor color = world[row][col];
        if (color == Actor.RED) {
            antiColor = Actor.BLUE;
        } else if (color == Actor.BLUE) {
            antiColor = Actor.RED;
        }
        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                if (isValidLocation(world.length, r, c)                                 // kollar om grannen finns i världen
                        && !(r == row && c == col)) {                                    // får inte vara sig själv
                    if (world[r][c] == color) {                                          // hitta rätt färg
                        good++;
                    } else if (world[r][c] == antiColor) {
                        bad++;
                    }
                }
            }
        }
        double count = (double) good / (double) (good + bad);                           // räkna ut procent
        return count;
    }

    //säger vad som är i banan
    boolean isValidLocation(int size, int row, int col) {
        return 0 <= row && row < size && 0 <= col && col < size;                          // kollar att platsen inte är mindre än 0 eller större än storleken
    }

    Actor[] matrix2Array(Actor[][] m) {
        Actor[] arr = new Actor[m.length * m.length];                                   //m=längden på alla rader
        int nCols = m.length;
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < m.length; c++) {
                arr[r * nCols + c] = m[r][c];
            }
        }
        return arr;
    }

    State[] state2Array(State[][] m) {
        State[] arr = new State[m.length * m.length];
        int nCols = m.length;
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < m.length; c++) {
                arr[r * nCols + c] = m[r][c];
            }
        }
        return arr;
    }

    void getNewPositions(Actor[] act, State[] state) {
        for (int i = act.length - 1; i >= 0; i--) {
            if (state[i] == State.UNSATISFIED) {                                      // kollar om platsen är missnöjd
                int place = rndNumb(act.length);
                if (state[place] == State.NA) {                                           // är den random platsen en tom plats?
                    Actor temp = act[i];
                    act[i] = act[place];
                    act[place] = temp;
                } else {
                    i++;                                                             // om platsen inte var tom så kör vi det igen
                }
            }
        }
    }

    // genererar ett random nummer
    int rndNumb(int r) {
        int result = rand.nextInt(r);
        return result;
    }

    // ------- Testing -------------------------------------

    // Here you run your tests i.e. call your logic methods
    // to see that they really work
    void test() {
        // A small hard coded world for testing
        Actor[][] testWorld = new Actor[][]{
                {Actor.RED, Actor.RED, Actor.NONE},
                {Actor.NONE, Actor.BLUE, Actor.NONE},
                {Actor.RED, Actor.NONE, Actor.BLUE}
        };
        double th = 0.5;   // Simple threshold used for testing
        int size = testWorld.length;

        // TODO test methods

        exit(0);
    }

    // Helper method for testing (NOTE: reference equality)
    <T> int count(T[] arr, T toFind) {
        int count = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == toFind) {
                count++;
            }
        }
        return count;
    }


    // *****   NOTHING to do below this row, it's JavaFX stuff  ******

    double width = 400;   // Size for window
    double height = 400;
    long previousTime = nanoTime();
    final long interval = 450000000;
    double dotSize;
    final double margin = 50;

    void fixScreenSize(int nLocations) {
        // Adjust screen window depending on nLocations
        dotSize = (width - 2 * margin) / sqrt(nLocations);
        if (dotSize < 1) {
            dotSize = 2;
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Build a scene graph
        Group root = new Group();
        Canvas canvas = new Canvas(width, height);
        root.getChildren().addAll(canvas);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Create a timer
        AnimationTimer timer = new AnimationTimer() {
            // This method called by FX, parameter is the current time
            public void handle(long currentNanoTime) {
                long elapsedNanos = currentNanoTime - previousTime;
                if (elapsedNanos > interval) {
                    updateWorld();
                    renderWorld(gc, world);
                    previousTime = currentNanoTime;
                }
            }
        };

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Simulation");
        primaryStage.show();

        timer.start();  // Start simulation
    }


    // Render the state of the world to the screen
    public void renderWorld(GraphicsContext g, Actor[][] world) {
        g.clearRect(0, 0, width, height);
        int size = world.length;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                double x = dotSize * col + margin;
                double y = dotSize * row + margin;

                if (world[row][col] == Actor.RED) {
                    g.setFill(Color.RED);
                } else if (world[row][col] == Actor.BLUE) {
                    g.setFill(Color.BLUE);
                } else {
                    g.setFill(Color.WHITE);
                }
                g.fillOval(x, y, dotSize, dotSize);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
