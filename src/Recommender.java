import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

/**
 * Created by huy on 6/11/14.
 */
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

    public static void main(String args[]) {
        Recommender recommender = new Recommender();
        System.out.println(recommender.mean(recommender.ratings));
        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        RealMatrix matrix = new Array2DRowRealMatrix(2,3);
        matrix.setRow(0, new double[]{1,1,0});
        matrix.setRow(1, new double[]{1,0,1});
        RealMatrix transpose = matrix.transpose();
        RealMatrix y = new Array2DRowRealMatrix(1,2);
        y.setRow(0, new double[]{2,3});
        regression.newSampleData(transpose.multiply(y.transpose()).transpose().getData()[0], transpose.multiply(matrix).getData());
        RealMatrix result = regression.calculateHat();
        System.out.println(result.toString());
    }

    private double mean(double[][] ratings) {
        int count = 0;
        long sum = 0;
        for (double[] rating : ratings) {
            for (int j = 0; j < rating.length; ++j) {
                if (rating[j] > 0) {
                    sum += rating[j];
                    count++;
                }
            }
        }
        return ((double) sum) / count;
    }
}
