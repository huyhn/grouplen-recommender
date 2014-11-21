import org.apache.commons.math3.linear.*;

import java.util.ArrayList;
import java.util.List;

public class Recommender {

    private double[] userBias;
    private double[] itemBias;

    public static void main(String args[]) {
        double[][] data = {
                {5,4,4,0,-5},
                {0,3,5,-3,4},
                {5,2,0,-2,3},
                {0,-2,3,1,2},
                {4,0,-5,4,5},
                {-5,3,0,3,5},
                {3,-2,3,2,0},
                {5,-3,4,0,5},
                {-4,2,5,4,0},
                {-5,0,5,3,4}
        };
        Recommender recommender = new Recommender();
        Ratings ratings = recommender.loadRatings(data);
        Ratings predictor = recommender.ratingMatrix(recommender.solveLeastSquare(ratings), ratings);
        double training_rmse = recommender.rmse(predictor.training, ratings.training);
        System.out.println("Training RMSE: " + training_rmse);
        double test_rmse = recommender.rmse(predictor.test, ratings.test);
        System.out.println("Test RMSE: " + test_rmse);
    }

    private double rmse(List<Rating> predictor, List<Rating> ratings) {
        double sum = 0.0;
        for (int index = 0; index < ratings.size(); ++index) {
            sum += Math.pow(predictor.get(index).rating - ratings.get(index).rating, 2);
        }
        return Math.sqrt(sum / predictor.size());
    }

    private Ratings loadRatings(double[][] data) {
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
        RealMatrix ratedMatrix = ratedMatrix(ratings);
        RealMatrix meanError = meanError(ratings);
        RealMatrix transpose = ratedMatrix.transpose();

        RealMatrix ATA = transpose.multiply(ratedMatrix);
        RealMatrix ATc = transpose.multiply(meanError);
        DecompositionSolver solver = new SingularValueDecomposition(ATA).getSolver();
        ArrayRealVector constants = new ArrayRealVector(ATc.getColumn(0));

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
        RealMatrix matrix = new Array2DRowRealMatrix(ratings.training.size(), 1);
        int count = 0;
        for (Rating rating : ratings.training) {
            meanDifference[count] = rating.rating - ratings.trainingMean();
            ++count;
        }
        matrix.setColumn(0, meanDifference);
        return matrix;
    }

    private RealMatrix ratedMatrix(Ratings ratings) {
        int count = 0;
        RealMatrix matrix = new Array2DRowRealMatrix(ratings.training.size(), ratings.userCount + ratings.itemCount);
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
