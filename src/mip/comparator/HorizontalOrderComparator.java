package mip.comparator;

import java.util.Comparator;

import model.MetroVertex;

public class HorizontalOrderComparator implements Comparator {

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 * 
	 * both arguments must be of type MetroVertex
	 * 
	 * returns -1 if the vertex arg0 lies strictly to the left of arg1 or arg0 and arg1 have the same x coordinate but arg0 has a smaller y coordinate than arg1 returns 1 if arg1 is lexicographically
	 * smaller than arg0 returns 0 if both vertices share the same position
	 */
	public int compare(Object arg0, Object arg1) {
		MetroVertex mv0, mv1;
		mv0 = (MetroVertex) arg0;
		mv1 = (MetroVertex) arg1;
		if ((mv0.getX() < mv1.getX()) || ((mv0.getX() == mv1.getX()) && (mv0.getY() < mv1.getY()))) {
			return -1;
		} else if (mv0.getX() > mv1.getX() || ((mv0.getX() == mv1.getX()) && (mv0.getY() > mv1.getY()))) {
			return 1;
		} else {
			return 0;
		}
	}

}
