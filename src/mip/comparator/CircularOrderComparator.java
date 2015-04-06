package mip.comparator;

import java.util.Comparator;

import model.MetroVertex;

public class CircularOrderComparator implements Comparator {
	MetroVertex mv;

	public CircularOrderComparator(MetroVertex mv) {
		this.mv = mv;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object) returns -1 if the angle between this.mv and v1 is smaller than between this.mv and v2 0 if the angles are equal 1 if the
	 * angle between this.mv and v1 is greater than between this.mv and v2
	 */
	public int compare(Object v1, Object v2) {
		MetroVertex m1 = (MetroVertex) v1;
		MetroVertex m2 = (MetroVertex) v2;
		double x_m1 = m1.getX() - mv.getX();
		double y_m1 = m1.getY() - mv.getY();
		double x_m2 = m2.getX() - mv.getX();
		double y_m2 = m2.getY() - mv.getY();
		double phi1 = (y_m1 >= 0) ? Math.acos(x_m1 / (Math.sqrt(x_m1 * x_m1 + y_m1 * y_m1))) : 2 * Math.PI - Math.acos(x_m1 / (Math.sqrt(x_m1 * x_m1 + y_m1 * y_m1)));
		double phi2 = (y_m2 >= 0) ? Math.acos(x_m2 / (Math.sqrt(x_m2 * x_m2 + y_m2 * y_m2))) : 2 * Math.PI - Math.acos(x_m2 / (Math.sqrt(x_m2 * x_m2 + y_m2 * y_m2)));
		if (phi1 < phi2) {
			return -1;
		} else if (phi1 == phi2) {
			return 0;
		} else {
			return 1;
		}
	}

}