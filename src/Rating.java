public class Rating implements Comparable<Rating>, Cloneable {
    int userid;
    int itemid;
    double rating;

    public Rating() { }

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

    @Override
    public int compareTo(Rating o) {
        int comp = (userid - o.userid);
        return comp == 0 ? itemid - o.itemid : comp;
    }

    @Override
    public Rating clone() throws CloneNotSupportedException {
        super.clone();
        Rating cloned = new Rating();
        cloned.userid = userid;
        cloned.itemid = itemid;
        cloned.rating = rating;
        return cloned;
    }
}
