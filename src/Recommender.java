import com.syvys.jaRBM.Math.Matrix;
import org.apache.commons.math3.linear.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Recommender {

    private double[] userBias;
    private double[] itemBias;

    public static void main(String args[]) {
        Recommender recommender = new Recommender();
        Ratings ratings = new Ratings().load(args[0], Integer.parseInt(args[1]));
        Ratings basePredictor = recommender.ratingMatrix(recommender.solveLeastSquare(ratings), ratings);
        System.out.println("Base predictor Training MSE: " + mse(basePredictor, ratings, Ratings.RatingsType.Training));
        System.out.println("Base predictor Test MSE: " + mse(basePredictor, ratings, Ratings.RatingsType.Test));

        Ratings neighbourhoodModel = recommender.neighbourhood(ratings, basePredictor);
        double[][] similarityMatrix = recommender.simlarity(neighbourhoodModel);
        Ratings neighbourhoodPredictor = recommender.neighbourhoodPredictor(basePredictor, neighbourhoodModel, similarityMatrix, 2);

        System.out.println("Neighbourhood predictor Training MSE: " + mse(neighbourhoodPredictor, ratings, Ratings.RatingsType.Training));
        System.out.println("Neighbourhood predictor Test MSE: " + mse(neighbourhoodPredictor, ratings, Ratings.RatingsType.Test));
    }

    private static double mse(Ratings predictor, Ratings raw, Ratings.RatingsType type) {
        return Matrix.getMeanSquaredError(predictor.getData(type), raw.getData(type));
    }

    private Ratings neighbourhoodPredictor(Ratings basePredictor, Ratings neighbourhoodModel, double[][] similarityMatrix, int knn) {
        Ratings predictor = new Ratings(basePredictor);
        double[][] model = neighbourhoodModel.getData(Ratings.RatingsType.Training);
        for(Rating rating : predictor.training) {
            rating.rating += nearestCosineCoefficients(rating.userid, rating.itemid, similarityMatrix, model, knn);
        }
        for(Rating rating : predictor.test) {
            rating.rating += nearestCosineCoefficients(rating.userid, rating.itemid, similarityMatrix, model, knn);
        }
        return predictor;
    }

    private double nearestCosineCoefficients(int userid, int itemid, double[][] similarityMatrix, double[][] model, int knn) {
        List<Tuple<Integer, Double>> similarities = new ArrayList<>(similarityMatrix[itemid - 1].length);
        for (int index = 0; index < similarityMatrix[itemid - 1].length; ++index) {
            similarities.add(new Tuple<>(index + 1, similarityMatrix[itemid - 1][index]));
        }
        Collections.sort(similarities, new Comparator<Tuple<Integer, Double>>() {
            @Override
            public int compare(Tuple<Integer, Double> o1, Tuple<Integer, Double> o2) {
                return (int)(Math.abs(o1.r) - Math.abs(o2.r));
            }
        });
        List<Tuple<Integer, Double>> nearest = similarities.subList(0, knn);
        double sumProducts = 0.0;
        double sumAbs = 0.0;
        for (Tuple<Integer, Double> tuple : nearest) {
            sumProducts = model[userid - 1][tuple.l - 1] * tuple.r;
            sumAbs += Math.abs(tuple.r);
        }
        return sumProducts / sumAbs;
    }

    private double[][] simlarity(Ratings neighbourhoodModel) {
        double[][] similarityMatrix = new double[neighbourhoodModel.itemCount][neighbourhoodModel.itemCount];
        for (int i = 1; i < neighbourhoodModel.itemCount; ++i) {
            for (int j = i + 1; j <= neighbourhoodModel.itemCount; ++j) {
                similarityMatrix[i-1][j-1] = similarityMatrix[j-1][i-1] = calculateSimilarity(neighbourhoodModel, i, j);
            }
        }
        return similarityMatrix;
    }

    private double calculateSimilarity(Ratings neighbourhoodModel, int i, int j) {
        List<Double> ri = new ArrayList<>();
        List<Double> rj = new ArrayList<>();
        int currentUser = 0;
        Double currentRi = null;
        Double currentRj = null;
        for (Rating rating : neighbourhoodModel.training) {
            if (rating.userid != currentUser) {
                if (currentRi != null && currentRj != null) {
                    ri.add(currentRi);
                    rj.add(currentRj);
                }
                currentRi = null;
                currentRj = null;
            }
            if (rating.itemid == i) currentRi = rating.rating;
            if (rating.itemid == j) currentRj = rating.rating;
            currentUser = rating.userid;
        }
        double sumProducts = 0.0;
        double sumri2 = 0.0;
        double sumrj2 = 0.0;
        for (int index = 0; index < ri.size() && index < rj.size(); ++index) {
            sumProducts += ri.get(index) * rj.get(index);
            sumri2 +=  Math.pow(ri.get(index), 2);
            sumrj2 +=  Math.pow(rj.get(index), 2);
        }
        return sumProducts / Math.sqrt(sumri2 * sumrj2);
    }

    private Ratings neighbourhood(Ratings ratings, Ratings basePredictor) {
        List<Rating> model = new ArrayList<>(ratings.training.size());
        for (int index = 0; index < ratings.training.size(); ++index) {
            Rating rating = ratings.training.get(index);
            Rating predictor = basePredictor.training.get(index);
            model.add(new Rating(rating.userid, rating.itemid, rating.rating - predictor.rating));
        }
        Ratings neighbourhood = new Ratings().load(model, new ArrayList<Rating>());
        neighbourhood.userCount = ratings.userCount;
        neighbourhood.itemCount = ratings.itemCount;
        return neighbourhood;
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

    private class Tuple<L, R> {
        public L l;
        public R r;
        public Tuple(L l, R r) {
            this.l = l;
            this.r = r;
        }
    }
}
