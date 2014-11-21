import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Ratings {

    public int userCount = 0;
    public int itemCount = 0;

    public int testUserCount = 0;
    public int testItemCount = 0;

    public int trainingUserCount = 0;
    public int trainingItemCount = 0;

    public List<Rating> training = new ArrayList<>(0);
    public List<Rating> test = new ArrayList<>(0);
    private double trainingMean = 0.0;
    private double testMean = 0.0;
    private double dataMean = 0.0;

    public Ratings(Ratings ratings) {
        for (Rating rating : ratings.training) {
            training.add(new Rating(rating));
        }
        for (Rating rating : ratings.test) {
            test.add(new Rating(rating));
        }

        userCount = ratings.userCount;
        itemCount = ratings.itemCount;
        testUserCount = ratings.testUserCount;
        testItemCount = ratings.testItemCount;
        trainingUserCount = ratings.trainingUserCount;
        trainingItemCount = ratings.trainingItemCount;
    }

    public Ratings() { }

    public void load(List<Rating> training, List<Rating> test) {
        Set<Integer> userIds = new HashSet<>();
        Set<Integer> itemIds = new HashSet<>();
        Set<Integer> testUserIds = new HashSet<>();
        Set<Integer> testItemsIds = new HashSet<>();
        Set<Integer> trainingUserIds = new HashSet<>();
        Set<Integer> trainingItemIds = new HashSet<>();

        for (Rating rating : training) {
            trainingUserIds.add(rating.userid);
            trainingItemIds.add(rating.itemid);
        }
        userIds.addAll(trainingUserIds);
        itemIds.addAll(trainingItemIds);
        for (Rating rating : test) {
            testUserIds.add(rating.userid);
            testItemsIds.add(rating.itemid);
        }
        userIds.addAll(testUserIds);
        itemIds.addAll(testItemsIds);

        this.training = training;
        this.test = test;

        testUserCount = testUserIds.size();
        testItemCount = testItemsIds.size();
        trainingUserCount = trainingUserIds.size();
        trainingItemCount = trainingItemIds.size();

        userCount = userIds.size();
        itemCount = itemIds.size();

        Collections.sort(training);
        Collections.sort(test);
    }

    public void load(String filename, double testPercentage) {
        if (filename == null || filename.isEmpty()) throw new RuntimeException("Filename cannot be null or empty.");
        if (testPercentage < 10) throw new RuntimeException("Test Percentage is too low.");
        List<Rating> ratings = new ArrayList<>(100);
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while((line = reader.readLine()) != null) {
                Rating rating = new Rating(line.split("\\s+"));
                if (rating.userid > userCount) userCount = rating.userid;
                if (rating.itemid > itemCount) itemCount = rating.itemid;
                ratings.add(rating);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Cannot find file: " + filename);
            return;
        } catch (IOException e) {
            System.out.println("Cannot read from file: " + filename);
            return;
        }
        partitionTestAndTraining(testPercentage, ratings);
    }

    private void partitionTestAndTraining(double testPercentage, List<Rating> ratings) {
        Set<Integer> testUserIds = new HashSet<>();
        Set<Integer> testItemsIds = new HashSet<>();
        Set<Integer> trainingUserIds = new HashSet<>();
        Set<Integer> trainingItemIds = new HashSet<>();
        int number_of_tests = (int)(testPercentage / 100) * ratings.size();
        test = new ArrayList<>(number_of_tests);
        Random rand = new Random(System.currentTimeMillis());
        while(number_of_tests > 0)  {
            int index = rand.nextInt(ratings.size());
            Rating testRating = ratings.remove(index);
            testUserIds.add(testRating.userid);
            testItemsIds.add(testRating.itemid);
            test.add(testRating);
            number_of_tests--;
        }
        training = ratings;
        for (Rating rating : training) {
            trainingUserIds.add(rating.userid);
            trainingItemIds.add(rating.itemid);
        }

        testUserCount = testUserIds.size();
        testItemCount = testItemsIds.size();
        trainingUserCount = trainingUserIds.size();
        trainingItemCount = trainingItemIds.size();

        Collections.sort(training);
        Collections.sort(test);
    }

    public double trainingMean() {
        if (trainingMean != 0.0) return trainingMean;
        double sum = 0.0;
        for (Rating rating : training) {
            sum += rating.rating;
        }
        trainingMean = sum / training.size();
        return trainingMean;
    }

    public double testMean() {
        if (testMean != 0.0) return testMean;
        double sum = 0.0;
        for (Rating rating : test) {
            sum += rating.rating;
        }
        testMean = sum / test.size();
        return testMean;
    }

    public double mean() {
        if (dataMean != 0.0) return dataMean;
        double sum = 0.0;
        for (Rating rating : training) {
            sum += rating.rating;
        }
        for (Rating rating : test) {
            sum += rating.rating;
        }
        dataMean = sum / (test.size() + training.size());
        return dataMean;
    }
}
