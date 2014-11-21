public class Rating implements Comparable<Rating> {
    int userid;
    int itemid;
    double rating;

    public Rating(String[] value) {
        userid = Integer.parseInt(value[0]);
        itemid = Integer.parseInt(value[1]);
        rating = Double.parseDouble(value[2]);
    }

    public Rating(int userid, int itemid, double rating) {
        this.userid = userid;
        this.itemid = itemid;
        this.rating = rating;
    }

    public Rating(Rating rating) {
        this.userid = rating.userid;
        this.itemid = rating.itemid;
        this.rating = rating.rating;
    }

    @Override
    public int compareTo(Rating o) {
        int comp = (userid - o.userid);
        return comp == 0 ? itemid - o.itemid : comp;
    }
}
