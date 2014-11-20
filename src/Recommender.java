import org.apache.commons.math3.linear.*;

public class Recommender {

    private double[][] ratings = {
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

    private int trainingCount = 0;
    private double mean = 0;
    private double[] userBias;
    private double[] itemBias;
    private double[][] ratingMatrix;

    public static void main(String args[]) {
        Recommender recommender = new Recommender();
        recommender.mean = recommender.mean(recommender.ratings);
        recommender.trainingCount = recommender.trainingCount(recommender.ratings);
        recommender.ratingMatrix = recommender.ratingMatrix(recommender.solveLeastSquare(recommender.ratings));
    }

    private RealVector solveLeastSquare(double[][] ratings) {
        RealMatrix ratedMatrix = ratedMatrix(ratings);
        RealMatrix meanError = meanError(ratings);
        RealMatrix transpose = ratedMatrix.transpose();

        RealMatrix ATA = transpose.multiply(ratedMatrix);
        RealMatrix ATc = transpose.multiply(meanError);
        DecompositionSolver solver = new SingularValueDecomposition(ATA).getSolver();
        ArrayRealVector constants = new ArrayRealVector(ATc.getColumn(0));

        return solver.solve(constants);
    }


    double[][] ratingMatrix(RealVector solution) {
        userBias = new double[ratings.length];
        itemBias = new double[ratings[0].length];

        for (int index = 0; index < userBias.length; ++index) {
            userBias[index] = solution.getEntry(index);
        }

        for (int index = 0; index < itemBias.length; ++index) {
            itemBias[index] = solution.getEntry(userBias.length + index);
        }
        double[][] ratingMatrix = new double[userBias.length][itemBias.length];
        for (int i = 0; i < userBias.length; ++i) {
            for (int j = 0; j < itemBias.length; ++j) {
                if (ratings[i][j] != 0) {
                    ratingMatrix[i][j] = Math.signum(ratings[i][j]) * clip(mean + userBias[i] + itemBias[j]);
                }
            }
        }
        return ratingMatrix;
    }

    private double clip(double v) {
        if (v < 1) return 1.0;
        if (v > 5) return 5.0;
        return v;
    }

    private int trainingCount(double[][] ratings) {
        int count = 0;
        for (double[] rating : ratings) {
            for (double aRating : rating) {
                if (aRating > 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private RealMatrix meanError(double[][] ratings) {
        double[] meanDifference = new double[trainingCount];
        RealMatrix matrix = new Array2DRowRealMatrix(trainingCount, 1);
        int count = 0;
        for (double[] rating : ratings) {
            for (double aRating : rating) {
                if (aRating > 0) {
                    meanDifference[count] = aRating - mean;
                    ++count;
                }
            }
        }
        matrix.setColumn(0, meanDifference);
        return matrix;
    }

    private RealMatrix ratedMatrix(double[][] ratings) {
        int count = 0;
        RealMatrix matrix = new Array2DRowRealMatrix(trainingCount, ratings.length + ratings[0].length);
        for (int i = 0; i < ratings.length; ++i) {
            for (int j = 0; j < ratings[i].length; ++j) {
                if (ratings[i][j] > 0) {
                    double[] rated = new double[ratings.length + ratings[0].length];
                    rated[i] = 1;
                    rated[ratings.length + j] = 1;
                    matrix.setRow(count, rated);
                    ++count;
                }
            }
        }
        return matrix;
    }

    private double mean(double[][] ratings) {
        int count = 0;
        long sum = 0;
        for (double[] rating : ratings) {
            for (double aRating : rating) {
                if (aRating > 0) {
                    sum += aRating;
                    count++;
                }
            }
        }
        return ((double) sum) / count;
    }
}
