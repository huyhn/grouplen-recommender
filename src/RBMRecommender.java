import com.syvys.jaRBM.Layers.Layer;
import com.syvys.jaRBM.Layers.StochasticBinaryLayer;
import com.syvys.jaRBM.RBM;
import com.syvys.jaRBM.RBMImpl;
import com.syvys.jaRBM.RBMLearn.CDStochasticRBMLearner;
import org.apache.commons.math3.random.GaussianRandomGenerator;
import org.apache.commons.math3.random.Well512a;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by huy on 22/11/14.
 */
public class RBMRecommender {
    private final Ratings ratings;
    private final RBM rbm;
    private final Layer visibleLayer;
    private final Layer hiddenLayer;
    private double[][] data = null;

    public RBMRecommender(Ratings ratings, int features) {
        this.ratings = ratings;
        visibleLayer = new StochasticBinaryLayer(ratings.itemCount);
        hiddenLayer = new StochasticBinaryLayer(features);
        rbm = new RBMImpl(visibleLayer, hiddenLayer);
        rbm.setConnectionWeights(randomize(visibleLayer.getNumUnits(), hiddenLayer.getNumUnits()));
        rbm.setLearningRate(0.1);
    }

    private static double[][] randomize(int w, int h) {
        GaussianRandomGenerator generator = new GaussianRandomGenerator(new Well512a(System.currentTimeMillis()));
        double[][] values = new double[w][h];
        for(int i = 0; i < w; ++i)
            for (int j = 0; j < h; ++j)
                values[i][j] =  0.1 * generator.nextNormalizedDouble();

        return values;
    }

    public RBM getRbm() {
        return rbm;
    }

    public void learn() {
        if (data == null) data = ratings.getRatedBinaryData(Ratings.RatingsType.Training);
        double error = CDStochasticRBMLearner.Learn(rbm, data);
        System.out.println("error: " + error);
    }

    public static void main(String[] args) {
        double data[][] = {{1,1,1,0,0,0},
                           {1,0,1,0,0,0},
                           {1,1,1,0,0,0},
                           {0,0,1,1,1,0},
                           {0,0,1,1,0,0},
                           {0,0,1,1,1,0}};

        Ratings ratings = loadRatings(data);
        RBMRecommender recommender = new RBMRecommender(ratings, 2);
        for(int i = 0; i < 1000; i++)
            recommender.learn();


        RBM rbm = recommender.getRbm();
        double[][] weights = rbm.getConnectionWeights();
        for (double[] weight : weights) {
            for (double aWeight : weight) System.out.print(aWeight + " ");
            System.out.println();
        }

        System.exit(0);

    }

    private static Ratings loadRatings(double[][] data) {
        List<Rating> training = new ArrayList<>();
        List<Rating> test = new ArrayList<>();
        for (int i = 0; i < data.length; ++i) {
            for (int j = 0; j < data[i].length; ++j) {
                if (data[i][j] > 0) {
                    training.add(new Rating(i+1, j+ 1, data[i][j]));
                } else if (data[i][j] < 0) {
                    test.add(new Rating(i+1, j+ 1, 0-data[i][j]));
                }
            }
        }
        Ratings ratings = new Ratings();
        ratings.load(training, test);
        return ratings;
    }


}
