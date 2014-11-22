import com.syvys.jaRBM.Math.Matrix;
import org.apache.commons.math3.linear.*;

import java.util.ArrayList;
import java.util.List;

public class Recommender {

    private double[] userBias;
    private double[] itemBias;

    public static void main(String args[]) {
        Recommender recommender = new Recommender();
        Ratings ratings = new Ratings().load("test.data", 10);
        Ratings predictor = recommender.ratingMatrix(recommender.solveLeastSquare(ratings), ratings);
        System.out.println("Training MSE: " + Math.sqrt(Matrix.getMeanSquaredError(predictor.getData(Ratings.RatingsType.Training), ratings.getData(Ratings.RatingsType.Training))));
        System.out.println("Test MSE: " + Math.sqrt(Matrix.getMeanSquaredError(predictor.getData(Ratings.RatingsType.Test), ratings.getData(Ratings.RatingsType.Test))));
    }

    private double rmse(List<Rating> predictor, List<Rating> ratings) {
        double sum = 0.0;
        for (int index = 0; index < ratings.size(); ++index) {
            sum += Math.pow(predictor.get(index).rating - ratings.get(index).rating, 2);
        }
        return Math.sqrt(sum / predictor.size());
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

    private RealVector solveLeastSquare(Ratings ratings) {
        System.out.println("Calulating rated sparse matrix A.");
        OpenMapRealMatrix ratedMatrix = ratedMatrix(ratings);
        System.out.println("Calulating mean error matrix c.");
        RealMatrix meanError = meanError(ratings);
        System.out.println("Transpose rated sparse matrix AT mean error matrix.");
        OpenMapRealMatrix transpose = (OpenMapRealMatrix)ratedMatrix.transpose();

        System.out.println("Calulating ATA");
        OpenMapRealMatrix ATA = transpose.multiply(ratedMatrix);
        System.out.println("Calulating ATc");
        RealMatrix ATc = transpose.multiply(meanError);
        System.out.println("Creating SVD Solver");
        DecompositionSolver solver = new SingularValueDecomposition(ATA).getSolver();
        ArrayRealVector constants = new ArrayRealVector(ATc.getColumn(0));

        System.out.println("Solving least square problem ATAx = ATc");
        return solver.solve(constants);
    }


    Ratings ratingMatrix(RealVector solution, Ratings ratings) {
        Ratings predictor = new Ratings(ratings);
        userBias = new double[ratings.userCount];
        itemBias = new double[ratings.itemCount];

        for (int index = 0; index < userBias.length; ++index) {
            userBias[index] = solution.getEntry(index);
        }

        for (int index = 0; index < itemBias.length; ++index) {
            itemBias[index] = solution.getEntry(userBias.length + index);
        }
        for(Rating rating : predictor.training) {
            rating.rating = clip(ratings.trainingMean() +  userBias[rating.userid-1] + itemBias[rating.itemid-1]);
        }
        for(Rating rating : predictor.test) {
            rating.rating = clip(ratings.testMean() +  userBias[rating.userid-1] + itemBias[rating.itemid-1]);
        }
        return predictor;
    }

    private double clip(double v) {
        if (v < 1) return 1.0;
        if (v > 5) return 5.0;
        return v;
    }

    private RealMatrix meanError(Ratings ratings) {
        double[] meanDifference = new double[ratings.training.size()];
        RealMatrix matrix = new OpenMapRealMatrix(ratings.training.size(), 1);
        int count = 0;
        for (Rating rating : ratings.training) {
            meanDifference[count] = rating.rating - ratings.trainingMean();
            ++count;
        }
        matrix.setColumn(0, meanDifference);
        return matrix;
    }

    private OpenMapRealMatrix ratedMatrix(Ratings ratings) {
        int count = 0;
        OpenMapRealMatrix matrix = new OpenMapRealMatrix(ratings.training.size(), ratings.userCount + ratings.itemCount);
        for (Rating rating : ratings.training) {
            double[] rated = new double[ratings.userCount + ratings.itemCount];
            rated[rating.userid - 1] = 1;
            rated[ratings.userCount + rating.itemid - 1] = 1;
            matrix.setRow(count, rated);
            ++count;
        }
        return matrix;
    }
}
